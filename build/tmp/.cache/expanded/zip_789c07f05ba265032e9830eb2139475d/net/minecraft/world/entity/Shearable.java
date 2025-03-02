package net.minecraft.world.entity;

import net.minecraft.sounds.SoundSource;

/** @deprecated Use {@link net.minecraftforge.common.IForgeShearable} */
public interface Shearable {
    void shear(SoundSource p_21749_);

    boolean readyForShearing();
}
