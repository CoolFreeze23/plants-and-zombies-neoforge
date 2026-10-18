package joshxviii.plantz.gametest.gui

import com.mojang.logging.LogUtils
import joshxviii.plantz.PazMain
import joshxviii.plantz.gui.GuiCursor
import net.minecraft.client.MouseHandler
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import java.io.File

/**
 * Dev-only GUI check driver (gametest source set, never packaged). If `pazdev-script.txt` exists in the game
 * directory, its lines run once a player is in a world, so screens and HUD can be exercised in a quick-play client:
 * ```
 * # <tick after join> <command> [args]
 * 20  server setblock 2 -60 2 plantz:mailbox      run as the player on the integrated server (op level 4)
 * 30  regmail                                      register the mailboxes within 16 blocks with MailboxManager
 * 40  use 2 -60 2                                  right-click (useWithoutItem) the block as the player
 * 60  type abc                                     type into the open screen
 * 65  selectall                                    select all text of the focused EditBox
 * 70  click 160 40 / release 160 40 / drag 160 60  mouse on the open screen, relative to the container's top-left
 * 80  scroll 100 50 -1                             mouse wheel on the open screen
 * 90  hover 158 32                                 move the game's mouse (relative to the container) without the OS cursor
 * 95  cursor                                       log the GUI cursor shape
 * 100 shot mailbox                                 screenshot to screenshots/mailbox.png
 * 110 close / quit
 * ```
 */
@EventBusSubscriber(modid = "plantz", value = [Dist.CLIENT])
object GuiDevScript {
    private val LOGGER = LogUtils.getLogger()
    private var lines: List<Pair<Int, List<String>>>? = null
    private var tick = 0

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        val mc = Minecraft.getInstance()
        if (mc.player == null || mc.level == null) return
        val script = lines ?: load(mc) ?: return
        tick++
        for ((at, cmd) in script) if (at == tick) runCatching { run(mc, cmd) }.onFailure { LOGGER.error("[pazdev] {} failed", cmd, it) }
    }

    private fun load(mc: Minecraft): List<Pair<Int, List<String>>>? {
        val file = File(mc.gameDirectory, "pazdev-script.txt")
        if (!file.isFile) return null
        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { line -> line.split(Regex("\\s+")).let { it[0].toInt() to it.drop(1) } }
            .also { lines = it; LOGGER.info("[pazdev] loaded {} script lines", it.size) }
    }

    private fun run(mc: Minecraft, cmd: List<String>) {
        LOGGER.info("[pazdev] t={} {}", tick, cmd.joinToString(" "))
        val args = cmd.drop(1)
        val screen = mc.screen
        val left = (screen as? AbstractContainerScreen<*>)?.guiLeft ?: 0
        val top = (screen as? AbstractContainerScreen<*>)?.guiTop ?: 0
        fun x(i: Int) = left + args[i].toDouble()
        fun y(i: Int) = top + args[i].toDouble()
        when (cmd[0]) {
            "server" -> onServer(mc) { player, server ->
                server.commands.performPrefixedCommand(player.createCommandSourceStack().withPermission(4), args.joinToString(" "))
            }
            "use" -> onServer(mc) { player, _ ->
                val pos = BlockPos(args[0].toInt(), args[1].toInt(), args[2].toInt())
                player.serverLevel().getBlockState(pos).useWithoutItem(player.serverLevel(), player, BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false))
            }
            "regmail" -> onServer(mc) { player, _ ->
                val level = player.serverLevel()
                BlockPos.betweenClosed(player.blockPosition().offset(-16, -4, -16), player.blockPosition().offset(16, 4, 16)).forEach { pos ->
                    level.getBlockEntity(pos)?.let { PazMain.onBlockEntityLoad(it, level) }
                }
            }
            "type" -> args.joinToString(" ").forEach { screen?.charTyped(it, 0) }
            "selectall" -> (screen?.focused as? EditBox)?.apply { moveCursorToEnd(false); setHighlightPos(0) }
            "click" -> screen?.mouseClicked(x(0), y(1), 0)
            "release" -> screen?.mouseReleased(x(0), y(1), 0)
            "drag" -> screen?.mouseDragged(x(0), y(1), 0, 0.0, 0.0)
            "scroll" -> screen?.mouseScrolled(x(0), y(1), 0.0, args[2].toDouble())
            "hover" -> setMouse(mc, x(0), y(1))
            "cursor" -> LOGGER.info("[pazdev] GUI cursor = {}", GuiCursor.current ?: "DEFAULT")
            "shot" -> Screenshot.grab(mc.gameDirectory, args[0] + ".png", mc.mainRenderTarget) { LOGGER.info("[pazdev] {}", it.string) }
            "close" -> mc.player?.closeContainer()
            "quit" -> mc.stop()
            else -> LOGGER.warn("[pazdev] unknown command {}", cmd[0])
        }
    }

    private fun onServer(mc: Minecraft, action: (net.minecraft.server.level.ServerPlayer, net.minecraft.server.MinecraftServer) -> Unit) {
        val server = mc.singleplayerServer ?: return LOGGER.warn("[pazdev] no integrated server")
        val uuid = mc.player!!.uuid
        server.execute { server.playerList.getPlayer(uuid)?.let { action(it, server) } }
    }

    /** Moves the game's idea of the mouse (MouseHandler.xpos/ypos, in window pixels) without touching the OS cursor. */
    private fun setMouse(mc: Minecraft, guiX: Double, guiY: Double) {
        val scale = mc.window.guiScale
        for ((name, value) in listOf("xpos" to guiX * scale, "ypos" to guiY * scale)) {
            MouseHandler::class.java.getDeclaredField(name).apply { isAccessible = true }.setDouble(mc.mouseHandler, value)
        }
    }
}
