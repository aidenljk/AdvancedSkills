package com.advancedskills.skills;

import com.advancedskills.CombatManager;
import com.advancedskills.MobStatsManager;
import com.advancedskills.PlayerStatsManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public interface ISkill {
    /** Gets the unique identifier for this skill (e.g., "elemental_blast"). */
    String getSkillId();

    /** Gets the display name of the skill. */
    String getDisplayName();

    /** Gets a brief description of what the skill does. */
    String getDescription();

    /** Executes the skill's logic. */
    void execute(Player player, PlayerStatsManager statsManager, CombatManager combatManager, MobStatsManager mobStatsManager);

    /** Returns the cooldown of the skill in game ticks. */
    int getCooldownTicks();

    /** Optional: Returns a ResourceLocation for the skill's icon. */
    // ResourceLocation getIcon();
    // For now, we can omit the icon until UI is planned.
}
