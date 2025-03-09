package com.advancedskills;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/**
 * 击杀统计UI屏幕
 * 在游戏中显示击杀统计，不暂停游戏
 */
public class KillStatsScreen extends Screen {
    // 背景纹理位置
    private static final ResourceLocation STATS_BACKGROUND = new ResourceLocation(AdvancedSkillsMod.MODID, "textures/gui/stats_background.png");
    
    // 玩家等级和经验
    private int playerLevel = 0;
    private int playerXp = 0;
    
    // 击杀统计数据
    private Map<String, Integer> killStats = null;
    
    // 元素类型和武器专精
    private AdvancedSkillsMod.ElementType elementType = AdvancedSkillsMod.ElementType.NONE;
    private AdvancedSkillsMod.WeaponSpecialty weaponSpecialty = AdvancedSkillsMod.WeaponSpecialty.NONE;
    
    // UI尺寸和位置
    private int uiWidth = 200;
    private int uiHeight = 150;
    private int x;
    private int y;
    
    // 关闭按钮
    private int closeX;
    private int closeY;
    private int closeWidth = 60;
    private int closeHeight = 15;

    /**
     * 构造函数
     */
    public KillStatsScreen() {
        super(Component.literal("技能统计"));
        this.minecraft = Minecraft.getInstance();
        
        // 计算位置
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        this.x = (screenWidth - uiWidth) / 2;
        this.y = (screenHeight - uiHeight) / 2;
        
        // 计算关闭按钮位置
        this.closeX = x + uiWidth / 2 - closeWidth / 2;
        this.closeY = y + uiHeight - 20;
    }
    
    /**
     * 更新统计数据
     */
    public void updateStats(int level, int xp, Map<String, Integer> stats, 
                          AdvancedSkillsMod.ElementType element, 
                          AdvancedSkillsMod.WeaponSpecialty specialty) {
        this.playerLevel = level;
        this.playerXp = xp;
        this.killStats = stats;
        this.elementType = element;
        this.weaponSpecialty = specialty;
    }
    
    /**
     * 渲染屏幕
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 不调用renderBackground方法，以避免高斯模糊
        // 直接绘制半透明背景和边框
        guiGraphics.fill(x, y, x + uiWidth, y + uiHeight, 0xD0000000); // 更高的透明度
        // 绘制边框
        guiGraphics.fill(x, y, x + uiWidth, y + 1, 0xFFFFFFFF); // 上
        guiGraphics.fill(x, y + uiHeight - 1, x + uiWidth, y + uiHeight, 0xFFFFFFFF); // 下
        guiGraphics.fill(x, y, x + 1, y + uiHeight, 0xFFFFFFFF); // 左
        guiGraphics.fill(x + uiWidth - 1, y, x + uiWidth, y + uiHeight, 0xFFFFFFFF); // 右
        
        // 标题
        guiGraphics.drawCenteredString(font, "玩家技能统计", x + uiWidth / 2, y + 10, 0xFFFFFF);
        
        // 玩家等级
        String levelText = "等级: " + playerLevel;
        guiGraphics.drawString(font, levelText, x + 10, y + 30, 0xFFFF00);
        
        // 玩家经验
        String xpText = "经验: " + playerXp;
        guiGraphics.drawString(font, xpText, x + 10, y + 45, 0xFFFF00);
        
        // 元素类型
        String elementText = "元素: " + elementType.getDisplayName();
        int elementColor = elementType.getColor().getColor();
        guiGraphics.drawString(font, elementText, x + 10, y + 60, elementColor);
        
        // 武器专精
        String specialtyText = "专精: " + weaponSpecialty.getDisplayName();
        int specialtyColor = weaponSpecialty.getColor().getColor();
        guiGraphics.drawString(font, specialtyText, x + 10, y + 75, specialtyColor);
        
        // 击杀统计标题
        guiGraphics.drawString(font, "击杀统计:", x + 10, y + 95, 0xFFFFFF);
        
        // 显示击杀统计
        int yOffset = 110;
        if (killStats != null && !killStats.isEmpty()) {
            for (Map.Entry<String, Integer> entry : killStats.entrySet()) {
                String statText = entry.getKey() + ": " + entry.getValue();
                guiGraphics.drawString(font, statText, x + 20, y + yOffset, 0xCCCCCC);
                yOffset += 15;
                
                // 避免统计信息超出界面
                if (yOffset > uiHeight - 10) {
                    break;
                }
            }
        } else {
            guiGraphics.drawString(font, "暂无击杀记录", x + 20, y + yOffset, 0xAAAAAA);
        }
        
        // 绘制关闭按钮
        guiGraphics.fill(closeX, closeY, closeX + closeWidth, closeY + closeHeight, 0xFF333333);
        guiGraphics.drawCenteredString(font, "关闭", closeX + closeWidth / 2, closeY + 3, 0xFFFFFF);
        
        // 调用父类方法渲染其他元素（如果有的话）
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    /**
     * 处理鼠标点击
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 检查点击位置是否在关闭按钮上
        if (mouseX >= closeX && mouseX <= closeX + closeWidth && 
            mouseY >= closeY && mouseY <= closeY + closeHeight) {
            onClose();
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * 屏幕是否暂停游戏
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 渲染背景 - 覆盖父类方法以禁用高斯模糊
     */
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 不调用super方法，这样就不会应用模糊效果
        // 如果需要半透明背景，可以在这里自己绘制
    }
}