package joshxviii.plantz

import joshxviii.plantz.block.entity.MailboxBlockEntity
import joshxviii.plantz.block.entity.MailboxManager
import joshxviii.plantz.block.entity.getMailboxMailQueue
import joshxviii.plantz.networking.ServerConfigResponsePayload
import joshxviii.plantz.raid.getZombieRaids
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.tick.LevelTickEvent
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.registries.DataPackRegistryEvent
import net.neoforged.neoforge.registries.RegisterEvent
import net.neoforged.neoforge.event.LootTableLoadEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(PazMain.MODID)
object PazMain {
	const val MODID = "plantz"
	@JvmField
	val LOGGER: Logger = LogManager.getLogger("Plants & Zombies")

	private var registered = false

	init {
		PazConfig.load()

		// mod bus
		MOD_BUS.addListener(::onRegister)
		MOD_BUS.addListener(PazNetwork::register)
		MOD_BUS.addListener { event: EntityAttributeCreationEvent -> PazEntities.registerAttributes(event) }
		MOD_BUS.addListener { event: RegisterSpawnPlacementsEvent -> PazSpawnPlacements.register(event) }
		MOD_BUS.addListener { event: DataPackRegistryEvent.NewRegistry -> PazRegistries.registerDataPackRegistries(event) }
		MOD_BUS.addListener { event: FMLCommonSetupEvent -> event.enqueueWork { PazWorldGen.onTerraBlenderInitialized() } }
		PazHooks.registerModBus(MOD_BUS)

		// game bus
		val bus = NeoForge.EVENT_BUS
		// clear client raid cache + load config
		bus.addListener { _: ServerStartingEvent ->
			PazNetwork.ZombieRaidClientCache.clear()
			PazConfig.load()
		}
		// config sync
		bus.addListener { event: PlayerEvent.PlayerLoggedInEvent ->
			val player = event.entity as? ServerPlayer ?: return@addListener
			val json = PazConfig.let { it.GSON.toJson(it.server) }
			PacketDistributor.sendToPlayer(player, ServerConfigResponsePayload(json))
			LOGGER.info("Sent server config to ${player.name.string}")
		}
		// raid ticking
		bus.addListener { event: LevelTickEvent.Post ->
			(event.level as? ServerLevel)?.let { it.getZombieRaids().tick(it) }
		}
		// goal injection for vanilla mobs
		bus.addListener { event: EntityJoinLevelEvent ->
			if (!event.level.isClientSide) PazEntities.onEntityLoad(event.entity, event.level)
		}
		bus.addListener { event: LootTableLoadEvent -> PazLootTables.onLootTableLoad(event) }
		PazHooks.registerGameBus(bus)

		if (FMLEnvironment.dist.isClient) PazClient.init(MOD_BUS)
	}

	/** Mailbox bookkeeping when a mailbox block entity loads on the server (Fabric's BLOCK_ENTITY_LOAD). */
	@JvmStatic
	fun onBlockEntityLoad(blockEntity: BlockEntity, level: ServerLevel) {
		(blockEntity as? MailboxBlockEntity)?.let {
			MailboxManager.registerMailbox(level, it)
			level.getMailboxMailQueue().deliverTo(it)
		}
	}

	/**
	 * Upstream registered everything eagerly from its Fabric initializer. NeoForge unfreezes every registry for the
	 * whole RegisterEvent phase, so running the same initializers during one RegisterEvent keeps the eager
	 * `Registry.register(...)` style intact.
	 */
	private fun onRegister(event: RegisterEvent) {
		// Wait for the entity type event: by then NeoForge's own attributes (swim_speed, ...) are bound, which
		// createMobAttributes() reads while the entity types register their default attributes.
		if (registered || event.registryKey != Registries.ENTITY_TYPE) return
		registered = true
		PazServerParticles.initialize()
		PazBlocks.initialize()
		PazItems.initialize()
		PazLootTables.initialize()
		PazEffects.initialize()
		PazCreativeTab.initialize()
		PazEntities.initialize()
		PazDamageTypes.initialize()
		PazCriteria.initialize()
		PazDataSerializers.initialize()
		PazAttributes.initialize()
		PazSounds.initialize()
		PazMenus.initialize()
		PazJukeboxSongs.initialize()
		PazBiomeModifiers.initialize()
	}
}
