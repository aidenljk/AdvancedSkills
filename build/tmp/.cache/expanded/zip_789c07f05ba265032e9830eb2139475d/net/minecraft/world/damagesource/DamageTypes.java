package net.minecraft.world.damagesource;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public interface DamageTypes {
    ResourceKey<DamageType> IN_FIRE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("in_fire"));
    ResourceKey<DamageType> LIGHTNING_BOLT = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("lightning_bolt"));
    ResourceKey<DamageType> ON_FIRE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("on_fire"));
    ResourceKey<DamageType> LAVA = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("lava"));
    ResourceKey<DamageType> HOT_FLOOR = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("hot_floor"));
    ResourceKey<DamageType> IN_WALL = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("in_wall"));
    ResourceKey<DamageType> CRAMMING = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("cramming"));
    ResourceKey<DamageType> DROWN = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("drown"));
    ResourceKey<DamageType> STARVE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("starve"));
    ResourceKey<DamageType> CACTUS = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("cactus"));
    ResourceKey<DamageType> FALL = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("fall"));
    ResourceKey<DamageType> FLY_INTO_WALL = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("fly_into_wall"));
    ResourceKey<DamageType> FELL_OUT_OF_WORLD = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("out_of_world"));
    ResourceKey<DamageType> GENERIC = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("generic"));
    ResourceKey<DamageType> MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("magic"));
    ResourceKey<DamageType> WITHER = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("wither"));
    ResourceKey<DamageType> DRAGON_BREATH = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("dragon_breath"));
    ResourceKey<DamageType> DRY_OUT = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("dry_out"));
    ResourceKey<DamageType> SWEET_BERRY_BUSH = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sweet_berry_bush"));
    ResourceKey<DamageType> FREEZE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("freeze"));
    ResourceKey<DamageType> STALAGMITE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("stalagmite"));
    ResourceKey<DamageType> FALLING_BLOCK = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("falling_block"));
    ResourceKey<DamageType> FALLING_ANVIL = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("falling_anvil"));
    ResourceKey<DamageType> FALLING_STALACTITE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("falling_stalactite"));
    ResourceKey<DamageType> STING = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sting"));
    ResourceKey<DamageType> MOB_ATTACK = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("mob_attack"));
    ResourceKey<DamageType> MOB_ATTACK_NO_AGGRO = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("mob_attack_no_aggro"));
    ResourceKey<DamageType> PLAYER_ATTACK = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("player_attack"));
    ResourceKey<DamageType> ARROW = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("arrow"));
    ResourceKey<DamageType> TRIDENT = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("trident"));
    ResourceKey<DamageType> MOB_PROJECTILE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("mob_projectile"));
    ResourceKey<DamageType> SPIT = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("spit"));
    ResourceKey<DamageType> WIND_CHARGE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("wind_charge"));
    ResourceKey<DamageType> FIREWORKS = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("fireworks"));
    ResourceKey<DamageType> FIREBALL = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("fireball"));
    ResourceKey<DamageType> UNATTRIBUTED_FIREBALL = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("unattributed_fireball"));
    ResourceKey<DamageType> WITHER_SKULL = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("wither_skull"));
    ResourceKey<DamageType> THROWN = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("thrown"));
    ResourceKey<DamageType> INDIRECT_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("indirect_magic"));
    ResourceKey<DamageType> THORNS = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("thorns"));
    ResourceKey<DamageType> EXPLOSION = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("explosion"));
    ResourceKey<DamageType> PLAYER_EXPLOSION = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("player_explosion"));
    ResourceKey<DamageType> SONIC_BOOM = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sonic_boom"));
    ResourceKey<DamageType> BAD_RESPAWN_POINT = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("bad_respawn_point"));
    ResourceKey<DamageType> OUTSIDE_BORDER = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("outside_border"));
    ResourceKey<DamageType> GENERIC_KILL = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("generic_kill"));

    static void bootstrap(BootstrapContext<DamageType> p_335750_) {
        p_335750_.register(IN_FIRE, new DamageType("inFire", 0.1F, DamageEffects.BURNING));
        p_335750_.register(LIGHTNING_BOLT, new DamageType("lightningBolt", 0.1F));
        p_335750_.register(ON_FIRE, new DamageType("onFire", 0.0F, DamageEffects.BURNING));
        p_335750_.register(LAVA, new DamageType("lava", 0.1F, DamageEffects.BURNING));
        p_335750_.register(HOT_FLOOR, new DamageType("hotFloor", 0.1F, DamageEffects.BURNING));
        p_335750_.register(IN_WALL, new DamageType("inWall", 0.0F));
        p_335750_.register(CRAMMING, new DamageType("cramming", 0.0F));
        p_335750_.register(DROWN, new DamageType("drown", 0.0F, DamageEffects.DROWNING));
        p_335750_.register(STARVE, new DamageType("starve", 0.0F));
        p_335750_.register(CACTUS, new DamageType("cactus", 0.1F));
        p_335750_.register(
            FALL, new DamageType("fall", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0F, DamageEffects.HURT, DeathMessageType.FALL_VARIANTS)
        );
        p_335750_.register(FLY_INTO_WALL, new DamageType("flyIntoWall", 0.0F));
        p_335750_.register(FELL_OUT_OF_WORLD, new DamageType("outOfWorld", 0.0F));
        p_335750_.register(GENERIC, new DamageType("generic", 0.0F));
        p_335750_.register(MAGIC, new DamageType("magic", 0.0F));
        p_335750_.register(WITHER, new DamageType("wither", 0.0F));
        p_335750_.register(DRAGON_BREATH, new DamageType("dragonBreath", 0.0F));
        p_335750_.register(DRY_OUT, new DamageType("dryout", 0.1F));
        p_335750_.register(SWEET_BERRY_BUSH, new DamageType("sweetBerryBush", 0.1F, DamageEffects.POKING));
        p_335750_.register(FREEZE, new DamageType("freeze", 0.0F, DamageEffects.FREEZING));
        p_335750_.register(STALAGMITE, new DamageType("stalagmite", 0.0F));
        p_335750_.register(FALLING_BLOCK, new DamageType("fallingBlock", 0.1F));
        p_335750_.register(FALLING_ANVIL, new DamageType("anvil", 0.1F));
        p_335750_.register(FALLING_STALACTITE, new DamageType("fallingStalactite", 0.1F));
        p_335750_.register(STING, new DamageType("sting", 0.1F));
        p_335750_.register(MOB_ATTACK, new DamageType("mob", 0.1F));
        p_335750_.register(MOB_ATTACK_NO_AGGRO, new DamageType("mob", 0.1F));
        p_335750_.register(PLAYER_ATTACK, new DamageType("player", 0.1F));
        p_335750_.register(ARROW, new DamageType("arrow", 0.1F));
        p_335750_.register(TRIDENT, new DamageType("trident", 0.1F));
        p_335750_.register(MOB_PROJECTILE, new DamageType("mob", 0.1F));
        p_335750_.register(SPIT, new DamageType("mob", 0.1F));
        p_335750_.register(FIREWORKS, new DamageType("fireworks", 0.1F));
        p_335750_.register(UNATTRIBUTED_FIREBALL, new DamageType("onFire", 0.1F, DamageEffects.BURNING));
        p_335750_.register(FIREBALL, new DamageType("fireball", 0.1F, DamageEffects.BURNING));
        p_335750_.register(WITHER_SKULL, new DamageType("witherSkull", 0.1F));
        p_335750_.register(THROWN, new DamageType("thrown", 0.1F));
        p_335750_.register(INDIRECT_MAGIC, new DamageType("indirectMagic", 0.0F));
        p_335750_.register(THORNS, new DamageType("thorns", 0.1F, DamageEffects.THORNS));
        p_335750_.register(EXPLOSION, new DamageType("explosion", DamageScaling.ALWAYS, 0.1F));
        p_335750_.register(PLAYER_EXPLOSION, new DamageType("explosion.player", DamageScaling.ALWAYS, 0.1F));
        p_335750_.register(SONIC_BOOM, new DamageType("sonic_boom", DamageScaling.ALWAYS, 0.0F));
        p_335750_.register(
            BAD_RESPAWN_POINT, new DamageType("badRespawnPoint", DamageScaling.ALWAYS, 0.1F, DamageEffects.HURT, DeathMessageType.INTENTIONAL_GAME_DESIGN)
        );
        p_335750_.register(OUTSIDE_BORDER, new DamageType("outsideBorder", 0.0F));
        p_335750_.register(GENERIC_KILL, new DamageType("genericKill", 0.0F));
    }
}