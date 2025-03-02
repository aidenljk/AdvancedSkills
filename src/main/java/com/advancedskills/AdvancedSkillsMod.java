package com.advancedskills;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraftforge.event.RegisterCommandsEvent;

@Mod(AdvancedSkillsMod.MODID)
public class AdvancedSkillsMod {
    public static final String MODID = "advancedskills";
    public static final Logger LOGGER = LoggerFactory.getLogger("AdvancedSkillsMod");
    
    // NBT数据键
    private static final String SKILL_XP_KEY = "AdvancedSkillsXP";
    
    // 存储怪物等级的映射
    private final Map<UUID, Integer> entityLevels = new HashMap<>();
    
    // 存储玩家经验值的映射 - 仅作为缓存，实际数据存在玩家NBT
    private final Map<UUID, Integer> playerSkillXp = new HashMap<>();
    
    // 爆炸大小的基础值和每玩家等级增加量
    private static final float BASE_EXPLOSION_RADIUS = 1.0F;
    private static final float EXPLOSION_RADIUS_PER_LEVEL = 0.1F;
    private static final float MAX_EXPLOSION_RADIUS = 15.0F;
    
    // 额外伤害的基础值和每玩家等级增加量
    private static final float BASE_EXTRA_DAMAGE = 0.0F;
    private static final float DAMAGE_PER_LEVEL = 0.5F;
    private static final float MAX_EXTRA_DAMAGE = 50.0F;
    
    // 怪物属性修改器ID
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("d34d1515-5d6e-4c5d-a91f-6c35d85c21d1");
    private static final UUID DAMAGE_MODIFIER_ID = UUID.fromString("d34d1515-5d6e-4c5d-a91f-6c35d85c21d2");
    
    // 随机数生成器
    private final Random random = new Random();

    public AdvancedSkillsMod() {
        LOGGER.info("高级技能 Mod 初始化 - 怪物等级和玩家进度系统");
        // 注册事件监听器
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    /**
     * 当玩家加入世界时，加载其技能数据
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        
        // 获取玩家的持久化数据
        int xp = loadPlayerSkillXp(player);
        
        // 计算当前等级
        int level = calculateLevelFromXp(xp);
        
        // 将数据存入缓存
        playerSkillXp.put(player.getUUID(), xp);
        
        // 通知玩家其当前等级
        player.sendSystemMessage(Component.literal("欢迎回来! 您的技能等级: " + level)
                .withStyle(ChatFormatting.AQUA));
        
        LOGGER.info("玩家 " + player.getName().getString() + " 登录，技能等级: " + level + ", 经验值: " + xp);
    }
    
    /**
     * 当玩家离开时，确保数据已保存
     */
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUUID();
        
        // 获取缓存中的经验值
        Integer xp = playerSkillXp.get(playerId);
        if (xp != null) {
            // 保存数据
            savePlayerSkillXp(player, xp);
            // 移除缓存
            playerSkillXp.remove(playerId);
        }
        
        LOGGER.info("玩家 " + player.getName().getString() + " 登出，保存了技能数据");
    }
    
    /**
     * 定期保存所有在线玩家的技能数据
     */
    @SubscribeEvent
    public void onWorldTick(TickEvent.LevelTickEvent event) {
        // 每10分钟保存一次数据 (12000 ticks)
        if (event.level.isClientSide() || event.phase != TickEvent.Phase.END) return;
        
        if (event.level.getGameTime() % 12000 == 0) {
            for (Player player : event.level.players()) {
                UUID playerId = player.getUUID();
                Integer xp = playerSkillXp.get(playerId);
                if (xp != null) {
                    savePlayerSkillXp(player, xp);
                    LOGGER.info("自动保存玩家 " + player.getName().getString() + " 的技能数据");
                }
            }
        }
    }
    
