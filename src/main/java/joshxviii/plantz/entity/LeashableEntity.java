package joshxviii.plantz.entity;

import com.google.common.collect.ImmutableList;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Leashable non-mob entity (balloons) with the 26.1 leash behaviour, which 1.21.1's {@link Leashable} lacks:
 * {@link #leashElasticDistance()}, {@link #leashSnapDistance()}, {@link #onElasticLeashPull()}, {@link #onLeashRemoved()},
 * {@link #dropLeash()}, {@link #removeLeash()} and the spring/torque leash pull. Vanilla's {@code Leashable.tickLeash}
 * calls {@link #handleLeashAtDistance}, which runs the 26.1 logic and returns false to skip the 1.21.1 distance rules.
 */
public abstract class LeashableEntity extends Entity implements Leashable {
    private static final Vec3 AXIS_SPECIFIC_ELASTICITY = new Vec3(0.8, 0.2, 0.8);
    private static final List<Vec3> ENTITY_ATTACHMENT_POINT = ImmutableList.of(new Vec3(0.0, 0.5, 0.5));
    private static final List<Vec3> LEASHER_ATTACHMENT_POINT = ImmutableList.of(new Vec3(0.0, 0.5, 0.0));

    /** 26.1 {@code LeashData.angularMomentum}. */
    private double leashAngularMomentum;

    public LeashableEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /**
     * 26.1 {@code Leashable.tickLeash}: when either end can no longer interact with the level the leash goes through
     * {@link #dropLeash()} (or {@link #removeLeash()} without entity drops). 1.21.1 would drop a lead directly, before
     * {@link #handleLeashAtDistance} runs, so this check comes first.
     */
    @Override
    public void baseTick() {
        Leashable.LeashData leashData = this.getLeashData();
        if (this.level() instanceof ServerLevel level && leashData != null && leashData.leashHolder != null
            && (!canInteractWithLevel(this) || !canInteractWithLevel(leashData.leashHolder))) {
            if (level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) this.dropLeash();
            else this.removeLeash();
        }
        super.baseTick();
    }

    private static boolean canInteractWithLevel(Entity entity) {
        return entity.isAlive() && !entity.isRemoved() && !entity.isSpectator();
    }

    public double leashSnapDistance() {
        return 12.0;
    }

    public double leashElasticDistance() {
        return 6.0;
    }

    public double leashDistanceTo(Entity entity) {
        return entity.getBoundingBox().getCenter().distanceTo(this.getBoundingBox().getCenter());
    }

    public void onElasticLeashPull() {
        this.checkSlowFallDistance();
    }

    public void onLeashRemoved() {
    }

    /** 26.1: drop the leash and the lead item. */
    public void dropLeash() {
        this.dropLeashInternal(true, true);
    }

    /** 26.1: drop the leash without the lead item. */
    public void removeLeash() {
        this.dropLeashInternal(true, false);
    }

    @Override
    public void dropLeash(boolean broadcastPacket, boolean dropItem) {
        if (broadcastPacket) {
            if (dropItem) this.dropLeash();
            else this.removeLeash();
        } else {
            this.dropLeashInternal(false, dropItem);
        }
    }

    private void dropLeashInternal(boolean broadcastPacket, boolean dropItem) {
        Leashable.LeashData leashData = this.getLeashData();
        if (leashData != null && leashData.leashHolder != null) {
            this.setLeashData(null);
            this.onLeashRemoved();
            if (this.level() instanceof ServerLevel serverLevel) {
                if (dropItem) this.spawnAtLocation(Items.LEAD);
                if (broadcastPacket) serverLevel.getChunkSource().broadcast(this, new ClientboundSetEntityLinkPacket(this, null));
            }
        }
    }

    @Override
    public boolean handleLeashAtDistance(Entity leashHolder, float distance) {
        if (!(this.level() instanceof ServerLevel level)) return false;
        Leashable.LeashData leashData = this.getLeashData();
        if (leashData == null) return false;
        double distanceTo = this.leashDistanceTo(leashHolder);
        if (distanceTo > this.leashSnapDistance()) {
            // 26.1 LEAD_BREAK (item.lead.break) is not in 1.21.1 or Vanilla Backport
            level.playSound(null, leashHolder.getX(), leashHolder.getY(), leashHolder.getZ(), SoundEvents.LEASH_KNOT_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
            this.leashTooFarBehaviour();
        } else if (distanceTo > this.leashElasticDistance() - (double) leashHolder.getBbWidth() - (double) this.getBbWidth()
            && this.checkElasticInteractions(leashHolder)) {
            this.onElasticLeashPull();
        } else {
            this.closeRangeLeashBehaviour(leashHolder);
        }

        this.setYRot((float) ((double) this.getYRot() - this.leashAngularMomentum));
        this.leashAngularMomentum = this.leashAngularMomentum * (double) this.leashAngularFriction();
        return false;
    }

    @Override
    public void leashTooFarBehaviour() {
        this.dropLeash();
    }

    private float leashAngularFriction() {
        if (this.onGround()) {
            return this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction() * 0.91F;
        } else {
            return this.isInLiquid() ? 0.8F : 0.91F;
        }
    }

    private boolean checkElasticInteractions(Entity leashHolder) {
        List<double[]> wrenches = this.computeElasticInteraction(leashHolder);
        if (wrenches.isEmpty()) return false;
        double x = 0.0, y = 0.0, z = 0.0, t = 0.0;
        for (double[] w : wrenches) {
            x += w[0];
            y += w[1];
            z += w[2];
            t += w[3];
        }
        this.leashAngularMomentum = this.leashAngularMomentum + 10.0 * t;
        Vec3 relativeVelocityToLeasher = getHolderMovement(leashHolder).subtract(this.getKnownMovement());
        this.addDeltaMovement(new Vec3(x, y, z).multiply(AXIS_SPECIFIC_ELASTICITY).add(relativeVelocityToLeasher.scale(0.11)));
        return true;
    }

    private static Vec3 getHolderMovement(Entity leashHolder) {
        if (leashHolder instanceof Mob mob && mob.isNoAi()) return Vec3.ZERO;
        return leashHolder.getKnownMovement();
    }

    /** Each wrench is {forceX, forceY, forceZ, torque}. */
    private List<double[]> computeElasticInteraction(Entity leashHolder) {
        double slackDistance = this.leashElasticDistance();
        Vec3 currentMovement = getHolderMovement(this);
        float entityYRot = this.getYRot() * (float) (Math.PI / 180.0);
        Vec3 entityDimensions = new Vec3(this.getBbWidth(), this.getBbHeight(), this.getBbWidth());
        float leashHolderYRot = leashHolder.getYRot() * (float) (Math.PI / 180.0);
        Vec3 leasherDimensions = new Vec3(leashHolder.getBbWidth(), leashHolder.getBbHeight(), leashHolder.getBbWidth());
        List<double[]> wrenches = new ArrayList<>();

        for (int i = 0; i < ENTITY_ATTACHMENT_POINT.size(); i++) {
            Vec3 entityAttachVector = ENTITY_ATTACHMENT_POINT.get(i).multiply(entityDimensions).yRot(-entityYRot);
            Vec3 entityAttachPos = this.position().add(entityAttachVector);
            Vec3 leasherAttachVector = LEASHER_ATTACHMENT_POINT.get(i).multiply(leasherDimensions).yRot(-leashHolderYRot);
            Vec3 leasherAttachPos = leashHolder.position().add(leasherAttachVector);
            computeDampenedSpringInteraction(leasherAttachPos, entityAttachPos, slackDistance, currentMovement, entityAttachVector).ifPresent(wrenches::add);
        }

        return wrenches;
    }

    private static Optional<double[]> computeDampenedSpringInteraction(Vec3 pivotPoint, Vec3 objectPosition, double springSlack, Vec3 objectMotion, Vec3 leverArm) {
        double distance = objectPosition.distanceTo(pivotPoint);
        if (distance < springSlack) return Optional.empty();
        Vec3 displacement = pivotPoint.subtract(objectPosition).normalize().scale(distance - springSlack);
        double torque = leverArm.z * displacement.x - leverArm.x * displacement.z;
        boolean sameDirectionToMovement = objectMotion.dot(displacement) >= 0.0;
        if (sameDirectionToMovement) displacement = displacement.scale(0.3F);
        return Optional.of(new double[]{displacement.x, displacement.y, displacement.z, torque});
    }
}
