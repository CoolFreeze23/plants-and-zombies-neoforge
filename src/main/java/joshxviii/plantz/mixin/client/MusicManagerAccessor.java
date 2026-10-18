package joshxviii.plantz.mixin.client;

import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** For RaidMusicManager: 26.1 MusicManager.setMinutesBetweenSongs restarted the between-songs delay. */
@Mixin(MusicManager.class)
public interface MusicManagerAccessor {
    @Accessor("nextSongDelay")
    void plantz$setNextSongDelay(int nextSongDelay);
}
