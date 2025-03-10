package net.minecraft.world.item.enchantment;

public class LootBonusEnchantment extends Enchantment {
    protected LootBonusEnchantment(Enchantment.EnchantmentDefinition p_333587_) {
        super(p_333587_);
    }

    @Override
    public boolean checkCompatibility(Enchantment p_45094_) {
        return super.checkCompatibility(p_45094_) && p_45094_ != Enchantments.SILK_TOUCH;
    }
}