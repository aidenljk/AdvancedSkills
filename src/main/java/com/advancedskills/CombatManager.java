package com.advancedskills;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombatManager.class);
    private final Random random = new Random();

    // Dependencies
    private final AdvancedSkillsMod mod; // For accessing other managers if needed, or global settings
    private final PlayerStatsManager playerStatsManager;

    // Elemental Effect Constants (can be moved from AdvancedSkillsMod)
    private static final float FIRE_BASE_DURATION = 4.0F;
    private static final float FIRE_DURATION_PER_LEVEL = 0.15F;
    private static final float FIRE_BASE_DAMAGE = 1.0F;
    private static final float FIRE_DAMAGE_PER_LEVEL = 0.1F;
    private static final float ICE_BASE_DURATION = 3.0F;
    private static final float ICE_DURATION_PER_LEVEL = 0.08F;
    private static final float ICE_BASE_SLOW_AMPLIFIER = 1.0F;
    private static final float ICE_SLOW_AMPLIFIER_PER_LEVEL = 0.06F;
    private static final float LIGHTNING_BASE_CHANCE = 0.2F;
    private static final float LIGHTNING_CHANCE_PER_LEVEL = 0.012F;
    private static final float LIGHTNING_CHAIN_RADIUS = 5.0F; // Reduced from 20 for safety
    private static final float LIGHTNING_BASE_DAMAGE = 3.0F;
    private static final float LIGHTNING_DAMAGE_PER_LEVEL = 0.3F;
    private static final float POISON_BASE_DURATION = 4.0F;
    private static final float POISON_DURATION_PER_LEVEL = 0.12F;
    private static final float POISON_BASE_AMPLIFIER = 1.0F;
    private static final float POISON_AMPLIFIER_PER_LEVEL = 0.04F;

    // Weapon Damage Constants (can be moved from AdvancedSkillsMod)
    private static final float BASE_EXTRA_ARROW_DAMAGE = 0.5F;
    private static final float ARROW_DAMAGE_PER_LEVEL = 0.6F;
    private static final float MAX_EXTRA_ARROW_DAMAGE = 75.0F;
    private static final float BASE_EXTRA_SWORD_DAMAGE = 0.5F;
    private static final float SWORD_DAMAGE_PER_LEVEL = 0.45F;
    private static final float MAX_EXTRA_SWORD_DAMAGE = 50.0F;

    // Crit System Constants (can be moved from AdvancedSkillsMod)
    private static final float BASE_CRIT_CHANCE = 0.05F;
    private static final float CRIT_CHANCE_PER_LEVEL = 0.005F;
    private static final float BOW_SPECIALTY_CRIT_BONUS = 0.15F;
    private static final float SWORD_SPECIALTY_CRIT_BONUS = 0.15F;
    private static final float FULL_DRAW_CRIT_BONUS = 0.25F;
    private static final float BASE_CRIT_DAMAGE_MULTIPLIER = 1.5F; // Renamed for clarity
    private static final float CRIT_DAMAGE_MULTIPLIER_PER_LEVEL = 0.02F; // Renamed
    private static final float BOW_SPECIALTY_CRIT_DAMAGE_BONUS = 0.5F;
    private static final float SWORD_SPECIALTY_CRIT_DAMAGE_BONUS = 0.5F;

    // Combo System
    public static class ComboTracker {
        private UUID targetId;
        private int comboCount;
        private long lastHitTime;
        private static final int COMBO_WINDOW_TICKS = 40; // 2 seconds
        private static final int MAX_COMBO = 5;

        public ComboTracker(UUID targetId) {
            this.targetId = targetId;
            this.comboCount = 1;
            this.lastHitTime = System.currentTimeMillis();
        }
        public UUID getTargetId() { return targetId; }
        public int getComboCount() { return comboCount; }
        public void incrementCombo() {
            comboCount = Math.min(comboCount + 1, MAX_COMBO);
            lastHitTime = System.currentTimeMillis();
        }
        public boolean isExpired() {
            return System.currentTimeMillis() - lastHitTime > COMBO_WINDOW_TICKS * 50; // 50ms per tick
        }
    }
    private final Map<UUID, ComboTracker> playerCombos = new ConcurrentHashMap<>();
    private static final float COMBO_CRIT_BONUS_PER_HIT = 0.1F;


    public CombatManager(AdvancedSkillsMod mod, PlayerStatsManager playerStatsManager) {
        this.mod = mod;
        this.playerStatsManager = playerStatsManager;
        LOGGER.info("CombatManager initialized");
    }

    public void handlePlayerTick(Player player) {
        if (player.level().isClientSide()) return;
        // Combo expiry
        UUID playerId = player.getUUID();
        ComboTracker combo = playerCombos.get(playerId);
        if (combo != null && combo.isExpired()) {
            playerCombos.remove(playerId);
            // player.sendSystemMessage(Component.literal("Combo expired.").withStyle(ChatFormatting.DARK_GRAY)); // Optional message
        }
    }

    // --- Main Damage Event Handlers (to be called by AdvancedSkillsMod) ---

    public void handleLivingHurt(LivingHurtEvent event, Player player, LivingEntity target) {
        // This method assumes 'player' is the attacker and 'target' is the LivingEntity being hurt.
        // It also assumes client-side check and non-player attacker check already happened.

        if (event.getSource().getDirectEntity() instanceof Arrow) {
            // Arrow damage is handled by handleArrowImpact for better context
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof SwordItem) && heldItem.getItem() != Items.TRIDENT) {
            return; // Only process for swords or tridents (example melee weapons)
        }

        UUID playerId = player.getUUID();
        int playerLevel = playerStatsManager.calculateLevelFromXp(playerStatsManager.getPlayerSkillXp(playerId));
        AdvancedSkillsMod.ElementType elementType = playerStatsManager.getPlayerElementType(playerId);
        AdvancedSkillsMod.WeaponSpecialty weaponSpecialty = playerStatsManager.getPlayerWeaponSpecialty(playerId);

        processMeleeDamage(event, player, target, playerLevel, weaponSpecialty, elementType);
    }

    public void handleArrowImpact(ProjectileImpactEvent event, Arrow arrow, Player shooter, LivingEntity target) {
        // This method assumes 'shooter' is the player, 'arrow' is the projectile, and 'target' is hit.

        UUID shooterId = shooter.getUUID();
        int playerLevel = playerStatsManager.calculateLevelFromXp(playerStatsManager.getPlayerSkillXp(shooterId));
        AdvancedSkillsMod.ElementType elementType = playerStatsManager.getPlayerElementType(shooterId);
        AdvancedSkillsMod.WeaponSpecialty weaponSpecialty = playerStatsManager.getPlayerWeaponSpecialty(shooterId);
        float drawPower = getPowerFromArrow(arrow); // Estimate arrow power

        // Calculate base arrow damage (Minecraft does this, we add bonus)
        // For simplicity, let's assume the event's original damage is what we modify.
        // If not, we might need to calculate it: arrow.getBaseDamage() * drawPower;

        processArrowDamage(event, arrow, shooter, target, playerLevel, weaponSpecialty, elementType, drawPower);
    }

    // --- Damage Processing Logic ---

    private void processMeleeDamage(LivingHurtEvent event, Player player, LivingEntity target,
                                   int playerLevel, AdvancedSkillsMod.WeaponSpecialty specialty, AdvancedSkillsMod.ElementType elementType) {
        UUID playerId = player.getUUID();
        float comboCritBonus = 0.0f;
        ComboTracker combo = playerCombos.get(playerId);

        if (combo != null && !combo.isExpired() && combo.getTargetId().equals(target.getUUID())) {
            combo.incrementCombo();
            comboCritBonus = COMBO_CRIT_BONUS_PER_HIT * (combo.getComboCount() - 1);
            if (combo.getComboCount() > 1) {
                player.sendSystemMessage(Component.translatable("advancedskills.combo", combo.getComboCount()).withStyle(ChatFormatting.GOLD));
            }
        } else {
            playerCombos.put(playerId, new ComboTracker(target.getUUID()));
        }

        float critChance = BASE_CRIT_CHANCE + (CRIT_CHANCE_PER_LEVEL * playerLevel) + comboCritBonus;
        if (specialty == AdvancedSkillsMod.WeaponSpecialty.SWORD) critChance += SWORD_SPECIALTY_CRIT_BONUS;

        boolean isCritical = random.nextFloat() < critChance;
        float extraDamage = Math.min(BASE_EXTRA_SWORD_DAMAGE + (SWORD_DAMAGE_PER_LEVEL * playerLevel), MAX_EXTRA_SWORD_DAMAGE);
        if (specialty == AdvancedSkillsMod.WeaponSpecialty.SWORD) extraDamage *= 1.5f;

        if (isCritical) {
            float critDamageMultiplier = BASE_CRIT_DAMAGE_MULTIPLIER + (CRIT_DAMAGE_MULTIPLIER_PER_LEVEL * playerLevel);
            if (specialty == AdvancedSkillsMod.WeaponSpecialty.SWORD) critDamageMultiplier += SWORD_SPECIALTY_CRIT_DAMAGE_BONUS;
            extraDamage *= critDamageMultiplier;
            player.sendSystemMessage(Component.literal("Critical Melee Hit!").withStyle(ChatFormatting.RED));
            player.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
        }

        event.setAmount(event.getAmount() + extraDamage);
        applyElementalEffect(player, target, elementType, playerLevel, false, specialty);
    }

    private void processArrowDamage(ProjectileImpactEvent impactEvent, Arrow arrow, Player player, LivingEntity target,
                                   int playerLevel, AdvancedSkillsMod.WeaponSpecialty specialty, AdvancedSkillsMod.ElementType elementType, float drawPower) {

        float critChance = BASE_CRIT_CHANCE + (CRIT_CHANCE_PER_LEVEL * playerLevel);
        if (specialty == AdvancedSkillsMod.WeaponSpecialty.BOW) critChance += BOW_SPECIALTY_CRIT_BONUS;
        if (drawPower >= 0.95f) critChance += FULL_DRAW_CRIT_BONUS; // Full draw bonus

        boolean isCritical = random.nextFloat() < critChance;
        float extraDamage = Math.min(BASE_EXTRA_ARROW_DAMAGE + (ARROW_DAMAGE_PER_LEVEL * playerLevel), MAX_EXTRA_ARROW_DAMAGE);
        if (specialty == AdvancedSkillsMod.WeaponSpecialty.BOW) extraDamage *= 1.5f;
        extraDamage *= drawPower; // Scale by draw power

        if (isCritical) {
            float critDamageMultiplier = BASE_CRIT_DAMAGE_MULTIPLIER + (CRIT_DAMAGE_MULTIPLIER_PER_LEVEL * playerLevel);
            if (specialty == AdvancedSkillsMod.WeaponSpecialty.BOW) critDamageMultiplier += BOW_SPECIALTY_CRIT_DAMAGE_BONUS;
            extraDamage *= critDamageMultiplier;
            player.sendSystemMessage(Component.literal("Critical Arrow Hit!").withStyle(ChatFormatting.RED));
            player.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 0.8F, 1.2F);
        }

        // Apply the extra damage. Minecraft handles base arrow damage.
        // We are adding bonus damage on top of what MC will calculate from the arrow entity itself.
        target.hurt(target.damageSources().arrow(arrow, player), extraDamage);
        applyElementalEffect(player, target, elementType, playerLevel, true, specialty);

        // The arrow is consumed or drops, don't cancel the impact event itself unless necessary
    }

    // --- Elemental Effect Application ---
    // This replaces the existing placeholder applyElementalEffect in CombatManager
    public void applyElementalEffect(Player player, LivingEntity target, AdvancedSkillsMod.ElementType elementType, int playerLevel, boolean isRanged, AdvancedSkillsMod.WeaponSpecialty specialty) {
        if (elementType == AdvancedSkillsMod.ElementType.NONE || player == null || !player.isAlive() || target == null || !target.isAlive()) {
            return;
        }

        float elementalBoost = 1.0f;
        // Weapon specialty can boost elemental effects
        if ((isRanged && specialty == AdvancedSkillsMod.WeaponSpecialty.BOW) ||
            (!isRanged && specialty == AdvancedSkillsMod.WeaponSpecialty.SWORD)) {
            elementalBoost = 1.2f; // 20% boost for matching specialty
        }

        switch (elementType) {
            case FIRE:
                applyFireEffect(player, target, playerLevel, elementalBoost);
                break;
            case ICE:
                applyIceEffect(player, target, playerLevel, elementalBoost);
                break;
            case LIGHTNING:
                applyLightningEffect(player, target, playerLevel, elementalBoost);
                break;
            case POISON:
                applyPoisonEffect(player, target, playerLevel, elementalBoost);
                break;
            default:
                LOGGER.debug("No specific elemental effect to apply for type: {}", elementType);
                break;
        }
    }

    private void applyFireEffect(Player player, LivingEntity target, int playerLevel, float elementalBoost) {
        try {
            float fireDuration = (FIRE_BASE_DURATION + (FIRE_DURATION_PER_LEVEL * playerLevel)) * elementalBoost;
            target.setRemainingFireTicks((int)(fireDuration * 20));

            float fireDamage = (FIRE_BASE_DAMAGE + (FIRE_DAMAGE_PER_LEVEL * playerLevel)) * elementalBoost;
            if (fireDamage > 0 && target.isAlive()) {
                target.hurt(target.damageSources().onFire(), fireDamage); // Consider using indirectMagic if appropriate for balance
            }
            if (fireDamage >= 1.0f) { // Adjusted threshold for message
                 player.sendSystemMessage(Component.literal("Target burns for " + String.format("%.1f", fireDamage) + " extra damage!").withStyle(ChatFormatting.GOLD));
            }
            spawnElementParticles(target, AdvancedSkillsMod.ElementType.FIRE);
            LOGGER.debug("Applied Fire effect to {} from {}", target.getName().getString(), player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Error applying fire effect: {}", e.getMessage(), e);
        }
    }

    private void applyIceEffect(Player player, LivingEntity target, int playerLevel, float elementalBoost) {
        try {
            float iceDuration = (ICE_BASE_DURATION + (ICE_DURATION_PER_LEVEL * playerLevel)) * elementalBoost;
            int slowAmplifier = (int)((ICE_BASE_SLOW_AMPLIFIER + (ICE_SLOW_AMPLIFIER_PER_LEVEL * playerLevel)) * elementalBoost);
            slowAmplifier = Math.max(0, Math.min(slowAmplifier, 4)); // Cap amplifier

            if (target.isAlive()) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, (int)(iceDuration * 20), slowAmplifier));
                if (playerLevel >= 30) { // Add mining fatigue at higher levels
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, (int)(iceDuration * 15), (playerLevel >= 60 ? 1 : 0) ));
                }
            }
            if (slowAmplifier >= 1) { // Adjusted threshold
                 player.sendSystemMessage(Component.literal("Target slowed!").withStyle(ChatFormatting.AQUA));
            }
            spawnElementParticles(target, AdvancedSkillsMod.ElementType.ICE);
            LOGGER.debug("Applied Ice effect to {} from {}", target.getName().getString(), player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Error applying ice effect: {}", e.getMessage(), e);
        }
    }

    private void applyLightningEffect(Player player, LivingEntity target, int playerLevel, float elementalBoost) {
        try {
            float lightningChance = (LIGHTNING_BASE_CHANCE + (LIGHTNING_CHANCE_PER_LEVEL * playerLevel)) * elementalBoost;
            lightningChance = Math.min(lightningChance, 0.75f); // Cap chance

            if (random.nextFloat() < lightningChance) {
                if (target.level() != null) {
                    float radius = Math.min(3.0f + (playerLevel / 50.0f), LIGHTNING_CHAIN_RADIUS);
                    net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(target.position().subtract(radius, radius, radius), target.position().add(radius, radius, radius));
                    java.util.List<LivingEntity> nearbyEntities = target.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                        e -> e != target && e != player && e.isAlive() && e instanceof net.minecraft.world.entity.monster.Monster);

                    int maxChain = Math.min(playerLevel >= 50 ? 3 : (playerLevel >= 25 ? 2 : 1), 3);
                    int chainedCount = 0;
                    for (int i = 0; i < Math.min(nearbyEntities.size(), maxChain); i++) {
                        LivingEntity chainTarget = nearbyEntities.get(i);
                        if (chainTarget == null || !chainTarget.isAlive()) continue;
                        float chainDamage = (LIGHTNING_BASE_DAMAGE + (LIGHTNING_DAMAGE_PER_LEVEL * playerLevel)) * elementalBoost;
                        chainTarget.hurt(target.damageSources().indirectMagic(player, player), chainDamage); // Using indirectMagic
                        spawnElementParticles(chainTarget, AdvancedSkillsMod.ElementType.LIGHTNING);
                        chainedCount++;
                    }
                    if (chainedCount > 0) {
                        player.sendSystemMessage(Component.literal("Lightning chains to " + chainedCount + " nearby hostiles!").withStyle(ChatFormatting.YELLOW));
                    }
                }
            }
            // Always apply particle to primary target
            spawnElementParticles(target, AdvancedSkillsMod.ElementType.LIGHTNING);
            LOGGER.debug("Applied Lightning effect to {} from {}", target.getName().getString(), player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Error applying lightning effect: {}", e.getMessage(), e);
        }
    }

    private void applyPoisonEffect(Player player, LivingEntity target, int playerLevel, float elementalBoost) {
        try {
            float poisonDuration = (POISON_BASE_DURATION + (POISON_DURATION_PER_LEVEL * playerLevel)) * elementalBoost;
            int poisonAmplifier = (int)((POISON_BASE_AMPLIFIER + (POISON_AMPLIFIER_PER_LEVEL * playerLevel)) * elementalBoost);
            poisonAmplifier = Math.max(0, Math.min(poisonAmplifier, 3)); // Cap amplifier

            if (target.isAlive()) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, (int)(poisonDuration * 20), poisonAmplifier));
                 if (playerLevel >= 35) { // Add weakness at higher levels
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, (int)(poisonDuration * 15), (playerLevel >= 70 ? 1 : 0)));
                }
            }
             if (poisonAmplifier >= 0) { // Message even for level 0 poison
                 player.sendSystemMessage(Component.literal("Target poisoned!").withStyle(ChatFormatting.DARK_GREEN));
            }
            spawnElementParticles(target, AdvancedSkillsMod.ElementType.POISON);
            LOGGER.debug("Applied Poison effect to {} from {}", target.getName().getString(), player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Error applying poison effect: {}", e.getMessage(), e);
        }
    }

    private void spawnElementParticles(LivingEntity entity, AdvancedSkillsMod.ElementType elementType) {
        if (entity.level().isClientSide()) return; // Particles are sent from server to clients

        net.minecraft.core.particles.ParticleOptions particle = null;
        switch (elementType) {
            case FIRE:
                particle = net.minecraft.core.particles.ParticleTypes.FLAME;
                break;
            case ICE:
                particle = net.minecraft.core.particles.ParticleTypes.SNOWFLAKE;
                break;
            case LIGHTNING:
                // Using SOUL_FIRE_FLAME for a blueish spark, or create custom
                particle = net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME;
                break;
            case POISON:
                particle = net.minecraft.core.particles.ParticleTypes.SNEEZE; // Or item_slime
                break;
            default:
                return;
        }

        if (particle != null && entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            double x = entity.getX();
            double y = entity.getY() + entity.getBbHeight() / 2.0;
            double z = entity.getZ();
            serverLevel.sendParticles(particle, x, y, z, 15, // count
                                     entity.getBbWidth() / 2.0, entity.getBbHeight() / 3.0, entity.getBbWidth() / 2.0, // spread
                                     0.03); // speed
        }
    }

    private float getPowerFromArrow(Arrow arrow) {
        if (arrow == null || arrow.getDeltaMovement() == null) return 1.0F;
        double velocity = arrow.getDeltaMovement().length();
        float power = (float) ((velocity - 0.5) / 2.5); // Approximate
        return Math.max(0.0F, Math.min(1.0F, power));
    }
}
