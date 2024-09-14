package eu.qrpth.mc.jmx_metrics.mixin;

import eu.qrpth.mc.jmx_metrics.JMXMetrics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.monitoring.jmx.MinecraftServerStatistics;
import net.minecraft.util.monitoring.jmx.MinecraftServerStatistics.AttributeDescription;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.management.openmbean.*;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

@Mixin(MinecraftServerStatistics.class)
public abstract class MinecraftServerStatisticsMixin {
    @Shadow
    @Final
    private Map<String, AttributeDescription> attributeDescriptionByName;

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(at = @At(value = "CTOR_HEAD", args = "enforce=PRE_BODY"), method = "<init>(Lnet/minecraft/server/MinecraftServer;)V")
    private void register(CallbackInfo ci) {
        this.addMetric("minTickTime", "Minimum tick time in the observation window (ms)", float.class,
                () -> ((float) Arrays.stream(this.server.getTickTimesNanos()).min().orElse(0)) / 1_000_000);
        this.addMetric("maxTickTime", "Maximum tick time in the observation window (ms)", float.class,
                () -> ((float) Arrays.stream(this.server.getTickTimesNanos()).max().orElse(0)) / 1_000_000);
        this.addMetric("players", "Number of connected players", int.class, () -> this.server.getPlayerCount());
        this.addMetric("maxPlayers", "The maximum number of players that can play on the server at the same time",
                int.class, () -> this.server.getMaxPlayers());
        this.addMetric("entities", "Number of loaded entities", TabularDataSupport.class, this::loadedEntities);
        this.addMetric("blockEntities", "Number of ticking block entities", TabularDataSupport.class,
                this::loadedBlockEntities);
        this.addMetric("chunks", "Number of loaded chunks", TabularDataSupport.class, this::loadedChunks);
    }

    @Unique
    @SuppressWarnings("unchecked")
    private <T> void addMetric(String name, String description, Class<T> type, Supplier<T> getter) {
        this.attributeDescriptionByName.put(name,
                new AttributeDescription(name, (Supplier<Object>) getter, description, type));
    }

    @Unique
    private TabularDataSupport loadedEntities() {
        try {
            CompositeType rowType = new CompositeType(
                    "LoadedEntitiesRow",
                    "Number of loaded entities",
                    new String[] { "level", "entity", "value" },
                    new String[] { "level ID", "entity ID", "value" },
                    new OpenType<?>[] { SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER });
            TabularDataSupport table = new TabularDataSupport(
                    new TabularType(
                            "LoadedEntities",
                            "Number of loaded entities",
                            rowType, new String[] { "level", "entity" }));

            for (ResourceKey<Level> key : this.server.levelKeys()) {
                ServerLevel level = this.server.getLevel(key);
                if (level == null) {
                    continue;
                }

                String levelId = key.location().toString();

                for (Map.Entry<ResourceKey<EntityType<?>>, EntityType<?>> entry : BuiltInRegistries.ENTITY_TYPE
                        .entrySet()) {
                    table.put(new CompositeDataSupport(rowType,
                            new String[] { "level", "entity", "value" },
                            new Object[] {
                                    levelId,
                                    entry.getKey().location().toString(),
                                    level.getEntities(entry.getValue(), entity -> true).size(),
                            }));
                }
            }

            return table;
        } catch (OpenDataException e) {
            JMXMetrics.LOGGER.error("entities metric failed", e);
            throw new RuntimeException(e);
        }
    }

    @Unique
    private TabularDataSupport loadedBlockEntities() {
        try {
            CompositeType rowType = new CompositeType(
                    "TickingBlockEntitiesRow",
                    "Number of ticking block entities",
                    new String[] { "level", "entity", "value" },
                    new String[] { "level ID", "entity ID", "value" },
                    new OpenType<?>[] { SimpleType.STRING, SimpleType.STRING, SimpleType.LONG });
            TabularDataSupport table = new TabularDataSupport(
                    new TabularType(
                            "TickingBlockEntities",
                            "Number of ticking block entities",
                            rowType, new String[] { "level", "entity" }));

            for (ResourceKey<Level> key : this.server.levelKeys()) {
                ServerLevel level = this.server.getLevel(key);
                if (level == null) {
                    continue;
                }

                String levelId = key.location().toString();

                for (ResourceLocation blockEntityType : BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet()) {
                    String blockEntityId = blockEntityType.toString();

                    table.put(new CompositeDataSupport(rowType,
                            new String[] { "level", "entity", "value" },
                            new Object[] {
                                    levelId,
                                    blockEntityId,
                                    level.blockEntityTickers.stream().filter(be -> be.getType().equals(blockEntityId))
                                            .count(),
                            }));
                }
            }

            return table;
        } catch (OpenDataException e) {
            JMXMetrics.LOGGER.error("blockEntities metric failed", e);
            throw new RuntimeException(e);
        }
    }

    @Unique
    private TabularDataSupport loadedChunks() {
        try {
            CompositeType rowType = new CompositeType(
                    "LoadedChunksRow",
                    "Number of loaded chunks",
                    new String[] { "level", "value" },
                    new String[] { "level ID", "value" },
                    new OpenType<?>[] { SimpleType.STRING, SimpleType.INTEGER });
            TabularDataSupport table = new TabularDataSupport(
                    new TabularType(
                            "LoadedChunks",
                            "Number of ticking block entities",
                            rowType, new String[] { "level" }));

            for (ResourceKey<Level> key : this.server.levelKeys()) {
                ServerLevel level = this.server.getLevel(key);
                if (level == null) {
                    continue;
                }

                table.put(new CompositeDataSupport(rowType,
                        new String[] { "level", "value" },
                        new Object[] {
                                key.location().toString(),
                                level.getChunkSource().getLoadedChunksCount(),
                        }));
            }

            return table;
        } catch (OpenDataException e) {
            JMXMetrics.LOGGER.error("chunks metric failed", e);
            throw new RuntimeException(e);
        }
    }
}
