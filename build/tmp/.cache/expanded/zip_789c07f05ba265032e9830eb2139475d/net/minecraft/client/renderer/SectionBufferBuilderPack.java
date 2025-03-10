package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SectionBufferBuilderPack implements AutoCloseable {
    public static final int TOTAL_BUFFERS_SIZE = RenderType.chunkBufferLayers().stream().mapToInt(RenderType::bufferSize).sum();
    private final Map<RenderType, BufferBuilder> builders = RenderType.chunkBufferLayers()
        .stream()
        .collect(Collectors.toMap(p_298452_ -> (RenderType)p_298452_, p_299751_ -> new BufferBuilder(p_299751_.bufferSize())));

    public BufferBuilder builder(RenderType p_299415_) {
        return this.builders.get(p_299415_);
    }

    public void clearAll() {
        this.builders.values().forEach(BufferBuilder::clear);
    }

    public void discardAll() {
        this.builders.values().forEach(BufferBuilder::discard);
    }

    @Override
    public void close() {
        this.builders.values().forEach(BufferBuilder::release);
    }
}