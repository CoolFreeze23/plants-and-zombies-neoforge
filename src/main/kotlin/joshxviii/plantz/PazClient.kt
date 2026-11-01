package joshxviii.plantz

import joshxviii.plantz.PazItems.DYE_BLASTER
import joshxviii.plantz.PazItems.FOOTBALL_HELMET
import joshxviii.plantz.PazItems.GARDENING_GLOVE
import joshxviii.plantz.PazItems.PLANT_POT_HELMET
import joshxviii.plantz.client.ModernAssets
import joshxviii.plantz.compat.client.renderer.layers.EquipmentLayerRenderer
import joshxviii.plantz.item.DyeBlasterItem
import joshxviii.plantz.item.FootballHelmetItem
import joshxviii.plantz.item.GardeningGloveItem
import joshxviii.plantz.item.PlantPotHelmetItem
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import joshxviii.plantz.client.GraveyardAmbience
import joshxviii.plantz.item.SeedPacketCooldowns
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent
import net.neoforged.neoforge.client.event.ViewportEvent
import joshxviii.plantz.item.SeedPacketItem
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.neoforged.neoforge.client.IItemDecorator
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.ModelEvent
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent
import net.neoforged.neoforge.client.event.RegisterShadersEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.AddPackFindersEvent
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent
import net.neoforged.neoforge.event.AddReloadListenerEvent

object PazClient {
	fun init(modBus: IEventBus) {
		PazConfig.load()
		modBus.addListener { event: EntityRenderersEvent.RegisterLayerDefinitions -> PazModels.registerLayers(event) }
		modBus.addListener { event: EntityRenderersEvent.RegisterRenderers -> PazModels.registerRenderers(event) }
		modBus.addListener { event: RegisterParticleProvidersEvent -> PazParticles.registerProviders(event) }
		modBus.addListener { event: RegisterMenuScreensEvent -> PazScreens.registerScreens(event) }
		modBus.addListener { event: RegisterGuiLayersEvent -> PazScreens.registerGuiLayers(event) }
		// 26.1 baby armor textures and sounds from a local 26.1+ game install, when there is one
		modBus.addListener { event: AddPackFindersEvent -> ModernAssets.addPackFinders(event) }
		modBus.addListener { event: RegisterClientReloadListenersEvent ->
			event.registerReloadListener(ResourceManagerReloadListener { EquipmentLayerRenderer.clearCaches() })
			// Fabric ResourceLoader CLIENT_RESOURCES
			event.registerReloadListener(ResourceManagerReloadListener { if (PazNetwork.ZombieRaidClientCache.isActive()) { RaidMusicManager.restart() } })
		}
		modBus.addListener { event: RegisterShadersEvent -> PazRenderPipelines.registerShaders(event) }
		modBus.addListener { event: ModelEvent.RegisterAdditional -> PazModels.registerAdditionalModels(event) }
		modBus.addListener { event: ModelEvent.ModifyBakingResult -> PazModels.modifyBakingResult(event) }
		modBus.addListener { event: RegisterItemDecorationsEvent -> registerSeedPacketCooldownOverlay(event) }
		modBus.addListener { event: RegisterColorHandlersEvent.Item -> joshxviii.plantz.data.PazItemModelProperties.registerItemColors(event) }
		modBus.addListener { event: FMLClientSetupEvent -> event.enqueueWork { PazClientHooks.clientSetup() } }
		PazClientHooks.registerModBus(modBus)

		val bus = NeoForge.EVENT_BUS
		bus.addListener { _: ClientTickEvent.Pre -> PazClientNetwork.onClientTick() }
		bus.addListener { _: ClientTickEvent.Post -> GraveyardAmbience.tick() }
		bus.addListener { event: ViewportEvent.RenderFog -> GraveyardAmbience.onRenderFog(event) }

		// Fabric ResourceLoader SERVER_DATA
		bus.addListener { event: AddReloadListenerEvent ->
			event.addListener(ResourceManagerReloadListener { PazNetwork.ZombieRaidClientCache.clear() })
		}

		// custom tooltips
		bus.addListener { event: ItemTooltipEvent ->
			val stack = event.itemStack
			val lines = event.toolTip
			// Fabric rendered these component tooltips through its ItemComponentTooltipProviderRegistry
			PazComponents.addComponentTooltips(stack, event.context, lines, event.flags)
			val minecraft = Minecraft.getInstance()
			val sneakKey = keybindFormat(minecraft.options.keyShift)
			val sprintKey = keybindFormat(minecraft.options.keySprint)

			if (stack.`is`(GARDENING_GLOVE)) GardeningGloveItem.addToTooltip(lines, stack, sneakKey)
			if (stack.`is`(PLANT_POT_HELMET)) PlantPotHelmetItem.addToTooltip(lines, sneakKey)
			if (stack.`is`(DYE_BLASTER)) DyeBlasterItem.addToTooltip(lines)
			if (stack.`is`(FOOTBALL_HELMET)) FootballHelmetItem.addToTooltip(lines, sprintKey)
		}
		PazClientHooks.registerGameBus(bus)
	}

	/** The hotbar cooldown overlay vanilla drew for 26.1 cooldown groups, now driven by SeedPacketCooldowns. */
	private fun registerSeedPacketCooldownOverlay(event: RegisterItemDecorationsEvent) {
		val decorator = IItemDecorator { graphics, _, stack, x, y ->
			val player = Minecraft.getInstance().player ?: return@IItemDecorator false
			val percent = SeedPacketCooldowns.getCooldownPercent(player, stack, Minecraft.getInstance().timer.getGameTimeDeltaPartialTick(true))
			if (percent > 0f) {
				val top = y + Mth.floor(16.0f * (1.0f - percent))
				val bottom = top + Mth.ceil(16.0f * percent)
				graphics.fill(RenderType.guiOverlay(), x, top, x + 16, bottom, Int.MAX_VALUE)
			}
			false
		}
		BuiltInRegistries.ITEM.filterIsInstance<SeedPacketItem>().forEach { event.register(it, decorator) }
	}

	fun keybindFormat(keybind: KeyMapping): Component {
		return Component.translatable("chat.square_brackets", keybind.translatedKeyMessage).withColor(0x51ff53)
	}
}
