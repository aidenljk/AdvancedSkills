package net.minecraft.server.level;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.util.DebugBuffer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class ChunkHolder {
    public static final ChunkResult<ChunkAccess> UNLOADED_CHUNK = ChunkResult.error("Unloaded chunk");
    public static final CompletableFuture<ChunkResult<ChunkAccess>> UNLOADED_CHUNK_FUTURE = CompletableFuture.completedFuture(UNLOADED_CHUNK);
    public static final ChunkResult<LevelChunk> UNLOADED_LEVEL_CHUNK = ChunkResult.error("Unloaded level chunk");
    public static final ChunkResult<ChunkAccess> NOT_DONE_YET = ChunkResult.error("Not done yet");
    private static final CompletableFuture<ChunkResult<LevelChunk>> UNLOADED_LEVEL_CHUNK_FUTURE = CompletableFuture.completedFuture(UNLOADED_LEVEL_CHUNK);
    private static final List<ChunkStatus> CHUNK_STATUSES = ChunkStatus.getStatusList();
    private final AtomicReferenceArray<CompletableFuture<ChunkResult<ChunkAccess>>> futures = new AtomicReferenceArray<>(CHUNK_STATUSES.size());
    private final LevelHeightAccessor levelHeightAccessor;
    private volatile CompletableFuture<ChunkResult<LevelChunk>> fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private volatile CompletableFuture<ChunkResult<LevelChunk>> tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private volatile CompletableFuture<ChunkResult<LevelChunk>> entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private CompletableFuture<ChunkAccess> chunkToSave = CompletableFuture.completedFuture(null);
    @Nullable
    private final DebugBuffer<ChunkHolder.ChunkSaveDebug> chunkToSaveHistory = null;
    private int oldTicketLevel;
    private int ticketLevel;
    private int queueLevel;
    private final ChunkPos pos;
    private boolean hasChangedSections;
    private final ShortSet[] changedBlocksPerSection;
    private final BitSet blockChangedLightSectionFilter = new BitSet();
    private final BitSet skyChangedLightSectionFilter = new BitSet();
    private final LevelLightEngine lightEngine;
    private final ChunkHolder.LevelChangeListener onLevelChange;
    private final ChunkHolder.PlayerProvider playerProvider;
    private boolean wasAccessibleSinceLastSave;
    private CompletableFuture<Void> pendingFullStateConfirmation = CompletableFuture.completedFuture(null);
    private CompletableFuture<?> sendSync = CompletableFuture.completedFuture(null);
    LevelChunk currentlyLoading; // Forge: Used to bypass future chain when loading chunks.

    public ChunkHolder(
        ChunkPos p_142986_,
        int p_142987_,
        LevelHeightAccessor p_142988_,
        LevelLightEngine p_142989_,
        ChunkHolder.LevelChangeListener p_142990_,
        ChunkHolder.PlayerProvider p_142991_
    ) {
        this.pos = p_142986_;
        this.levelHeightAccessor = p_142988_;
        this.lightEngine = p_142989_;
        this.onLevelChange = p_142990_;
        this.playerProvider = p_142991_;
        this.oldTicketLevel = ChunkLevel.MAX_LEVEL + 1;
        this.ticketLevel = this.oldTicketLevel;
        this.queueLevel = this.oldTicketLevel;
        this.setTicketLevel(p_142987_);
        this.changedBlocksPerSection = new ShortSet[p_142988_.getSectionsCount()];
    }

    public CompletableFuture<ChunkResult<ChunkAccess>> getFutureIfPresentUnchecked(ChunkStatus p_331856_) {
        CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = this.futures.get(p_331856_.getIndex());
        return completablefuture == null ? UNLOADED_CHUNK_FUTURE : completablefuture;
    }

    public CompletableFuture<ChunkResult<ChunkAccess>> getFutureIfPresent(ChunkStatus p_330273_) {
        return ChunkLevel.generationStatus(this.ticketLevel).isOrAfter(p_330273_) ? this.getFutureIfPresentUnchecked(p_330273_) : UNLOADED_CHUNK_FUTURE;
    }

    public CompletableFuture<ChunkResult<LevelChunk>> getTickingChunkFuture() {
        return this.tickingChunkFuture;
    }

    public CompletableFuture<ChunkResult<LevelChunk>> getEntityTickingChunkFuture() {
        return this.entityTickingChunkFuture;
    }

    public CompletableFuture<ChunkResult<LevelChunk>> getFullChunkFuture() {
        return this.fullChunkFuture;
    }

    @Nullable
    public LevelChunk getTickingChunk() {
        return this.getTickingChunkFuture().getNow(UNLOADED_LEVEL_CHUNK).orElse(null);
    }

    public CompletableFuture<?> getChunkSendSyncFuture() {
        return this.sendSync;
    }

    @Nullable
    public LevelChunk getChunkToSend() {
        return !this.sendSync.isDone() ? null : this.getTickingChunk();
    }

    @Nullable
    public ChunkStatus getLastAvailableStatus() {
        for (int i = CHUNK_STATUSES.size() - 1; i >= 0; i--) {
            ChunkStatus chunkstatus = CHUNK_STATUSES.get(i);
            CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = this.getFutureIfPresentUnchecked(chunkstatus);
            if (completablefuture.getNow(UNLOADED_CHUNK).isSuccess()) {
                return chunkstatus;
            }
        }

        return null;
    }

    @Nullable
    public ChunkAccess getLastAvailable() {
        for (int i = CHUNK_STATUSES.size() - 1; i >= 0; i--) {
            ChunkStatus chunkstatus = CHUNK_STATUSES.get(i);
            CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = this.getFutureIfPresentUnchecked(chunkstatus);
            if (!completablefuture.isCompletedExceptionally()) {
                ChunkAccess chunkaccess = completablefuture.getNow(UNLOADED_CHUNK).orElse(null);
                if (chunkaccess != null) {
                    return chunkaccess;
                }
            }
        }

        return null;
    }

    public CompletableFuture<ChunkAccess> getChunkToSave() {
        return this.chunkToSave;
    }

    public void blockChanged(BlockPos p_140057_) {
        LevelChunk levelchunk = this.getTickingChunk();
        if (levelchunk != null) {
            int i = this.levelHeightAccessor.getSectionIndex(p_140057_.getY());
            if (this.changedBlocksPerSection[i] == null) {
                this.hasChangedSections = true;
                this.changedBlocksPerSection[i] = new ShortOpenHashSet();
            }

            this.changedBlocksPerSection[i].add(SectionPos.sectionRelativePos(p_140057_));
        }
    }

    public void sectionLightChanged(LightLayer p_140037_, int p_140038_) {
        ChunkAccess chunkaccess = this.getFutureIfPresent(ChunkStatus.INITIALIZE_LIGHT).getNow(UNLOADED_CHUNK).orElse(null);
        if (chunkaccess != null) {
            chunkaccess.setUnsaved(true);
            LevelChunk levelchunk = this.getTickingChunk();
            if (levelchunk != null) {
                int i = this.lightEngine.getMinLightSection();
                int j = this.lightEngine.getMaxLightSection();
                if (p_140038_ >= i && p_140038_ <= j) {
                    int k = p_140038_ - i;
                    if (p_140037_ == LightLayer.SKY) {
                        this.skyChangedLightSectionFilter.set(k);
                    } else {
                        this.blockChangedLightSectionFilter.set(k);
                    }
                }
            }
        }
    }

    public void broadcastChanges(LevelChunk p_140055_) {
        if (this.hasChangedSections || !this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
            Level level = p_140055_.getLevel();
            if (!this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
                List<ServerPlayer> list = this.playerProvider.getPlayers(this.pos, true);
                if (!list.isEmpty()) {
                    ClientboundLightUpdatePacket clientboundlightupdatepacket = new ClientboundLightUpdatePacket(
                        p_140055_.getPos(), this.lightEngine, this.skyChangedLightSectionFilter, this.blockChangedLightSectionFilter
                    );
                    this.broadcast(list, clientboundlightupdatepacket);
                }

                this.skyChangedLightSectionFilter.clear();
                this.blockChangedLightSectionFilter.clear();
            }

            if (this.hasChangedSections) {
                List<ServerPlayer> list1 = this.playerProvider.getPlayers(this.pos, false);

                for (int j = 0; j < this.changedBlocksPerSection.length; j++) {
                    ShortSet shortset = this.changedBlocksPerSection[j];
                    if (shortset != null) {
                        this.changedBlocksPerSection[j] = null;
                        if (!list1.isEmpty()) {
                            int i = this.levelHeightAccessor.getSectionYFromSectionIndex(j);
                            SectionPos sectionpos = SectionPos.of(p_140055_.getPos(), i);
                            if (shortset.size() == 1) {
                                BlockPos blockpos = sectionpos.relativeToBlockPos(shortset.iterator().nextShort());
                                BlockState blockstate = level.getBlockState(blockpos);
                                this.broadcast(list1, new ClientboundBlockUpdatePacket(blockpos, blockstate));
                                this.broadcastBlockEntityIfNeeded(list1, level, blockpos, blockstate);
                            } else {
                                LevelChunkSection levelchunksection = p_140055_.getSection(j);
                                ClientboundSectionBlocksUpdatePacket clientboundsectionblocksupdatepacket = new ClientboundSectionBlocksUpdatePacket(
                                    sectionpos, shortset, levelchunksection
                                );
                                this.broadcast(list1, clientboundsectionblocksupdatepacket);
                                clientboundsectionblocksupdatepacket.runUpdates((p_288761_, p_288762_) -> this.broadcastBlockEntityIfNeeded(list1, level, p_288761_, p_288762_));
                            }
                        }
                    }
                }

                this.hasChangedSections = false;
            }
        }
    }

    private void broadcastBlockEntityIfNeeded(List<ServerPlayer> p_288982_, Level p_289011_, BlockPos p_288969_, BlockState p_288973_) {
        if (p_288973_.hasBlockEntity()) {
            this.broadcastBlockEntity(p_288982_, p_289011_, p_288969_);
        }
    }

    private void broadcastBlockEntity(List<ServerPlayer> p_288988_, Level p_289005_, BlockPos p_288981_) {
        BlockEntity blockentity = p_289005_.getBlockEntity(p_288981_);
        if (blockentity != null) {
            Packet<?> packet = blockentity.getUpdatePacket();
            if (packet != null) {
                this.broadcast(p_288988_, packet);
            }
        }
    }

    private void broadcast(List<ServerPlayer> p_288998_, Packet<?> p_289013_) {
        p_288998_.forEach(p_296560_ -> p_296560_.connection.send(p_289013_));
    }

    public CompletableFuture<ChunkResult<ChunkAccess>> getOrScheduleFuture(ChunkStatus p_330909_, ChunkMap p_140051_) {
        int i = p_330909_.getIndex();
        CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = this.futures.get(i);
        if (completablefuture != null) {
            ChunkResult<ChunkAccess> chunkresult = completablefuture.getNow(NOT_DONE_YET);
            if (chunkresult == null) {
                String s = "value in future for status: " + p_330909_ + " was incorrectly set to null at chunk: " + this.pos;
                throw p_140051_.debugFuturesAndCreateReportedException(new IllegalStateException("null value previously set for chunk status"), s);
            }

            if (chunkresult == NOT_DONE_YET || chunkresult.isSuccess()) {
                return completablefuture;
            }
        }

        if (ChunkLevel.generationStatus(this.ticketLevel).isOrAfter(p_330909_)) {
            CompletableFuture<ChunkResult<ChunkAccess>> completablefuture1 = p_140051_.schedule(this, p_330909_);
            this.updateChunkToSave(completablefuture1, "schedule " + p_330909_);
            this.futures.set(i, completablefuture1);
            return completablefuture1;
        } else {
            return completablefuture == null ? UNLOADED_CHUNK_FUTURE : completablefuture;
        }
    }

    protected void addSaveDependency(String p_200417_, CompletableFuture<?> p_200418_) {
        if (this.chunkToSaveHistory != null) {
            this.chunkToSaveHistory.push(new ChunkHolder.ChunkSaveDebug(Thread.currentThread(), p_200418_, p_200417_));
        }

        this.chunkToSave = this.chunkToSave.thenCombine((CompletionStage<? extends Object>)p_200418_, (p_200414_, p_200415_) -> (ChunkAccess)p_200414_);
    }

    private void updateChunkToSave(CompletableFuture<? extends ChunkResult<? extends ChunkAccess>> p_143018_, String p_143019_) {
        if (this.chunkToSaveHistory != null) {
            this.chunkToSaveHistory.push(new ChunkHolder.ChunkSaveDebug(Thread.currentThread(), p_143018_, p_143019_));
        }

        this.chunkToSave = this.chunkToSave
            .thenCombine(p_143018_, (p_326373_, p_326374_) -> ChunkResult.orElse((ChunkResult<? extends ChunkAccess>)p_326374_, (ChunkAccess)p_326373_));
    }

    public void addSendDependency(CompletableFuture<?> p_300440_) {
        if (this.sendSync.isDone()) {
            this.sendSync = p_300440_;
        } else {
            this.sendSync = this.sendSync.thenCombine((CompletionStage<? extends Object>)p_300440_, (p_296561_, p_296562_) -> null);
        }
    }

    public FullChunkStatus getFullStatus() {
        return ChunkLevel.fullStatus(this.ticketLevel);
    }

    public ChunkPos getPos() {
        return this.pos;
    }

    public int getTicketLevel() {
        return this.ticketLevel;
    }

    public int getQueueLevel() {
        return this.queueLevel;
    }

    private void setQueueLevel(int p_140087_) {
        this.queueLevel = p_140087_;
    }

    public void setTicketLevel(int p_140028_) {
        this.ticketLevel = p_140028_;
    }

    private void scheduleFullChunkPromotion(ChunkMap p_142999_, CompletableFuture<ChunkResult<LevelChunk>> p_143000_, Executor p_143001_, FullChunkStatus p_287621_) {
        this.pendingFullStateConfirmation.cancel(false);
        CompletableFuture<Void> completablefuture = new CompletableFuture<>();
        completablefuture.thenRunAsync(() -> p_142999_.onFullChunkStatusChange(this.pos, p_287621_), p_143001_);
        this.pendingFullStateConfirmation = completablefuture;
        p_143000_.thenAccept(p_326372_ -> p_326372_.ifSuccess(p_200424_ -> completablefuture.complete(null)));
    }

    private void demoteFullChunk(ChunkMap p_287599_, FullChunkStatus p_287649_) {
        this.pendingFullStateConfirmation.cancel(false);
        p_287599_.onFullChunkStatusChange(this.pos, p_287649_);
    }

    protected void updateFutures(ChunkMap p_143004_, Executor p_143005_) {
        ChunkStatus chunkstatus = ChunkLevel.generationStatus(this.oldTicketLevel);
        ChunkStatus chunkstatus1 = ChunkLevel.generationStatus(this.ticketLevel);
        boolean flag = ChunkLevel.isLoaded(this.oldTicketLevel);
        boolean flag1 = ChunkLevel.isLoaded(this.ticketLevel);
        FullChunkStatus fullchunkstatus = ChunkLevel.fullStatus(this.oldTicketLevel);
        FullChunkStatus fullchunkstatus1 = ChunkLevel.fullStatus(this.ticketLevel);
        if (flag) {
            ChunkResult<ChunkAccess> chunkresult = ChunkResult.error(() -> "Unloaded ticket level " + this.pos);

            for (int i = flag1 ? chunkstatus1.getIndex() + 1 : 0; i <= chunkstatus.getIndex(); i++) {
                CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = this.futures.get(i);
                if (completablefuture == null) {
                    this.futures.set(i, CompletableFuture.completedFuture(chunkresult));
                }
            }
        }

        boolean flag5 = fullchunkstatus.isOrAfter(FullChunkStatus.FULL);
        boolean flag6 = fullchunkstatus1.isOrAfter(FullChunkStatus.FULL);
        this.wasAccessibleSinceLastSave |= flag6;
        if (!flag5 && flag6) {
            this.fullChunkFuture = p_143004_.prepareAccessibleChunk(this);
            this.scheduleFullChunkPromotion(p_143004_, this.fullChunkFuture, p_143005_, FullChunkStatus.FULL);
            this.updateChunkToSave(this.fullChunkFuture, "full");
        }

        if (flag5 && !flag6) {
            this.fullChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }

        boolean flag7 = fullchunkstatus.isOrAfter(FullChunkStatus.BLOCK_TICKING);
        boolean flag2 = fullchunkstatus1.isOrAfter(FullChunkStatus.BLOCK_TICKING);
        if (!flag7 && flag2) {
            this.tickingChunkFuture = p_143004_.prepareTickingChunk(this);
            this.scheduleFullChunkPromotion(p_143004_, this.tickingChunkFuture, p_143005_, FullChunkStatus.BLOCK_TICKING);
            this.updateChunkToSave(this.tickingChunkFuture, "ticking");
        }

        if (flag7 && !flag2) {
            this.tickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }

        boolean flag3 = fullchunkstatus.isOrAfter(FullChunkStatus.ENTITY_TICKING);
        boolean flag4 = fullchunkstatus1.isOrAfter(FullChunkStatus.ENTITY_TICKING);
        if (!flag3 && flag4) {
            if (this.entityTickingChunkFuture != UNLOADED_LEVEL_CHUNK_FUTURE) {
                throw (IllegalStateException)Util.pauseInIde(new IllegalStateException());
            }

            this.entityTickingChunkFuture = p_143004_.prepareEntityTickingChunk(this);
            this.scheduleFullChunkPromotion(p_143004_, this.entityTickingChunkFuture, p_143005_, FullChunkStatus.ENTITY_TICKING);
            this.updateChunkToSave(this.entityTickingChunkFuture, "entity ticking");
        }

        if (flag3 && !flag4) {
            this.entityTickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }

        if (!fullchunkstatus1.isOrAfter(fullchunkstatus)) {
            this.demoteFullChunk(p_143004_, fullchunkstatus1);
        }

        this.onLevelChange.onLevelChange(this.pos, this::getQueueLevel, this.ticketLevel, this::setQueueLevel);
        this.oldTicketLevel = this.ticketLevel;
    }

    public boolean wasAccessibleSinceLastSave() {
        return this.wasAccessibleSinceLastSave;
    }

    public void refreshAccessibility() {
        this.wasAccessibleSinceLastSave = ChunkLevel.fullStatus(this.ticketLevel).isOrAfter(FullChunkStatus.FULL);
    }

    public void replaceProtoChunk(ImposterProtoChunk p_140053_) {
        for (int i = 0; i < this.futures.length(); i++) {
            CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = this.futures.get(i);
            if (completablefuture != null) {
                ChunkAccess chunkaccess = completablefuture.getNow(UNLOADED_CHUNK).orElse(null);
                if (chunkaccess instanceof ProtoChunk) {
                    this.futures.set(i, CompletableFuture.completedFuture(ChunkResult.of(p_140053_)));
                }
            }
        }

        this.updateChunkToSave(CompletableFuture.completedFuture(ChunkResult.of(p_140053_.getWrapped())), "replaceProto");
    }

    public List<Pair<ChunkStatus, CompletableFuture<ChunkResult<ChunkAccess>>>> getAllFutures() {
        List<Pair<ChunkStatus, CompletableFuture<ChunkResult<ChunkAccess>>>> list = new ArrayList<>();

        for (int i = 0; i < CHUNK_STATUSES.size(); i++) {
            list.add(Pair.of(CHUNK_STATUSES.get(i), this.futures.get(i)));
        }

        return list;
    }

    static record ChunkSaveDebug(Thread thread, CompletableFuture<?> future, String source) {
    }

    @FunctionalInterface
    public interface LevelChangeListener {
        void onLevelChange(ChunkPos p_140119_, IntSupplier p_140120_, int p_140121_, IntConsumer p_140122_);
    }

    public interface PlayerProvider {
        List<ServerPlayer> getPlayers(ChunkPos p_183717_, boolean p_183718_);
    }
}
