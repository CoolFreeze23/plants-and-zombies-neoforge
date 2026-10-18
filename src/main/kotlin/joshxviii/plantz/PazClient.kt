package joshxviii.plantz

import joshxviii.plantz.PazItems.DYE_BLASTER
import joshxviii.plantz.PazItems.FOOTBALL_HELMET
import joshxviii.plantz.PazItems.GARDENING_GLOVE
import joshxviii.plantz.PazItems.PLANT_POT_HELMET
import joshxviii.plantz.item.DyeBlasterItem
import joshxviii.plantz.item.FootballHelmetItem
import joshxviii.plantz.item.GardeningGloveItem
import joshxviii.plantz.item.PlantPotHelmetItem
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
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
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent

object PazClient {
	fun init(modBus: IEventBus) {
		PazConfig.load()
		modBus.addListener { event: EntityRenderersEvent.RegisterLayerDefinitions -> PazModels.registerLayers(event) }
		modBus.addListener { event: EntityRenderersEvent.RegisterRenderers -> PazModels.registerRenderers(event) }
		modBus.addListener { event: RegisterParticleProvidersEvent -> PazParticles.registerProviders(event) }
		modBus.addListener { event: RegisterMenuScreensEvent -> PazScreens.registerScreens(event) }
		modBus.addListener { event: RegisterGuiLayersEvent -> PazScreens.registerGuiLayers(event) }
		modBus.addListener { event: RegisterShadersEvent -> PazRenderPipelines.registerShaders(event) }
		modBus.addListener { event: FMLClientSetupEvent -> event.enqueueWork { PazClientHooks.clientSetup() } }
		PazClientHooks.registerModBus(modBus)

		val bus = NeoForge.EVENT_BUS
		bus.addListener { _: ClientTickEvent.Pre -> PazClientNetwork.onClientTick() }

		// custom tooltips
		bus.addListener { event: ItemTooltipEvent ->
			val stack = event.itemStack
			val lines = event.toolTip
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

	fun keybindFormat(keybind: KeyMapping): Component {
		return Component.translatable("chat.square_brackets", keybind.translatedKeyMessage).withColor(0x51ff53)
	}
}
