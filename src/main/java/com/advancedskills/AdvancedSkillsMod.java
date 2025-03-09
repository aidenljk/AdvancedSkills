package com.advancedskills;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

@Mod(AdvancedSkillsMod.MODID)
public class AdvancedSkillsMod {
    public static final String MODID = "advancedskills";
    public static final Logger LOGGER = LoggerFactory.getLogger("AdvancedSkillsMod");
    
    // 玩家等级数据
    private static final String SKILL_XP_KEY = "AdvancedSkillsXP";
    
    // 怪物等级数据
    private final Map<UUID, Integer> entityLevels = new HashMap<>();
    
    // 玩家技能经验数据
    private final Map<UUID, Integer> playerSkillXp = new HashMap<>();
    
    // 弓箭伤害系数
    private static final float BASE_EXTRA_ARROW_DAMAGE = 0.5F; // 初始0.5点额外伤害
    private static final float ARROW_DAMAGE_PER_LEVEL = 0.6F; // 每级增加0.6点伤害
    private static final float MAX_EXTRA_ARROW_DAMAGE = 75.0F; // 最多增加75点伤害
    
    // 弓箭射程提升系数
    private static final float BASE_ARROW_RANGE_BOOST = 0.1F; // 初始10%射程提升
    private static final float ARROW_RANGE_BOOST_PER_LEVEL = 0.025F; // 每级增加2.5%射程
    private static final float MAX_ARROW_RANGE_BOOST = 1.5F; // 最多增加150%射程
    
    // 剑伤害系数
    private static final float BASE_EXTRA_SWORD_DAMAGE = 0.5F; // 初始0.5点额外伤害
    private static final float SWORD_DAMAGE_PER_LEVEL = 0.45F; // 每级增加0.45点伤害
    private static final float MAX_EXTRA_SWORD_DAMAGE = 50.0F; // 最多增加50点伤害
    
    // 属性修改器UUID
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("d34d1515-5d6e-4c5d-a91f-6c35d85c21d1");
    private static final UUID DAMAGE_MODIFIER_ID = UUID.fromString("d34d1515-5d6e-4c5d-a91f-6c35d85c21d2");
    private static final UUID PLAYER_HEALTH_MODIFIER_ID = UUID.fromString("d34d1515-5d6e-4c5d-a91f-6c35d85c21d3");
    private static final UUID PLAYER_ATTACK_MODIFIER_ID = UUID.fromString("d34d1515-5d6e-4c5d-a91f-6c35d85c21d4");
    private static final UUID PLAYER_SPEED_MODIFIER_ID = UUID.fromString("d34d1515-5d6e-4c5d-a91f-6c35d85c21d5");
    private static final UUID PLAYER_ARMOR_MODIFIER_ID = UUID.fromString("d34d1515-5d6e-4c5d-a91f-6c35d85c21d6");
    
    // 玩家属性加成系数
    private static final float PLAYER_HEALTH_PER_LEVEL = 0.01F; // 每级增加1%生命值
    private static final float PLAYER_MAX_HEALTH_BOOST = 1.0F; // 最多增加100%生命值
    private static final float PLAYER_ATTACK_PER_LEVEL = 0.01F; // 每级增加1%攻击力
    private static final float PLAYER_MAX_ATTACK_BOOST = 1.0F; // 最多增加100%攻击力
    private static final float PLAYER_ARMOR_PER_LEVEL = 0.01F; // 每级增加1%护甲
    private static final float PLAYER_MAX_ARMOR_BOOST = 0.5F; // 最多增加50%护甲
    private static final float PLAYER_SPEED_PER_LEVEL = 0.002F; // 每级增加0.2%速度
    private static final float PLAYER_MAX_SPEED_BOOST = 0.3F; // 最多增加30%速度
    
    // 随机数生成器
    private final Random random = new Random();
    
    // 怪物等级NBT键
    private static final String MONSTER_LEVEL_KEY = "AdvancedSkillsMonsterLevel";
    private static final String MONSTER_LEVEL_SET_KEY = "AdvancedSkillsMonsterLevelSet";
    
    // 玩家击杀统计NBT键
    private static final String KILLS_STATS_KEY = "AdvancedSkillsKillStats";
    
    // 玩家击杀统计
    private final Map<UUID, Map<String, Integer>> playerKillStats = new HashMap<>();
    
    // 等级分段
    private static final String[] LEVEL_TIERS = {
        "微弱(0-20)", "普通(21-40)", "强大(41-60)", "精英(61-80)", "传奇(81-100)"
    };
    
    // 统计按键
    private static KeyMapping statsKeyMapping;

    // 用于客户端访问Mod实例的静态引用
    private static AdvancedSkillsMod INSTANCE;

    // 元素类型枚举
    public enum ElementType {
        NONE(ChatFormatting.GRAY, "无"),
        FIRE(ChatFormatting.RED, "火"),
        ICE(ChatFormatting.AQUA, "冰"),
        LIGHTNING(ChatFormatting.YELLOW, "雷"),
        POISON(ChatFormatting.DARK_GREEN, "毒");
        
        private final ChatFormatting color;
        private final String displayName;
        
        ElementType(ChatFormatting color, String displayName) {
            this.color = color;
            this.displayName = displayName;
        }
        
        public ChatFormatting getColor() {
            return color;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static ElementType fromLevel(int level) {
            if (level >= 55) return POISON;
            if (level >= 40) return LIGHTNING;
            if (level >= 25) return ICE;
            if (level >= 10) return FIRE;
            return NONE;
        }
    }
    
    // 元素效果的NBT键
    private static final String ELEMENT_TYPE_KEY = "AdvancedSkillsElementType";
    
    // 当前玩家选择的元素类型
    private final Map<UUID, ElementType> playerElementTypes = new HashMap<>();
    
    // 元素效果强度常量 - 增强版
    private static final float FIRE_BASE_DURATION = 4.0F; // 初始4秒燃烧
    private static final float FIRE_DURATION_PER_LEVEL = 0.15F; // 每级增加0.15秒
    private static final float FIRE_BASE_DAMAGE = 1.0F; // 火焰额外伤害基础值
    private static final float FIRE_DAMAGE_PER_LEVEL = 0.1F; // 每级增加0.1点火焰伤害
    
    private static final float ICE_BASE_DURATION = 3.0F; // 初始3秒减速
    private static final float ICE_DURATION_PER_LEVEL = 0.08F; // 每级增加0.08秒
    private static final float ICE_BASE_SLOW_AMPLIFIER = 1.0F; // 基础减速等级1
    private static final float ICE_SLOW_AMPLIFIER_PER_LEVEL = 0.06F; // 每级增加0.06减速等级
    
    private static final float LIGHTNING_BASE_CHANCE = 0.2F; // 20%基础连锁几率
    private static final float LIGHTNING_CHANCE_PER_LEVEL = 0.012F; // 每级增加1.2%几率
    private static final float LIGHTNING_CHAIN_RADIUS = 5.0F; // 5格连锁半径
    private static final float LIGHTNING_BASE_DAMAGE = 3.0F; // 3点基础连锁伤害
    private static final float LIGHTNING_DAMAGE_PER_LEVEL = 0.3F; // 每级增加0.3点连锁伤害
    
    private static final float POISON_BASE_DURATION = 4.0F; // 初始4秒中毒
    private static final float POISON_DURATION_PER_LEVEL = 0.12F; // 每级增加0.12秒
    private static final float POISON_BASE_AMPLIFIER = 1.0F; // 基础中毒等级1
    private static final float POISON_AMPLIFIER_PER_LEVEL = 0.04F; // 每级增加0.04中毒等级
    
    // 按键切换元素
    private static KeyMapping elementKeyMapping;

    // 武器专精枚举
    public enum WeaponSpecialty {
        NONE(ChatFormatting.GRAY, "无"),
        BOW(ChatFormatting.GREEN, "弓箭"),
        SWORD(ChatFormatting.BLUE, "剑术");
        
        private final ChatFormatting color;
        private final String displayName;
        
        WeaponSpecialty(ChatFormatting color, String displayName) {
            this.color = color;
            this.displayName = displayName;
        }
        
