package net.minecraft.data.loot;

import java.util.function.BiConsumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

@FunctionalInterface
public interface LootTableSubProvider {
    void generate(HolderLookup.Provider p_332477_, BiConsumer<ResourceKey<LootTable>, LootTable.Builder> p_249643_);
}