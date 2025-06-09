package com.advancedskills;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerStatsManager.class);

    // Player data maps
    private final Map<UUID, Integer> playerSkillXp = new HashMap<>();
    private final Map<UUID, AdvancedSkillsMod.ElementType> playerElementTypes = new HashMap<>();
    private final Map<UUID, AdvancedSkillsMod.WeaponSpecialty> playerWeaponSpecialties = new HashMap<>();
    private final Map<UUID, Long> specialtyCooldowns = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> playerKillStats = new HashMap<>();
    private final Map<UUID, Map<String, Long>> playerSkillCooldowns = new HashMap<>(); // PlayerID -> SkillID -> LastUsedGameTime (in game ticks)

    // NBT Keys
    private static final String SKILL_XP_KEY = "AdvancedSkillsXP";
    private static final String ELEMENT_TYPE_KEY = "AdvancedSkillsElementType";
    private static final String WEAPON_SPECIALTY_KEY = "AdvancedSkillsWeaponSpecialty";
    private static final String KILLS_STATS_KEY = "AdvancedSkillsKillStats";
    private static final String SPECIALTY_COOLDOWN_KEY = "AdvancedSkillsSpecialtyCooldown"; // Corrected key name
    private static final String LAST_APPLIED_LEVEL_KEY = "AdvancedSkillsLastLevel";


    // Player Attribute Constants
    private static final UUID PLAYER_HEALTH_MODIFIER_ID = UUID.fromString("d34d1515-5d6e-4c5d-a91f-6c35d85c21d3");
    private static final UUID PLAYER_ATTACK_MODIFIER_ID = UUID.fromString("d34d1515-5d6e-4c5d-a91f-6c35d85c21d4");
    private static final UUID PLAYER_SPEED_MODIFIER_ID = UUID.fromString("d34d1515-5d6e-4c5d-a91f-6c35d85c21d5");
    private static final UUID PLAYER_ARMOR_MODIFIER_ID = UUID.fromString("d34d1515-5d6e-4c5d-a91f-6c35d85c21d6");

    private static final float PLAYER_HEALTH_PER_LEVEL = 0.01F;
    private static final float PLAYER_MAX_HEALTH_BOOST = 1.0F;
    private static final float PLAYER_ATTACK_PER_LEVEL = 0.01F;
    private static final float PLAYER_MAX_ATTACK_BOOST = 1.0F;
    private static final float PLAYER_ARMOR_PER_LEVEL = 0.01F;
    private static final float PLAYER_MAX_ARMOR_BOOST = 0.5F;
    private static final float PLAYER_SPEED_PER_LEVEL = 0.002F;
    private static final float PLAYER_MAX_SPEED_BOOST = 0.3F;

    // Weapon Specialty Cooldown
    private static final int SPECIALTY_COOLDOWN_SECONDS = 300; // 5 minutes


    public PlayerStatsManager() {
        LOGGER.info("PlayerStatsManager initialized");
    }

    // --- XP and Level Methods ---
    public void addPlayerSkillXp(Player player, int xpAmount) {
        UUID playerId = player.getUUID();
        int currentXp = playerSkillXp.getOrDefault(playerId, loadPlayerSkillXp(player));
        int newXp = currentXp + xpAmount;

        int oldLevel = calculateLevelFromXp(currentXp);
        int newLevel = calculateLevelFromXp(newXp);

        playerSkillXp.put(playerId, newXp);

        if (player instanceof ServerPlayer) {
            savePlayerSkillXp(player, newXp);
            if (newLevel > oldLevel) {
                player.sendSystemMessage(Component.literal("Congratulations! Skill level increased to " + newLevel)
                        .withStyle(ChatFormatting.GOLD));
                player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
                // Attribute update will be handled by handlePlayerTick or onPlayerLoggedIn
            }
        }
        LOGGER.debug("Player {} gained {} XP. Total XP: {}, Level: {}", player.getName().getString(), xpAmount, newXp, newLevel);
    }

    public int calculateLevelFromXp(int xp) {
        return Math.min(100, (int) Math.sqrt(xp / 10.0)); // Ensure float division
    }

    public int calculateXpForLevel(int level) {
        return level * level * 10; // Matches the division by 10 in calculateLevelFromXp
    }

    public int loadPlayerSkillXp(Player player) {
        return player.getPersistentData().getInt(SKILL_XP_KEY);
    }

    public void savePlayerSkillXp(Player player, int xp) {
        player.getPersistentData().putInt(SKILL_XP_KEY, xp);
    }

    public int getPlayerSkillXp(UUID playerId) {
        return playerSkillXp.getOrDefault(playerId, 0);
    }

    // --- Elemental Type Methods ---
    public void cycleElementType(Player player) {
        UUID playerId = player.getUUID();
        int playerLevel = calculateLevelFromXp(playerSkillXp.getOrDefault(playerId, 0));
        AdvancedSkillsMod.ElementType currentElement = playerElementTypes.getOrDefault(playerId, AdvancedSkillsMod.ElementType.NONE);
        AdvancedSkillsMod.ElementType nextElement = currentElement.getNext(playerLevel); // Moved logic to enum

        playerElementTypes.put(playerId, nextElement);
        if (player instanceof ServerPlayer serverPlayer) {
            saveElementType(serverPlayer, nextElement);
        }
        player.sendSystemMessage(
            Component.translatable("advancedskills.element.switch", Component.translatable(nextElement.getTranslationKey()))
                .withStyle(nextElement.getColor())
        );
        player.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);
    }

    public void saveElementType(ServerPlayer player, AdvancedSkillsMod.ElementType elementType) {
        player.getPersistentData().putInt(ELEMENT_TYPE_KEY, elementType.ordinal());
    }

    public AdvancedSkillsMod.ElementType loadElementType(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(ELEMENT_TYPE_KEY)) {
            int ordinal = persistentData.getInt(ELEMENT_TYPE_KEY);
            if (ordinal >= 0 && ordinal < AdvancedSkillsMod.ElementType.values().length) {
                return AdvancedSkillsMod.ElementType.values()[ordinal];
            }
        }
        return AdvancedSkillsMod.ElementType.NONE;
    }

    public AdvancedSkillsMod.ElementType getPlayerElementType(UUID playerId) {
        return playerElementTypes.getOrDefault(playerId, AdvancedSkillsMod.ElementType.NONE);
    }

    // --- Weapon Specialty Methods ---
    public void cycleWeaponSpecialty(Player player) {
        UUID playerId = player.getUUID();
        long currentTime = player.level().getGameTime();
        long lastSwitchTime = specialtyCooldowns.getOrDefault(playerId, 0L);

        if ((currentTime - lastSwitchTime) / 20 < SPECIALTY_COOLDOWN_SECONDS && lastSwitchTime != 0) {
            long remaining = SPECIALTY_COOLDOWN_SECONDS - ((currentTime - lastSwitchTime) / 20);
            player.sendSystemMessage(Component.literal("You can switch specialty in " + remaining + " seconds.").withStyle(ChatFormatting.RED));
            return;
        }

        int playerLevel = calculateLevelFromXp(playerSkillXp.getOrDefault(playerId, 0));
        AdvancedSkillsMod.WeaponSpecialty currentSpecialty = playerWeaponSpecialties.getOrDefault(playerId, AdvancedSkillsMod.WeaponSpecialty.NONE);
        AdvancedSkillsMod.WeaponSpecialty nextSpecialty = currentSpecialty.getNext(playerLevel); // Moved logic to enum

        playerWeaponSpecialties.put(playerId, nextSpecialty);
        specialtyCooldowns.put(playerId, currentTime);

        if (player instanceof ServerPlayer serverPlayer) {
            saveWeaponSpecialty(serverPlayer, nextSpecialty);
            saveSpecialtyCooldown(serverPlayer, currentTime);
        }
        player.sendSystemMessage(
            Component.translatable("advancedskills.specialty.switch", Component.translatable(nextSpecialty.getTranslationKey()))
                .withStyle(nextSpecialty.getColor())
        );
        player.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);
        applyPlayerLevelAttributes(player); // Re-apply attributes as specialty might affect them
    }

    public void saveWeaponSpecialty(ServerPlayer player, AdvancedSkillsMod.WeaponSpecialty weaponSpecialty) {
        player.getPersistentData().putInt(WEAPON_SPECIALTY_KEY, weaponSpecialty.ordinal());
    }

    public AdvancedSkillsMod.WeaponSpecialty loadWeaponSpecialty(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(WEAPON_SPECIALTY_KEY)) {
            int ordinal = persistentData.getInt(WEAPON_SPECIALTY_KEY);
             if (ordinal >= 0 && ordinal < AdvancedSkillsMod.WeaponSpecialty.values().length) {
                return AdvancedSkillsMod.WeaponSpecialty.values()[ordinal];
            }
        }
        return AdvancedSkillsMod.WeaponSpecialty.NONE;
    }

    public void saveSpecialtyCooldown(ServerPlayer player, Long cooldownTimeTicks) {
        player.getPersistentData().putLong(SPECIALTY_COOLDOWN_KEY, cooldownTimeTicks);
    }

    public Long loadSpecialtyCooldown(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(SPECIALTY_COOLDOWN_KEY)) {
            return persistentData.getLong(SPECIALTY_COOLDOWN_KEY);
        }
        return 0L; // Default to 0 if not found, meaning no cooldown active
    }

    public AdvancedSkillsMod.WeaponSpecialty getPlayerWeaponSpecialty(UUID playerId) {
        return playerWeaponSpecialties.getOrDefault(playerId, AdvancedSkillsMod.WeaponSpecialty.NONE);
    }

    // --- Attribute Methods ---
    public void applyPlayerLevelAttributes(Player player) {
        if (player.level().isClientSide()) return; // Attributes are server-side

        int level = calculateLevelFromXp(playerSkillXp.getOrDefault(player.getUUID(), 0));
        LOGGER.debug("Applying attributes for player {} at level {}", player.getName().getString(), level);

        removePlayerAttributeModifiers(player); // Clear existing modifiers from this mod

        if (level <= 0) return;

        float healthBoost = Math.min(level * PLAYER_HEALTH_PER_LEVEL, PLAYER_MAX_HEALTH_BOOST);
        float attackBoost = Math.min(level * PLAYER_ATTACK_PER_LEVEL, PLAYER_MAX_ATTACK_BOOST);
        float armorBoost = Math.min(level * PLAYER_ARMOR_PER_LEVEL, PLAYER_MAX_ARMOR_BOOST);
        float speedBoost = Math.min(level * PLAYER_SPEED_PER_LEVEL, PLAYER_MAX_SPEED_BOOST);

        AdvancedSkillsMod.WeaponSpecialty specialty = getPlayerWeaponSpecialty(player.getUUID());
        if (specialty == AdvancedSkillsMod.WeaponSpecialty.BOW) {
            attackBoost *= 1.1f; // Bow specialty: 10% attack boost
            speedBoost *= 1.1f;  // Bow specialty: 10% speed boost
        } else if (specialty == AdvancedSkillsMod.WeaponSpecialty.SWORD) {
            attackBoost *= 1.15f; // Sword specialty: 15% attack boost
            armorBoost *= 1.1f;  // Sword specialty: 10% armor boost
        }

        // Apply modifiers
        if (healthBoost > 0) player.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(new AttributeModifier(PLAYER_HEALTH_MODIFIER_ID, "AS Health Boost", healthBoost, AttributeModifier.Operation.MULTIPLY_BASE));
        if (attackBoost > 0) player.getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(new AttributeModifier(PLAYER_ATTACK_MODIFIER_ID, "AS Attack Boost", attackBoost, AttributeModifier.Operation.MULTIPLY_BASE));
        if (armorBoost > 0) player.getAttribute(Attributes.ARMOR).addTransientModifier(new AttributeModifier(PLAYER_ARMOR_MODIFIER_ID, "AS Armor Boost", armorBoost, AttributeModifier.Operation.MULTIPLY_BASE));
        if (speedBoost > 0) player.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(new AttributeModifier(PLAYER_SPEED_MODIFIER_ID, "AS Speed Boost", speedBoost, AttributeModifier.Operation.MULTIPLY_BASE));

        LOGGER.debug("Applied: Health +{}%, Attack +{}%, Armor +{}%, Speed +{}% for {}",
            String.format("%.2f", healthBoost * 100), String.format("%.2f", attackBoost * 100),
            String.format("%.2f", armorBoost * 100), String.format("%.2f", speedBoost * 100), player.getName().getString());

        // Heal player to new max health if it increased
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
    }

    public void removePlayerAttributeModifiers(Player player) {
        player.getAttribute(Attributes.MAX_HEALTH).removeModifier(PLAYER_HEALTH_MODIFIER_ID);
        player.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(PLAYER_ATTACK_MODIFIER_ID);
        player.getAttribute(Attributes.ARMOR).removeModifier(PLAYER_ARMOR_MODIFIER_ID);
        player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(PLAYER_SPEED_MODIFIER_ID);
        LOGGER.debug("Removed attribute modifiers for player {}", player.getName().getString());
    }

    // --- Kill Stats Methods ---
    public void updateKillStats(Player player, int monsterLevel, String[] levelTiers) {
        UUID playerId = player.getUUID();
        Map<String, Integer> killStats = playerKillStats.computeIfAbsent(playerId, id -> {
            Map<String, Integer> newStats = new HashMap<>();
            if (player instanceof ServerPlayer) loadKillStats((ServerPlayer) player, newStats, levelTiers);
            return newStats;
        });
        String tier = getLevelTier(monsterLevel, levelTiers);
        killStats.put(tier, killStats.getOrDefault(tier, 0) + 1);
        if (player instanceof ServerPlayer) saveKillStats((ServerPlayer) player, killStats);
    }

    public void loadKillStats(ServerPlayer player, Map<String, Integer> stats, String[] levelTiers) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(KILLS_STATS_KEY)) {
            CompoundTag killsTag = persistentData.getCompound(KILLS_STATS_KEY);
            for (String tier : levelTiers) { // Use passed levelTiers
                if (killsTag.contains(tier)) {
                    stats.put(tier, killsTag.getInt(tier));
                } else {
                    stats.put(tier, 0); // Ensure all tiers are present
                }
            }
        } else {
            for (String tier : levelTiers) { // Initialize if no NBT tag
                stats.put(tier, 0);
            }
        }
    }

    public void saveKillStats(ServerPlayer player, Map<String, Integer> stats) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag killsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            killsTag.putInt(entry.getKey(), entry.getValue());
        }
        persistentData.put(KILLS_STATS_KEY, killsTag);
    }

    public Map<String, Integer> getPlayerKillStats(UUID playerId) {
        return playerKillStats.getOrDefault(playerId, new HashMap<>());
    }

    public String getLevelTier(int level, String[] levelTiers) {
        // Example: levelTiers = {"Weak (0-20)", "Normal (21-40)", ...}
        // This logic assumes tier boundaries are regular. A more robust way would be to parse from levelTiers.
        // For simplicity, using the original logic if LEVEL_TIERS was like: "TierName(min-max)"
        // The provided LEVEL_TIERS in AdvancedSkillsMod is:
        // "微弱(0-20)", "普通(21-40)", "强大(41-60)", "精英(61-80)", "传奇(81-100)"
        // This implies index calculation is fine.
        if (level <= 20) return levelTiers[0];
        if (level <= 40) return levelTiers[1];
        if (level <= 60) return levelTiers[2];
        if (level <= 80) return levelTiers[3];
        return levelTiers[4]; // For levels > 80 or up to 100
    }

    // --- Skill Cooldown Methods ---
    public boolean isSkillReady(UUID playerId, String skillId, int cooldownTicks, long currentWorldTime) {
        Map<String, Long> skillCooldowns = playerSkillCooldowns.get(playerId);
        if (skillCooldowns == null) {
            return true; // No cooldowns recorded for this player yet
        }
        long lastUsedTime = skillCooldowns.getOrDefault(skillId, 0L);
        return (currentWorldTime - lastUsedTime) >= cooldownTicks;
    }

    public void setSkillOnCooldown(UUID playerId, String skillId, long currentWorldTime) {
        playerSkillCooldowns.computeIfAbsent(playerId, k -> new HashMap<>()).put(skillId, currentWorldTime);
    }

    // Optional: Method to get remaining cooldown for display
    public long getRemainingCooldownTicks(UUID playerId, String skillId, int cooldownTicks, long currentWorldTime) {
        Map<String, Long> skillCooldowns = playerSkillCooldowns.get(playerId);
        if (skillCooldowns == null) {
            return 0;
        }
        long lastUsedTime = skillCooldowns.getOrDefault(skillId, 0L);
        long timePassed = currentWorldTime - lastUsedTime;
        if (timePassed >= cooldownTicks) {
            return 0;
        }
        return cooldownTicks - timePassed;
    }

    // --- Event-like handlers to be called by AdvancedSkillsMod ---
    public void handlePlayerLogin(Player player) {
        if (player.level().isClientSide()) return;

        ServerPlayer serverPlayer = (ServerPlayer) player;
        UUID playerId = player.getUUID();
        LOGGER.info("Player {} logged in. Loading data.", player.getName().getString());

        // Load XP and update map
        int xp = loadPlayerSkillXp(player);
        playerSkillXp.put(playerId, xp);
        int level = calculateLevelFromXp(xp); // Calculate level after loading XP

        // Load Element Type
        AdvancedSkillsMod.ElementType elementType = loadElementType(player);
        playerElementTypes.put(playerId, elementType);

        // Load Weapon Specialty
        AdvancedSkillsMod.WeaponSpecialty specialty = loadWeaponSpecialty(player);
        playerWeaponSpecialties.put(playerId, specialty);

        // Load Specialty Cooldown
        long cooldown = loadSpecialtyCooldown(player);
        specialtyCooldowns.put(playerId, cooldown);

        // Load Kill Stats
        Map<String, Integer> currentKillStats = playerKillStats.computeIfAbsent(playerId, id -> new HashMap<>());
        loadKillStats(serverPlayer, currentKillStats, AdvancedSkillsMod.LEVEL_TIERS); // Assumes AdvancedSkillsMod.LEVEL_TIERS is accessible

        // Apply attributes based on loaded stats
        applyPlayerLevelAttributes(player);
        serverPlayer.getPersistentData().putInt(LAST_APPLIED_LEVEL_KEY, level);


        player.sendSystemMessage(Component.literal("Welcome back! Your skill level is: " + level).withStyle(ChatFormatting.GREEN));
        if (elementType != AdvancedSkillsMod.ElementType.NONE) {
            player.sendSystemMessage(Component.literal("Current Element: " + Component.translatable(elementType.getTranslationKey()).getString()).withStyle(elementType.getColor()));
        }
        if (specialty != AdvancedSkillsMod.WeaponSpecialty.NONE) {
            player.sendSystemMessage(Component.literal("Current Specialty: " + Component.translatable(specialty.getTranslationKey()).getString()).withStyle(specialty.getColor()));
        }
    }

    public void handlePlayerTick(Player player) {
        if (player.level().isClientSide() || player.tickCount % 20 != 0) return;

        UUID playerId = player.getUUID();
        int currentXp = playerSkillXp.getOrDefault(playerId, 0); // Use map data
        int currentLevel = calculateLevelFromXp(currentXp);

        CompoundTag persistentData = player.getPersistentData();
        // Ensure "AdvancedSkillsLastLevel" is initialized if not present, e.g. on first check
        int lastAppliedLevel = persistentData.contains(LAST_APPLIED_LEVEL_KEY) ? persistentData.getInt(LAST_APPLIED_LEVEL_KEY) : -1;

        if (currentLevel != lastAppliedLevel) {
            LOGGER.info("Player {} level changed from {} to {}. Re-applying attributes.", player.getName().getString(), lastAppliedLevel, currentLevel);
            applyPlayerLevelAttributes(player); // This also heals the player
            persistentData.putInt(LAST_APPLIED_LEVEL_KEY, currentLevel);

            if (lastAppliedLevel != -1 && currentLevel > lastAppliedLevel) { // Check if it's an actual level up from a known state
                 player.sendSystemMessage(Component.literal("Your skill level has increased to " + currentLevel + "!").withStyle(ChatFormatting.GOLD));
                 player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
                 if (player instanceof ServerPlayer sp) {
                     sp.getFoodData().setFoodLevel(20); // Restore food on level up
                 }
            }

            // Check for new element unlock messages
            AdvancedSkillsMod.ElementType newElementTypeFromLevel = AdvancedSkillsMod.ElementType.fromLevel(currentLevel);
            AdvancedSkillsMod.ElementType oldElementTypeFromLevel = AdvancedSkillsMod.ElementType.fromLevel(lastAppliedLevel);
            if (newElementTypeFromLevel != oldElementTypeFromLevel && newElementTypeFromLevel != AdvancedSkillsMod.ElementType.NONE) {
                boolean newUnlock = true;
                // Check if this element tier was already passed without a message (e.g. gaining many levels at once)
                for(int l = lastAppliedLevel + 1; l < currentLevel; l++){ // Iterate through intermediate levels
                    if(AdvancedSkillsMod.ElementType.fromLevel(l) == newElementTypeFromLevel){
                        newUnlock = false; // Already would have been unlocked at an intermediate level 'l'
                        break;
                    }
                }
                if(newUnlock){
                    player.sendSystemMessage(Component.literal("You have unlocked a new element tier: " + Component.translatable(newElementTypeFromLevel.getTranslationKey()).getString()).withStyle(newElementTypeFromLevel.getColor()));
                }
            }
        }
    }

    public void handleWorldSave(ServerPlayer player) { // Called per player during server save or periodically
        if (player.level().isClientSide()) return;
        UUID playerId = player.getUUID();

        if (playerSkillXp.containsKey(playerId)) {
            savePlayerSkillXp(player, playerSkillXp.get(playerId));
        }
        if (playerKillStats.containsKey(playerId)) {
            saveKillStats(player, playerKillStats.get(playerId));
        }
        if (specialtyCooldowns.containsKey(playerId)) {
            saveSpecialtyCooldown(player, specialtyCooldowns.get(playerId));
        }
         if (playerElementTypes.containsKey(playerId)) {
            saveElementType(player, playerElementTypes.get(playerId));
        }
        if (playerWeaponSpecialties.containsKey(playerId)) {
            saveWeaponSpecialty(player, playerWeaponSpecialties.get(playerId));
        }
        LOGGER.debug("Saved data for player {}", player.getName().getString());
    }
}
