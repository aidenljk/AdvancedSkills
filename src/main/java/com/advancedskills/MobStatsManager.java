package com.advancedskills;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class MobStatsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MobStatsManager.class);

    // Mob data
    private final Map<UUID, Integer> entityLevels = new HashMap<>();
    private final Random random = new Random();

    // NBT Keys for mobs
    public static final String MONSTER_LEVEL_KEY = "AdvancedSkillsMonsterLevel";
    public static final String MONSTER_LEVEL_SET_KEY = "AdvancedSkillsMonsterLevelSet";

    // Attribute Modifier UUIDs for mobs
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("d34d1515-5d6e-4c5d-a91f-6c35d85c21d1");
    private static final UUID DAMAGE_MODIFIER_ID = UUID.fromString("d34d1515-5d6e-4c5d-a91f-6c35d85c21d2");
    // Note: No need for a general speed modifier ID here if it's randomUUID per application in applyLevelAttributesToMob

    // Level Tiers - mob related for display and stats, can be here or passed from AdvancedSkillsMod
    public static final String[] LEVEL_TIERS = {
        "微弱(0-20)", "普通(21-40)", "强大(41-60)", "精英(61-80)", "传奇(81-100)"
    };

    private final AdvancedSkillsMod mod; // Reference to main mod class if needed (e.g. for config access later)

    public MobStatsManager(AdvancedSkillsMod mod) {
        this.mod = mod; // Store reference if needed, or remove if not used
        LOGGER.info("MobStatsManager initialized");
    }

    public void handleMobJoinWorld(Monster monster) {
        if (monster.level().isClientSide()) return;

        UUID entityId = monster.getUUID();
        CompoundTag persistentData = monster.getPersistentData();

        if (entityLevels.containsKey(entityId)) { // Check cache first (e.g., from /summon command)
            int commandSetLevel = entityLevels.get(entityId);
            LOGGER.debug("[MobJoin] Applying cached level {} to mob {}", commandSetLevel, entityId);
            persistentData.putBoolean(MONSTER_LEVEL_SET_KEY, true);
            persistentData.putInt(MONSTER_LEVEL_KEY, commandSetLevel);
            applyLevelAttributesToMob(monster, commandSetLevel);
            setMobDisplayName(monster, commandSetLevel);
            return;
        }

        if (persistentData.contains(MONSTER_LEVEL_SET_KEY) && persistentData.getBoolean(MONSTER_LEVEL_SET_KEY)) {
            int storedLevel = persistentData.getInt(MONSTER_LEVEL_KEY);
            LOGGER.debug("[MobJoin] Mob {} already has NBT level {}.", entityId, storedLevel);
            entityLevels.put(entityId, storedLevel); // Ensure cache is updated
            applyLevelAttributesToMob(monster, storedLevel); // Re-apply attributes to be safe
            setMobDisplayName(monster, storedLevel);
            return;
        }

        int randomLevel = calculateRandomEntityLevel();
        LOGGER.debug("[MobJoin] Assigning random level {} to new mob {}", randomLevel, entityId);
        persistentData.putBoolean(MONSTER_LEVEL_SET_KEY, true);
        persistentData.putInt(MONSTER_LEVEL_KEY, randomLevel);
        entityLevels.put(entityId, randomLevel);
        applyLevelAttributesToMob(monster, randomLevel);
        setMobDisplayName(monster, randomLevel);
    }

    public void setSummonedMobLevel(Monster monster, int level) {
        // Called by the summon command BEFORE the mob is added to the world
        // This ensures its level is set in NBT and cached.
        CompoundTag persistentData = monster.getPersistentData();
        persistentData.putBoolean(MONSTER_LEVEL_SET_KEY, true);
        persistentData.putInt(MONSTER_LEVEL_KEY, level);
        entityLevels.put(monster.getUUID(), level); // Cache it immediately
        LOGGER.info("[MobSummon] Pre-set level {} for mob {}", level, monster.getUUID());
        // Attributes and display name will be applied by handleMobJoinWorld when it spawns.
    }


    public void applyLevelAttributesToMob(Monster monster, int level) {
        LOGGER.debug("Applying level {} attributes to mob {}", level, monster.getName().getString());

        double healthMultiplier, damageMultiplier;
        if (level <= 20) {
            healthMultiplier = 1.0 + (level * 0.02); damageMultiplier = 1.0 + (level * 0.02);
        } else if (level <= 50) {
            healthMultiplier = 1.4 + ((level - 20) * 0.04); damageMultiplier = 1.4 + ((level - 20) * 0.04);
        } else if (level <= 80) {
            healthMultiplier = 2.6 + ((level - 50) * 0.06); damageMultiplier = 2.6 + ((level - 50) * 0.06);
        } else {
            healthMultiplier = 4.4 + ((level - 80) * 0.1); damageMultiplier = 4.4 + ((level - 80) * 0.1);
        }

        String entityTypeName = EntityType.getKey(monster.getType()).getPath().toLowerCase();
        if (entityTypeName.contains("zombie") || entityTypeName.contains("brute") || entityTypeName.contains("piglin")) {
            healthMultiplier *= 1.3; damageMultiplier *= 0.9;
        } else if (entityTypeName.contains("skeleton") || entityTypeName.contains("stray") || entityTypeName.contains("pillager")) {
            healthMultiplier *= 0.8; damageMultiplier *= 1.4;
        } else if (entityTypeName.contains("creeper")) {
            healthMultiplier *= 0.9; damageMultiplier *= 1.5; // Consider creeper explosion radius/power too
        } else if (entityTypeName.contains("spider") || entityTypeName.contains("cave_spider")) {
            damageMultiplier *= 1.1;
            monster.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(new AttributeModifier(UUID.randomUUID(), "AS_SpeedBoost", Math.min(level * 0.005, 0.3), AttributeModifier.Operation.MULTIPLY_BASE));
        } else if (entityTypeName.contains("witch")) {
            healthMultiplier *= 1.1; damageMultiplier *= 1.2;
        }

        healthMultiplier = Math.min(healthMultiplier, 10.0);
        damageMultiplier = Math.min(damageMultiplier, 10.0);

        monster.getAttribute(Attributes.MAX_HEALTH).removeModifier(HEALTH_MODIFIER_ID); // Remove before adding
        monster.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(new AttributeModifier(HEALTH_MODIFIER_ID, "AS_HealthBoost", healthMultiplier - 1.0, AttributeModifier.Operation.MULTIPLY_BASE));

        if (monster.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            monster.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(DAMAGE_MODIFIER_ID); // Remove before adding
            monster.getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(new AttributeModifier(DAMAGE_MODIFIER_ID, "AS_DamageBoost", damageMultiplier - 1.0, AttributeModifier.Operation.MULTIPLY_BASE));
        }

        monster.setHealth(monster.getMaxHealth()); // Heal to full

        if (level >= 50 && random.nextFloat() < 0.5f) addRandomBuffToMonster(monster, level);
        if (level >= 75) monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, (level >= 90) ? 1 : 0, false, false));

        LOGGER.debug("Mob {} got {}x HP, {}x DMG", monster.getName().getString(), String.format("%.2f", healthMultiplier), String.format("%.2f", damageMultiplier));
    }

    private void addRandomBuffToMonster(Monster monster, int level) {
        int effectChoice = random.nextInt(5);
        int duration = 12000; // 10 minutes
        int amplifier = level >= 80 ? 1 : 0;
        switch (effectChoice) {
            case 0: monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, amplifier, false, false)); break;
            case 1: monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier, false, false)); break;
            case 2: monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, amplifier, false, false)); break;
            case 3: monster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier, false, false)); break;
            case 4: monster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, amplifier, false, false)); break;
        }
    }

    private int calculateRandomEntityLevel() {
        double chance = random.nextDouble();
        int level;

        if (chance < 0.60) { // 60% chance for Tier 1 (Levels 1-40)
            // Skews towards lower levels within this tier
            level = 1 + (int) (40 * Math.pow(random.nextDouble(), 2.5)); // Increased exponent for more skew
        } else if (chance < 0.90) { // 30% chance for Tier 2 (Levels 41-75)
            // More linear distribution within this tier
            level = 41 + random.nextInt(35); // 41 to 75 (35 possible values)
        } else { // 10% chance for Tier 3 (Levels 76-100)
            // More linear distribution within this tier
            level = 76 + random.nextInt(25); // 76 to 100 (25 possible values)
        }
        return Math.max(1, Math.min(100, level)); // Ensure it's within 1-100
    }

    private void setMobDisplayName(Monster monster, int level) {
        String originalName = EntityType.getKey(monster.getType()).getPath();
        Component newName = Component.literal("Lv." + level + " " + originalName)
                                .withStyle(getLevelTextColor(level));
        monster.setCustomName(newName);
        monster.setCustomNameVisible(true);
    }

    public ChatFormatting getLevelTextColor(int level) {
        if (level >= 81) return ChatFormatting.DARK_RED;
        if (level >= 61) return ChatFormatting.RED;
        if (level >= 41) return ChatFormatting.GOLD;
        if (level >= 21) return ChatFormatting.YELLOW;
        if (level >= 10) return ChatFormatting.GREEN;
        return ChatFormatting.GRAY;
    }

    public int getMobLevelFromNBTOrCache(Monster monster) {
        CompoundTag persistentData = monster.getPersistentData();
        if (persistentData.contains(MONSTER_LEVEL_KEY)) {
            return persistentData.getInt(MONSTER_LEVEL_KEY);
        }
        return entityLevels.getOrDefault(monster.getUUID(), 0);
    }

    public void removeMobData(UUID mobId) {
        entityLevels.remove(mobId);
    }

    // Needed for PlayerStatsManager to update kill stats correctly.
    // Could also be static in AdvancedSkillsMod if preferred and passed around.
    public static String getLevelTierName(int level) {
        if (level <= 20) return LEVEL_TIERS[0];
        if (level <= 40) return LEVEL_TIERS[1];
        if (level <= 60) return LEVEL_TIERS[2];
        if (level <= 80) return LEVEL_TIERS[3];
        return LEVEL_TIERS[4];
    }
}
