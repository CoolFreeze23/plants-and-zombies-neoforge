package joshxviii.plantz.item

import joshxviii.plantz.entity.projectile.PaintBall
import joshxviii.plantz.percentFormat
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.neoforged.neoforge.common.Tags
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.item.DyeItem
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim
import net.minecraft.world.item.ProjectileWeaponItem
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import java.util.function.Predicate

class DyeBlasterItem(properties: Properties) : ProjectileWeaponItem(properties) {

    companion object {
        fun addToTooltip(consumer: MutableList<Component>) {
            consumer.add(1, Component.translatable("item.plantz.dye_blaster.description", AMMO_CHANCE.percentFormat())
                .withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC))
        }

        val DYES: Predicate<ItemStack> = Predicate { itemStack: ItemStack -> itemStack.`is`(Tags.Items.DYES) }
        const val AMMO_CHANCE: Float = 0.4f
    }

    override fun onUseTick(level: Level, livingEntity: LivingEntity, itemStack: ItemStack, ticksRemaining: Int) {
        super.onUseTick(level, livingEntity, itemStack, ticksRemaining)
        val ammo = livingEntity.getProjectile(itemStack)
        if (ammo.isEmpty) return
        if (ticksRemaining%4 != 0) return
        level.playSound(
            null,
            livingEntity.x,
            livingEntity.y,
            livingEntity.z,
            SoundEvents.LAVA_POP,
            SoundSource.PLAYERS,
            1.0f,
            1.0f / (level.getRandom().nextFloat() * 0.4f + 1.2f) + 4.5f
        )
        itemStack.hurtAndBreak(1, livingEntity, LivingEntity.getSlotForHand(livingEntity.usedItemHand))
        if (level.random.nextFloat() < AMMO_CHANCE) draw(itemStack, ammo, livingEntity)
        if (level is ServerLevel) shoot(level, livingEntity, livingEntity.usedItemHand, itemStack, listOf(ammo), 1.2f, 8.0f, false, null)
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val itemStack: ItemStack = player.getItemInHand(hand)
        val foundProjectile = !player.getProjectile(itemStack).isEmpty
        if (!player.hasInfiniteMaterials() && !foundProjectile && !player.isAlive) return InteractionResultHolder.fail(itemStack)
        else {
            player.startUsingItem(hand)
            return InteractionResultHolder.consume(itemStack)
        }
    }

    // 26.1 repairable(BRAINZ_ALLOY)
    override fun isValidRepairItem(stack: ItemStack, repairCandidate: ItemStack): Boolean = repairCandidate.`is`(joshxviii.plantz.PazItems.BRAINZ_ALLOY)

    override fun getUseDuration(itemStack: ItemStack, user: LivingEntity): Int {
        return 72000
    }

    override fun getUseAnimation(itemStack: ItemStack): UseAnim {
        return UseAnim.CROSSBOW
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        return super.useOn(context)
    }

    override fun getAllSupportedProjectiles(): Predicate<ItemStack> = DYES

    override fun getDefaultProjectileRange(): Int = 12

    override fun createProjectile(
        level: Level, shooter: LivingEntity, heldItem: ItemStack, projectile: ItemStack, isCrit: Boolean
    ): Projectile {
        val color = (projectile.item as? DyeItem)?.dyeColor ?: DyeColor.entries.toTypedArray().filter { it != DyeColor.BLACK }.random()
        return PaintBall(
            level,
            shooter,
            spawnOffset = Vec2(0.5f,-0.0f),
            color = color
        )
    }

    override fun shootProjectile(
        shooter: LivingEntity,
        projectileEntity: Projectile,
        index: Int,
        power: Float,
        uncertainty: Float,
        angle: Float,
        targetOverrride: LivingEntity?
    ) {
        projectileEntity.shootFromRotation(shooter, shooter.xRot, shooter.yRot + angle, 0f, power, uncertainty)
    }
}