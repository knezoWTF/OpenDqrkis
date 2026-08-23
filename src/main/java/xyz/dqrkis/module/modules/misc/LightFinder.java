package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public final class LightFinder extends Module implements TickListener {
    private final NumberSetting scanRadius = new NumberSetting(EncryptedString.of("Scan Radius"), 1, 16, 4, 1);
    private final NumberSetting maxY = new NumberSetting(EncryptedString.of("Max Y"), 10, 120, 50, 5);
    private final NumberSetting minY = new NumberSetting(EncryptedString.of("Min Y"), -64, 60, -60, 5);
    private final NumberSetting lightThreshold = new NumberSetting(EncryptedString.of("Light Threshold"), 1, 15, 8, 1);
    private final NumberSetting minCluster = new NumberSetting(EncryptedString.of("Min Cluster"), 2, 30, 5, 1);
    private final NumberSetting clusterRadius = new NumberSetting(EncryptedString.of("Cluster Radius"), 3, 24, 8, 1);
    private final NumberSetting scanInterval = new NumberSetting(EncryptedString.of("Scan Interval"), 10, 300, 60, 10);
    private final BooleanSetting ignoreLava = new BooleanSetting(EncryptedString.of("Ignore Lava"), true);
    private final BooleanSetting showCoords = new BooleanSetting(EncryptedString.of("Show Coords"), true);

    private int ticksSinceScan;
    private final Set<Long> scannedChunks = new HashSet<>();
    private final List<LightCluster> foundClusters = new ArrayList<>();

    private record LightCluster(BlockPos center, int lightCount) {}

    public LightFinder() {
        super(EncryptedString.of("Light Finder"),
                EncryptedString.of("Finds underground bases using light data analysis"),
                -1,
                Category.MISC);
        addSettings(scanRadius, maxY, minY, lightThreshold, minCluster, clusterRadius, scanInterval, ignoreLava, showCoords);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        resetState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        resetState();
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null)
            return;

        if (++ticksSinceScan < scanInterval.getValueInt())
            return;

        ticksSinceScan = 0;
        mc.execute(this::scan);
    }

    private void scan() {
        if (mc.player == null || mc.world == null)
            return;

        ClientWorld world = mc.world;
        ChunkPos playerChunk = mc.player.getChunkPos();
        int radius = scanRadius.getValueInt();
        int topY = maxY.getValueInt();
        int bottomY = minY.getValueInt();
        int threshold = lightThreshold.getValueInt();
        boolean skipLava = ignoreLava.getValue();

        List<BlockPos> candidates = new ArrayList<>();

        for (int chunkX = playerChunk.x - radius; chunkX <= playerChunk.x + radius; chunkX++) {
            for (int chunkZ = playerChunk.z - radius; chunkZ <= playerChunk.z + radius; chunkZ++) {
                long chunkKey = ChunkPos.toLong(chunkX, chunkZ);
                if (scannedChunks.contains(chunkKey))
                    continue;

                WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkX, chunkZ);
                if (chunk == null)
                    continue;

                scannedChunks.add(chunkKey);
                int startWX = chunkX << 4;
                int startWZ = chunkZ << 4;

                for (int x = startWX; x < startWX + 16; x += 2) {
                    for (int z = startWZ; z < startWZ + 16; z += 2) {
                        for (int y = bottomY; y < topY; y += 2) {
                            BlockPos pos = new BlockPos(x, y, z);
                            int blockLight = world.getLightLevel(LightType.BLOCK, pos);
                            if (blockLight < threshold || (skipLava && isLavaNearby(world, pos)))
                                continue;

                            if (world.getLightLevel(LightType.SKY, pos) <= 4)
                                candidates.add(pos);
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty())
            return;

        List<LightCluster> clusters = cluster(candidates);
        for (LightCluster cluster : clusters) {
            if (cluster.lightCount() < minCluster.getValueInt() || alreadyReported(cluster.center()))
                continue;

            foundClusters.add(cluster);

            if (showCoords.getValue() && mc.player != null) {
                int distance = (int) Math.sqrt(mc.player.getBlockPos().getSquaredDistance(cluster.center()));
                mc.player.sendMessage(Text.literal(String.format(
                        "§6[§eLightFinder§6]§r Possible base: §b%d, %d, %d §7(%d lights, %dm away)",
                        cluster.center().getX(), cluster.center().getY(), cluster.center().getZ(),
                        cluster.lightCount(), distance)), false);
            }
        }
    }

    private List<LightCluster> cluster(List<BlockPos> positions) {
        List<LightCluster> clusters = new ArrayList<>();
        boolean[] visited = new boolean[positions.size()];
        double maxSquaredDistance = (double) clusterRadius.getValueInt() * clusterRadius.getValueInt();

        for (int i = 0; i < positions.size(); i++) {
            if (visited[i])
                continue;

            List<BlockPos> group = new ArrayList<>();
            LinkedList<Integer> queue = new LinkedList<>();
            queue.add(i);
            visited[i] = true;

            while (!queue.isEmpty()) {
                BlockPos current = positions.get(queue.poll());
                group.add(current);

                for (int j = 0; j < positions.size(); j++) {
                    if (!visited[j] && current.getSquaredDistance(positions.get(j)) <= maxSquaredDistance) {
                        visited[j] = true;
                        queue.add(j);
                    }
                }
            }

            if (group.size() < 2)
                continue;

            int sumX = 0, sumY = 0, sumZ = 0;
            for (BlockPos pos : group) {
                sumX += pos.getX();
                sumY += pos.getY();
                sumZ += pos.getZ();
            }

            clusters.add(new LightCluster(
                    new BlockPos(sumX / group.size(), sumY / group.size(), sumZ / group.size()),
                    group.size()));
        }

        return clusters;
    }

    private boolean isLavaNearby(ClientWorld world, BlockPos pos) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos check = pos.add(dx, dy, dz);
                    if (world.getBlockState(check).isOf(Blocks.LAVA))
                        return true;
                }
            }
        }
        return false;
    }

    private boolean alreadyReported(BlockPos center) {
        for (LightCluster cluster : foundClusters) {
            if (cluster.center().getSquaredDistance(center) < 256.0D)
                return true;
        }
        return false;
    }

    private void resetState() {
        scannedChunks.clear();
        foundClusters.clear();
        ticksSinceScan = 0;
    }
}