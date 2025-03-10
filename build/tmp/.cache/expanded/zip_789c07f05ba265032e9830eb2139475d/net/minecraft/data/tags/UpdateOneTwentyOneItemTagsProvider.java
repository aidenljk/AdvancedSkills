package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public class UpdateOneTwentyOneItemTagsProvider extends ItemTagsProvider {
    public UpdateOneTwentyOneItemTagsProvider(
        PackOutput p_309895_,
        CompletableFuture<HolderLookup.Provider> p_312150_,
        CompletableFuture<TagsProvider.TagLookup<Item>> p_311361_,
        CompletableFuture<TagsProvider.TagLookup<Block>> p_310346_
    ) {
        super(p_309895_, p_312150_, p_311361_, p_310346_);
    }

    @Override
    protected void addTags(HolderLookup.Provider p_312918_) {
        this.tag(ItemTags.STAIRS).add(Items.TUFF_STAIRS, Items.POLISHED_TUFF_STAIRS, Items.TUFF_BRICK_STAIRS);
        this.tag(ItemTags.SLABS).add(Items.TUFF_SLAB, Items.POLISHED_TUFF_SLAB, Items.TUFF_BRICK_SLAB);
        this.tag(ItemTags.WALLS).add(Items.TUFF_WALL, Items.POLISHED_TUFF_WALL, Items.TUFF_BRICK_WALL);
        this.tag(ItemTags.DOORS)
            .add(Items.COPPER_DOOR, Items.EXPOSED_COPPER_DOOR, Items.WEATHERED_COPPER_DOOR, Items.OXIDIZED_COPPER_DOOR, Items.WAXED_COPPER_DOOR, Items.WAXED_EXPOSED_COPPER_DOOR, Items.WAXED_WEATHERED_COPPER_DOOR, Items.WAXED_OXIDIZED_COPPER_DOOR);
        this.tag(ItemTags.TRAPDOORS)
            .add(Items.COPPER_TRAPDOOR, Items.EXPOSED_COPPER_TRAPDOOR, Items.WEATHERED_COPPER_TRAPDOOR, Items.OXIDIZED_COPPER_TRAPDOOR, Items.WAXED_COPPER_TRAPDOOR, Items.WAXED_EXPOSED_COPPER_TRAPDOOR, Items.WAXED_WEATHERED_COPPER_TRAPDOOR, Items.WAXED_OXIDIZED_COPPER_TRAPDOOR);
        this.tag(ItemTags.MACE_ENCHANTABLE).add(Items.MACE);
        this.tag(ItemTags.DECORATED_POT_SHERDS).add(Items.FLOW_POTTERY_SHERD, Items.GUSTER_POTTERY_SHERD, Items.SCRAPE_POTTERY_SHERD);
        this.tag(ItemTags.DECORATED_POT_INGREDIENTS).add(Items.FLOW_POTTERY_SHERD).add(Items.GUSTER_POTTERY_SHERD).add(Items.SCRAPE_POTTERY_SHERD);
        this.tag(ItemTags.TRIM_TEMPLATES).add(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE).add(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
        this.tag(ItemTags.DURABILITY_ENCHANTABLE).add(Items.MACE);
        this.tag(ItemTags.WEAPON_ENCHANTABLE).add(Items.MACE);
        this.tag(ItemTags.FIRE_ASPECT_ENCHANTABLE).add(Items.MACE);
        this.tag(ItemTags.VANISHING_ENCHANTABLE).add(Items.MACE);
        this.tag(ItemTags.BREAKS_DECORATED_POTS).add(Items.MACE);
    }
}