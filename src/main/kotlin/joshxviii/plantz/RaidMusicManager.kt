package joshxviii.plantz

import joshxviii.plantz.PazNetwork.ZombieRaidClientCache
import joshxviii.plantz.PazSounds.RAID_MUSIC_ARMY
import joshxviii.plantz.PazSounds.RAID_MUSIC_BUCKET
import joshxviii.plantz.PazSounds.RAID_MUSIC_HALFTIME
import joshxviii.plantz.PazSounds.RAID_MUSIC_HIGH
import joshxviii.plantz.PazSounds.RAID_MUSIC_LEAGUE
import joshxviii.plantz.PazSounds.RAID_MUSIC_LOW
import joshxviii.plantz.PazSounds.RAID_MUSIC_MEDIUM
import joshxviii.plantz.PazSounds.RAID_MUSIC_PIRATE
import joshxviii.plantz.PazSounds.RAID_MUSIC_WINTER
import joshxviii.plantz.PazSounds.RAID_MUSIC_ZOMBOSS
import joshxviii.plantz.networking.ZombieRaidClientData
import joshxviii.plantz.raid.WaveType
import joshxviii.plantz.raid.ZombieRaid
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation
import net.minecraft.client.sounds.MusicManager
import net.minecraft.client.sounds.SoundEngine
import net.minecraft.client.sounds.WeighedSoundEvents
import net.minecraft.core.Holder
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource

enum class RaidMusicLayer(
    val music: Holder.Reference<SoundEvent>,
    val layerIndex: Int,
    val musicName: String
) {
    LOW(RAID_MUSIC_LOW, 0, "low"),
    MEDIUM(RAID_MUSIC_MEDIUM, 1, "medium"),
    HIGH(RAID_MUSIC_HIGH, 2, "high"),
    BUCKET(RAID_MUSIC_BUCKET, 3, "bucket"),
    HALFTIME(RAID_MUSIC_HALFTIME, 4, "halftime"),
    WINTER(RAID_MUSIC_WINTER, 5, "winter"),
    PIRATE(RAID_MUSIC_PIRATE, 6, "pirate"),
    ARMY(RAID_MUSIC_ARMY, 7, "army"),
    LEAGUE(RAID_MUSIC_LEAGUE, 8, "league"),
    ZOMBOSS(RAID_MUSIC_ZOMBOSS, 9, "zomboss");
}

object RaidMusicManager {
    const val FADE_IN_TIME = 140
    const val TRANSITION_TIME = 60
    const val FADE_OUT_TIME = 140

    private val random = RandomSource.create()
    val minecraft = Minecraft.getInstance()
    var activeLayers: MutableMap<RaidMusicLayer, RaidMusicSoundInstance> = mutableMapOf()
    var targetMusicLayer: RaidMusicLayer? = null
    var raidEvent: ZombieRaidClientData? = null

    fun tick() {
        raidEvent = ZombieRaidClientCache.get().also { event ->
            if (event == null && activeLayers.isNotEmpty()) {// fade out when leaving raid
               activeLayers.values.forEach { it.volume -= 1f / FADE_OUT_TIME.coerceAtLeast(1) }
               activeLayers[targetMusicLayer]?.let { if (it.volume <= 0.0f) stop() }
            }
        }
        val event = raidEvent?: return

        //stop music when raid is complete
        if (event.status != ZombieRaid.ZombieRaidStatus.ONGOING && event.status != ZombieRaid.ZombieRaidStatus.NEXT_WAVE) {
            stop()
            return
        }

        // start silent music track for music toast
        minecraft.musicManager.stopPlaying()
        minecraft.musicManager.setMinutesBetweenSongs(MusicManager.MusicFrequency.DEFAULT)
        //minecraft.soundManager.reload()

        // transition between song layers
        if (shouldUpdateMusic()) {
            targetMusicLayer = getMusicFromRaidContext(event)
            val slowFadeIn = activeLayers.values.count { it.volume >= 0.0f } <= 1
            val fadeInDuration = if (slowFadeIn) FADE_IN_TIME.coerceAtLeast(1) else TRANSITION_TIME.coerceAtLeast(1)
            val fadeInStep = 1f / fadeInDuration
            val fadeOutStep = 1f / TRANSITION_TIME.coerceAtLeast(1)

            activeLayers.forEach { (index, layer) ->
                if (index == targetMusicLayer)
                    layer.volume += fadeInStep
                else
                    layer.volume -= fadeOutStep
            }
        }
    }

    fun start() {
        // to avoid resetting the music, only start if there are no active layers already
        if (activeLayers.isNotEmpty()) return
        stop()
        RaidMusicLayer.entries.forEach {
            val layer = RaidMusicSoundInstance(it.music.value())
            layer.volume = 0.0f
            activeLayers[it] = layer
            when (minecraft.soundManager.play(layer)) {
                SoundEngine.PlayResult.STARTED -> minecraft.toastManager.showNowPlayingToast()
                SoundEngine.PlayResult.STARTED_SILENTLY -> {}
                SoundEngine.PlayResult.NOT_STARTED -> {}
            }
        }
    }

    fun stop() {
        activeLayers.forEach { (_, layer) ->
            layer.stopLayer()
        }
        minecraft.toastManager.hideNowPlayingToast()
        activeLayers.clear()
        targetMusicLayer = null
    }

    fun isPlaying(): Boolean = targetMusicLayer != null

    fun getMusicFromRaidContext(raidEvent: ZombieRaidClientData): RaidMusicLayer {
        val totalWaves = raidEvent.numWaves
        val currentWave = raidEvent.wavesSpawned
        val waveType = raidEvent.currentWaveType

        return when (waveType) {
            WaveType.DEFAULT -> when {
                (currentWave == 10 || currentWave == totalWaves || currentWave in 12..ZombieRaid.MAXIMUM_WAVE_COUNT) ->
                    RaidMusicLayer.HIGH
                currentWave in 6..11 ->
                    RaidMusicLayer.MEDIUM
                else ->
                    RaidMusicLayer.LOW
            }
            WaveType.BUCKET_BRIGADE -> RaidMusicLayer.BUCKET
            WaveType.HALFTIME_SHOWDOWN -> RaidMusicLayer.HALFTIME
            WaveType.WINTER_WONDERLAND -> RaidMusicLayer.WINTER
            WaveType.PIRATE_INVASION -> RaidMusicLayer.PIRATE
            WaveType.ROBO_ARMY -> RaidMusicLayer.ARMY
            WaveType.LEAGUE_OF_AWESOME -> RaidMusicLayer.LEAGUE
            WaveType.ZOMBOSS -> RaidMusicLayer.ZOMBOSS
        }
    }

    private fun shouldUpdateMusic(): Boolean {
        if (activeLayers.isEmpty() || activeLayers[targetMusicLayer]?.isStopped == true) start()
        raidEvent?.let {
            if (getMusicFromRaidContext(it) != targetMusicLayer) return true
        }
        activeLayers[targetMusicLayer]?.let {
            if (it.volume < 1.0f) return true
        }
        return false
    }
}

class RaidMusicSoundInstance(
    val layer: SoundEvent,
): AbstractTickableSoundInstance(layer, SoundSource.MUSIC, SoundInstance.createUnseededRandom()) {

    init {
        volume = 0.0f
        looping = true
        delay = 0
        attenuation = Attenuation.NONE
        relative = true
    }

    fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0.0f, 1.0f)
    }

    fun stopLayer() = this.stop()

    override fun tick() {}
}