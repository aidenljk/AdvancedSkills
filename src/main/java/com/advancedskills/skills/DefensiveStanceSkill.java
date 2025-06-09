package com.advancedskills.skills;

import com.advancedskills.AdvancedSkillsMod;
import com.advancedskills.CombatManager;
import com.advancedskills.MobStatsManager;
import com.advancedskills.PlayerStatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class DefensiveStanceSkill implements ISkill {

    private static final String SKILL_ID = "defensive_stance";
    private static final String DISPLAY_NAME = "Defensive Stance";
    private static final String DESCRIPTION = "Temporarily enhances your defenses.";
    private static final int COOLDOWN_TICKS = 300; // 15 seconds
    private static final int DURATION_TICKS = 120; // 6 seconds

    @Override
    public String getSkillId() { return SKILL_ID; }

    @Override
    public String getDisplayName() { return DISPLAY_NAME; }

    @Override
    public String getDescription() { return DESCRIPTION; }

    @Override
    public int getCooldownTicks() { return COOLDOWN_TICKS; }

    @Override
    public void execute(Player player, PlayerStatsManager statsManager, CombatManager combatManager, MobStatsManager mobManager) {
        if (player.level().isClientSide()) return;
        ServerPlayer serverPlayer = (ServerPlayer) player;
        int playerLevel = statsManager.calculateLevelFromXp(statsManager.getPlayerSkillXp(player.getUUID()));

        // Apply effects
        // Resistance I for DURATION_TICKS. Amplifier 0 is Resistance I.
        serverPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, 0));

        // Add Absorption based on player level, e.g., Absorption I at level 10, II at 30
        int absorptionAmplifier = -1;
        if (playerLevel >= 10) absorptionAmplifier = 0; // Absorption I
        if (playerLevel >= 30) absorptionAmplifier = 1; // Absorption II

        if (absorptionAmplifier >= 0) {
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, DURATION_TICKS, absorptionAmplifier));
        }

        // Set cooldown
        statsManager.setSkillOnCooldown(player.getUUID(), SKILL_ID, player.level().getGameTime());

        // Feedback
        player.sendSystemMessage(Component.literal("Defensive Stance activated!").withStyle(ChatFormatting.BLUE));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7F, 1.0F);
         player.sendSystemMessage(Component.literal(getDisplayName() + " on cooldown (" + (COOLDOWN_TICKS/20) + "s).").withStyle(ChatFormatting.DARK_AQUA));
    }
}
