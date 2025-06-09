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
    public final PlayerStatsManager playerStatsManager;
    public final CombatManager combatManager;
    
    // Weapon/Combat related constants (damage, range, crit, elemental effects etc.)
    // have been moved to CombatManager.java
    
    // Player Attribute Modifier UUIDs and constants are now in PlayerStatsManager.
    
    // 随机数生成器
    private final Random random = new Random();
    
    // Mob-specific NBT keys (MONSTER_LEVEL_KEY, MONSTER_LEVEL_SET_KEY),
    // attribute modifier UUIDs (HEALTH_MODIFIER_ID, DAMAGE_MODIFIER_ID for mobs),
    // and the entityLevels map have been moved to or are managed by MobStatsManager.
    // LEVEL_TIERS array is also now in MobStatsManager.
    
    // 统计按键
    public static KeyMapping statsKeyMapping;

    // 用于客户端访问Mod实例的静态引用
    private static AdvancedSkillsMod INSTANCE;
    public static final com.advancedskills.skills.ISkill ELEMENTAL_BLAST_SKILL = new com.advancedskills.skills.ElementalBlastSkill();
    public static final com.advancedskills.skills.ISkill DEFENSIVE_STANCE_SKILL = new com.advancedskills.skills.DefensiveStanceSkill();

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

        public String getTranslationKey() { // For component translation
            return "advancedskills.element." + this.name().toLowerCase();
        }
        
        public static ElementType fromLevel(int level) {
            if (level >= 55) return POISON;
            if (level >= 40) return LIGHTNING;
            if (level >= 25) return ICE;
            if (level >= 10) return FIRE;
            return NONE;
        }

        public ElementType getNext(int playerLevel) {
            ElementType next = NONE;
            if (playerLevel < 10) return NONE; // No elements if below level 10

            switch (this) {
                case NONE: next = FIRE; break;
                case FIRE: next = (playerLevel >= 25) ? ICE : NONE; break;
                case ICE: next = (playerLevel >= 40) ? LIGHTNING : NONE; break;
                case LIGHTNING: next = (playerLevel >= 55) ? POISON : NONE; break;
                case POISON: next = NONE; break;
            }
            // This logic ensures that a player cannot cycle to an element they haven't unlocked yet.
            // It checks if the 'next' element is of a higher tier than what is currently available based on fromLevel.
            if (next != NONE && ElementType.fromLevel(playerLevel).ordinal() < next.ordinal() && !(this == NONE && next == FIRE && playerLevel >=10) ) {
                 return NONE; // Cannot cycle to this element yet
            }
            return next;
        }
    }
    
    // Elemental effect, weapon damage, and crit system constants
    // have been moved to CombatManager.java.
    
    // 按键切换元素
    public static KeyMapping elementKeyMapping;

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

        public String getTranslationKey() { // For component translation
            return "advancedskills.specialty." + this.name().toLowerCase();
        }

        public WeaponSpecialty getNext(int playerLevel) {
            if (playerLevel < 10) return NONE; // No specialties if below level 10
            switch (this) {
                case NONE: return BOW;
                case BOW: return SWORD;
                case SWORD: return NONE;
            }
            return NONE;
        }
    }
    
    // Combo system constants (COMBO_WINDOW_TICKS, etc.)
    // have been moved to CombatManager.java or its ComboTracker class.

    // Combo system (ComboTracker class, playerCombos map, and related constants)
    // has been moved to CombatManager.java.

    // The field 'private final Map<UUID, Integer> entityLevels' has been removed as its functionality is in MobStatsManager.
    
    // 专精切换按键
    public static KeyMapping specialtyKeyMapping;
    public static KeyMapping activeSkill1KeyMapping;
    public static KeyMapping activeSkill2KeyMapping;

    // 显示统计信息标志
    private static boolean showStatsInfo = false;

    private Minecraft client;
    // public final PlayerStatsManager playerStatsManager; // Duplicate removed
    public final MobStatsManager mobStatsManager; // Instance of the new manager

    public AdvancedSkillsMod() {
        LOGGER.info("初始化高级技能Mod");
        this.client = Minecraft.getInstance();
        
        // 设置实例引用
        INSTANCE = this;
        this.playerStatsManager = new PlayerStatsManager(); // Initialize with no-arg constructor
        this.mobStatsManager = new MobStatsManager(this);
        this.combatManager = new CombatManager(this, this.playerStatsManager);

        // 获取MOD事件总线和Forge事件总线
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
        
        // 注册客户端设置和按键绑定监听器
        modEventBus.addListener(this::clientSetup);
        // Register KeyInputHandler and KeyMappings
        modEventBus.addListener(this::registerKeyBindings); // Make sure this is listening on the MOD bus
        MinecraftForge.EVENT_BUS.register(new KeyInputHandler(this)); // Pass instance if needed, or rely on static access
        com.advancedskills.init.ModEntityTypes.register(modEventBus); // Added EntityType registration
        
        LOGGER.info("注册热键：切换元素类型(G)，显示统计信息(K)，切换武器专精(L)");
        // The onKeyInput method from AdvancedSkillsMod is removed as per refactoring plan.
        // KeyInputHandler will now be the sole handler for these key presses.
    }
    
    /**
     * 客户端设置
     */
    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("==== 初始化客户端UI与热键 - 开始 ====");
        
        event.enqueueWork(() -> {
            LOGGER.info("AdvancedSkillsMod client setup completed.");
        });
        
        LOGGER.info("==== 初始化客户端UI与热键 - 完成 ====");
    }
    
    /**
     * 注册按键绑定
     */
    @SubscribeEvent
    public void registerKeyBindings(final RegisterKeyMappingsEvent event) {
        LOGGER.info("==== 高级技能 Mod 注册按键绑定开始 ====");
        
        statsKeyMapping = new KeyMapping(
            "key.advancedskills.stats", // 键位名称
            GLFW.GLFW_KEY_K,           // 默认键位（K键）
            "key.categories.advancedskills" // 键位分类
        );
        event.register(statsKeyMapping);
        LOGGER.info("Registered key mapping: {} ({})", statsKeyMapping.getName(), statsKeyMapping.getKey().getDisplayName().getString());
        
        elementKeyMapping = new KeyMapping(
            "key.advancedskills.element", // 键位名称
            GLFW.GLFW_KEY_G,             // 默认键位（G键，避免冲突）
            "key.categories.advancedskills" // 键位分类
        );
        event.register(elementKeyMapping);
        LOGGER.info("Registered key mapping: {} ({})", elementKeyMapping.getName(), elementKeyMapping.getKey().getDisplayName().getString());
        
        specialtyKeyMapping = new KeyMapping(
            "key.advancedskills.specialty", // 键位名称
            GLFW.GLFW_KEY_M,                // 默认键位（M键）
            "key.categories.advancedskills"  // 键位分类
        );
        event.register(specialtyKeyMapping);
        LOGGER.info("Registered key mapping: {} ({})", specialtyKeyMapping.getName(), specialtyKeyMapping.getKey().getDisplayName().getString());

        activeSkill1KeyMapping = new KeyMapping(
            "key.advancedskills.activeskill1", // Key name
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, // Input type
            org.lwjgl.glfw.GLFW.GLFW_KEY_R, // Default key (R)
            "key.categories.advancedskills" // Category
        );
        event.register(activeSkill1KeyMapping);
        LOGGER.info("Registered key mapping: {} ({})", activeSkill1KeyMapping.getName(), activeSkill1KeyMapping.getKey().getDisplayName().getString());

        activeSkill2KeyMapping = new KeyMapping(
            "key.advancedskills.activeskill2", // Key name
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, // Input type
            org.lwjgl.glfw.GLFW.GLFW_KEY_F, // Default key (F)
            "key.categories.advancedskills" // Category
        );
        event.register(activeSkill2KeyMapping);
        LOGGER.info("Registered key mapping: {} ({})", activeSkill2KeyMapping.getName(), activeSkill2KeyMapping.getKey().getDisplayName().getString());
        
        LOGGER.info("==== 高级技能 Mod 按键绑定注册完成 ====");
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
        int playerXp = playerStatsManager.getPlayerSkillXp(playerId);
        int playerLevel = playerStatsManager.calculateLevelFromXp(playerXp);
        ElementType elementType = playerStatsManager.getPlayerElementType(playerId);
        WeaponSpecialty specialty = playerStatsManager.getPlayerWeaponSpecialty(playerId);
        
        // 获取击杀统计 via PlayerStatsManager
        Map<String, Integer> stats = playerStatsManager.getPlayerKillStats(playerId);
        
        // 创建并显示统计屏幕
        KillStatsScreen statsScreen = new KillStatsScreen();
        statsScreen.updateStats(playerLevel, playerXp, stats, elementType, specialty);
        Minecraft.getInstance().setScreen(statsScreen);
    }
    
    /**
     * 循环切换玩家的元素类型
     */
    public void cycleElementType(Player player) {
        playerStatsManager.cycleElementType(player);
    }

    // saveElementType, loadElementType are now in PlayerStatsManager
    
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
            playerStatsManager.handlePlayerLogin(serverPlayer); // Delegate to manager

            // Retrieve info from manager for login messages
            int level = playerStatsManager.calculateLevelFromXp(playerStatsManager.getPlayerSkillXp(player.getUUID()));
            ElementType elementType = playerStatsManager.getPlayerElementType(player.getUUID());
            WeaponSpecialty specialty = playerStatsManager.getPlayerWeaponSpecialty(player.getUUID());

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
        if (event.level.getGameTime() % 600 == 0) { // Every 30 seconds
            if (this.playerStatsManager != null) {
                for (ServerPlayer player : event.level.getServer().getPlayerList().getPlayers()) {
                    this.playerStatsManager.handleWorldSave(player); // Corrected method name
                    LOGGER.debug("Player data save requested for {} during world tick.", player.getName().getString());
                }
            } else {
                LOGGER.error("PlayerStatsManager is null in onWorldTick.");
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

        if (entity instanceof Monster monster && killer instanceof Player player) {
            if (player.level().isClientSide()) return;

            int monsterLevel = 0;
            if (this.mobStatsManager != null) {
                monsterLevel = this.mobStatsManager.getMobLevelFromNBTOrCache(monster);
            } else {
                LOGGER.error("MobStatsManager is null in onEntityDeath for monster {}", monster.getName().getString());
            }
            
            int baseXp = 5;
            int bonusXp = calculateBonusXp(monsterLevel); // calculateBonusXp is still local or needs to be moved
            int totalXp = baseXp + bonusXp;

            if (this.playerStatsManager != null) {
                this.playerStatsManager.addPlayerSkillXp(player, totalXp);
                // Assuming PlayerStatsManager.updateKillStats now takes tier name string
                this.playerStatsManager.updateKillStats(player, monsterLevel, MobStatsManager.LEVEL_TIERS); // Use LEVEL_TIERS from MobStatsManager
            } else {
                LOGGER.error("PlayerStatsManager is null in onEntityDeath for killer {}", player.getName().getString());
            }
            
            if (this.mobStatsManager != null) {
                this.mobStatsManager.removeMobData(monster.getUUID());
            }

            player.sendSystemMessage(Component.literal("Gained " + totalXp + " skill XP! (Mob Level: " + monsterLevel + ")")
                    .withStyle(ChatFormatting.GREEN));
            LOGGER.info("Player {} killed level {} {} and gained {} XP.",
                player.getName().getString(), monsterLevel, EntityType.getKey(monster.getType()).getPath(), totalXp);
        }
    }
    
    // Player stat helper methods (updateKillStats, loadKillStats, saveKillStats, getLevelTier for player stats)
    // and their associated NBT Key constants (SKILL_XP_KEY, KILLS_STATS_KEY etc.) are removed.
    // These functionalities are now within PlayerStatsManager.
    // LEVEL_TIERS is static in MobStatsManager (AdvancedSkillsMod.LEVEL_TIERS is an alias for now if needed, or pass directly).
    // getLevelTier for mob display purposes is now static in MobStatsManager.
    
    /**
     * 清理过期连击
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player != null) {
            // Player Level change and attribute application
            if (!event.player.level().isClientSide() && this.playerStatsManager != null) {
                this.playerStatsManager.handlePlayerLevelChange(event.player); // Renamed from handlePlayerTick
            }
            // Combo expiry
            if (this.combatManager != null) {
                this.combatManager.handlePlayerTick(event.player);
            }
        }
    }

    // Elemental effect methods (applyElementalEffect, applyFireEffect, applyIceEffect,
    // applyLightningEffect, applyPoisonEffect, spawnElementParticles) and getPowerFromArrow
    // have been removed as their logic is now in CombatManager.

    public void cycleWeaponSpecialty(Player player) {
        playerStatsManager.cycleWeaponSpecialty(player);
    }

    // loadWeaponSpecialty, saveWeaponSpecialty, saveSpecialtyCooldown, loadSpecialtyCooldown are in PlayerStatsManager

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
        playerStatsManager.addPlayerSkillXp(targetPlayer, amount);
        
        int xp = playerStatsManager.getPlayerSkillXp(targetPlayer.getUUID());
        int level = playerStatsManager.calculateLevelFromXp(xp);
        
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
        int xp = playerStatsManager.getPlayerSkillXp(targetPlayer.getUUID());
        int level = playerStatsManager.calculateLevelFromXp(xp);
        int nextLevelXp = playerStatsManager.calculateXpForLevel(level + 1);
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
            LOGGER.info("[SUMMON] Attempting to summon entity: {}, level: {}, position: {}", entityTypeStr, entityLevel, position);
            
            EntityType<?> entityType = null;
            try {
                ResourceLocation entityId = new ResourceLocation(entityTypeStr);
                entityType = EntityType.byString(entityId.toString()).orElse(null);
            } catch (Exception e) { /* no-op */ }

            if (entityType == null && !entityTypeStr.contains(":")) {
                try {
                    ResourceLocation entityId = new ResourceLocation("minecraft", entityTypeStr);
                    entityType = EntityType.byString(entityId.toString()).orElse(null);
                } catch (Exception e) { /* no-op */ }
            }
            // Simplified common entities mapping, can be expanded if needed
            if (entityType == null) {
                 Map<String, String> commonEntities = Map.of("zombie", "minecraft:zombie", "skeleton", "minecraft:skeleton", "creeper", "minecraft:creeper");
                 String mapped = commonEntities.get(entityTypeStr.toLowerCase());
                 if(mapped != null) try{ entityType = EntityType.byString(mapped).orElse(null); } catch (Exception e) { /*no-op*/ }
            }

            if (entityType == null) {
                source.sendFailure(Component.literal("Unknown entity type: " + entityTypeStr));
                return 0;
            }

            Entity testEntity = entityType.create(source.getLevel());
            if (!(testEntity instanceof Monster)) {
                source.sendFailure(Component.literal("Can only summon Monster type entities!"));
                if (testEntity != null) testEntity.discard();
                return 0;
            }
            if (testEntity != null) testEntity.discard();

            Entity newEntity = entityType.create(source.getLevel());
            if (newEntity == null) {
                source.sendFailure(Component.literal("Failed to create entity!"));
                return 0;
            }
            newEntity.moveTo(position.x, position.y, position.z, 0, 0);

            if (newEntity instanceof Monster newMonster) {
                if (this.mobStatsManager != null) {
                    this.mobStatsManager.setSummonedMobLevel(newMonster, entityLevel);
                    // Display name and attributes will be set by handleMobJoinWorld via MobStatsManager
                } else {
                     LOGGER.error("MobStatsManager is null in executeSummonCommand for new monster {}", newMonster.getName().getString());
                }
            }

            if (!source.getLevel().addFreshEntity(newEntity)) {
                source.sendFailure(Component.literal("Failed to add entity to world!"));
                return 0;
            }
            // Attributes and display name are now handled when the mob joins the world by MobStatsManager.
            // The old direct calls to applyLevelAttributesToMob and setCustomName are removed from here.
            final String finalEntityName = entityType.getDescription().getString();
            source.sendSuccess(() -> Component.literal("Summoned level " + entityLevel + " " + finalEntityName), true);
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error summoning mob: ", e);
            source.sendFailure(Component.literal("Error summoning mob: " + e.getMessage()));
            return 0;
        }
    }
    
    // getLevelTextColor() has been removed and its functionality moved to MobStatsManager.
    
    // applyLevelAttributesToMob() has been removed and its functionality moved to MobStatsManager.

    // addRandomBuffToMonster() has been removed and its functionality moved to MobStatsManager.

    /**
     * 监听实体加入世界事件，设置怪物等级
     */
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (entity instanceof Monster monster) {
            if (this.mobStatsManager != null) {
                this.mobStatsManager.handleMobJoinWorld(monster);
            } else {
                LOGGER.error("MobStatsManager is null in onEntityJoinWorld for mob {}", monster.getName().getString());
            }
        }
    }
    
    // calculateRandomEntityLevel() has been removed and its functionality moved to MobStatsManager.

    /**
     * 根据XP计算玩家等级
     */
    // calculateLevelFromXp, calculateXpForLevel, loadPlayerSkillXp, savePlayerSkillXp are now in PlayerStatsManager.
    // SKILL_XP_KEY is also in PlayerStatsManager.

    /**
     * 获取单例实例
     */
    public static AdvancedSkillsMod getInstance() {
        return INSTANCE;
    }

    /**
     * 根据怪物等级计算额外经验
     */
    private static int calculateBonusXp(int monsterLevel) { // Added static
        // 基础经验 + 等级平方/10 的额外经验
        return (monsterLevel * monsterLevel) / 10;
    }

    // isPlayerArrowDamage() and getArrowPowerFromEvent() methods removed as their logic
    // is incorporated into or made obsolete by CombatManager.
    
    /**
     * 处理所有类型的伤害事件：近战、弓箭等
     * 统一处理所有玩家造成的伤害
     */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        // Initial checks for client side and non-player source remain
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return; // Ensure target is LivingEntity

        if (this.combatManager != null) {
            this.combatManager.handleLivingHurt(event, player, target);
        } else {
            LOGGER.error("CombatManager is null in onLivingHurt for attacker {}", player.getName().getString());
        }
    }
    
    // processMeleeDamage() and processArrowDamage() methods removed as their logic
    // has been moved to CombatManager.
    
    /**
     * 监听玩家等级变化事件
     */
    @SubscribeEvent
    public void onPlayerLevelChange(TickEvent.PlayerTickEvent event) {
        // 只在服务器端处理等级变化
        if (event.player.level().isClientSide() || event.phase != TickEvent.Phase.END) return;
        
        Player player = event.player;
        UUID playerId = player.getUUID();
        
        // This logic is now handled by playerStatsManager.handlePlayerLevelChange(player)
    }

    // applyPlayerLevelAttributes and removePlayerAttributeModifiers are now in PlayerStatsManager.
    
    /**
     * 检测弓箭击中，直接修改实体血量并应用元素效果
     * 最简单直接的方法，避免复杂的事件处理逻辑
     */
    @SubscribeEvent
    public void onArrowImpact(ProjectileImpactEvent event) {
        if (event.getProjectile().level().isClientSide()) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        if (!(arrow.getOwner() instanceof Player shooter)) return;
        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHit)) return;
        if (!(entityHit.getEntity() instanceof LivingEntity target && target.isAlive())) return;

        if (this.combatManager != null) {
            this.combatManager.handleArrowImpact(event, arrow, shooter, target);
        } else {
            LOGGER.error("CombatManager is null in onArrowImpact for shooter {}", shooter.getName().getString());
        }
    }

    /**
     * 处理鼠标点击事件
     * 用于处理UI界面的交互
     */
    // Player-specific getters are removed as PlayerStatsManager should be used directly
    // by external classes needing this information, typically via AdvancedSkillsMod.getInstance().playerStatsManager.

    // The onKeyInput method has been removed from this class.
    // All mod-specific key press handling is now done in KeyInputHandler.java.
} 