    /**
     * 当怪物加入世界时分配等级
     */
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        
        // 只处理怪物实体
        if (entity instanceof Monster monster) {
            // 随机分配0-100的等级，高等级较少出现
            int level = calculateRandomEntityLevel();
            entityLevels.put(monster.getUUID(), level);
            
            // 根据等级增加怪物属性
            applyLevelAttributesToMob(monster, level);
            
            // 给怪物添加带有等级的名称
            String originalName = monster.getName().getString();
            Component newName = Component.literal("Lv." + level + " " + originalName)
                    .withStyle(getLevelTextColor(level));
            monster.setCustomName(newName);
            monster.setCustomNameVisible(true);
            
            LOGGER.debug("生成了等级 " + level + " 的怪物: " + originalName);
        }
    }
    
    /**
     * 当怪物死亡时，根据其等级为玩家提供经验
     */
    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Entity killer = event.getSource().getEntity();
        
        // 检查是否是怪物，以及是否被玩家杀死
        if (entity instanceof Monster monster && killer instanceof Player player) {
            UUID monsterId = monster.getUUID();
            
            // 获取怪物等级
            int monsterLevel = entityLevels.getOrDefault(monsterId, 0);
            
            // 根据怪物等级计算额外经验
            int baseXp = 5; // 基础经验值
            int bonusXp = calculateBonusXp(monsterLevel);
            int totalXp = baseXp + bonusXp;
            
            // 为玩家添加技能经验
            addPlayerSkillXp(player, totalXp);
            
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
     * 监听箭矢撞击事件
     */
    @SubscribeEvent
    public void onArrowImpact(ProjectileImpactEvent event) {
        // 检查是否是箭矢
        if (event.getProjectile() instanceof Arrow arrow) {
            Level level = arrow.level();
            
            // 获取发射箭矢的玩家
            Entity shooter = arrow.getOwner();
            if (!(shooter instanceof Player player)) {
                return; // 不是玩家发射的箭
            }
            
            LOGGER.debug("处理玩家 " + player.getName().getString() + " 的箭矢撞击事件");
            
            // 获取玩家技能等级
            int playerLevel = getPlayerSkillLevel(player);
            
            LOGGER.debug("玩家 " + player.getName().getString() + " 的技能等级: " + playerLevel);
            
            // 获取拉弓程度 (0.0 到 1.0)
            float drawPercentage = getPowerFromArrow(arrow);
            
            LOGGER.debug("玩家的拉弓程度: " + drawPercentage);
            
            // 根据玩家等级和拉弓程度计算爆炸大小
            float maxPlayerExplosionRadius = Math.min(
                BASE_EXPLOSION_RADIUS + (EXPLOSION_RADIUS_PER_LEVEL * playerLevel), 
                MAX_EXPLOSION_RADIUS
            );
            float explosionRadius = maxPlayerExplosionRadius * drawPercentage;
            
            // 根据玩家等级和拉弓程度计算额外伤害
            float maxPlayerExtraDamage = Math.min(
                BASE_EXTRA_DAMAGE + (DAMAGE_PER_LEVEL * playerLevel),
                MAX_EXTRA_DAMAGE
            );
            float extraDamage = maxPlayerExtraDamage * drawPercentage;
            
            LOGGER.debug("计算的额外伤害: " + extraDamage + ", 爆炸半径: " + explosionRadius);
            
            // 应用额外伤害
            double originalDamage = arrow.getBaseDamage();
            arrow.setBaseDamage(originalDamage + extraDamage);
            
            LOGGER.debug("原始伤害: " + originalDamage + ", 增加后的伤害: " + arrow.getBaseDamage());
            
            // 当玩家技能等级足够高，才生成爆炸
            if (playerLevel >= 5 && explosionRadius > 0.5f) {
                // 在箭矢位置创建爆炸
                level.explode(arrow, arrow.getX(), arrow.getY(), arrow.getZ(), explosionRadius, 
                        Level.ExplosionInteraction.TNT);
                
                LOGGER.info("技能等级 " + playerLevel + " 的玩家箭矢撞击 - 生成爆炸! 拉弓程度: " + drawPercentage + 
                          ", 爆炸大小: " + explosionRadius + ", 额外伤害: " + extraDamage);
            } else {
                LOGGER.info("箭矢撞击 - 技能等级不足或爆炸半径太小，不生成爆炸");
            }
            
            // 移除箭矢以防止多次爆炸
            arrow.discard();
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
     * 根据怪物等级应用属性修改
     */
    private void applyLevelAttributesToMob(Monster monster, int level) {
        // 计算血量和伤害倍率 (0.0 ~ 10.0)
        double healthMultiplier = level / 10.0;
        double damageMultiplier = level / 10.0;
        
        // 应用血量修改器
        monster.getAttribute(Attributes.MAX_HEALTH).removeModifier(HEALTH_MODIFIER_ID);
        monster.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(
            new AttributeModifier(
                HEALTH_MODIFIER_ID,
                "Level health bonus",
                healthMultiplier,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            )
        );
        
        // 设置满血
        monster.setHealth(monster.getMaxHealth());
        
        // 应用伤害修改器
        if (monster.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            monster.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(DAMAGE_MODIFIER_ID);
            monster.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(
                new AttributeModifier(
                    DAMAGE_MODIFIER_ID,
                    "Level damage bonus",
                    damageMultiplier,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                )
            );
        }
    }
    
    /**
     * 根据怪物等级计算额外经验
     */
    private int calculateBonusXp(int monsterLevel) {
        // 基础经验 + 等级平方/10 的额外经验
        return (monsterLevel * monsterLevel) / 10;
    }
    
    /**
     * 为玩家添加技能经验，并可能升级
     */
    private void addPlayerSkillXp(Player player, int xpAmount) {
        UUID playerId = player.getUUID();
        
        // 获取当前经验值
        int currentXp = playerSkillXp.getOrDefault(playerId, loadPlayerSkillXp(player));
        
        // 计算当前等级
        int currentLevel = calculateLevelFromXp(currentXp);
        
        // 添加经验
        int newXp = currentXp + xpAmount;
        
        // 计算新等级
        int newLevel = calculateLevelFromXp(newXp);
        
        // 保存新经验值
        playerSkillXp.put(playerId, newXp);
        savePlayerSkillXp(player, newXp);
        
        // 检查是否升级
        if (newLevel > currentLevel) {
            // 通知玩家升级
            player.sendSystemMessage(Component.literal("技能等级提升到 " + newLevel + "!")
                    .withStyle(ChatFormatting.GOLD));
            
            LOGGER.info("玩家 " + player.getName().getString() + " 技能等级提升到 " + newLevel);
        }
    }
    
    /**
     * 从经验值计算等级
     */
    private int calculateLevelFromXp(int xp) {
        if (xp <= 0) return 0;
        
        // 从经验值计算等级
        for (int level = 1; level <= 100; level++) {
            int xpForNextLevel = calculateXpForLevel(level + 1);
            if (xp < xpForNextLevel) {
                return level;
            }
        }
        return 100; // 达到最大等级
    }
    
    /**
     * 获取指定等级所需的经验总量
     */
    private int calculateXpForLevel(int level) {
        // 采用平方关系：等级^2 * 1（原来是 * 100，现在降低100倍）
        return level * level;
    }
    
    /**
     * 获取玩家当前技能等级
     */
    private int getPlayerSkillLevel(Player player) {
        UUID playerId = player.getUUID();
        
        // 从缓存获取经验值，如果缓存没有则从NBT加载
        int xp = playerSkillXp.getOrDefault(playerId, loadPlayerSkillXp(player));
        
        // 确保缓存最新值
        playerSkillXp.put(playerId, xp);
        
        // 从经验值计算等级
        return calculateLevelFromXp(xp);
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
     * 获取与等级对应的文本颜色
     */
    private ChatFormatting getLevelTextColor(int level) {
        if (level >= 90) return ChatFormatting.DARK_RED;
        if (level >= 70) return ChatFormatting.RED;
        if (level >= 50) return ChatFormatting.GOLD;
        if (level >= 30) return ChatFormatting.YELLOW;
        if (level >= 10) return ChatFormatting.GREEN;
        return ChatFormatting.GRAY;
    }
    
    /**
     * 从箭矢中获取拉弓力度
     * 
     * @param arrow 箭矢实体
     * @return 拉弓力度 (0.0 到 1.0)
     */
    private float getPowerFromArrow(Arrow arrow) {
        // 获取箭矢的速度，用于计算拉弓程度
        double velocity = arrow.getDeltaMovement().length();
        
        // 箭矢的典型速度范围在 0.5 (未拉满) 到 3.0 (拉满) 之间
        // 将速度范围映射到 0.0 - 1.0
        float power = (float) ((velocity - 0.5) / 2.5);
        
        // 确保值在 0.0 到 1.0 之间
        return Math.max(0.0F, Math.min(1.0F, power));
    }

    /**
     * 注册游戏命令
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("注册游戏命令");
        
        // 注册exp命令
        event.getDispatcher().register(
            Commands.literal("askill")
                .requires(source -> source.hasPermission(2)) // 需要权限等级2（op权限）
                .then(Commands.literal("exp")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(context -> executeAddExpCommand(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "player"),
                                IntegerArgumentType.getInteger(context, "amount")
                            ))
                        )
                    )
                    .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> executeGetExpCommand(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "player")
                            ))
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
        source.sendSuccess(() -> Component.literal("已给予 ")
                .append(targetPlayer.getDisplayName())
                .append(" " + amount + " 点技能经验。当前等级：" + level + " (总经验：" + xp + ")")
                .withStyle(ChatFormatting.GREEN), true);
        
        // 通知目标玩家
        if (source.getEntity() != targetPlayer) {
            targetPlayer.sendSystemMessage(Component.literal("你获得了 " + amount + " 点技能经验！当前等级：" + level)
                    .withStyle(ChatFormatting.GREEN));
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
        source.sendSuccess(() -> Component.literal(targetPlayer.getDisplayName().getString())
                .append(" 的技能等级：" + level + " (总经验：" + xp + ")")
                .append("\n距离下一级还需：" + xpNeeded + " 经验")
                .withStyle(ChatFormatting.GOLD), false);
        
        LOGGER.info("管理员 " + source.getDisplayName().getString() + 
                  " 查询了玩家 " + targetPlayer.getName().getString() + 
                  " 的技能经验数据");
        
        return 1; // 成功返回非0值
    }
} 