        public ChatFormatting getColor() {
            return color;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // 武器专精NBT键
    private static final String WEAPON_SPECIALTY_KEY = "AdvancedSkillsWeaponSpecialty";
    
    // 玩家武器专精
    private final Map<UUID, WeaponSpecialty> playerWeaponSpecialties = new HashMap<>();
    
    // 专精切换冷却时间（秒）
    private static final int SPECIALTY_COOLDOWN_SECONDS = 300; // 5分钟
    private final Map<UUID, Long> specialtyCooldowns = new HashMap<>();
    
    // 暴击系统常量
    private static final float BASE_CRIT_CHANCE = 0.05F; // 5%基础暴击率
    private static final float CRIT_CHANCE_PER_LEVEL = 0.005F; // 每级增加0.5%暴击率
    private static final float BOW_SPECIALTY_CRIT_BONUS = 0.15F; // 弓箭专精额外15%暴击率
    private static final float SWORD_SPECIALTY_CRIT_BONUS = 0.15F; // 剑专精额外15%暴击率
    private static final float FULL_DRAW_CRIT_BONUS = 0.25F; // 满弓额外25%暴击率
    
    private static final float BASE_CRIT_DAMAGE = 1.5F; // 基础暴击倍率150%
    private static final float CRIT_DAMAGE_PER_LEVEL = 0.02F; // 每级增加2%暴击伤害
    private static final float BOW_SPECIALTY_CRIT_DAMAGE_BONUS = 0.5F; // 弓箭专精额外50%暴击伤害
    private static final float SWORD_SPECIALTY_CRIT_DAMAGE_BONUS = 0.5F; // 剑专精额外50%暴击伤害
    
    // 连击系统
    private static final int COMBO_WINDOW_TICKS = 40; // 2秒连击窗口
    private static final float COMBO_CRIT_BONUS_PER_HIT = 0.1F; // 每次连击增加10%暴击率
    private static final int MAX_COMBO = 5; // 最多5连击
    
    // 跟踪玩家连击
    private static class ComboTracker {
        private UUID targetId;
        private int comboCount;
        private long lastHitTime;
        
        public ComboTracker(UUID targetId) {
            this.targetId = targetId;
            this.comboCount = 1;
            this.lastHitTime = System.currentTimeMillis();
        }
        
        public UUID getTargetId() {
            return targetId;
        }
        
        public int getComboCount() {
            return comboCount;
        }
        
        public long getLastHitTime() {
            return lastHitTime;
        }
        
        public void incrementCombo() {
            comboCount = Math.min(comboCount + 1, MAX_COMBO);
            lastHitTime = System.currentTimeMillis();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - lastHitTime > COMBO_WINDOW_TICKS * 50; // 50ms per tick
        }
    }
    
    private final Map<UUID, ComboTracker> playerCombos = new ConcurrentHashMap<>();
    
    // 专精切换按键
    private static KeyMapping specialtyKeyMapping;

    // 显示统计信息标志
    private static boolean showStatsInfo = false;

    private Minecraft client;

    public AdvancedSkillsMod() {
        LOGGER.info("初始化高级技能Mod");
        this.client = Minecraft.getInstance();
        
        // 设置实例引用
        INSTANCE = this;

        // 获取MOD事件总线和Forge事件总线
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
        
        // 注册客户端设置和按键绑定监听器
        modEventBus.addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(new KeyInputHandler());
        
        LOGGER.info("注册热键：切换元素类型(G)，显示统计信息(K)，切换武器专精(L)");
    }
    
    /**
     * 客户端设置
     */
    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("==== 初始化客户端UI与热键 - 开始 ====");
        
        // 在游戏启动时检查热键状态和冲突
        event.enqueueWork(() -> {
            LOGGER.info("检查按键绑定状态...");
            
            // 获取当前所有按键映射
            KeyMapping[] allKeys = Minecraft.getInstance().options.keyMappings;
            LOGGER.info("游戏总共有 " + allKeys.length + " 个按键绑定");
            
            // 检查我们的热键是否正确注册，以及是否与原版冲突
            boolean elementKeyFound = false;
            boolean statsKeyFound = false;
            boolean specialtyKeyFound = false;
            
            // 记录L键和G键的使用情况
            LOGGER.info("==== 按键使用情况 ====");
            for (KeyMapping key : allKeys) {

                if (key.getKey().getValue() == GLFW.GLFW_KEY_G) {
                    LOGGER.warn("G键(71)已被绑定到: " + key.getName());
                }
                
                // 检查我们的热键是否在列表中
                if (key.getName().equals("key.advancedskills.element")) {
                    elementKeyFound = true;
                    LOGGER.info("元素切换键已设置为: " + key.getKey().getDisplayName().getString() + 
                               " (键值:" + key.getKey().getValue() + ")");
                    
                    // 如果元素切换键不是G键，记录错误
                    if (key.getKey().getValue() != GLFW.GLFW_KEY_G) {
                        LOGGER.error("元素切换键被设置为 " + key.getKey().getDisplayName().getString() + 
                                    "，而不是预期的G键(71)");
                    }
                }
                if (key.getName().equals("key.advancedskills.stats")) {
                    statsKeyFound = true;
                    LOGGER.info("统计键已设置为: " + key.getKey().getDisplayName().getString() + 
                               " (键值:" + key.getKey().getValue() + ")");
                }
                if (key.getName().equals("key.advancedskills.specialty")) {
                    specialtyKeyFound = true;
                    LOGGER.info("专精键已设置为: " + key.getKey().getDisplayName().getString() + 
                               " (键值:" + key.getKey().getValue() + ")");
                }
            }
            LOGGER.info("======================");
            
            // 热键没有正确注册的警告
            if (!elementKeyFound) {
                LOGGER.error("元素切换键未正确注册! 请删除options.txt并重启游戏");
            }
            if (!statsKeyFound) {
                LOGGER.error("统计键未正确注册! 请删除options.txt并重启游戏");
            }
            if (!specialtyKeyFound) {
                LOGGER.error("专精键未正确注册! 请删除options.txt并重启游戏");
            }
            
            // 重新保存我们的元素切换按键为G键
            if (elementKeyMapping != null) {
                LOGGER.info("当前元素切换键设置为: " + 
                           elementKeyMapping.getKey().getDisplayName().getString() + 
                           " (键值:" + elementKeyMapping.getKey().getValue() + ")");
                
                // 如果不是G键，尝试强制设置为G键
                if (elementKeyMapping.getKey().getValue() != GLFW.GLFW_KEY_G) {
                    try {
                        LOGGER.warn("尝试强制更新元素切换键为G键...");
                        elementKeyMapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_G));
                        LOGGER.info("元素切换键已强制更新为G键");
                    } catch (Exception e) {
                        LOGGER.error("无法强制更新元素切换键: " + e.getMessage(), e);
                    }
                }
            } else {
                LOGGER.error("elementKeyMapping为null，无法检查或更新");
            }
        });
        
        LOGGER.info("==== 初始化客户端UI与热键 - 完成 ====");
    }
    
    /**
     * 注册按键绑定
     */
    private void registerKeyBindings(final RegisterKeyMappingsEvent event) {
        LOGGER.info("==== 高级技能 Mod 注册按键绑定开始 ====");
        
        // 清除任何可能存在的旧热键实例
        if (statsKeyMapping != null) {
            LOGGER.info("清除旧的统计键实例");
            statsKeyMapping = null;
        }
        if (elementKeyMapping != null) {
            LOGGER.info("清除旧的元素切换键实例");
            elementKeyMapping = null;
        }
        if (specialtyKeyMapping != null) {
            LOGGER.info("清除旧的专精键实例");
            specialtyKeyMapping = null;
        }
        
        // 创建并注册统计热键
        try {
            statsKeyMapping = new KeyMapping(
                "key.advancedskills.stats", // 键位名称
                GLFW.GLFW_KEY_K,           // 默认键位（K键）
                "key.categories.advancedskills" // 键位分类
            );
            event.register(statsKeyMapping);
            LOGGER.info("【注册成功】统计键: K");
        } catch (Exception e) {
            LOGGER.error("注册统计键失败: " + e.getMessage());
        }
        
        // 创建并注册元素切换热键 - 使用G键而不是V键
        try {
            elementKeyMapping = new KeyMapping(
                "key.advancedskills.element", // 键位名称
                GLFW.GLFW_KEY_G,             // 默认键位（G键，避免冲突）
                "key.categories.advancedskills" // 键位分类
            );
            event.register(elementKeyMapping);
            LOGGER.info("【注册成功】元素切换键: G (原计划V键可能有冲突)");
        } catch (Exception e) {
            LOGGER.error("注册元素切换键失败: " + e.getMessage());
        }
        
        // 创建并注册专精切换热键
        try {
            specialtyKeyMapping = new KeyMapping(
                "key.advancedskills.specialty", // 键位名称
                GLFW.GLFW_KEY_M,                // 默认键位（M键）
                "key.categories.advancedskills"  // 键位分类
            );
            event.register(specialtyKeyMapping);
            LOGGER.info("【注册成功】专精键: M");
        } catch (Exception e) {
            LOGGER.error("注册专精键失败: " + e.getMessage());
        }
        
        LOGGER.info("==== 高级技能 Mod 按键绑定注册完成 ====");
    }
    
    /**
     * 监听键盘输入事件
     */
    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        // 获取当前玩家实例
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        // 记录当前按下的键
        int key = event.getKey();
        int scanCode = event.getScanCode();
        int action = event.getAction();
        int modifiers = event.getModifiers();
        
        // 只处理按下动作(action == 1)，避免重复触发
        if (action == 1) {
            LOGGER.debug("键盘输入: key=" + key + 
                       ", scanCode=" + scanCode + 
                       ", action=" + action + 
                       ", modifiers=" + modifiers);
            
            // 直接检查是否是G键 (GLFW_KEY_G = 71)
            if (key == GLFW.GLFW_KEY_G) {
                LOGGER.info("直接检测到G键(71)按下，切换元素类型");
                cycleElementType(player);
                return;
            }
            
            // 兼容老版本的L键检测，可以在修复后移除这部分
            if (key == GLFW.GLFW_KEY_L) {
                LOGGER.info("检测到L键(76)按下 - 兼容老版本，切换元素类型");
                cycleElementType(player);
                return;
            }
        }
        
        // 通过KeyMapping检测按键，作为备用方案
        if (elementKeyMapping != null && elementKeyMapping.isDown()) {
            LOGGER.info("通过KeyMapping检测到元素切换键按下: " + 
                       elementKeyMapping.getKey().getDisplayName().getString() + 
                       " (键值:" + elementKeyMapping.getKey().getValue() + ")");
            cycleElementType(player);
        }
        
        if (statsKeyMapping != null && statsKeyMapping.isDown()) {
            LOGGER.info("检测到统计键按下");
            // 切换显示状态
            showStatsInfo = !showStatsInfo;
            
            if (showStatsInfo) {
                // 使用UI方式显示技能统计信息
                updateStatsDisplay(player);
            } else {
                // 显示隐藏消息
                player.sendSystemMessage(Component.literal("技能统计信息已隐藏").withStyle(ChatFormatting.GRAY));
            }
        }
        
        if (specialtyKeyMapping != null && specialtyKeyMapping.isDown()) {
            LOGGER.info("检测到专精键按下");
            cycleWeaponSpecialty(player);
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
        
        // 获取玩家数据
        int playerXp = playerSkillXp.getOrDefault(playerId, 0);
        int playerLevel = calculateLevelFromXp(playerXp);
        ElementType elementType = playerElementTypes.getOrDefault(playerId, ElementType.NONE);
        WeaponSpecialty specialty = playerWeaponSpecialties.getOrDefault(playerId, WeaponSpecialty.NONE);
        
        // 获取击杀统计
        Map<String, Integer> stats = playerKillStats.getOrDefault(playerId, new HashMap<>());
        
        // 创建并显示统计屏幕
        KillStatsScreen statsScreen = new KillStatsScreen();
        statsScreen.updateStats(playerLevel, playerXp, stats, elementType, specialty);
        Minecraft.getInstance().setScreen(statsScreen);
    }
    
