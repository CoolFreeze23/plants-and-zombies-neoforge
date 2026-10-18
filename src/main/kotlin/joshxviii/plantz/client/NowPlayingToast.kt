package joshxviii.plantz.client

import joshxviii.plantz.compat.ARGB
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.item.DyeColor

/**
 * 26.1's NowPlayingToast (slides in at the top left with the song name and colour-cycling music notes), which upstream
 * shows when raid music starts. 1.21.1 has no music toast, so it is drawn next to the toasts. The 26.1 sprites
 * (`toast/now_playing`, `icon/music_notes`) don't exist in 1.21.1: it uses the system toast background and a note glyph.
 */
object NowPlayingToast {
    private val BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/system")
    private const val PADDING = 7
    private const val HEIGHT = 30
    private const val MUSIC_NOTES_SPACE = 30
    private const val VISIBILITY_DURATION_MS = 5000.0
    private const val SLIDE_DURATION_MS = 600L
    private val TEXT_COLOR = DyeColor.LIGHT_GRAY.textColor
    private const val MUSIC_COLOR_CHANGE_FREQUENCY_MS = 25L

    // 26.1 ColorLerper.Type.MUSIC_NOTE: dye colours at 1.25 brightness, 30 ticks each
    private val MUSIC_NOTE_COLORS = listOf(
        DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.LIGHT_BLUE, DyeColor.BLUE, DyeColor.CYAN, DyeColor.GREEN,
        DyeColor.LIME, DyeColor.YELLOW, DyeColor.ORANGE, DyeColor.PINK, DyeColor.RED, DyeColor.MAGENTA
    ).map { musicNoteColor(it) }
    private const val COLOR_DURATION = 30

    private var song: Component? = null
    private var shownAt = 0L
    private var musicNoteColorTick = 0
    private var lastMusicNoteColorChange = 0L

    /** 26.1 ToastManager.showNowPlayingToast() with NowPlayingToastMixin's song name. */
    fun show(songName: Component) {
        song = songName
        shownAt = 0L // the animation starts when the toast is first drawn, like 26.1's ToastInstance
    }

    /** 26.1 ToastManager.hideNowPlayingToast(). */
    fun hide() {
        if (song == null) return
        val now = System.currentTimeMillis()
        shownAt = if (shownAt == 0L) now - visibleMs() - 2 * SLIDE_DURATION_MS else minOf(shownAt, now - visibleMs() - SLIDE_DURATION_MS)
    }

    /** Drawn with the toasts (ToastComponentMixin), above screens like 26.1's toasts. */
    @JvmStatic
    fun render(graphics: GuiGraphics) {
        val text = song ?: return
        val minecraft = Minecraft.getInstance()
        val now = System.currentTimeMillis()
        if (shownAt == 0L) shownAt = now
        val elapsed = now - shownAt
        val visibleMs = visibleMs()
        if (elapsed > visibleMs + 2 * SLIDE_DURATION_MS) {
            song = null
            return
        }
        // ToastInstance: slide in, stay, slide out
        val visiblePortion = when {
            elapsed < SLIDE_DURATION_MS -> elapsed.toFloat() / SLIDE_DURATION_MS
            elapsed < SLIDE_DURATION_MS + visibleMs -> 1f
            else -> 1f - (elapsed - SLIDE_DURATION_MS - visibleMs).toFloat() / SLIDE_DURATION_MS
        }.coerceIn(0f, 1f)
        tickMusicNotes(now)

        val font = minecraft.font
        val width = MUSIC_NOTES_SPACE + font.width(text) + PADDING
        val x = width * visiblePortion - width
        graphics.pose().pushPose()
        graphics.pose().translate(x, 0f, 800f)
        graphics.blitSprite(BACKGROUND_SPRITE, 0, 0, width, HEIGHT)
        graphics.drawString(font, "♫", PADDING + 4, HEIGHT / 2 - font.lineHeight / 2, musicNoteColor(), false)
        graphics.drawString(font, text, MUSIC_NOTES_SPACE, HEIGHT / 2 - font.lineHeight / 2, TEXT_COLOR, false)
        graphics.pose().popPose()
    }

    private fun visibleMs(): Long =
        (VISIBILITY_DURATION_MS * Minecraft.getInstance().options.notificationDisplayTime().get()).toLong()

    private fun tickMusicNotes(now: Long) {
        if (now > lastMusicNoteColorChange + MUSIC_COLOR_CHANGE_FREQUENCY_MS) {
            musicNoteColorTick++
            lastMusicNoteColorChange = now
        }
    }

    /** 26.1 ColorLerper.getLerpedColor(MUSIC_NOTE, tick). */
    private fun musicNoteColor(): Int {
        val value = musicNoteColorTick / COLOR_DURATION
        val c1 = MUSIC_NOTE_COLORS[value % MUSIC_NOTE_COLORS.size]
        val c2 = MUSIC_NOTE_COLORS[(value + 1) % MUSIC_NOTE_COLORS.size]
        return ARGB.srgbLerp((musicNoteColorTick % COLOR_DURATION).toFloat() / COLOR_DURATION, c1, c2)
    }

    private fun musicNoteColor(color: DyeColor): Int {
        if (color == DyeColor.WHITE) return -1644826
        val src = color.textureDiffuseColor
        return ARGB.color(255, Mth.floor(ARGB.red(src) * 1.25f).coerceAtMost(255), Mth.floor(ARGB.green(src) * 1.25f).coerceAtMost(255),
            Mth.floor(ARGB.blue(src) * 1.25f).coerceAtMost(255))
    }
}
