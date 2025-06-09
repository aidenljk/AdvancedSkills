package com.advancedskills;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.advancedskills.skills.ISkill; // Added import
import net.minecraft.ChatFormatting; // Added import

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 处理所有键盘输入事件
 */
public class KeyInputHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("KeyInputHandler");
    private final AdvancedSkillsMod mod;
    
    // 保存当前屏幕实例
    private KillStatsScreen statsScreen = null;
    
    public KeyInputHandler(AdvancedSkillsMod mod) {
        this.mod = mod;
    }
    
    /**
     * 处理键盘输入事件
     */
    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        // 获取当前玩家
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        // 只处理按下动作
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        
        int key = event.getKey();
        LOGGER.debug("键盘输入: key=" + key);
        
        // 检测K键 - 显示统计UI
        if (key == GLFW.GLFW_KEY_K) {
            LOGGER.info("检测到K键按下 - 显示统计UI");
            toggleStatsDisplay(player);
            return;
        }
        
        // 检测G键 - 切换元素类型
        if (key == GLFW.GLFW_KEY_G) {
            LOGGER.info("检测到G键按下 - 切换元素类型");
            mod.cycleElementType(player);
            return;
        }
        
        // 检测L键(用于兼容) - 切换元素类型
        if (key == GLFW.GLFW_KEY_L) {
            LOGGER.info("检测到L键按下(兼容模式) - 切换元素类型");
            mod.cycleElementType(player);
            return;
        }
        
        // 检测M键 - 切换武器专精
        if (key == GLFW.GLFW_KEY_M) {
            LOGGER.info("检测到M键按下 - 切换武器专精");
            mod.cycleWeaponSpecialty(player);
            return;
        }

        // Check for Active Skill 1 Key (R by default)
        if (AdvancedSkillsMod.activeSkill1KeyMapping.matches(event.getKey(), event.getScanCode())) {
            AdvancedSkillsMod modInstance = AdvancedSkillsMod.getInstance();
            if (modInstance != null && modInstance.playerStatsManager != null && modInstance.combatManager != null && modInstance.mobStatsManager != null) {
                ISkill skillToUse = AdvancedSkillsMod.ELEMENTAL_BLAST_SKILL; // Get the skill

                if (modInstance.playerStatsManager.isSkillReady(player.getUUID(), skillToUse.getSkillId(), skillToUse.getCooldownTicks(), player.level().getGameTime())) {
                    LOGGER.info("Executing skill: {} for player {}", skillToUse.getDisplayName(), player.getName().getString());
                    skillToUse.execute(player, modInstance.playerStatsManager, modInstance.combatManager, modInstance.mobStatsManager);
                    // Skill's execute method should set cooldown via playerStatsManager
                } else {
                    long remaining = modInstance.playerStatsManager.getRemainingCooldownTicks(player.getUUID(), skillToUse.getSkillId(), skillToUse.getCooldownTicks(), player.level().getGameTime());
                    player.sendSystemMessage(Component.literal(skillToUse.getDisplayName() + " is on cooldown for " + String.format("%.1f", remaining / 20.0) + "s.").withStyle(ChatFormatting.RED));
                }
            }
            return; // Consume the event
        }

        // Check for Active Skill 2 Key (F by default)
        if (AdvancedSkillsMod.activeSkill2KeyMapping.matches(event.getKey(), event.getScanCode())) {
            AdvancedSkillsMod modInstance = AdvancedSkillsMod.getInstance();
            // Player player is already defined and checked for null at the beginning of the method

            if (modInstance != null && modInstance.playerStatsManager != null && modInstance.combatManager != null && modInstance.mobStatsManager != null) {
                ISkill skillToUse = AdvancedSkillsMod.DEFENSIVE_STANCE_SKILL;

                if (modInstance.playerStatsManager.isSkillReady(player.getUUID(), skillToUse.getSkillId(), skillToUse.getCooldownTicks(), player.level().getGameTime())) {
                    LOGGER.info("Executing skill: {} for player {}", skillToUse.getDisplayName(), player.getName().getString());
                    skillToUse.execute(player, modInstance.playerStatsManager, modInstance.combatManager, modInstance.mobStatsManager);
                } else {
                    long remaining = modInstance.playerStatsManager.getRemainingCooldownTicks(player.getUUID(), skillToUse.getSkillId(), skillToUse.getCooldownTicks(), player.level().getGameTime());
                    player.sendSystemMessage(Component.literal(skillToUse.getDisplayName() + " is on cooldown for " + String.format("%.1f", remaining / 20.0) + "s.").withStyle(ChatFormatting.RED));
                }
            }
            return; // Consume the event
        }
    }
    
    /**
     * 切换统计显示
     */
    private void toggleStatsDisplay(Player player) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            
            // 如果当前有统计UI显示，则关闭
            if (minecraft.screen instanceof KillStatsScreen) {
                minecraft.setScreen(null);
                LOGGER.info("关闭统计UI");
            } else {
                // 否则显示统计UI
                updateStatsDisplay(player);
            }
        } catch (Exception e) {
            LOGGER.error("切换统计显示时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 更新并显示统计UI
     */
    private void updateStatsDisplay(Player player) {
        // 只在客户端执行
        if (!player.level().isClientSide()) {
            return;
        }
        
        UUID playerId = player.getUUID();
        
        // 获取玩家数据 via PlayerStatsManager
        int playerXp = mod.playerStatsManager.getPlayerSkillXp(playerId);
        int playerLevel = mod.playerStatsManager.calculateLevelFromXp(playerXp);
        AdvancedSkillsMod.ElementType elementType = mod.playerStatsManager.getPlayerElementType(playerId);
        AdvancedSkillsMod.WeaponSpecialty specialty = mod.playerStatsManager.getPlayerWeaponSpecialty(playerId);
        
        // 获取击杀统计 via PlayerStatsManager
        Map<String, Integer> stats = mod.playerStatsManager.getPlayerKillStats(playerId);
        
        // 创建或更新UI数据
        if (statsScreen == null) {
            statsScreen = new KillStatsScreen();
        }
        
        // 更新UI数据
        statsScreen.updateStats(playerLevel, playerXp, stats, elementType, specialty);
        
        // 显示UI
        Minecraft.getInstance().setScreen(statsScreen);
        LOGGER.info("显示统计UI");
    }
} 