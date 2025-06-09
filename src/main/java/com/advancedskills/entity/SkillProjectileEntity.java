package com.advancedskills.entity;

import com.advancedskills.AdvancedSkillsMod; // For ElementType
import com.advancedskills.CombatManager;    // To apply effects on hit
import com.advancedskills.init.ModEntityTypes; // For EntityType registration
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class SkillProjectileEntity extends Projectile {

    private static final EntityDataAccessor<Integer> DATA_ELEMENT_ORDINAL = SynchedEntityData.defineId(SkillProjectileEntity.class, EntityDataSerializers.INT);
    private float damage = 5.0f; // Default damage

    public SkillProjectileEntity(EntityType<? extends SkillProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public SkillProjectileEntity(Level level, Player owner, Vec3 lookVec, float damage, AdvancedSkillsMod.ElementType element) {
        this(ModEntityTypes.SKILL_PROJECTILE.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getEyePosition().subtract(0, 0.1, 0).add(lookVec.scale(0.5))); // Start slightly in front
        this.setDeltaMovement(lookVec.scale(1.5D)); // Adjust speed factor as needed
        this.damage = damage;
        this.setElement(element);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ELEMENT_ORDINAL, AdvancedSkillsMod.ElementType.NONE.ordinal());
    }

    public void setElement(AdvancedSkillsMod.ElementType element) {
        this.entityData.set(DATA_ELEMENT_ORDINAL, element.ordinal());
    }

    public AdvancedSkillsMod.ElementType getElement() {
        int ordinal = this.entityData.get(DATA_ELEMENT_ORDINAL);
        if (ordinal >= 0 && ordinal < AdvancedSkillsMod.ElementType.values().length) {
            return AdvancedSkillsMod.ElementType.values()[ordinal];
        }
        return AdvancedSkillsMod.ElementType.NONE;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return this.damage;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !this.isAlive()) {
            return;
        }

        HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitresult.getType() != HitResult.Type.MISS) {
            this.onHit(hitresult);
        }

        Vec3 currentPos = this.position();
        Vec3 newPos = currentPos.add(this.getDeltaMovement());
        this.setPos(newPos);

        // Particle trail (simple example)
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.02);
        }

        // Remove if too old or out of bounds
        if (this.tickCount > 100) { // 5 seconds lifetime
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        Entity target = entityHitResult.getEntity();
        Entity owner = this.getOwner();

        if (target instanceof LivingEntity livingTarget && owner instanceof Player player) {
            // Apply damage
            livingTarget.hurt(this.damageSources().indirectMagic(this, player), damage);

            // Apply elemental effect via CombatManager
            AdvancedSkillsMod modInstance = AdvancedSkillsMod.getInstance();
            if (modInstance != null && modInstance.combatManager != null && modInstance.playerStatsManager != null) {
                int playerLevel = modInstance.playerStatsManager.calculateLevelFromXp(modInstance.playerStatsManager.getPlayerSkillXp(player.getUUID()));
                AdvancedSkillsMod.WeaponSpecialty specialty = modInstance.playerStatsManager.getPlayerWeaponSpecialty(player.getUUID()); // Assuming ranged skill
                modInstance.combatManager.applyElementalEffect(player, livingTarget, getElement(), playerLevel, true, specialty);
            }
            this.playSound(SoundEvents.GENERIC_HURT, 0.5F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
        }
        this.discard(); // Remove projectile on hit
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        // Example particle effect on block hit
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.05);
        }
        this.playSound(SoundEvents.STONE_HIT, 0.5F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
        this.discard(); // Remove projectile
    }

    @Override
    protected boolean canHitEntity(Entity p_37250_) {
        // Prevent hitting owner or other non-targetable entities
        if (!super.canHitEntity(p_37250_)) {
             return false;
        } else {
            Entity owner = this.getOwner();
            // Corrected logic: allow hitting other players, but not self/passengers.
            // The original `p_37250_ instanceof Player` would PREVENT hitting other players if owner was null or not the same.
            // We want to hit any LivingEntity that is not the owner or its passengers.
            // And if it's a player, it should not be the owner.
            if (owner == null) return true; // Can hit anything if no owner (e.g. spawned by command block)
            if (p_37250_.is пассажирOrSame(owner)) return false; // Don't hit owner or its passengers
            // At this point, it's not the owner or its passenger.
            // If the target is a Player, it's fine (it's another player).
            // If the target is not a Player (e.g. a Mob), it's also fine.
            return true;
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        // For persistence if needed
        compoundTag.putInt("Element", getElement().ordinal());
        compoundTag.putFloat("Damage", this.damage);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putInt("Element", this.entityData.get(DATA_ELEMENT_ORDINAL));
        compoundTag.putFloat("Damage", this.damage);
    }
}
