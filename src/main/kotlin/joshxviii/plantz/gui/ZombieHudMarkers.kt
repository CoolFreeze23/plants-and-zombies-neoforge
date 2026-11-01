package joshxviii.plantz.gui

import joshxviii.plantz.PazBlocks
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazNetwork
import joshxviii.plantz.entity.isActiveInRaid
import joshxviii.plantz.entity.zombie.PazZombie
import joshxviii.plantz.mixin.client.GameRendererAccessor
import joshxviii.plantz.getHeldBalloons
import joshxviii.plantz.renderer.entity.zombie.PazZombieRenderer
import joshxviii.plantz.compat.client.model.HumanoidModel
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import kotlin.math.tan

object ZombieHudMarkers {

    const val ICON_SIZE = 8

    private const val MIN_DISTANCE = 10.0
    private const val MAX_DISTANCE = 96.0

    fun renderIcons(graphics: GuiGraphics, deltaTracker: DeltaTracker) {
        val mc = Minecraft.getInstance()
        if (mc.level == null || mc.player == null) return
        if (!PazConfig.ALWAYS_SHOW_HEAD_MARKERS) PazNetwork.ZombieRaidClientCache.get()
            ?: return

        val markers = collect(mc)
        if (markers.isEmpty()) return
        // 26.1 Camera.fov; 1.21.1 only has the private GameRenderer.getFov
        val delta = deltaTracker.getGameTimeDeltaPartialTick(true)
        val fov = (mc.gameRenderer as GameRendererAccessor).`plantz$getFov`(mc.gameRenderer.mainCamera, delta, true)

        for ((worldPos, zombie) in markers) {
            val screen = project(worldPos, fov) ?: continue

            val x = Mth.floor(screen.x - ICON_SIZE / 2f)
            val y = Mth.floor(screen.y - ICON_SIZE / 2f)

            headIcon(graphics, zombie, x, y)
        }
    }

    fun headIcon(graphics: GuiGraphics, zombie: Zombie, x: Int, y: Int) {
        val mc = Minecraft.getInstance()
        val partial = mc.timer.getGameTimeDeltaPartialTick(false)
        val pazZombie = zombie as? PazZombie ?: return
        val renderer = mc.entityRenderDispatcher.getRenderer(pazZombie) as? PazZombieRenderer ?: return
        // 26.1 entityRenderDispatcher.extractEntity(zombie, partial): extract into a fresh state
        val state = renderer.createRenderState()
        try {
            state.compatEntity = pazZombie
            state.compatPartialTick = partial
            renderer.extractRenderState(pazZombie, state, partial)
        } finally {
            state.compatEntity = null
        }
        // Only the bare head part is drawn below, so the other 26.1 pose overrides on the state
        // (walk animation, rotations, cleared hand items) have no effect and are skipped.
        state.headOnly = true

        val model = if(state.isBaby) renderer.babyModel else renderer.defaultModel
        val texture = renderer.getTextureLocation(state)

        val cx = x + ICON_SIZE * 0.5
        val cy = y + ICON_SIZE * 0.5
        val baseScale = ICON_SIZE * 2.0f

        // outline
        //submitHead(graphics, zombie, model, texture, zombie.getItemBySlot(EquipmentSlot.HEAD), cx, cy, baseScale * state.scale, outlinePad = 1.15f)

        // head icon
        // TODO [use3dIcon] uses a copied render of the entity model with everything except the head removed.
        //  it definitely has an impact on performance, but I can't find a better way to render worn head items like the cones/football helmets.

        // TODO I should probably take a screen shot when resources are loaded of each entity in the zombie raider tag list and save the head marker image to reuse here
        submitHead(graphics, zombie, model, texture, zombie.getItemBySlot(EquipmentSlot.HEAD), cx, cy, baseScale * state.scale)

        if (zombie.mainHandItem.`is`(PazBlocks.BRAINZ_FLAG.asItem()) || zombie.offhandItem.`is`(PazBlocks.BRAINZ_FLAG.asItem())) submitItem(graphics,
            PazBlocks.BRAINZ_FLAG.asItem().defaultInstance, cx, cy, baseScale)

        val balloon = pazZombie.getHeldBalloons().firstOrNull()
        if (balloon != null) submitItem(graphics, balloon.pickResult, cx, cy-8, baseScale)
    }

    private fun submitItem(
        graphics: GuiGraphics,
        item: ItemStack,
        cx: Double,
        cy: Double,
        scale: Float
    ) {
        graphics.pose().pushPose()
        graphics.pose().translate(cx.toFloat(), cy.toFloat(), 0f)
        graphics.pose().scale(scale/24f, scale/24f, 1f)
        graphics.renderItem(item, -4, -4)
        graphics.pose().popPose()
    }

    private fun submitHead(
        graphics: GuiGraphics,
        zombie: Zombie,
        model: HumanoidModel<*>,
        texture: ResourceLocation,
        itemStack: ItemStack,
        cx: Double,
        cy: Double,
        scale: Float,
        outlinePad: Float = 0f
    ) {
        HeadHudMarker(zombie, model, texture, itemStack, cx, cy, scale, outlinePad).render(graphics)
    }

    fun collect(client: Minecraft): List<Marker> {
        val level = client.level ?: return emptyList()
        val cam = client.gameRenderer.mainCamera.position

        return buildList {
            for (entity in level.entitiesForRendering()) {
                if (entity !is Zombie) continue

                if (!PazConfig.ALWAYS_SHOW_HEAD_MARKERS) {
                    if (!entity.isActiveInRaid()) continue
                }
                if (!isViewObscured(cam, entity)) continue

                val pos = entity.getPosition(client.timer.getGameTimeDeltaPartialTick(false)).add(0.0, entity.eyeHeight.toDouble(), 0.0)
                val dist = pos.distanceToSqr(cam)
                if (dist !in (MIN_DISTANCE*MIN_DISTANCE)..(MAX_DISTANCE*MAX_DISTANCE)) continue
                add(Marker(pos, entity))
            }
        }.sortedByDescending { it.zombie.distanceToSqr(cam) }
    }

    fun isViewObscured(camPos: Vec3, target: LivingEntity): Boolean {
        return target.level().clip(ClipContext(target.eyePosition, camPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, target)).type != HitResult.Type.MISS
    }

    fun project(worldPos: Vec3, fov: Double): Vec2? {
        val mc = Minecraft.getInstance()
        val cam = mc.gameRenderer.mainCamera
        val w = mc.window.guiScaledWidth.toDouble()
        val h = mc.window.guiScaledHeight.toDouble()

        val rel = worldPos.subtract(cam.position).toVector3f()
        val z = rel.dot(cam.lookVector)
        if (z <= 0.05) return null

        val x = -rel.dot(cam.leftVector)
        val y = rel.dot(cam.upVector)

        val tanHalf = tan(fov * Mth.DEG_TO_RAD * 0.5)
        val ndcX = x / (z * tanHalf * (w / h))
        val ndcY = y / (z * tanHalf)

        val sx = (ndcX * 0.5 + 0.5) * w
        val sy = (1.0 - (ndcY * 0.5 + 0.5)) * h

        if (sx !in -32.0..w + 32.0 || sy !in -32.0..h + 32.0) return null
        return Vec2(sx.toFloat(), sy.toFloat())
    }

    data class Marker(val worldPos: Vec3, val zombie: Zombie)
}
