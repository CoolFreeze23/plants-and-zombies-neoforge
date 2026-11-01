package joshxviii.plantz.item

import net.minecraft.core.component.DataComponents
import joshxviii.plantz.PazSounds
import joshxviii.plantz.entity.projectile.PaintBall
import joshxviii.plantz.percentFormat
import net.minecraft.ChatFormatting
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
import net.minecraft.world.item.component.ChargedProjectiles
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec2
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

    override fun useOnRelease(itemStack: ItemStack): Boolean {
        return super.useOnRelease(itemStack)
    }

    override fun onUseTick(level: Level, livingEntity: LivingEntity, itemStack: ItemStack, ticksRemaining: Int) {
        super.onUseTick(level, livingEntity, itemStack, ticksRemaining)
        val ammo = itemStack.get(DataComponents.CHARGED_PROJECTILES)?.items?.firstOrNull() // 26.1 ItemStackTemplate.create()
        if (ammo == null) {
            reload(level, livingEntity, itemStack)
            return
        }
        if (ticksRemaining%4 != 0) return
        level.playSound(livingEntity, livingEntity.blockPosition(), PazSounds.DYE_BLASTER, SoundSource.NEUTRAL, 0.3f, 0.7f)
        itemStack.hurtAndBreak(1, livingEntity, LivingEntity.getSlotForHand(livingEntity.usedItemHand))
        if (level is ServerLevel) {
            if (level.random.nextFloat() < AMMO_CHANCE) {
                itemStack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY)
                itemStack.remove(DataComponents.DYED_COLOR)
                reload(level, livingEntity, itemStack)
            }
            shoot(level, livingEntity, livingEntity.usedItemHand, itemStack, listOf(ammo), 1.2f, 8.0f, false, null)
        }
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

    // 26.1 enchantable(1)
    override fun getEnchantmentValue(stack: ItemStack): Int = 1

    fun reload(level: Level, livingEntity: LivingEntity, itemStack: ItemStack): ItemStack {
        val ammo = draw(itemStack, livingEntity.getProjectile(itemStack), livingEntity).firstOrNull()?: ItemStack.EMPTY
        if (!ammo.isEmpty && ammo.`is`(Tags.Items.DYES)) { // 26.1 ItemTags.DYES
            // 26.1 ChargedProjectiles(ItemStackTemplate) / DataComponents.DYE
            itemStack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(ItemStack(ammo.item)))
            (ammo.item as? DyeItem)?.dyeColor?.fireworkColor?.let { itemStack.set(DataComponents.DYED_COLOR, DyedItemColor(it, false)) }
            level.playSound(livingEntity as? Player, livingEntity.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.NEUTRAL, 0.3f, 1.4f)
        }
        return ammo
    }
    override fun getUseDuration(itemStack: ItemStack, user: LivingEntity): Int {
        return 72000
    }

    override fun getUseAnimation(itemStack: ItemStack): UseAnim {
        return UseAnim.CROSSBOW
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        return super.useOn(context)
    }

    @Suppress("OVERRIDE_DEPRECATION") // abstract in 1.21.1
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