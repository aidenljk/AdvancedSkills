package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;

public class UpdateOneTwentyOneEntityTypeTagsProvider extends IntrinsicHolderTagsProvider<EntityType<?>> {
    public UpdateOneTwentyOneEntityTypeTagsProvider(PackOutput p_312388_, CompletableFuture<HolderLookup.Provider> p_311973_) {
        super(p_312388_, Registries.ENTITY_TYPE, p_311973_, p_312136_ -> p_312136_.builtInRegistryHolder().key());
    }

    @Override
    protected void addTags(HolderLookup.Provider p_312640_) {
        this.tag(EntityTypeTags.FALL_DAMAGE_IMMUNE).add(EntityType.BREEZE);
        this.tag(EntityTypeTags.DEFLECTS_PROJECTILES).add(EntityType.BREEZE);
        this.tag(EntityTypeTags.CAN_TURN_IN_BOATS).add(EntityType.BREEZE);
        this.tag(EntityTypeTags.IMPACT_PROJECTILES).add(EntityType.WIND_CHARGE, EntityType.BREEZE_WIND_CHARGE);
        this.tag(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE)
            .add(
                EntityType.BREEZE,
                EntityType.SKELETON,
                EntityType.BOGGED,
                EntityType.STRAY,
                EntityType.ZOMBIE,
                EntityType.HUSK,
                EntityType.SPIDER,
                EntityType.CAVE_SPIDER,
                EntityType.SLIME
            );
        this.tag(EntityTypeTags.SKELETONS).add(EntityType.BOGGED);
        this.tag(EntityTypeTags.IMMUNE_TO_INFESTED).add(EntityType.SILVERFISH);
        this.tag(EntityTypeTags.IMMUNE_TO_OOZING).add(EntityType.SLIME);
        this.tag(EntityTypeTags.REDIRECTABLE_PROJECTILE).add(EntityType.WIND_CHARGE, EntityType.BREEZE_WIND_CHARGE);
    }
}