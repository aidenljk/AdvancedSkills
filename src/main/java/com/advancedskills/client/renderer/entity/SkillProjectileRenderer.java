package com.advancedskills.client.renderer.entity;

import com.advancedskills.entity.SkillProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis; // Updated import for modern Forge
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes; // For fallback
import org.joml.Matrix3f; // Updated import for modern Forge
import org.joml.Matrix4f; // Updated import for modern Forge

public class SkillProjectileRenderer extends EntityRenderer<SkillProjectileEntity> {

    // Using a generic particle texture, like a small orb or energy ball.
    // For this example, let's reference a common particle texture like the 'crit' particle's texture,
    // or a simple colored quad if no texture is readily available.
    // Most vanilla particle textures are part of larger texture atlases (particles.png).
    // A very simple approach is to not bind any specific texture and just draw a colored quad.
    // For a slightly better visual, we could try to use an existing item like a snowball.
    private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation("minecraft", "textures/item/fire_charge.png"); // Example texture

    public SkillProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1f; // Small shadow
    }

    @Override
    public void render(SkillProjectileEntity projectile, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Billboard effect: make the quad always face the camera
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        // Rotate 180 degrees on Y to face the correct direction if texture is backwards
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        // Rotate on X if texture is upside down
        // poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));


        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();

        // VertexConsumer to draw the quad
        // Using RenderType.entityCutoutNoCull to handle transparency if the texture has it.
        VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(projectile)));

        float size = 0.25f; // Size of the rendered quad

        // Define vertices for a quad
        vertex(vertexconsumer, matrix4f, matrix3f, packedLight, -size, -size, 0, 0, 1);
        vertex(vertexconsumer, matrix4f, matrix3f, packedLight, -size, +size, 0, 0, 0);
        vertex(vertexconsumer, matrix4f, matrix3f, packedLight, +size, +size, 0, 1, 0);
        vertex(vertexconsumer, matrix4f, matrix3f, packedLight, +size, -size, 0, 1, 1);

        poseStack.popPose();
        super.render(projectile, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    // Helper method for vertices
    private static void vertex(VertexConsumer consumer, Matrix4f matrix4f, Matrix3f matrix3f, int packedLight,
                               float x, float y, float z, float u, float v) {
        consumer.vertex(matrix4f, x, y, z)
                .color(255, 255, 255, 255) // White color, no tint
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(matrix3f, 0.0F, 1.0F, 0.0F) // Normal pointing up
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SkillProjectileEntity entity) {
        // Potentially change texture based on entity.getElement()
        // For now, one texture for all.
        return TEXTURE_LOCATION;
    }
}
