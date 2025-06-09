package com.advancedskills.skills;

import com.advancedskills.AdvancedSkillsMod;
import com.advancedskills.CombatManager;
import com.advancedskills.MobStatsManager;
import com.advancedskills.PlayerStatsManager;
import net.minecraft.ChatFormatting; // Added import
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Random;

public class ElementalBlastSkill implements ISkill {

    private static final String SKILL_ID = "elemental_blast";
    private static final int COOLDOWN_TICKS = 200; // 10 seconds
    private final Random random = new Random();

    @Override
    public String getSkillId() { return SKILL_ID; }

    @Override
    public String getDisplayName() { return "Elemental Blast"; }

    @Override
    public String getDescription() { return "Unleashes a powerful blast of your attuned element."; }

    @Override
    public int getCooldownTicks() { return COOLDOWN_TICKS; }

    @Override
    public void execute(Player player, PlayerStatsManager statsManager, CombatManager combatManager, MobStatsManager mobManager) {
        if (player.level().isClientSide()) return;
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ServerLevel serverLevel = serverPlayer.serverLevel();

        AdvancedSkillsMod.ElementType currentElement = statsManager.getPlayerElementType(player.getUUID());
        int playerLevel = statsManager.calculateLevelFromXp(statsManager.getPlayerSkillXp(player.getUUID()));

        if (currentElement == AdvancedSkillsMod.ElementType.NONE) {
            player.sendSystemMessage(Component.literal("You have no element attuned for Elemental Blast."));
            return;
        }

        // Skill is used, set it on cooldown
        statsManager.setSkillOnCooldown(player.getUUID(), SKILL_ID, player.level().getGameTime());
        player.sendSystemMessage(Component.literal(getDisplayName() + " on cooldown (" + (COOLDOWN_TICKS/20) + "s)."));


        switch (currentElement) {
            case FIRE:
                executeFireBlast(serverPlayer, serverLevel, playerLevel, combatManager);
                break;
            case ICE:
                executeIceBlast(serverPlayer, serverLevel, playerLevel, combatManager);
                break;
            case LIGHTNING:
                executeLightningBlast(serverPlayer, serverLevel, playerLevel, combatManager);
                break;
            case POISON:
                executePoisonBlast(serverPlayer, serverLevel, playerLevel, combatManager);
                break;
        }
    }

    private void executeFireBlast(ServerPlayer player, ServerLevel level, int playerLevel, CombatManager combatManager) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();
        float damage = 3.0F + playerLevel * 0.2F; // Example scaling

        for (int i = 1; i <= 4; i++) { // Check points along the cone
            Vec3 targetPos = eyePos.add(lookVec.scale(i));
            AABB area = new AABB(targetPos.subtract(1.5, 1.5, 1.5), targetPos.add(1.5, 1.5, 1.5));
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive() && !(e instanceof Player));

            for (LivingEntity entity : entities) {
                // Check line of sight or proximity more accurately if needed
                entity.hurt(level.damageSources().indirectMagic(player, player), damage);
                entity.setSecondsOnFire(3 + playerLevel / 10);
                //combatManager.spawnElementParticles(entity, AdvancedSkillsMod.ElementType.FIRE); // Assuming this method exists
                 level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY(0.5), entity.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.sendSystemMessage(Component.literal("Fire Blast unleashed!").withStyle(ChatFormatting.GOLD));
    }

    private void executeIceBlast(ServerPlayer player, ServerLevel level, int playerLevel, CombatManager combatManager) {
        AABB area = player.getBoundingBox().inflate(4.0D + playerLevel * 0.05D);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive() && !(e instanceof Player));
        float damage = 1.0F + playerLevel * 0.1F;
        int slowDuration = (4 + playerLevel / 10) * 20; // ticks
        int slowAmplifier = 1 + playerLevel / 20;

        for (LivingEntity entity : entities) {
            entity.hurt(level.damageSources().indirectMagic(player, player), damage);
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowDuration, slowAmplifier));
            //combatManager.spawnElementParticles(entity, AdvancedSkillsMod.ElementType.ICE);
            level.sendParticles(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY(0.5), entity.getZ(), 30, 0.6, 0.6, 0.6, 0.05);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
         player.sendSystemMessage(Component.literal("Ice Nova chills your foes!").withStyle(ChatFormatting.AQUA));
    }

    private void executeLightningBlast(ServerPlayer player, ServerLevel level, int playerLevel, CombatManager combatManager) {
        BlockHitResult rayTraceResult = level.clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(15)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 targetPos;

        HitResult entityRayTrace = player.pick(15, 0, false); // Raytrace for entities
        if (entityRayTrace instanceof EntityHitResult && ((EntityHitResult)entityRayTrace).getEntity() instanceof LivingEntity directTarget && directTarget.isAlive()) {
            targetPos = ((EntityHitResult)entityRayTrace).getEntity().position();
        } else {
            targetPos = rayTraceResult.getBlockPos().getCenter(); // Center of the block
        }

        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning != null) {
            lightning.moveTo(targetPos);
            lightning.setVisualOnly(false); // Make it deal damage
            lightning.setCause(player); // Set player as cause for drops/XP
            level.addFreshEntity(lightning); // This handles damage and sound
        }
        //combatManager.spawnElementParticles for general area?
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, targetPos.x, targetPos.y, targetPos.z, 50, 0.5, 1.0, 0.5, 0.1);
        player.sendSystemMessage(Component.literal("Lightning strikes!").withStyle(ChatFormatting.YELLOW));
    }

    private void executePoisonBlast(ServerPlayer player, ServerLevel level, int playerLevel, CombatManager combatManager) {
        // Create a lingering cloud effect - this is more complex, involves AreaEffectCloudEntity
        // For simplicity, let's do an AoE application of poison
        AABB area = player.getBoundingBox().inflate(3.0D + playerLevel * 0.05D);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive() && !(e instanceof Player));
        int poisonDuration = (5 + playerLevel / 8) * 20;
        int poisonAmplifier = 0 + playerLevel / 25;

        for (LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDuration, poisonAmplifier));
            //combatManager.spawnElementParticles(entity, AdvancedSkillsMod.ElementType.POISON);
            level.sendParticles(ParticleTypes.SNEEZE, entity.getX(), entity.getY(0.5), entity.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1.0F, 0.5F);
        player.sendSystemMessage(Component.literal("Poison Cloud erupts!").withStyle(ChatFormatting.DARK_GREEN));
    }
}
