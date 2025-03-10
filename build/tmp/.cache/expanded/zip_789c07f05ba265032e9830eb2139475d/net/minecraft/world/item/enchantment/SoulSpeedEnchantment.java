package net.minecraft.world.item.enchantment;

public class SoulSpeedEnchantment extends Enchantment {
    public SoulSpeedEnchantment(Enchantment.EnchantmentDefinition p_328156_) {
        super(p_328156_);
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
    }

    @Override
    public boolean isTradeable() {
        return false;
    }

    @Override
    public boolean isDiscoverable() {
        return false;
    }
}