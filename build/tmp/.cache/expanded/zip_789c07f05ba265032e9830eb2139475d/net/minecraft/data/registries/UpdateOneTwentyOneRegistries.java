package net.minecraft.data.registries;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.UpdateOneTwentyOneDamageTypes;
import net.minecraft.data.worldgen.UpdateOneTwentyOnePools;
import net.minecraft.data.worldgen.UpdateOneTwentyOneProcessorLists;
import net.minecraft.data.worldgen.UpdateOneTwentyOneStructureSets;
import net.minecraft.data.worldgen.UpdateOneTwentyOneStructures;
import net.minecraft.data.worldgen.biome.UpdateOneTwentyOneBiomeData;
import net.minecraft.world.item.armortrim.UpdateOneTwentyOneArmorTrims;
import net.minecraft.world.level.block.entity.UpdateOneTwentyOneBannerPatterns;

public class UpdateOneTwentyOneRegistries {
    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
        .add(Registries.BIOME, UpdateOneTwentyOneBiomeData::bootstrap)
        .add(Registries.TEMPLATE_POOL, UpdateOneTwentyOnePools::bootstrap)
        .add(Registries.STRUCTURE, UpdateOneTwentyOneStructures::bootstrap)
        .add(Registries.STRUCTURE_SET, UpdateOneTwentyOneStructureSets::bootstrap)
        .add(Registries.PROCESSOR_LIST, UpdateOneTwentyOneProcessorLists::bootstrap)
        .add(Registries.DAMAGE_TYPE, UpdateOneTwentyOneDamageTypes::bootstrap)
        .add(Registries.BANNER_PATTERN, UpdateOneTwentyOneBannerPatterns::bootstrap)
        .add(Registries.TRIM_PATTERN, UpdateOneTwentyOneArmorTrims::bootstrap);

    public static CompletableFuture<RegistrySetBuilder.PatchedRegistries> createLookup(CompletableFuture<HolderLookup.Provider> p_310387_) {
        return RegistryPatchGenerator.createLookup(p_310387_, BUILDER);
    }
}