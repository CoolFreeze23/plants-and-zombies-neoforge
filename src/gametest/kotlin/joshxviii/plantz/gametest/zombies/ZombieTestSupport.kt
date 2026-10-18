package joshxviii.plantz.gametest.zombies

import com.mojang.authlib.GameProfile
import io.netty.channel.embedded.EmbeddedChannel
import joshxviii.plantz.networking.MailboxListResponsePayload
import joshxviii.plantz.networking.SeedPacketCooldownPayload
import joshxviii.plantz.networking.SendMailResponsePayload
import joshxviii.plantz.networking.ServerConfigResponsePayload
import joshxviii.plantz.networking.ZombieRaidResponsePayload
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.Connection
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.network.registration.ChannelAttributes
import java.util.UUID

/** Area of the 15x6x15 `platform` template, plus a margin for mobs that step off it. */
fun GameTestHelper.platformArea(margin: Double = 4.0): AABB =
    AABB(absoluteVec(Vec3.ZERO), absoluteVec(Vec3(15.0, 6.0, 15.0))).inflate(margin)

fun <T : Entity> GameTestHelper.entitiesNearby(type: Class<T>, margin: Double = 4.0): List<T> =
    level.getEntitiesOfClass(type, platformArea(margin))

/** Removes every non-player entity on and around the platform (mobs that walked off it included). */
fun GameTestHelper.discardEntities() {
    level.getEntitiesOfClass(Entity::class.java, platformArea()) { it !is Player }.forEach { it.discard() }
}

/** Succeeds once [criterion] stops throwing, then clears the platform's entities. */
fun GameTestHelper.succeedWhenThenClean(criterion: () -> Unit) = succeedWhen {
    criterion()
    discardEntities()
}

/** A villager without AI to be targeted; [unhurtable] gives it full damage resistance so it outlives the test. */
fun GameTestHelper.victim(x: Int, y: Int, z: Int, unhurtable: Boolean = false): Villager {
    val villager = spawnWithNoFreeWill(EntityType.VILLAGER, x, y, z)
    if (unhurtable) villager.addEffect(MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 4, false, false))
    return villager
}

fun <T : Mob> T.targeting(target: LivingEntity): T = also { it.target = target }

/** One of the mob's (often private) goals, by class name, to drive it directly. */
fun Mob.goal(simpleName: String): Goal = goalSelector.availableGoals.map { it.goal }.first { it.javaClass.simpleName == simpleName }

fun LivingEntity.wasHurt(): Boolean = !isAlive || health < maxHealth

private val MOD_PAYLOADS = listOf(
    ZombieRaidResponsePayload.ID, ServerConfigResponsePayload.ID, SeedPacketCooldownPayload.ID,
    SendMailResponsePayload.ID, MailboxListResponsePayload.ID,
).map { it.id() }

/**
 * [GameTestHelper.makeMockServerPlayerInLevel] with the mod's client payloads allowed on the fake connection
 * (an embedded connection never negotiates channels, and the mod messages joining players). Remove it with
 * [removeMockPlayer] so it does not linger in later tests.
 */
fun GameTestHelper.makeModMockServerPlayer(pos: Vec3): ServerPlayer {
    val cookie = CommonListenerCookie.createInitial(GameProfile(UUID.randomUUID(), "test-mock-player"), false)
    val player = object : ServerPlayer(level.server, level, cookie.gameProfile(), cookie.clientInformation()) {
        override fun isSpectator(): Boolean = false
        override fun isCreative(): Boolean = true
    }
    val connection = Connection(PacketFlow.SERVERBOUND)
    EmbeddedChannel(connection)
    ChannelAttributes.getOrCreateAdHocChannels(connection).addAll(MOD_PAYLOADS)
    level.server.playerList.placeNewPlayer(connection, player, cookie)
    val absolute = absoluteVec(pos)
    player.teleportTo(level, absolute.x, absolute.y, absolute.z, 0f, 0f)
    return player
}

fun GameTestHelper.removeMockPlayer(player: ServerPlayer) {
    level.server.playerList.remove(player)
}
