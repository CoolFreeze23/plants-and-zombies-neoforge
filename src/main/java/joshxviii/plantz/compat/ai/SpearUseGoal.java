package joshxviii.plantz.compat.ai;

import java.lang.reflect.Method;
import java.util.EnumSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 26.1 SpearUseGoal: approach the target, charge it with the spear down, back off, repeat.
 * 1.21.1 has no spears; the {@code minecraft:kinetic_weapon} component and the spear items come from a backport mod
 * such as Backported Spears, so without one this goal never starts. Backported Spears ships its own copy of the goal,
 * but that copy never resets after its first charge (the mob then stands still for as long as it has a target), so
 * the mod's mobs use this one.
 * <p>
 * Differences from 26.1: the use duration is read reflectively from the backport's component ({@code getUseTicks()},
 * 26.1 {@code computeDamageUseDuration()}); 1.21.1's {@code LandRandomPos.getPosAway} has no minimum distance; the
 * mount speed uses 26.1's {@code chargeSpeedModifier} value for zombie horses directly.
 */
public class SpearUseGoal<T extends Monster> extends Goal {
    private static final ResourceLocation KINETIC_WEAPON_ID = ResourceLocation.withDefaultNamespace("kinetic_weapon");
    private static final double MAX_FLEEING_TIME = reducedTickDelay(100);
    private final T mob;
    @Nullable
    private SpearUseState state;
    private final double speedModifierWhenCharging;
    private final double speedModifierWhenRepositioning;
    private final float approachDistanceSq;
    private final float targetInRangeRadiusSq;

    public SpearUseGoal(T mob, double speedModifierWhenCharging, double speedModifierWhenRepositioning, float approachDistance, float targetInRangeRadius) {
        this.mob = mob;
        this.speedModifierWhenCharging = speedModifierWhenCharging;
        this.speedModifierWhenRepositioning = speedModifierWhenRepositioning;
        this.approachDistanceSq = approachDistance * approachDistance;
        this.targetInRangeRadiusSq = targetInRangeRadius * targetInRangeRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.ableToAttack() && !this.mob.isUsingItem();
    }

    private boolean ableToAttack() {
        return this.mob.getTarget() != null && this.kineticWeapon() != null;
    }

    @Nullable
    private Object kineticWeapon() {
        DataComponentType<?> type = KineticWeapon.TYPE;
        return type == null ? null : this.mob.getMainHandItem().get(type);
    }

    private int getKineticWeaponUseDuration() {
        Object kineticWeapon = this.kineticWeapon();
        return reducedTickDelay(kineticWeapon == null ? 0 : KineticWeapon.useTicks(kineticWeapon));
    }

    @Override
    public boolean canContinueToUse() {
        return this.state != null && !this.state.done && this.ableToAttack();
    }

    @Override
    public void start() {
        super.start();
        this.mob.setAggressive(true);
        this.state = new SpearUseState();
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.getNavigation().stop();
        this.mob.setAggressive(false);
        this.state = null;
        this.mob.stopUsingItem();
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (this.state != null && target != null) {
            double targetDistSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
            Entity mount = this.mob.getRootVehicle();
            float speedModifier = mount instanceof ZombieHorse ? 1.4F : 1.0F;
            int mountDistance = this.mob.isPassenger() ? 2 : 0;
            this.mob.lookAt(target, 30.0F, 30.0F);
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (this.state.notEngagedYet()) {
                if (targetDistSqr > (double) this.approachDistanceSq) {
                    this.mob.getNavigation().moveTo(target, (double) speedModifier * this.speedModifierWhenRepositioning);
                    return;
                }

                this.state.startEngagement(this.getKineticWeaponUseDuration());
                this.mob.startUsingItem(InteractionHand.MAIN_HAND);
            }

            if (this.state.tickAndCheckEngagement()) {
                this.mob.stopUsingItem();
                double distance = Math.sqrt(targetDistSqr);
                this.state.awayPos = this.getPosAway(Math.max(1.0, (double) (11 + mountDistance) - distance), target.position());
                this.state.fleeingTime = 1;
            }

            if (!this.state.tickAndCheckFleeing()) {
                if (this.state.awayPos != null) {
                    this.mob.getNavigation().moveTo(this.state.awayPos.x, this.state.awayPos.y, this.state.awayPos.z, (double) speedModifier * this.speedModifierWhenRepositioning);
                    if (this.mob.getNavigation().isDone()) {
                        if (this.state.fleeingTime > 0) {
                            this.state.done = true;
                            return;
                        }

                        this.state.awayPos = null;
                    }
                } else {
                    this.mob.getNavigation().moveTo(target, (double) speedModifier * this.speedModifierWhenCharging);
                    if (targetDistSqr < (double) this.targetInRangeRadiusSq || this.mob.getNavigation().isDone()) {
                        double distance = Math.sqrt(targetDistSqr);
                        this.state.awayPos = this.getPosAway((double) (7 + mountDistance) - distance, target.position());
                    }
                }
            }
        }
    }

    @Nullable
    private Vec3 getPosAway(double maxHorizontalDist, Vec3 avoidPos) {
        return LandRandomPos.getPosAway(this.mob, Math.max(1, Mth.ceil(maxHorizontalDist)), 7, avoidPos);
    }

    public static class SpearUseState {
        private int engageTime = -1;
        private int fleeingTime = -1;
        @Nullable
        private Vec3 awayPos;
        private boolean done = false;

        public boolean notEngagedYet() {
            return this.engageTime < 0;
        }

        public void startEngagement(int spearDownTime) {
            this.engageTime = spearDownTime;
        }

        public boolean tickAndCheckEngagement() {
            if (this.engageTime > 0) {
                this.engageTime--;
                if (this.engageTime == 0) {
                    return true;
                }
            }

            return false;
        }

        public boolean tickAndCheckFleeing() {
            if (this.fleeingTime > 0) {
                this.fleeingTime++;
                if ((double) this.fleeingTime > MAX_FLEEING_TIME) {
                    this.done = true;
                    return true;
                }
            }

            return false;
        }
    }

    /** The backport's {@code minecraft:kinetic_weapon} component; loaded on first use, after registration. */
    private static final class KineticWeapon {
        @Nullable
        static final DataComponentType<?> TYPE = BuiltInRegistries.DATA_COMPONENT_TYPE.get(KINETIC_WEAPON_ID);
        @Nullable
        private static Method useTicks;

        static int useTicks(Object kineticWeapon) {
            try {
                if (useTicks == null) useTicks = kineticWeapon.getClass().getMethod("getUseTicks");
                return (Integer) useTicks.invoke(kineticWeapon);
            } catch (ReflectiveOperationException | ClassCastException e) {
                return 0;
            }
        }
    }
}