    /**
     * 循环切换玩家的元素类型
     */
    public void cycleElementType(Player player) {
        UUID playerId = player.getUUID();
        int playerLevel = calculateLevelFromXp(playerSkillXp.getOrDefault(playerId, 0));
        
        // 获取当前选择的元素
        ElementType currentElement = playerElementTypes.getOrDefault(playerId, ElementType.NONE);
        ElementType nextElement = ElementType.NONE;
        
        // 根据玩家等级判断可用元素
        if (playerLevel >= 10) {
            switch (currentElement) {
                case NONE:
                    nextElement = ElementType.FIRE;
                    break;
                case FIRE:
                    nextElement = (playerLevel >= 25) ? ElementType.ICE : ElementType.NONE;
                    break;
                case ICE:
                    nextElement = (playerLevel >= 40) ? ElementType.LIGHTNING : ElementType.NONE;
                    break;
                case LIGHTNING:
                    nextElement = (playerLevel >= 55) ? ElementType.POISON : ElementType.NONE;
                    break;
                case POISON:
                    nextElement = ElementType.NONE;
                    break;
            }
        }
        
        // 更新玩家选择的元素类型
        playerElementTypes.put(playerId, nextElement);
        
        // 保存选择到NBT
        if (player instanceof ServerPlayer serverPlayer) {
            saveElementType(serverPlayer, nextElement);
        }
        
        // 通知玩家
        String elementName = Component.translatable("advancedskills.element." + nextElement.name().toLowerCase()).getString();
        player.sendSystemMessage(
            Component.translatable("advancedskills.element.switch", elementName)
                .withStyle(nextElement.getColor())
        );
        
        // 播放元素切换音效
        player.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);
    }
    
    /**
     * 保存元素类型到玩家NBT
     */
    private void saveElementType(ServerPlayer player, ElementType elementType) {
        CompoundTag persistentData = player.getPersistentData();
        persistentData.putInt(ELEMENT_TYPE_KEY, elementType.ordinal());
    }
    
    /**
     * 从玩家NBT加载元素类型
     */
    private ElementType loadElementType(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(ELEMENT_TYPE_KEY)) {
            int ordinal = persistentData.getInt(ELEMENT_TYPE_KEY);
            return ElementType.values()[ordinal];
        }
        return ElementType.NONE;
    }
    
    /**
     * 当玩家登录时，加载数据
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        
        // 客户端处理
        if (player.level().isClientSide()) {
            if (player == Minecraft.getInstance().player) {
                // 更新实例引用
                INSTANCE = this;
            }
            return;
        }
        
        if (player instanceof ServerPlayer serverPlayer) {
            LOGGER.info("玩家 " + player.getName().getString() + " 登录，加载数据并应用增益");
            
            // 加载元素类型
            ElementType elementType = loadElementType(player);
            playerElementTypes.put(player.getUUID(), elementType);
            
            // 加载武器专精
            WeaponSpecialty specialty = loadWeaponSpecialty(player);
            playerWeaponSpecialties.put(player.getUUID(), specialty);
            
            // 加载专精切换冷却时间
            Long cooldown = loadSpecialtyCooldown(player);
            if (cooldown != null) {
                specialtyCooldowns.put(player.getUUID(), cooldown);
            }
            
            // 加载玩家经验值并应用等级属性加成
            int xp = loadPlayerSkillXp(player);
            playerSkillXp.put(player.getUUID(), xp);
            
            // 加载击杀统计
            CompoundTag persistentData = serverPlayer.getPersistentData();
            if (persistentData.contains(KILLS_STATS_KEY)) {
                // 从NBT读取击杀统计
                CompoundTag killStatsTag = persistentData.getCompound(KILLS_STATS_KEY);
                Map<String, Integer> killStats = new HashMap<>();
                
                // 读取每个等级段的击杀数量
                for (String tier : LEVEL_TIERS) {
                    if (killStatsTag.contains(tier)) {
                        killStats.put(tier, killStatsTag.getInt(tier));
                    }
                }
                
                // 保存到内存
                playerKillStats.put(player.getUUID(), killStats);
            }
            
            // 应用玩家等级属性增益
            applyPlayerLevelAttributes(player);
            
            // 计算玩家当前等级
            int level = calculateLevelFromXp(xp);
            
            // 告知玩家当前等级和效果
            player.sendSystemMessage(Component.literal("欢迎回来！你当前的等级是: " + level).withStyle(ChatFormatting.GREEN));
            
            if (elementType != ElementType.NONE) {
                player.sendSystemMessage(Component.literal("当前元素: " + elementType.getDisplayName()).withStyle(elementType.getColor()));
            }
            
            if (specialty != WeaponSpecialty.NONE) {
                player.sendSystemMessage(Component.literal("当前武器专精: " + specialty.getDisplayName()).withStyle(specialty.getColor()));
            }
            
            // 显示新的热键提示
            player.sendSystemMessage(Component.literal("按 K 键查看等级和统计信息").withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.literal("按 G 键切换元素类型").withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.literal("按 M 键切换武器专精").withStyle(ChatFormatting.GRAY));
        }
    }
    
    /**
     * 监听世界tick事件 - 定期保存数据
     */
    @SubscribeEvent
    public void onWorldTick(TickEvent.LevelTickEvent event) {
        // 只在服务器端和每秒一次处理
        if (event.level.isClientSide() || event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // 每600 ticks (30秒) 保存玩家数据
        if (event.level.getGameTime() % 600 == 0) {
            for (ServerPlayer player : event.level.getServer().getPlayerList().getPlayers()) {
                UUID playerId = player.getUUID();
                
                // 保存经验数据
                if (playerSkillXp.containsKey(playerId)) {
                    int xp = playerSkillXp.get(playerId);
                    savePlayerSkillXp(player, xp);
                }
                
                // 保存击杀统计数据
                if (playerKillStats.containsKey(playerId)) {
                    Map<String, Integer> killStats = playerKillStats.get(playerId);
                    saveKillStats(player, killStats);
                }
                
                // 保存武器专精冷却时间
                if (specialtyCooldowns.containsKey(playerId)) {
                    Long cooldown = specialtyCooldowns.get(playerId);
                    saveSpecialtyCooldown(player, cooldown);
                }
                
                LOGGER.debug("自动保存玩家 " + player.getName().getString() + " 的数据");
            }
        }
    }
    
    /**
     * 当怪物死亡时，根据其等级为玩家提供经验，并更新击杀统计
     */
    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Entity killer = event.getSource().getEntity();
        
        // 检查是否是怪物，以及是否被玩家杀死
        if (entity instanceof Monster monster && killer instanceof Player player) {
            UUID monsterId = monster.getUUID();
            
            // 优先从NBT获取怪物等级，确保数据一致性
            int monsterLevel = 0;
            CompoundTag persistentData = monster.getPersistentData();
            if (persistentData.contains(MONSTER_LEVEL_KEY)) {
                monsterLevel = persistentData.getInt(MONSTER_LEVEL_KEY);
            } else {
                // 作为备选，从缓存中获取
                monsterLevel = entityLevels.getOrDefault(monsterId, 0);
                LOGGER.warn("怪物NBT中未找到等级数据，使用缓存数据: " + monsterLevel);
            }
            
            // 根据怪物等级计算额外经验
            int baseXp = 5; // 基础经验值
            int bonusXp = calculateBonusXp(monsterLevel);
            int totalXp = baseXp + bonusXp;
            
            // 为玩家添加技能经验
            addPlayerSkillXp(player, totalXp);
            
            // 更新击杀统计
            updateKillStats(player, monsterLevel);
            
            // 移除怪物等级记录
            entityLevels.remove(monsterId);
            
            // 通知玩家获得技能经验
            player.sendSystemMessage(Component.literal("获得 " + totalXp + " 点技能经验! (怪物等级: " + monsterLevel + ")")
                    .withStyle(ChatFormatting.GREEN));
            
            LOGGER.info("玩家 " + player.getName().getString() + " 击杀了等级 " + monsterLevel + 
                      " 的怪物，获得 " + totalXp + " 点技能经验");
        }
    }
    
    /**
     * 当玩家击杀怪物增加经验时
     */
    private void addPlayerSkillXp(Player player, int xpAmount) {
        UUID playerId = player.getUUID();
        
        // 获取当前经验值
        int currentXp = playerSkillXp.getOrDefault(playerId, loadPlayerSkillXp(player));
        int newXp = currentXp + xpAmount;
        
        // 计算新旧等级
        int oldLevel = calculateLevelFromXp(currentXp);
        int newLevel = calculateLevelFromXp(newXp);
        
        // 更新经验值
        playerSkillXp.put(playerId, newXp);
        
        // 如果玩家在服务器上，保存数据
        if (player instanceof ServerPlayer) {
            // 保存数据
            savePlayerSkillXp(player, newXp);
            
            // 如果等级提升，通知玩家
            if (newLevel > oldLevel) {
                player.sendSystemMessage(Component.literal("恭喜! 技能等级提升到 " + newLevel)
                        .withStyle(ChatFormatting.GOLD));
                
                // 播放等级提升音效
                player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
            }
        }
        
        LOGGER.debug("玩家 " + player.getName().getString() + " 获得 " + xpAmount + 
                  " 点技能经验，当前总经验: " + newXp + ", 等级: " + newLevel);
    }
    
    /**
     * 更新玩家的击杀统计
     */
    private void updateKillStats(Player player, int monsterLevel) {
        UUID playerId = player.getUUID();
        
        // 获取玩家的击杀统计
        Map<String, Integer> killStats = playerKillStats.computeIfAbsent(playerId, id -> {
            // 初始化新玩家的击杀统计
            Map<String, Integer> newStats = new HashMap<>();
            for (String tier : LEVEL_TIERS) {
                newStats.put(tier, 0);
            }
            
            // 尝试从NBT加载数据
            if (player instanceof ServerPlayer) {
                loadKillStats((ServerPlayer)player, newStats);
            }
            
            return newStats;
        });
        
        // 找到对应的等级段
        String levelTier = getLevelTier(monsterLevel);
        
        // 更新击杀计数
        killStats.put(levelTier, killStats.getOrDefault(levelTier, 0) + 1);
        
        // 如果是服务器玩家，保存统计数据
        if (player instanceof ServerPlayer) {
            saveKillStats((ServerPlayer)player, killStats);
        }
    }
    
    /**
     * 获取怪物等级对应的等级段
     */
    private String getLevelTier(int level) {
        int tierIndex = Math.min(level / 10, 9); // 0-9 对应 10个等级段
        return LEVEL_TIERS[tierIndex];
    }
    
    /**
     * 从NBT加载击杀统计数据
     */
    private void loadKillStats(ServerPlayer player, Map<String, Integer> stats) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(KILLS_STATS_KEY)) {
            CompoundTag killsTag = persistentData.getCompound(KILLS_STATS_KEY);
            
            // 加载每个等级段的击杀数据
            for (String tier : LEVEL_TIERS) {
                if (killsTag.contains(tier)) {
                    stats.put(tier, killsTag.getInt(tier));
                }
            }
            
            LOGGER.debug("已从NBT加载玩家 " + player.getName().getString() + " 的击杀统计");
        }
    }
    
    /**
     * 保存击杀统计数据到NBT
     */
    private void saveKillStats(ServerPlayer player, Map<String, Integer> stats) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag killsTag = new CompoundTag();
        
        // 保存每个等级段的击杀数据
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            killsTag.putInt(entry.getKey(), entry.getValue());
        }
        
        persistentData.put(KILLS_STATS_KEY, killsTag);
        LOGGER.debug("已保存玩家 " + player.getName().getString() + " 的击杀统计到NBT");
    }
    
    /**
     * 清理过期连击
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player != null) {
            UUID playerId = event.player.getUUID();
            ComboTracker combo = playerCombos.get(playerId);
            
            // 如果存在连击且已过期，移除它
            if (combo != null && combo.isExpired()) {
                playerCombos.remove(playerId);
            }
        }
    }

    /**
     * 应用元素效果到目标
     */
    private void applyElementalEffect(Player player, LivingEntity target, ElementType elementType, int playerLevel, boolean isRanged) {
        // 基础安全检查
        if (elementType == ElementType.NONE) return;
        if (player == null || !player.isAlive()) {
            LOGGER.debug("无法应用元素效果：玩家为null或已死亡");
            return;
        }
        if (target == null || !target.isAlive()) {
            LOGGER.debug("无法应用元素效果：目标为null或已死亡");
            return;
        }
        if (target.level() == null || player.level() == null) {
            LOGGER.debug("无法应用元素效果：目标或玩家所在世界为null");
            return;
        }
        
        LOGGER.debug("开始应用" + elementType.getDisplayName() + "元素效果 - 玩家等级:" + playerLevel);
        
        try {
            // 根据武器专精可能加强元素效果
            float elementalBoost = 1.0f;
            WeaponSpecialty specialty = playerWeaponSpecialties.getOrDefault(player.getUUID(), WeaponSpecialty.NONE);
            
            // 如果是近战且玩家有剑专精，或是远程且玩家有弓专精，元素效果提升20%
            if ((isRanged && specialty == WeaponSpecialty.BOW) || (!isRanged && specialty == WeaponSpecialty.SWORD)) {
                elementalBoost = 1.2f;
            }
            
            // 应用对应元素效果
            switch (elementType) {
                case FIRE -> applyFireEffect(player, target, playerLevel, elementalBoost);
                case ICE -> applyIceEffect(player, target, playerLevel, elementalBoost);
                case LIGHTNING -> applyLightningEffect(player, target, playerLevel, elementalBoost);
                case POISON -> applyPoisonEffect(player, target, playerLevel, elementalBoost);
                default -> LOGGER.debug("未知元素类型: " + elementType);
            }
        } catch (Exception e) {
            LOGGER.error("应用元素效果时发生未预期的错误: " + e.getMessage());
            e.printStackTrace(); // 打印详细堆栈跟踪以便调试
        }
    }
    
    /**
     * 应用火元素效果
     */
    private void applyFireEffect(Player player, LivingEntity target, int playerLevel, float elementalBoost) {
        try {
            // 火元素：点燃目标，增加伤害
            float fireDuration = (FIRE_BASE_DURATION + (FIRE_DURATION_PER_LEVEL * playerLevel)) * elementalBoost;
            
            // 设置实体着火
            try {
                target.setRemainingFireTicks((int)(fireDuration * 20));
            } catch (Exception e) {
                LOGGER.error("设置实体着火时出错: " + e.getMessage());
            }
            
            // 额外火焰伤害 - 使用try-catch避免潜在问题
            try {
                float fireDamage = (FIRE_BASE_DAMAGE + (FIRE_DAMAGE_PER_LEVEL * playerLevel)) * elementalBoost;
                if (fireDamage > 0 && target.isAlive()) {
                    target.hurt(target.damageSources().onFire(), fireDamage);
                }
                
                // 提示玩家(只在伤害明显时)
                if (fireDamage >= 3.0f) {
                    String message = String.format("【火元素】造成 %.1f 额外伤害", fireDamage);
                    player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
                }
            } catch (Exception e) {
                LOGGER.error("应用火元素伤害时出错: " + e.getMessage());
            }
            
            // 显示效果
            try {
                spawnElementParticles(target, ElementType.FIRE);
            } catch (Exception e) {
                LOGGER.error("生成火元素粒子效果时出错: " + e.getMessage());
            }
            
            LOGGER.debug("对 " + target.getName().getString() + " 应用火元素效果");
        } catch (Exception e) {
            LOGGER.error("应用火元素效果时出错: " + e.getMessage());
        }
    }
    
    /**
     * 应用冰元素效果
     */
    private void applyIceEffect(Player player, LivingEntity target, int playerLevel, float elementalBoost) {
        try {
            // 冰元素：减速目标，更强的减速效果
            float iceDuration = (ICE_BASE_DURATION + (ICE_DURATION_PER_LEVEL * playerLevel)) * elementalBoost;
            int slowAmplifier = (int)((ICE_BASE_SLOW_AMPLIFIER + (ICE_SLOW_AMPLIFIER_PER_LEVEL * playerLevel)) * elementalBoost);
            
            // 确保最少1级减速效果
            slowAmplifier = Math.max(0, Math.min(slowAmplifier, 4)); // 限制最大减速等级为4
            
            if (target.isAlive()) {
                try {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int)(iceDuration * 20), slowAmplifier));
                    
                    // 高等级时添加挖掘疲劳效果
                    if (playerLevel >= 30) {
                        int fatigueAmplifier = playerLevel >= 60 ? 1 : 0;
                        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, (int)(iceDuration * 15), fatigueAmplifier));
                    }
                } catch (Exception e) {
                    LOGGER.error("添加冰元素状态效果时出错: " + e.getMessage());
                }
            }
            
            // 提示玩家(只在效果明显时)
            if (slowAmplifier >= 2) {
                try {
                    String message = String.format("【冰元素】减速 %d 级，持续 %.1f 秒", slowAmplifier + 1, iceDuration);
                    player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.AQUA));
                } catch (Exception e) {
                    LOGGER.error("显示冰元素提示时出错: " + e.getMessage());
                }
            }
            
            // 显示效果
            try {
                spawnElementParticles(target, ElementType.ICE);
            } catch (Exception e) {
                LOGGER.error("生成冰元素粒子效果时出错: " + e.getMessage());
            }
            
            LOGGER.debug("对 " + target.getName().getString() + " 应用冰元素效果");
        } catch (Exception e) {
            LOGGER.error("应用冰元素效果时出错: " + e.getMessage());
        }
    }
    
    /**
     * 应用雷元素效果
     */
    private void applyLightningEffect(Player player, LivingEntity target, int playerLevel, float elementalBoost) {
        try {
            // 雷元素：有几率连锁伤害，范围更大
            float lightningChance = (LIGHTNING_BASE_CHANCE + (LIGHTNING_CHANCE_PER_LEVEL * playerLevel)) * elementalBoost;
            
            // 增加伤害几率上限
            lightningChance = Math.min(lightningChance, 0.75f);
            
            if (random.nextFloat() < lightningChance) {
                // 尝试寻找范围内其他目标
                Level level = target.level();
                if (level == null) {
                    LOGGER.warn("无法获取目标所在的世界Level，无法应用闪电连锁效果");
                    return;
                }
                
                // 大幅降低搜索半径，防止处理过多实体导致崩溃
                float radius = Math.min(3.0f + (playerLevel / 50.0f), 5.0f);
                Vec3 targetPos = target.position();
                
                // 创建搜索范围
                AABB searchBox = new AABB(
                    targetPos.x - radius, 
                    targetPos.y - radius, 
                    targetPos.z - radius,
                    targetPos.x + radius, 
                    targetPos.y + radius, 
                    targetPos.z + radius
                );
                
                try {
                    // 限制检索数量，避免处理过多实体
                    List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
                        LivingEntity.class, 
                        searchBox, 
                        e -> e != target && e != player && 
                             e.isAlive() &&
                             e instanceof Monster // 只对怪物生效
                    );
                    
                    // 安全检查，避免列表为null
                    if (nearbyEntities == null) {
                        LOGGER.debug("获取的附近实体列表为null");
                        nearbyEntities = List.of(); // 使用空列表
                    }
                    
                    // 严格限制最大连锁数量，降低崩溃风险
                    int maxChainAllowed = Math.min(playerLevel >= 50 ? 3 : (playerLevel >= 25 ? 2 : 1), 3);
                    int chainCount = Math.min(nearbyEntities.size(), maxChainAllowed);
                    
                    // 如果找到了连锁目标
                    if (chainCount > 0) {
                        // 计算连锁伤害
                        float chainDamage = (LIGHTNING_BASE_DAMAGE + (LIGHTNING_DAMAGE_PER_LEVEL * playerLevel)) * elementalBoost;
                        int processedCount = 0;
                        
                        for (int i = 0; i < Math.min(chainCount, nearbyEntities.size()); i++) {
                            LivingEntity chainTarget = nearbyEntities.get(i);
                            // 检查目标是否有效且活着
                            if (chainTarget == null || !chainTarget.isAlive()) {
                                continue;
                            }
                            
                            try {
                                // 应用伤害
                                chainTarget.hurt(chainTarget.damageSources().indirectMagic(player, player), chainDamage);
                                
                                // 仅显示粒子效果，不生成实际的闪电
                                spawnElementParticles(chainTarget, ElementType.LIGHTNING);
                                
                                // 记录日志
                                LOGGER.debug("闪电连锁影响实体: " + chainTarget.getName().getString());
                                
                                processedCount++;
                                if (processedCount >= maxChainAllowed) {
                                    break; // 达到最大连锁数量，强制退出
                                }
                            } catch (Exception e) {
                                LOGGER.error("处理闪电连锁单个目标时出错: " + e.getMessage());
                            }
                        }
                        
                        // 通知玩家
                        if (processedCount > 0) {
                            try {
                                String message = String.format("【雷元素】连锁触发！命中 %d 个额外目标", processedCount);
                                player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW));
                            } catch (Exception e) {
                                LOGGER.error("显示雷元素连锁提示时出错: " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("处理闪电连锁搜索实体时出错: " + e.getMessage(), e);
                }
            }
            
            // 无论连锁是否成功，都在原目标显示效果
            try {
                spawnElementParticles(target, ElementType.LIGHTNING);
            } catch (Exception e) {
                LOGGER.error("生成雷元素粒子效果时出错: " + e.getMessage());
            }
            
            LOGGER.debug("对 " + target.getName().getString() + " 应用雷元素效果");
        } catch (Exception e) {
            LOGGER.error("应用雷元素效果时出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 应用毒元素效果
     */
    private void applyPoisonEffect(Player player, LivingEntity target, int playerLevel, float elementalBoost) {
        try {
            // 毒元素：持续伤害，更高等级和持续时间
            float poisonDuration = (POISON_BASE_DURATION + (POISON_DURATION_PER_LEVEL * playerLevel)) * elementalBoost;
            int poisonAmplifier = (int)((POISON_BASE_AMPLIFIER + (POISON_AMPLIFIER_PER_LEVEL * playerLevel)) * elementalBoost);
            
            // 确保最低1级毒性，且最高不超过4级
            poisonAmplifier = Math.max(0, Math.min(poisonAmplifier, 4));
            
            if (target.isAlive()) {
                try {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, (int)(poisonDuration * 20), poisonAmplifier));
                    
                    // 高等级时添加虚弱效果
                    if (playerLevel >= 35) {
                        int weaknessLevel = playerLevel >= 70 ? 1 : 0; 
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, (int)(poisonDuration * 15), weaknessLevel));
                    }
                } catch (Exception e) {
                    LOGGER.error("添加毒元素状态效果时出错: " + e.getMessage());
                }
            }
            
            // 提示玩家(只在效果明显时)
            if (poisonAmplifier >= 2 || poisonDuration >= 5.0f) {
                try {
                    String message = String.format("【毒元素】中毒 %d 级，持续 %.1f 秒", poisonAmplifier + 1, poisonDuration);
                    player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.DARK_GREEN));
                } catch (Exception e) {
                    LOGGER.error("显示毒元素提示时出错: " + e.getMessage());
                }
            }
            
            // 显示效果
            try {
                spawnElementParticles(target, ElementType.POISON);
            } catch (Exception e) {
                LOGGER.error("生成毒元素粒子效果时出错: " + e.getMessage());
            }
            
            LOGGER.debug("对 " + target.getName().getString() + " 应用毒元素效果");
        } catch (Exception e) {
            LOGGER.error("应用毒元素效果时出错: " + e.getMessage());
        }
    }
    
    /**
     * 生成元素粒子效果
     */
    private void spawnElementParticles(LivingEntity entity, ElementType elementType) {
        try {
            // 安全检查
            if (entity == null) {
                LOGGER.debug("无法生成元素粒子效果：实体为null");
                return;
            }
            if (entity.level() == null) {
                LOGGER.debug("无法生成元素粒子效果：实体所在世界为null");
                return;
            }
            
            // 只在客户端执行效果
            if (entity.level().isClientSide()) {
                // 记录粒子效果请求，实际不生成粒子以避免崩溃
                LOGGER.debug("为 " + entity.getName().getString() + " 生成 " + elementType.getDisplayName() + " 元素粒子效果");
            }
        } catch (Exception e) {
            LOGGER.error("生成元素粒子效果时出错: " + e.getMessage());
        }
    }

    /**
     * 从箭矢中获取拉弓力度
     * 返回0.0-1.0之间的值，表示拉弓程度
     */
    private float getPowerFromArrow(Arrow arrow) {
        try {
            // 安全检查
            if (arrow == null || arrow.getDeltaMovement() == null) {
                LOGGER.debug("无法计算弓箭力度：箭矢或移动向量为null");
                return 1.0F; // 默认返回满力
            }
            
            // 获取箭矢的速度，用于计算拉弓程度
            double velocity = arrow.getDeltaMovement().length();
            
            // 箭矢的典型速度范围在 0.5 (未拉满) 到 3.0 (拉满) 之间
            // 将速度范围映射到 0.0 - 1.0
            float power = (float) ((velocity - 0.5) / 2.5);
            
            // 确保值在 0.0 到 1.0 之间
            return Math.max(0.0F, Math.min(1.0F, power));
        } catch (Exception e) {
            LOGGER.error("计算弓箭力度时出错: " + e.getMessage());
            return 1.0F; // 发生错误时默认返回最大值
        }
    }

    // 武器专精相关方法
    private WeaponSpecialty loadWeaponSpecialty(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(WEAPON_SPECIALTY_KEY)) {
            int ordinal = persistentData.getInt(WEAPON_SPECIALTY_KEY);
            return WeaponSpecialty.values()[ordinal];
        }
        return WeaponSpecialty.NONE;
    }

    private void saveWeaponSpecialty(ServerPlayer player, WeaponSpecialty weaponSpecialty) {
        CompoundTag persistentData = player.getPersistentData();
        persistentData.putInt(WEAPON_SPECIALTY_KEY, weaponSpecialty.ordinal());
    }

    /**
     * 循环切换玩家的武器专精
     */
    public void cycleWeaponSpecialty(Player player) {
        UUID playerId = player.getUUID();
        WeaponSpecialty currentSpecialty = playerWeaponSpecialties.getOrDefault(playerId, WeaponSpecialty.NONE);
        WeaponSpecialty nextSpecialty = WeaponSpecialty.NONE;
        
        // 根据玩家等级判断可用专精
        if (calculateLevelFromXp(playerSkillXp.getOrDefault(playerId, 0)) >= 10) {
            switch (currentSpecialty) {
                case NONE:
                    nextSpecialty = WeaponSpecialty.BOW;
                    break;
                case BOW:
                    nextSpecialty = WeaponSpecialty.SWORD;
                    break;
                case SWORD:
                    nextSpecialty = WeaponSpecialty.NONE;
                    break;
            }
        }
        
        // 更新玩家选择的武器专精
        playerWeaponSpecialties.put(playerId, nextSpecialty);
        
        // 保存选择到NBT
        if (player instanceof ServerPlayer serverPlayer) {
            saveWeaponSpecialty(serverPlayer, nextSpecialty);
        }
        
        // 通知玩家
        String specialtyName = Component.translatable("advancedskills.specialty." + nextSpecialty.name().toLowerCase()).getString();
        player.sendSystemMessage(
            Component.translatable("advancedskills.specialty.switch", specialtyName)
                .withStyle(nextSpecialty.getColor())
        );
        
        // 播放专精切换音效
        player.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);
    }

    // 专精冷却相关方法
    private void saveSpecialtyCooldown(ServerPlayer player, Long cooldown) {
        CompoundTag persistentData = player.getPersistentData();
        persistentData.putLong("AdvancedSkillsSpecialtyCooldown", cooldown);
    }

    private Long loadSpecialtyCooldown(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains("AdvancedSkillsSpecialtyCooldown")) {
            return persistentData.getLong("AdvancedSkillsSpecialtyCooldown");
        }
        return null;
    }

    private void applySpecialtyEffect(Player player, WeaponSpecialty weaponSpecialty, int playerLevel) {
        // 实现专精效果的逻辑
    }

    /**
     * 注册游戏命令
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("注册高级技能命令...");
        
        // 创建命令分发器
        event.getDispatcher().register(
            Commands.literal("askill")
                .requires(source -> source.hasPermission(0)) // 所有玩家可用
                // 经验值命令
                .then(Commands.literal("exp")
                    .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(context -> executeAddExpCommand(
                                    context.getSource(),
                                    EntityArgument.getPlayer(context, "player"),
                                    IntegerArgumentType.getInteger(context, "amount")
                                ))))
                    )
                    .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> executeGetExpCommand(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "player")
                            )))
                    )
                )
                // 生成怪物命令 - 优先处理没有坐标的情况
                .then(Commands.literal("summon")
                    .then(Commands.argument("entityType", StringArgumentType.word())
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                            .executes(context -> {
                                // 无坐标情况下，使用命令执行者位置
                                Vec3 pos = context.getSource().getPosition();
                                return executeSummonCommand(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "entityType"),
                                    IntegerArgumentType.getInteger(context, "level"),
                                    pos
                                );
                            })
                            .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(context -> executeSummonCommand(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "entityType"),
                                    IntegerArgumentType.getInteger(context, "level"),
                                    Vec3Argument.getVec3(context, "pos")
                                ))
                            )
                        )
                    )
                )
        );
    }
    
    /**
     * 执行添加经验命令
     */
    private int executeAddExpCommand(CommandSourceStack source, ServerPlayer targetPlayer, int amount) {
        // 添加技能经验
        addPlayerSkillXp(targetPlayer, amount);
        
        // 获取更新后的等级
        int xp = playerSkillXp.getOrDefault(targetPlayer.getUUID(), loadPlayerSkillXp(targetPlayer));
        int level = calculateLevelFromXp(xp);
        
        // 通知命令执行者
        source.sendSuccess(() -> Component.translatable(
            "commands.advancedskills.exp.add.success", 
            targetPlayer.getDisplayName(),
            amount,
            level,
            xp
        ).withStyle(ChatFormatting.GREEN), true);
        
        // 通知目标玩家
        if (source.getEntity() != targetPlayer) {
            targetPlayer.sendSystemMessage(
                Component.translatable(
                    "commands.advancedskills.exp.add.target",
                    amount,
                    level
                ).withStyle(ChatFormatting.GREEN)
            );
        }
        
        LOGGER.info("管理员 " + source.getDisplayName().getString() + 
                  " 给予玩家 " + targetPlayer.getName().getString() + 
                  " " + amount + " 点技能经验");
        
        return 1; // 成功返回非0值
    }
    
    /**
     * 执行查询经验命令
     */
    private int executeGetExpCommand(CommandSourceStack source, ServerPlayer targetPlayer) {
        // 获取当前经验和等级
        int xp = playerSkillXp.getOrDefault(targetPlayer.getUUID(), loadPlayerSkillXp(targetPlayer));
        int level = calculateLevelFromXp(xp);
        
        // 计算下一级所需经验
        int nextLevel = level + 1;
        int nextLevelXp = calculateXpForLevel(nextLevel);
        int xpNeeded = nextLevelXp - xp;
        
        // 通知命令执行者
        source.sendSuccess(() -> Component.translatable(
            "commands.advancedskills.exp.get.success",
            targetPlayer.getDisplayName().getString(),
            level,
            xp,
            xpNeeded
        ).withStyle(ChatFormatting.GOLD), false);
        
        LOGGER.info("管理员 " + source.getDisplayName().getString() + 
                  " 查询了玩家 " + targetPlayer.getName().getString() + 
                  " 的技能经验数据");
        
        return 1; // 成功返回非0值
    }
    
    /**
     * 执行生成怪物命令
     */
    private int executeSummonCommand(CommandSourceStack source, String entityTypeStr, int entityLevel, Vec3 position) {
        try {
            LOGGER.info("[SUMMON] 尝试生成实体: " + entityTypeStr + ", 等级: " + entityLevel + ", 位置: " + position);
            
            // 检查实体类型是否是有效的 Minecraft 实体类型
            EntityType<?> entityType = null;
            
            // 1. 尝试解析完整的实体类型ID（包含命名空间）
            try {
                ResourceLocation entityId = new ResourceLocation(entityTypeStr);
                entityType = EntityType.byString(entityId.toString()).orElse(null);
                LOGGER.debug("[SUMMON] 尝试直接解析: " + entityId.toString() + ", 结果: " + (entityType != null ? "成功" : "失败"));
            } catch (Exception e) {
                LOGGER.debug("[SUMMON] 直接解析实体ID失败: " + e.getMessage());
            }
            
            // 2. 如果解析失败，尝试添加minecraft:前缀再次解析
            if (entityType == null && !entityTypeStr.contains(":")) {
                try {
                    ResourceLocation entityId = new ResourceLocation("minecraft", entityTypeStr);
                    entityType = EntityType.byString(entityId.toString()).orElse(null);
                    LOGGER.debug("[SUMMON] 尝试添加minecraft:前缀: " + entityId.toString() + ", 结果: " + (entityType != null ? "成功" : "失败"));
                } catch (Exception e) {
                    LOGGER.debug("[SUMMON] 添加minecraft:前缀解析失败: " + e.getMessage());
                }
            }
            
            // 3. 如果还是解析失败，尝试常见简写
            if (entityType == null) {
                // 常见实体类型的映射
                Map<String, String> commonEntities = new HashMap<>();
                commonEntities.put("zombie", "minecraft:zombie");
                commonEntities.put("skeleton", "minecraft:skeleton");
                commonEntities.put("creeper", "minecraft:creeper");
                commonEntities.put("spider", "minecraft:spider");
                commonEntities.put("witch", "minecraft:witch");
                commonEntities.put("slime", "minecraft:slime");
                commonEntities.put("drowned", "minecraft:drowned");
                commonEntities.put("husk", "minecraft:husk");
                commonEntities.put("stray", "minecraft:stray");
                
                String mappedEntityType = commonEntities.get(entityTypeStr.toLowerCase());
                if (mappedEntityType != null) {
                    try {
                        entityType = EntityType.byString(mappedEntityType).orElse(null);
                        LOGGER.debug("[SUMMON] 尝试使用常见映射: " + mappedEntityType + ", 结果: " + (entityType != null ? "成功" : "失败"));
                    } catch (Exception e) {
                        LOGGER.debug("[SUMMON] 使用常见映射解析失败: " + e.getMessage());
                    }
                }
            }
            
            // 如果实体类型无效，返回错误
            if (entityType == null) {
                source.sendFailure(Component.literal("未知的实体类型: " + entityTypeStr));
                return 0;
            }
            
            // 检查是否能生成怪物
            Entity testEntity = entityType.create(source.getLevel());
            if (testEntity == null || !(testEntity instanceof Monster)) {
                source.sendFailure(Component.literal("只能召唤怪物类型的实体!"));
                return 0;
            }
            
            // 如果测试实体创建成功，销毁它
            testEntity.discard();
            
            // 创建实际的实体
            Entity entity = entityType.create(source.getLevel());
            if (entity == null) {
                source.sendFailure(Component.literal("无法创建实体!"));
                return 0;
            }
            
            // 设置实体位置
            entity.moveTo(position.x, position.y, position.z, 0, 0);
            
            // 在添加到世界前，标记为通过命令生成的等级
            if (entity instanceof Monster monster) {
                // 1. 设置持久性NBT数据
                CompoundTag persistentData = monster.getPersistentData();
                persistentData.putBoolean(MONSTER_LEVEL_SET_KEY, true);
                persistentData.putInt(MONSTER_LEVEL_KEY, entityLevel);
                
                // 2. 保存到内存缓存 - 这是修复的关键！
                entityLevels.put(monster.getUUID(), entityLevel);
                
                // 3. 记录将要应用的等级
                LOGGER.info("[SUMMON] 已设置怪物 " + monster.getUUID() + " 的等级为 " + entityLevel + " (NBT+内存)");
            }
            
            // 添加到世界
            Level world = source.getLevel();
            boolean success = world.addFreshEntity(entity);
            
            if (success) {
                if (entity instanceof Monster monster) {
                    // 立即强制应用属性，不等待事件
                    LOGGER.info("[SUMMON] 立即应用怪物等级属性: " + entityLevel);
                    
                    // 获取怪物的最终UUID - 可能在添加到世界时有变化
                    UUID monsterId = monster.getUUID();
                    entityLevels.put(monsterId, entityLevel);
                    
                    // 应用等级属性
                    applyLevelAttributesToMob(monster, entityLevel);
                    
                    // 设置名称
                    String originalName = EntityType.getKey(monster.getType()).getPath();
                    Component newName = Component.literal("Lv." + entityLevel + " " + originalName)
                            .withStyle(getLevelTextColor(entityLevel));
                    monster.setCustomName(newName);
                    monster.setCustomNameVisible(true);
                    
                    // 再次确认持久性数据已设置
                    CompoundTag persistentData = monster.getPersistentData();
                    persistentData.putBoolean(MONSTER_LEVEL_SET_KEY, true);
                    persistentData.putInt(MONSTER_LEVEL_KEY, entityLevel);
                    
                    // 保存变量的最终值以在lambda中使用
                    final int finalLevel = entityLevel;
                    final String entityName = entityType.getDescription().getString();
                    
                    // 成功消息
                    source.sendSuccess(() -> Component.literal("已生成等级为 " + finalLevel + " 的 " + entityName), true);
                    return 1;
                } else {
                    source.sendFailure(Component.literal("实体不是怪物类型，无法应用等级!"));
                    return 0;
                }
            } else {
                source.sendFailure(Component.literal("无法将实体添加到世界!"));
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("生成怪物时发生错误: ", e);
            e.printStackTrace(); // 打印详细堆栈供调试
            source.sendFailure(Component.literal("生成怪物时发生错误: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 获取与等级对应的文本颜色
     */
    private ChatFormatting getLevelTextColor(int level) {
        if (level >= 81) return ChatFormatting.DARK_RED; // 81-100 深红色
        if (level >= 61) return ChatFormatting.RED;      // 61-80 红色
        if (level >= 41) return ChatFormatting.GOLD;     // 41-60 金色
        if (level >= 21) return ChatFormatting.YELLOW;   // 21-40 黄色
        if (level >= 10) return ChatFormatting.GREEN;    // 10-20 绿色
        return ChatFormatting.GRAY;                      // 0-9 灰色
    }
    
    /**
     * 根据等级应用属性增益到怪物
     */
    private void applyLevelAttributesToMob(Monster monster, int level) {
        LOGGER.debug("应用等级 " + level + " 的属性增益到 " + monster.getName().getString());
        
        // 根据等级计算增益百分比 - 优化为更平滑的曲线
        // 公式设计为：低等级线性增长，高等级指数增长
        // 例如：等级10 = 增加20%，等级50 = 增加200%，等级100 = 增加900%
        double healthMultiplier;
        double damageMultiplier;
        
        if (level <= 20) {
            // 低等级：线性增长，每级增加2%
            healthMultiplier = 1.0 + (level * 0.02);
            damageMultiplier = 1.0 + (level * 0.02);
        } else if (level <= 50) {
            // 中等级：每级增加4%
            healthMultiplier = 1.4 + ((level - 20) * 0.04);
            damageMultiplier = 1.4 + ((level - 20) * 0.04);
        } else if (level <= 80) {
            // 高等级：每级增加6%
            healthMultiplier = 2.6 + ((level - 50) * 0.06);
            damageMultiplier = 2.6 + ((level - 50) * 0.06);
        } else {
            // 极高等级：每级增加10%
            healthMultiplier = 4.4 + ((level - 80) * 0.1);
            damageMultiplier = 4.4 + ((level - 80) * 0.1);
        }
        
        // 为不同类型的怪物应用不同的属性倾向
        // 例如：僵尸类更健壮，骷髅类更具攻击性
        String entityType = monster.getType().toString().toLowerCase();
        
        if (entityType.contains("zombie") || entityType.contains("brute") || entityType.contains("piglin")) {
            // 僵尸类型：更高的生命值，较低的攻击力
            healthMultiplier *= 1.3;
            damageMultiplier *= 0.9;
        } else if (entityType.contains("skeleton") || entityType.contains("stray") || entityType.contains("pillager")) {
            // 弓箭手类型：较低的生命值，更高的攻击力
            healthMultiplier *= 0.8;
            damageMultiplier *= 1.4;
        } else if (entityType.contains("creeper")) {
            // 苦力怕：爆炸伤害随等级提升
            healthMultiplier *= 0.9;
            damageMultiplier *= 1.5;
        } else if (entityType.contains("spider") || entityType.contains("cave_spider")) {
            // 蜘蛛类型：更快的移动速度
            healthMultiplier *= 1.0;
            damageMultiplier *= 1.1;
            
            // 增加移动速度
            monster.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(
                new AttributeModifier(
                    UUID.randomUUID(), 
                    "AdvancedSkills level boost", 
                    Math.min(level * 0.005, 0.3), // 最多增加30%速度
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                )
            );
        } else if (entityType.contains("witch")) {
            // 女巫：更高等级的药水效果
            healthMultiplier *= 1.1;
            damageMultiplier *= 1.2;
        }
        
        // 限制最大增益值，防止怪物过于强大
        healthMultiplier = Math.min(healthMultiplier, 10.0); // 最多10倍生命值
        damageMultiplier = Math.min(damageMultiplier, 10.0); // 最多10倍攻击力
        
        // 从属性中移除之前的修改器
        monster.getAttribute(Attributes.MAX_HEALTH).removeModifier(HEALTH_MODIFIER_ID);
        monster.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(DAMAGE_MODIFIER_ID);
        
        // 应用生命值增益
        monster.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(
            new AttributeModifier(
                HEALTH_MODIFIER_ID, 
                "AdvancedSkills level health boost", 
                healthMultiplier - 1.0, // 转换为增加的百分比
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            )
        );
        
        // 应用攻击力增益
        if (monster.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            monster.getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(
                new AttributeModifier(
                    DAMAGE_MODIFIER_ID, 
                    "AdvancedSkills level damage boost", 
                    damageMultiplier - 1.0, // 转换为增加的百分比
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                )
            );
        }
        
        // 恢复满血
        monster.setHealth(monster.getMaxHealth());
        
        // 高等级怪物增加特殊效果
        if (level >= 50) {
            // 50级以上怪物有几率获得随机增益效果
            if (random.nextFloat() < 0.5f) { // 50%几率
                addRandomBuffToMonster(monster, level);
            }
        }
        
        // 超高等级怪物具有永久的抗性提升
        if (level >= 75) {
            int resistanceLevel = (level >= 90) ? 1 : 0;
            monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, resistanceLevel, false, false));
        }
        
        LOGGER.debug("怪物 " + monster.getName().getString() + " 获得了 " + String.format("%.2f", healthMultiplier) + 
                     "倍生命值和 " + String.format("%.2f", damageMultiplier) + "倍攻击力");
    }

    /**
     * 为高等级怪物添加随机增益效果
     */
    private void addRandomBuffToMonster(Monster monster, int level) {
        int effectChoice = random.nextInt(5); // 0-4随机效果
        int duration = 12000; // 10分钟 (600秒 * 20 ticks)
        int amplifier = level >= 80 ? 1 : 0; // 80级以上效果更强
        
        switch (effectChoice) {
            case 0: // 速度提升
                monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, amplifier, false, false));
                break;
            case 1: // 抗性提升
                monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier, false, false));
                break;
            case 2: // 力量增强
                monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, amplifier, false, false));
                break;
            case 3: // 生命恢复
                monster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier, false, false));
                break;
            case 4: // 火焰抗性
                monster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, amplifier, false, false));
                break;
        }
    }

    /**
     * 监听实体加入世界事件，设置怪物等级
     */
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        
        // 仅在服务器端处理
        if (entity.level().isClientSide()) return;
        
        // 处理怪物等级
        if (entity instanceof Monster monster) {
            UUID entityId = monster.getUUID();
            
            // 首先检查缓存中是否已有此实体的等级信息（由命令生成）
            // 这是最高优先级，缓存中的值表示是由summon命令设置的
            if (entityLevels.containsKey(entityId)) {
                int commandSetLevel = entityLevels.get(entityId);
                LOGGER.debug("[实体加入] 从缓存应用指定等级: " + entityId + ", 等级: " + commandSetLevel);
                
                // 重新应用设置，确保不会被覆盖
                CompoundTag persistentData = monster.getPersistentData();
                persistentData.putBoolean(MONSTER_LEVEL_SET_KEY, true);
                persistentData.putInt(MONSTER_LEVEL_KEY, commandSetLevel);
                
                // 确保属性已正确应用
                applyLevelAttributesToMob(monster, commandSetLevel);
                
                // 设置显示名称
                String originalName = EntityType.getKey(monster.getType()).getPath();
                Component newName = Component.literal("Lv." + commandSetLevel + " " + originalName)
                        .withStyle(getLevelTextColor(commandSetLevel));
                monster.setCustomName(newName);
                monster.setCustomNameVisible(true);
                return;
            }
            
            // 检查持久化数据
            CompoundTag persistentData = monster.getPersistentData();
            
            // 检查是否已经设置了等级
            if (persistentData.contains(MONSTER_LEVEL_SET_KEY) && persistentData.getBoolean(MONSTER_LEVEL_SET_KEY)) {
                int entityLevel = persistentData.getInt(MONSTER_LEVEL_KEY);
                LOGGER.debug("[实体加入] 检测到预设等级的怪物: " + entityLevel);
                
                // 确保保存到缓存，这样下次检查时可以直接使用
                entityLevels.put(entityId, entityLevel);
                
                // 确保属性已正确应用
                applyLevelAttributesToMob(monster, entityLevel);
                
                // 设置显示名称
                String originalName = EntityType.getKey(monster.getType()).getPath();
                Component newName = Component.literal("Lv." + entityLevel + " " + originalName)
                        .withStyle(getLevelTextColor(entityLevel));
                monster.setCustomName(newName);
                monster.setCustomNameVisible(true);
                
                return;
            }
            
            // 如果是自然生成的怪物，随机分配等级
            int entityLevel = calculateRandomEntityLevel();
            LOGGER.debug("[实体加入] 为自然生成的怪物设置随机等级: " + entityLevel);
            
            // 保存等级到实体数据
            persistentData.putBoolean(MONSTER_LEVEL_SET_KEY, true);
            persistentData.putInt(MONSTER_LEVEL_KEY, entityLevel);
            
            // 保存到缓存
            entityLevels.put(entityId, entityLevel);
            
            // 应用等级属性
            applyLevelAttributesToMob(monster, entityLevel);
            
            // 设置显示名称
            String originalName = EntityType.getKey(monster.getType()).getPath();
            Component newName = Component.literal("Lv." + entityLevel + " " + originalName)
                    .withStyle(getLevelTextColor(entityLevel));
            monster.setCustomName(newName);
            monster.setCustomNameVisible(true);
        }
    }
    
    /**
     * 计算随机怪物等级，使高等级较少出现
     */
    private int calculateRandomEntityLevel() {
        // 使用二次方分布，使低等级更常见，高等级较少出现
        double randomValue = random.nextDouble();
        return (int) (100 * Math.pow(randomValue, 2));
    }

    /**
     * 根据XP计算玩家等级
     */
    public int calculateLevelFromXp(int xp) {
        return Math.min(100, (int) Math.sqrt(xp / 10));
    }
    
    /**
     * 获取指定等级所需的经验总量
     */
    private int calculateXpForLevel(int level) {
        // 采用平方关系：等级^2 * 1（原来是 * 100，现在降低100倍）
        return level * level;
    }
    
    /**
     * 从NBT加载玩家技能经验
     */
    private int loadPlayerSkillXp(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        return persistentData.getInt(SKILL_XP_KEY);
    }
    
    /**
     * 保存玩家技能经验到NBT
     */
    private void savePlayerSkillXp(Player player, int xp) {
        CompoundTag persistentData = player.getPersistentData();
        persistentData.putInt(SKILL_XP_KEY, xp);
    }

    /**
     * 获取单例实例
     */
    public static AdvancedSkillsMod getInstance() {
        return INSTANCE;
    }

    /**
     * 根据怪物等级计算额外经验
     */
    private int calculateBonusXp(int monsterLevel) {
        // 基础经验 + 等级平方/10 的额外经验
        return (monsterLevel * monsterLevel) / 10;
    }

    /**
     * 检测伤害是否来自玩家射出的箭矢
     * 安全地检查伤害来源，避免空引用导致崩溃
     */
    private boolean isPlayerArrowDamage(LivingHurtEvent event) {
        try {
            // 获取伤害来源
            if (event == null || event.getSource() == null) {
                return false;
            }
            
            // 检查直接伤害实体（如果是箭矢，这将是箭矢实体）
            Entity directEntity = event.getSource().getDirectEntity();
            if (!(directEntity instanceof Arrow)) {
                return false;
            }
            
            // 检查原始伤害实体（射手，应该是玩家）
            Entity sourceEntity = event.getSource().getEntity();
            return (sourceEntity instanceof Player);
        } catch (Exception e) {
            LOGGER.error("检查箭矢伤害来源时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取箭矢的拉弓力度（安全版本）
     * 从伤害事件中安全地提取拉弓力度
     */
    private float getArrowPowerFromEvent(LivingHurtEvent event) {
        try {
            if (event == null || event.getSource() == null) {
                return 1.0f; // 默认满力
            }
            
            Entity directEntity = event.getSource().getDirectEntity();
            if (!(directEntity instanceof Arrow arrow)) {
                return 1.0f; // 默认满力
            }
            
            // 尝试获取箭矢的速度作为估计
            return getPowerFromArrow(arrow);
        } catch (Exception e) {
            LOGGER.error("从事件获取箭矢力度时出错: " + e.getMessage());
            return 1.0f; // 默认满力
        }
    }
    
    /**
     * 处理所有类型的伤害事件：近战、弓箭等
     * 统一处理所有玩家造成的伤害
     */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        try {
            // 跳过客户端事件处理
            if (event.getEntity().level().isClientSide()) {
                return;
            }
            
            // 获取基本信息
            Entity sourceEntity = event.getSource().getEntity();
            if (!(sourceEntity instanceof Player player)) {
                return; // 不是玩家造成的伤害
            }
            
            // 跳过箭矢造成的伤害，因为箭矢伤害现在由onArrowImpact专门处理
            if (event.getSource().getDirectEntity() instanceof Arrow) {
                return;
            }
            
            // 检查玩家手持物品（剑类型近战武器）
            ItemStack heldItem = player.getMainHandItem();
            if (!(heldItem.getItem() instanceof SwordItem) && heldItem.getItem() != Items.TRIDENT) {
                return;
            }
            
            // 获取玩家技能数据
            UUID playerId = player.getUUID();
            int playerLevel = calculateLevelFromXp(playerSkillXp.getOrDefault(playerId, 0));
            ElementType elementType = playerElementTypes.getOrDefault(playerId, ElementType.NONE);
            WeaponSpecialty weaponSpecialty = playerWeaponSpecialties.getOrDefault(playerId, WeaponSpecialty.NONE);
            
            // 获取目标
            LivingEntity target = event.getEntity();
            if (target == null || !target.isAlive()) {
                return;
            }
            
            // 处理近战伤害
            processMeleeDamage(event, player, target, playerLevel, weaponSpecialty, elementType);
            
        } catch (Exception e) {
            // 捕获所有异常，防止崩溃
            LOGGER.error("处理伤害事件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 处理近战伤害
     * 从主方法分离出来的逻辑，处理剑的伤害和效果
     */
    private void processMeleeDamage(LivingHurtEvent event, Player player, LivingEntity target, 
                                   int playerLevel, WeaponSpecialty weaponSpecialty, ElementType elementType) {
        try {
            UUID playerId = player.getUUID();
            
            // 处理连击系统
            float comboCritBonus = 0.0f;
            UUID targetId = target.getUUID();
            ComboTracker combo = playerCombos.get(playerId);
            
            // 检查是否有进行中的连击，以及目标是否相同
            if (combo != null && !combo.isExpired() && combo.getTargetId().equals(targetId)) {
                // 增加连击计数
                combo.incrementCombo();
                
                // 根据连击次数增加暴击几率
                comboCritBonus = COMBO_CRIT_BONUS_PER_HIT * (combo.getComboCount() - 1);
                
                // 提示玩家连击次数
                if (combo.getComboCount() > 1) {
                    try {
                        player.sendSystemMessage(
                            Component.translatable("advancedskills.combo", combo.getComboCount())
                                .withStyle(ChatFormatting.GOLD)
                        );
                    } catch (Exception e) {
                        LOGGER.error("显示连击提示时出错: " + e.getMessage());
                    }
                }
            } else {
                // 开始新的连击
                playerCombos.put(playerId, new ComboTracker(targetId));
            }
            
            // 计算暴击几率
            float critChance = BASE_CRIT_CHANCE + (CRIT_CHANCE_PER_LEVEL * playerLevel) + comboCritBonus;
            
            // 剑专精额外暴击几率
            if (weaponSpecialty == WeaponSpecialty.SWORD) {
                critChance += SWORD_SPECIALTY_CRIT_BONUS;
            }
            
            // 判断是否暴击
            boolean isCritical = random.nextFloat() < critChance;
            
            // 应用基础伤害增益
            float maxPlayerExtraDamage = Math.min(
                BASE_EXTRA_SWORD_DAMAGE + (SWORD_DAMAGE_PER_LEVEL * playerLevel),
                MAX_EXTRA_SWORD_DAMAGE
            );
            
            // 剑专精额外伤害
            if (weaponSpecialty == WeaponSpecialty.SWORD) {
                maxPlayerExtraDamage *= 1.5f; // 剑专精增加50%伤害
            }
            
            float extraDamage = maxPlayerExtraDamage;
            
            // 如果暴击，计算暴击伤害
            if (isCritical) {
                float critDamage = BASE_CRIT_DAMAGE + (CRIT_DAMAGE_PER_LEVEL * playerLevel);
                
                // 剑专精额外暴击伤害
                if (weaponSpecialty == WeaponSpecialty.SWORD) {
                    critDamage += SWORD_SPECIALTY_CRIT_DAMAGE_BONUS;
                }
                
                // 应用暴击伤害倍率
                extraDamage *= critDamage;
                
                // 暴击提示
                try {
                    String message = String.format("【暴击】造成 %.1f 额外伤害！", extraDamage);
                    player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
                    
                    // 播放暴击音效
                    player.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
                } catch (Exception e) {
                    LOGGER.error("显示近战暴击提示时出错: " + e.getMessage());
                }
            }
            
            // 增加伤害
            float originalDamage = event.getAmount();
            event.setAmount(originalDamage + extraDamage);
            
            // 应用元素效果
            try {
                applyElementalEffect(player, target, elementType, playerLevel, false);
            } catch (Exception e) {
                LOGGER.error("应用近战元素效果时出错: " + e.getMessage());
            }
            
            // 在高等级时显示伤害提示（非暴击时）
            if (!isCritical && playerLevel >= 15 && extraDamage >= 5.0f) {
                try {
                    player.sendSystemMessage(
                        Component.literal("造成 " + String.format("%.1f", extraDamage) + " 额外伤害")
                            .withStyle(ChatFormatting.YELLOW)
                    );
                } catch (Exception e) {
                    LOGGER.error("显示额外伤害提示时出错: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("处理近战伤害时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 处理弓箭伤害
     * 从主方法分离出来的逻辑，安全地处理箭矢的伤害和效果
     */
    private void processArrowDamage(LivingHurtEvent event, Player player, LivingEntity target, 
                                   int playerLevel, WeaponSpecialty weaponSpecialty, ElementType elementType) {
        try {
            // 获取拉弓力度
            float drawPercentage = getArrowPowerFromEvent(event);
            
            // 计算暴击几率
            float critChance = BASE_CRIT_CHANCE + (CRIT_CHANCE_PER_LEVEL * playerLevel);
            
            // 弓箭专精额外暴击几率
            if (weaponSpecialty == WeaponSpecialty.BOW) {
                critChance += BOW_SPECIALTY_CRIT_BONUS;
            }
            
            // 满弓额外暴击几率
            if (drawPercentage >= 0.95f) {
                critChance += FULL_DRAW_CRIT_BONUS;
            }
            
            // 判断是否暴击
            boolean isCritical = random.nextFloat() < critChance;
            
            // 应用基础伤害增益
            float maxPlayerExtraDamage = Math.min(
                BASE_EXTRA_ARROW_DAMAGE + (ARROW_DAMAGE_PER_LEVEL * playerLevel),
                MAX_EXTRA_ARROW_DAMAGE
            );
            
            // 弓箭专精额外伤害
            if (weaponSpecialty == WeaponSpecialty.BOW) {
                maxPlayerExtraDamage *= 1.5f; // 弓箭专精增加50%伤害
            }
            
            // 根据拉弓力度调整伤害
            float extraDamage = maxPlayerExtraDamage * drawPercentage;
            
            // 如果暴击，计算暴击伤害
            if (isCritical) {
                float critDamage = BASE_CRIT_DAMAGE + (CRIT_DAMAGE_PER_LEVEL * playerLevel);
                
                // 弓箭专精额外暴击伤害
                if (weaponSpecialty == WeaponSpecialty.BOW) {
                    critDamage += BOW_SPECIALTY_CRIT_DAMAGE_BONUS;
                }
                
                // 应用暴击伤害倍率
                extraDamage *= critDamage;
                
                // 暴击提示
                try {
                    String message = String.format("【暴击】造成 %.1f 额外伤害！", extraDamage);
                    player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
                    
                    // 播放暴击音效
                    player.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 0.5F, 1.0F);
                } catch (Exception e) {
                    LOGGER.error("显示弓箭暴击提示时出错: " + e.getMessage());
                }
            }
            
            // 设置伤害量
            float originalDamage = event.getAmount();
            event.setAmount(originalDamage + extraDamage);
            LOGGER.debug("设置弓箭伤害: " + originalDamage + " -> " + (originalDamage + extraDamage));
            
            // 应用元素效果
            try {
                applyElementalEffect(player, target, elementType, playerLevel, true);
            } catch (Exception e) {
                LOGGER.error("应用弓箭元素效果时出错: " + e.getMessage());
            }
            
            // 在高等级时显示伤害提示（非暴击时）
            if (!isCritical && playerLevel >= 15 && extraDamage >= 5.0f) {
                try {
                    player.sendSystemMessage(
                        Component.literal("弓箭造成 " + String.format("%.1f", extraDamage) + " 额外伤害")
                            .withStyle(ChatFormatting.YELLOW)
                    );
                } catch (Exception e) {
                    LOGGER.error("显示弓箭伤害提示时出错: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("处理弓箭伤害时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 监听玩家等级变化事件
     */
    @SubscribeEvent
    public void onPlayerLevelChange(TickEvent.PlayerTickEvent event) {
        // 只在服务器端处理等级变化
        if (event.player.level().isClientSide() || event.phase != TickEvent.Phase.END) return;
        
        Player player = event.player;
        UUID playerId = player.getUUID();
        
        // 检查玩家是否在我们的系统中
        if (!playerSkillXp.containsKey(playerId)) return;
        
        // 每20ticks(1秒)检查一次
        if (player.tickCount % 20 == 0) {
            int currentXp = playerSkillXp.get(playerId);
            int currentLevel = calculateLevelFromXp(currentXp);
            
            // 玩家数据中存储的上次应用属性时的等级
            CompoundTag persistentData = player.getPersistentData();
            int lastAppliedLevel = persistentData.getInt("AdvancedSkillsLastLevel");
            
            // 如果等级变化，重新应用属性
            if (currentLevel != lastAppliedLevel) {
                LOGGER.info("玩家 " + player.getName().getString() + " 等级从 " + lastAppliedLevel + " 变为 " + currentLevel + "，重新应用属性加成");
                
                // 更新存储的等级
                persistentData.putInt("AdvancedSkillsLastLevel", currentLevel);
                
                // 应用新的属性加成
                applyPlayerLevelAttributes(player);
                
                // 如果是升级（而非首次加载）
                if (lastAppliedLevel > 0 && currentLevel > lastAppliedLevel) {
                    // 通知玩家升级
                    player.sendSystemMessage(Component.literal("恭喜！你的等级提升到了 " + currentLevel + "!").withStyle(ChatFormatting.GOLD));
                    
                    // 播放升级音效
                    player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
                    
                    // 恢复满血满饱食度
                    player.setHealth(player.getMaxHealth());
                    if (player instanceof ServerPlayer) {
                        ((ServerPlayer) player).getFoodData().setFoodLevel(20);
                    }
                    
                    // 解锁元素能力提示
                    ElementType newElementType = ElementType.fromLevel(currentLevel);
                    if (newElementType != ElementType.NONE && newElementType != ElementType.fromLevel(lastAppliedLevel)) {
                        player.sendSystemMessage(Component.literal("你解锁了新的元素类型: " + newElementType.getDisplayName() + "！").withStyle(newElementType.getColor()));
                        player.sendSystemMessage(Component.literal("按 G 键切换元素类型").withStyle(ChatFormatting.GRAY));
                    }
                }
            }
        }
    }
    
    /**
     * 根据玩家等级应用属性加成
     */
    private void applyPlayerLevelAttributes(Player player) {
        try {
            // 获取玩家当前等级
            int level = calculateLevelFromXp(playerSkillXp.getOrDefault(player.getUUID(), 0));
            if (level <= 0) return; // 如果等级为0，不应用任何加成
            
            LOGGER.debug("为玩家 " + player.getName().getString() + " 应用等级 " + level + " 的属性加成");
            
            // 计算各项属性的加成百分比
            float healthBoost = Math.min(level * PLAYER_HEALTH_PER_LEVEL, PLAYER_MAX_HEALTH_BOOST);
            float attackBoost = Math.min(level * PLAYER_ATTACK_PER_LEVEL, PLAYER_MAX_ATTACK_BOOST);
            float armorBoost = Math.min(level * PLAYER_ARMOR_PER_LEVEL, PLAYER_MAX_ARMOR_BOOST);
            float speedBoost = Math.min(level * PLAYER_SPEED_PER_LEVEL, PLAYER_MAX_SPEED_BOOST);
            
            // 根据武器专精增加特定属性
            WeaponSpecialty specialty = playerWeaponSpecialties.getOrDefault(player.getUUID(), WeaponSpecialty.NONE);
            if (specialty == WeaponSpecialty.BOW) {
                attackBoost *= 1.2f; // 弓箭专精增加20%攻击力
                speedBoost *= 1.2f;  // 增加20%移动速度
            } else if (specialty == WeaponSpecialty.SWORD) {
                attackBoost *= 1.2f; // 剑专精增加20%攻击力
                armorBoost *= 1.2f;  // 增加20%护甲
            }
            
            // 先移除旧的修改器
            removePlayerAttributeModifiers(player);
            
            // 应用生命值加成
            if (player.getAttribute(Attributes.MAX_HEALTH) != null && healthBoost > 0) {
                player.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(
                    new AttributeModifier(
                        PLAYER_HEALTH_MODIFIER_ID,
                        "AdvancedSkills player health boost",
                        healthBoost,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
                );
                LOGGER.debug("玩家生命值增加 " + (healthBoost * 100) + "%");
            }
            
            // 应用攻击力加成
            if (player.getAttribute(Attributes.ATTACK_DAMAGE) != null && attackBoost > 0) {
                player.getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(
                    new AttributeModifier(
                        PLAYER_ATTACK_MODIFIER_ID,
                        "AdvancedSkills player attack boost",
                        attackBoost,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
                );
                LOGGER.debug("玩家攻击力增加 " + (attackBoost * 100) + "%");
            }
            
            // 应用护甲加成
            if (player.getAttribute(Attributes.ARMOR) != null && armorBoost > 0) {
                player.getAttribute(Attributes.ARMOR).addTransientModifier(
                    new AttributeModifier(
                        PLAYER_ARMOR_MODIFIER_ID,
                        "AdvancedSkills player armor boost",
                        armorBoost,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
                );
                LOGGER.debug("玩家护甲增加 " + (armorBoost * 100) + "%");
            }
            
            // 应用移动速度加成
            if (player.getAttribute(Attributes.MOVEMENT_SPEED) != null && speedBoost > 0) {
                player.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(
                    new AttributeModifier(
                        PLAYER_SPEED_MODIFIER_ID,
                        "AdvancedSkills player speed boost",
                        speedBoost,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
                );
                LOGGER.debug("玩家移动速度增加 " + (speedBoost * 100) + "%");
            }
        } catch (Exception e) {
            LOGGER.error("应用玩家属性加成时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 移除玩家属性修改器
     */
    private void removePlayerAttributeModifiers(Player player) {
        try {
            if (player.getAttribute(Attributes.MAX_HEALTH) != null) {
                player.getAttribute(Attributes.MAX_HEALTH).removeModifier(PLAYER_HEALTH_MODIFIER_ID);
            }
            
            if (player.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                player.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(PLAYER_ATTACK_MODIFIER_ID);
            }
            
            if (player.getAttribute(Attributes.ARMOR) != null) {
                player.getAttribute(Attributes.ARMOR).removeModifier(PLAYER_ARMOR_MODIFIER_ID);
            }
            
            if (player.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(PLAYER_SPEED_MODIFIER_ID);
            }
        } catch (Exception e) {
            LOGGER.error("移除玩家属性修改器时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 检测弓箭击中，直接修改实体血量并应用元素效果
     * 最简单直接的方法，避免复杂的事件处理逻辑
     */
    @SubscribeEvent
    public void onArrowImpact(ProjectileImpactEvent event) {
        // 只在服务器端处理
        if (event.getProjectile().level().isClientSide()) {
            return;
        }
        
        try {
            // 判断是否为箭矢
            if (!(event.getProjectile() instanceof Arrow arrow)) {
                return;
            }
            
            // 检查是否击中了实体
            if (!(event.getRayTraceResult() instanceof EntityHitResult entityHit)) {
                return;
            }
            
            // 获取射箭的玩家
            Entity shooter = arrow.getOwner();
            if (!(shooter instanceof Player player)) {
                return;
            }
            
            // 获取被击中的实体
            Entity hitEntity = entityHit.getEntity();
            if (!(hitEntity instanceof LivingEntity target) || !target.isAlive()) {
                return;
            }
            
            // 获取玩家技能数据
            UUID playerId = player.getUUID();
            int playerLevel = calculateLevelFromXp(playerSkillXp.getOrDefault(playerId, 0));
            ElementType elementType = playerElementTypes.getOrDefault(playerId, ElementType.NONE);
            WeaponSpecialty weaponSpecialty = playerWeaponSpecialties.getOrDefault(playerId, WeaponSpecialty.NONE);
            
            // 基本伤害计算(简化版)
            float extraDamage = 2.0f + (playerLevel * 0.5f);
            
            // 弓箭专精额外伤害
            if (weaponSpecialty == WeaponSpecialty.BOW) {
                extraDamage *= 1.5f; // 弓箭专精增加50%伤害
            }
            
            // 如果玩家有等级，应用伤害和效果
            if (playerLevel > 0) {
                // 直接对实体造成伤害
                target.hurt(target.damageSources().arrow(arrow, player), extraDamage);
                
                // 直接应用元素效果
                if (elementType != ElementType.NONE) {
                    try {
                        applyElementalEffect(player, target, elementType, playerLevel, true);
                    } catch (Exception e) {
                        LOGGER.error("应用弓箭元素效果时出错: " + e.getMessage());
                    }
                }
                
                // 简单的提示消息
                if (playerLevel >= 15) {
                    player.sendSystemMessage(
                        Component.literal("弓箭造成 " + String.format("%.1f", extraDamage) + " 额外伤害")
                            .withStyle(ChatFormatting.YELLOW)
                    );
                }
            }
        } catch (Exception e) {
            // 捕获所有异常，不让游戏崩溃
            LOGGER.error("处理箭矢撞击时出错: " + e.getMessage());
        }
    }

    /**
     * 处理鼠标点击事件
     * 用于处理UI界面的交互
     */
    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseButton event) {
        // 只处理按下动作
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        
        // 获取当前玩家
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        // 不再需要处理UI点击，因为KillStatsScreen已经处理了
    }

    /**
     * 获取玩家技能经验
     */
    public int getPlayerSkillXp(UUID playerId) {
        return playerSkillXp.getOrDefault(playerId, 0);
    }

    /**
     * 获取玩家元素类型
     */
    public ElementType getPlayerElementType(UUID playerId) {
        return playerElementTypes.getOrDefault(playerId, ElementType.NONE);
    }

    /**
     * 获取玩家武器专精
     */
    public WeaponSpecialty getPlayerWeaponSpecialty(UUID playerId) {
        return playerWeaponSpecialties.getOrDefault(playerId, WeaponSpecialty.NONE);
    }

    /**
     * 获取玩家击杀统计
     */
    public Map<String, Integer> getPlayerKillStats(UUID playerId) {
        return playerKillStats.getOrDefault(playerId, new HashMap<>());
    }
} 