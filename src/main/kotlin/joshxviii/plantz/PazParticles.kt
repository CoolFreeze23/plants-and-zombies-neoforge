package joshxviii.plantz

import joshxviii.plantz.particles.*
import net.minecraft.client.particle.CritParticle
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.SpellParticle
import net.minecraft.client.particle.SplashParticle
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent

object PazParticles {
    // 26.1 particle groups (Fabric ParticleGroupRegistry) -> custom render types whose particles draw their own geometry
    @JvmField
    val ELECTRIC_ARC: ParticleRenderType = CustomGeometryParticleRenderType(pazResource("electric_arc").toString())
    @JvmField
    val BEAM: ParticleRenderType = CustomGeometryParticleRenderType(pazResource("beam").toString())

    /** Called by PazClient from RegisterParticleProvidersEvent (mod bus). */
    fun registerProviders(event: RegisterParticleProvidersEvent) {
        val it = event
        it.registerSpriteSet(PazServerParticles.BUTTER_DRIP) { sprite ->
            SplashParticle.Provider(sprite)
        }

        it.registerSpriteSet(PazServerParticles.CHILLED) { sprite ->
            SplashParticle.Provider(sprite)
        }

        it.registerSpriteSet(PazServerParticles.PEA_HIT) { spriteSet ->
            PeaParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.ICE_PEA_HIT) { spriteSet ->
            PeaParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.FIRE_PEA_HIT) { spriteSet ->
            PeaParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.ELECTRIC_PEA_HIT) { spriteSet ->
            PeaParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.HYPNO_SPORE) { spriteSet ->
            HypnoSporeParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.SPORE) { spriteSet ->
            SporeParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.SPORE_HIT) {
            spriteSet -> CritParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.FUME_BUBBLE) { spriteSet ->
            FumeBubbleParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.EMBER) { spriteSet ->
            EmberParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.ELECTRIFIED) { spriteSet ->
            ElectrifiedParticle.Provider(spriteSet)
        }

        it.registerSpecial(PazServerParticles.ELECTRIC_ARC, ElectricArcParticle.Provider())

        it.registerSpecial(PazServerParticles.BEAM, BeamParticle.Provider())

        it.registerSpriteSet(PazServerParticles.ENERGIZED) { spriteSet ->
            EnergizedParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.POWERED_UP) { spriteSet ->
            PoweredUpParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.POP) { sprite ->
            PopParticle.Provider(sprite)
        }

        it.registerSpriteSet(PazServerParticles.SLEEP) { spriteSet ->
            SleepParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.NOTIFY) { spriteSet ->
            NotifyParticle.Provider(spriteSet)
        }
        it.registerSpriteSet(PazServerParticles.NEEDS_SUN) { spriteSet ->
            NotifyParticle.Provider(spriteSet)
        }
        it.registerSpriteSet(PazServerParticles.NEEDS_WATER) { spriteSet ->
            NotifyParticle.Provider(spriteSet)
        }
        it.registerSpriteSet(PazServerParticles.NEEDS_TIME) { spriteSet ->
            NotifyParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.NUKE_WAVE) { spriteSet ->
            NukeWaveParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.NUKE_BLAST) { spriteSet ->
            NukeBlastParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.NUKE_SMOKE) { spriteSet ->
            NukeSmokeParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.PLASMA_BURST) { spriteSet ->
            PlasmaBurstParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.PAINT_BALL) { spriteSet ->
            PaintBallParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.CONFETTI) { spriteSet ->
            ConfettiParticle.Provider(spriteSet)
        }

        it.registerSpriteSet(PazServerParticles.ZOMBIE_OMEN) { sprite ->
            SpellParticle.Provider(sprite)
        }
    }
}
