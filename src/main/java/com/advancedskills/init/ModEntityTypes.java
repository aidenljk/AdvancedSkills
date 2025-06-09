package com.advancedskills.init;

import com.advancedskills.AdvancedSkillsMod;
import com.advancedskills.entity.SkillProjectileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AdvancedSkillsMod.MODID);

    public static final RegistryObject<EntityType<SkillProjectileEntity>> SKILL_PROJECTILE =
        ENTITY_TYPES.register("skill_projectile",
            () -> EntityType.Builder.<SkillProjectileEntity>of(SkillProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F) // Small size
                    .clientTrackingRange(4) // How far to track from client
                    .updateInterval(10) // How often to send updates
                    .build(AdvancedSkillsMod.MODID + ":skill_projectile"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
