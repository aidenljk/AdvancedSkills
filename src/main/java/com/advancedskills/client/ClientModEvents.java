package com.advancedskills.client;

import com.advancedskills.AdvancedSkillsMod;
import com.advancedskills.client.renderer.entity.SkillProjectileRenderer;
import com.advancedskills.init.ModEntityTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AdvancedSkillsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.SKILL_PROJECTILE.get(), SkillProjectileRenderer::new);
        AdvancedSkillsMod.LOGGER.info("Registered SkillProjectileRenderer");
    }
}
