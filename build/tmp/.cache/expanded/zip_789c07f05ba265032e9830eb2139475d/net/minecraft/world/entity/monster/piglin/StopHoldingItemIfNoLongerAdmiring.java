package net.minecraft.world.entity.monster.piglin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.Items;

public class StopHoldingItemIfNoLongerAdmiring {
    public static BehaviorControl<Piglin> create() {
        return BehaviorBuilder.create(
            p_259197_ -> p_259197_.group(p_259197_.absent(MemoryModuleType.ADMIRING_ITEM)).apply(p_259197_, p_259512_ -> (p_327034_, p_327035_, p_327036_) -> {
                        if (!p_327035_.getOffhandItem().isEmpty() && !p_327035_.getOffhandItem().canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)) {
                            PiglinAi.stopHoldingOffHandItem(p_327035_, true);
                            return true;
                        } else {
                            return false;
                        }
                    })
        );
    }
}
