package joshxviii.plantz.gametest.plants

import com.mojang.authlib.GameProfile
import io.netty.channel.embedded.EmbeddedChannel
import joshxviii.plantz.mixin.LivingEntityAccessor
import joshxviii.plantz.networking.MailboxListResponsePayload
import joshxviii.plantz.networking.SeedPacketCooldownPayload
import joshxviii.plantz.networking.SendMailResponsePayload
import joshxviii.plantz.networking.ServerConfigResponsePayload
import joshxviii.plantz.networking.ZombieRaidResponsePayload
import joshxviii.plantz.pazResource
import net.minecraft.core.Holder
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.Connection
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.world.InteractionHand
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.network.registration.ChannelAttributes
import java.util.UUID

/*
 * Shared helpers for PlantGameTests. Test coordinates are relative to the structure block, which sits one block
 * below the template, so the platform's grass floor is at y=1 and mobs stand at y=2. Batches keep time-dependent
 * tests apart (all tests of a batch run at once in the same level): plants and zombies stay within x/z 4..10 of the
 * 15x15 platform so neighbouring tests (20 blocks apart) are out of the default 14 block follow range. Tests get a
 * barrier ceiling at y=7 unless they ask for skyAccess (lobbed projectiles need it).
 */

/** Night: zombies don't burn and mushrooms are awake. */
const val NIGHT_BATCH = "plantz_night"
/** Noon: plants that sleep at night are awake, mushrooms sleep. */
const val DAY_BATCH = "plantz_day"
/** Night, for plants that pick the furthest target in a long range (they would shoot into neighbouring tests). */
const val LONG_RANGE_BATCH = "plantz_long_range"
/** Gnomes go after any plant, zombie or player in a 16 block range. */
const val GNOME_BATCH = "plantz_gnome"

const val NIGHT_TIME = 18000L
const val NOON_TIME = 6000L

fun setTime(level: ServerLevel, time: Long) {
    level.dayTime = time
    level.setWeatherParameters(20000000, 0, false, false)
}

private val MOD_CLIENT_PAYLOADS = listOf(
    ZombieRaidResponsePayload.ID, ServerConfigResponsePayload.ID, SeedPacketCooldownPayload.ID,
    SendMailResponsePayload.ID, MailboxListResponsePayload.ID,
).map { it.id() }

/**
 * GameTestHelper.makeMockServerPlayerInLevel with the mod's client payloads allowed on the embedded connection (it
 * never negotiates channels, and the mod messages joining players), placed in the test area. It plays in survival so
 * items get used up, while isCreative() still says true so zombies ignore it. Its living tick (effects, movement) only
 * runs when a test calls [ServerPlayer.doTick]. Remove it with [removeMockPlayer].
 */
fun GameTestHelper.mockPlayer(x: Double = 7.5, y: Double = 2.0, z: Double = 2.5): ServerPlayer {
    val cookie = CommonListenerCookie.createInitial(GameProfile(UUID.randomUUID(), "test-mock-player"), false)
    val player = object : ServerPlayer(level.server, level, cookie.gameProfile(), cookie.clientInformation()) {
        override fun isSpectator(): Boolean = false
        override fun isCreative(): Boolean = true
    }
    val connection = Connection(PacketFlow.SERVERBOUND)
    EmbeddedChannel(connection)
    ChannelAttributes.getOrCreateAdHocChannels(connection).addAll(MOD_CLIENT_PAYLOADS)
    level.server.playerList.placeNewPlayer(connection, player, cookie)
    player.setGameMode(GameType.SURVIVAL)// the game test level is creative; survival players use up items
    val pos = absoluteVec(Vec3(x, y, z))
    player.teleportTo(level, pos.x, pos.y, pos.z, 0f, 0f)
    return player
}

fun GameTestHelper.removeMockPlayer(player: ServerPlayer) = level.server.playerList.remove(player)

/** [GameTestHelper.succeedWhen] that runs [cleanup] (removing mock players) right before succeeding. */
fun GameTestHelper.succeedWhenThen(cleanup: () -> Unit, check: () -> Unit) {
    startSequence().thenWaitUntil(check).thenExecute(cleanup).thenSucceed()
}

fun GameTestHelper.relativePosition(entity: Entity): Vec3 = relativeVec(entity.position())

fun ServerPlayer.hasPazAdvancement(id: String): Boolean {
    val advancement = server.advancements.get(pazResource(id)) ?: return false
    return advancements.getOrStartProgress(advancement).isDone
}

fun ServerPlayer.holding(stack: ItemStack): ServerPlayer = apply { setItemInHand(InteractionHand.MAIN_HAND, stack) }

val LivingEntity.isHurt: Boolean
    get() = !isAlive || health < maxHealth

/** Raises max health so a target survives long enough to watch several attacks. */
fun LivingEntity.toughen(maxHealth: Double = 500.0) {
    getAttribute(Attributes.MAX_HEALTH)!!.baseValue = maxHealth
    health = maxHealth.toFloat()
}

/** The effect flags LivingEntityMixin syncs (read through the accessor the client renderers use). */
val LivingEntity.effectFlags: LivingEntityAccessor
    get() = this as LivingEntityAccessor

/** State dump for assertion messages. */
fun describe(entity: LivingEntity): String {
    val source = entity.lastDamageSource
    return "${entity.type.description.string}[health=${entity.health}/${entity.maxHealth} alive=${entity.isAlive}" +
        " removed=${entity.removalReason} lastDamage=${source?.type()?.msgId()} by=${source?.entity?.type?.description?.string}" +
        " effects=${entity.activeEffects.map { it.effect.registeredName }} target=${(entity as? Mob)?.target?.type?.description?.string}]"
}

fun GameTestHelper.assertHurt(entity: LivingEntity, what: String) = assertTrue(entity.isHurt, "$what: ${describe(entity)}")

fun GameTestHelper.assertHasEffect(entity: LivingEntity, effect: Holder<MobEffect>, what: String) =
    assertTrue(entity.hasEffect(effect), "$what: ${describe(entity)}")
