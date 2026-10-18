package joshxviii.plantz.gametest.blocks

import com.mojang.authlib.GameProfile
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.GameTestInfo
import net.minecraft.gametest.framework.GameTestListener
import net.minecraft.gametest.framework.GameTestRunner
import net.minecraft.network.Connection
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.network.registration.NetworkRegistry
import java.util.UUID

/**
 * A connected mock player (like [GameTestHelper.makeMockServerPlayerInLevel]) with a real game mode, standing at the
 * relative position [pos]. It is logged out again when the test passes or fails.
 */
fun GameTestHelper.mockPlayer(pos: Vec3, gameType: GameType = GameType.SURVIVAL): MockPlayer {
    val cookie = CommonListenerCookie.createInitial(GameProfile(UUID.randomUUID(), "test-mock-player"), false)
    val player = MockPlayer(this, cookie)
    val connection = Connection(PacketFlow.SERVERBOUND)
    EmbeddedChannel(connection)
    NetworkRegistry.configureMockConnection(connection) // lets the mod's payloads through, as for a NeoForge client
    level.server.playerList.placeNewPlayer(connection, player, cookie)
    player.setGameMode(gameType)
    val abs = absoluteVec(pos)
    player.teleportTo(level, abs.x, abs.y, abs.z, 0f, 0f)
    testInfo.addListener(object : GameTestListener {
        override fun testStructureLoaded(testInfo: GameTestInfo) {}
        override fun testPassed(test: GameTestInfo, runner: GameTestRunner) = logout()
        override fun testFailed(test: GameTestInfo, runner: GameTestRunner) = logout()
        override fun testAddedForRerun(oldTest: GameTestInfo, newTest: GameTestInfo, runner: GameTestRunner) {}
        private fun logout() {
            if (level.server.playerList.getPlayer(player.uuid) != null) level.server.playerList.remove(player)
        }
    })
    return player
}

/** A test player that remembers the chat / action bar messages it was sent. */
class MockPlayer(helper: GameTestHelper, cookie: CommonListenerCookie) :
    ServerPlayer(helper.level.server, helper.level, cookie.gameProfile(), cookie.clientInformation()) {
    val messages = mutableListOf<String>()
    override fun isSpectator(): Boolean = false
    override fun displayClientMessage(chatComponent: Component, actionBar: Boolean) {
        messages += chatComponent.string
        super.displayClientMessage(chatComponent, actionBar)
    }
}

/** Uses the held item on the block at the relative [pos] (what a sneaking right-click does), skipping the block's own interaction. */
fun GameTestHelper.useItemOn(player: ServerPlayer, pos: BlockPos, face: Direction = Direction.UP, hand: InteractionHand = InteractionHand.MAIN_HAND): InteractionResult {
    val abs = absolutePos(pos)
    val hit = BlockHitResult(Vec3.atCenterOf(abs).relative(face, 0.5), face, abs, false)
    return player.getItemInHand(hand).useOn(UseOnContext(player, hand, hit))
}

/** Puts [stack] in the player's main hand. */
fun ServerPlayer.hold(stack: ItemStack): ItemStack {
    setItemInHand(InteractionHand.MAIN_HAND, stack)
    return stack
}

/** Entities of [type] within [radius] blocks of the relative [pos]. */
fun <T : Entity> GameTestHelper.entitiesNear(type: Class<T>, pos: BlockPos, radius: Double = 1.5): List<T> =
    level.getEntitiesOfClass(type, AABB(absolutePos(pos)).inflate(radius))

/** Fails with [message] unless [condition] holds ([GameTestHelper.assertTrue] with a lazy message). */
fun GameTestHelper.check(condition: Boolean, message: () -> String) {
    if (!condition) fail(message())
}
