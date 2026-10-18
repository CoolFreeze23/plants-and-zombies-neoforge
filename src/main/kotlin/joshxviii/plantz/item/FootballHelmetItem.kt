package joshxviii.plantz.item

import joshxviii.plantz.entity.zombie.AllStar.Companion.CHARGE_BOOST_ID
import net.minecraft.ChatFormatting
import net.minecraft.core.Holder
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Equipable
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt

/** Not an ArmorItem: upstream has no equipment asset for it, the head layer draws the item model. */
class FootballHelmetItem(properties: Properties) : Item(properties), Equipable {
    override fun getEquipmentSlot(): EquipmentSlot = EquipmentSlot.HEAD
    override fun getEquipSound(): Holder<SoundEvent> = SoundEvents.ARMOR_EQUIP_IRON

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> =
        swapWithEquipmentSlot(level, player, hand, EquipmentSlot.HEAD)

    // 26.1 humanoidArmor(CHAINMAIL, HELMET): chainmail enchantability and repair material
    override fun getEnchantmentValue(stack: ItemStack): Int = 12
    override fun isValidRepairItem(stack: ItemStack, repairCandidate: ItemStack): Boolean = repairCandidate.`is`(Items.IRON_INGOT)

    companion object {
        /** ArmorType.HELMET durability (11) x chainmail multiplier (15) */
        const val DURABILITY = 11 * 15

        fun addToTooltip(consumer: MutableList<Component>, sprintKey: Component) {
            consumer.add(1, Component.translatable("item.plantz.football_helmet.description", sprintKey)
                .withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC))
        }

        private const val DAMAGE_INTERVAL = 35
        private const val KNOCKBACK_STRENGTH = 0.8
    }

    private fun isWearingFootballHelmet(entity: LivingEntity): Boolean {
        return entity.getItemBySlot(EquipmentSlot.HEAD).item is FootballHelmetItem
    }

    fun removeModifiers(entity: LivingEntity?) {
        if (entity == null) return
        entity.getAttribute(Attributes.MOVEMENT_SPEED)?.removeModifier(CHARGE_BOOST_ID)
        entity.getAttribute(Attributes.STEP_HEIGHT)?.removeModifier(CHARGE_BOOST_ID)
    }

    override fun inventoryTick(itemStack: ItemStack, level: Level, owner: Entity, slotId: Int, isSelected: Boolean) {
        super.inventoryTick(itemStack, level, owner, slotId, isSelected)
        if (level.isClientSide || level !is ServerLevel || owner !is LivingEntity) return
        // 26.1 passed the equipment slot the stack ticks in (HEAD for the worn helmet)
        val slot: EquipmentSlot? = if (owner.getItemBySlot(EquipmentSlot.HEAD) === itemStack) EquipmentSlot.HEAD else null
        if (!isWearingFootballHelmet(owner)) {
            removeModifiers(owner)
            return
        }
        if (!owner.isSprinting || !owner.onGround() || owner.isCrouching) {
            removeModifiers(owner)
            return
        }
        else if (owner.getAttribute(Attributes.MOVEMENT_SPEED)?.getModifier(CHARGE_BOOST_ID) == null) {
            level.playSound(null, owner.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.25f, 1.3f)
            owner.getAttribute(Attributes.MOVEMENT_SPEED)?.addTransientModifier(
                AttributeModifier(CHARGE_BOOST_ID, 0.025, AttributeModifier.Operation.ADD_VALUE)
            )
            owner.getAttribute(Attributes.STEP_HEIGHT)?.addTransientModifier(
                AttributeModifier(CHARGE_BOOST_ID, 0.5, AttributeModifier.Operation.ADD_VALUE)
            )
        }
        if (!level.isClientSide && owner.tickCount % DAMAGE_INTERVAL == 0 && owner.getRandom().nextFloat() > 0.5f) {
            if (slot != null) itemStack.hurtAndBreak(1, owner, slot)
        }

        level.sendParticles(
            BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(owner.blockPosition().below())),
            owner.x, owner.y + 0.05, owner.z, 1, 0.0, 0.0, 0.0, 0.6
        )
        if (owner.tickCount%2==0) level.sendParticles(
            ParticleTypes.WHITE_SMOKE,
            owner.x, owner.randomY, owner.z, 2, 0.1, 0.1, 0.1, 0.05
        )

        val look = owner.lookAngle
        val horizontalLook = Vec3(look.x, 0.0, look.z).normalize()
        if (horizontalLook.lengthSqr() < 0.001) return

        val hitBox: AABB = owner.boundingBox
            .inflate(0.45, 0.15, 0.45)
            .expandTowards(horizontalLook.scale(0.75))

        val targets = level.getEntitiesOfClass(LivingEntity::class.java, hitBox) { target ->
            target !== owner &&
                    target.isAlive &&
                    !target.isSpectator
        }
        if (targets.isEmpty()) return

        for (target in targets) {
            val awayX = target.x - owner.x
            val awayZ = target.z - owner.z
            val length = sqrt(awayX * awayX + awayZ * awayZ).coerceAtLeast(0.001)

            target.knockback(
                KNOCKBACK_STRENGTH,
                -awayX / length,
                -awayZ / length
            )
        }
    }
}