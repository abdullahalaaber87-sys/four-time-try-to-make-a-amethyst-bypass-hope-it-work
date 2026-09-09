package com.radar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DeepslateAmethystRadar implements ClientModInitializer {
    private static KeyBinding toggleKey;
    private static boolean radarActive = false;
    private static final Set<ChunkPos> flaggedChunks = ConcurrentHashMap.newKeySet();
    private static long lastScanTime = 0;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.radar.toggle",
                InputUtil.Type.KEY_SYM,
                GLFW.GLFW_KEY_H,
                "category.radar"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKey.wasPressed()) {
                radarActive = !radarActive;
                flaggedChunks.clear();
            }

            if (radarActive && client.world != null && client.player != null) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastScanTime > 1500) {
                    lastScanTime = currentTime;
                    scanWorld(client.world, client.player.getBlockPos());
                }
            }
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            if (!radarActive) return;
            MatrixStack matrices = context.matrixStack();
            Camera camera = context.camera();
            Vec3d camPos = camera.getPos();

            matrices.push();
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);

            for (ChunkPos chunkPos : flaggedChunks) {
                renderChunkHighlight(matrices, chunkPos);
            }

            matrices.pop();
        });
    }

    private void scanWorld(World world, centerPos playerPos) {
        int radius = 6; 
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkPos chunkPos = new ChunkPos(playerPos.getX() >> 4 + x, playerPos.getZ() >> 4 + z);
                if (flaggedChunks.contains(chunkPos)) continue;

                boolean hasLargeAmethyst = checkChunkForAmethyst(world, chunkPos);
                if (hasLargeAmethyst) {
                    flaggedChunks.add(chunkPos);
                }
            }
        }
    }

    private boolean checkChunkForAmethyst(World world, ChunkPos chunkPos) {
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        
        for (int y = -64; y < 0; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    BlockState state = world.getBlockState(pos);
                    
                    if (state.isOf(Blocks.AMETHYST_CLUSTER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void renderChunkHighlight(MatrixStack matrices, ChunkPos chunkPos) {
        int minX = chunkPos.getStartX();
        int minZ = chunkPos.getStartZ();
        Box box = new Box(minX, -64, minZ, minX + 16, 0, minZ + 16);
        WorldRenderer.drawBox(matrices, null, box, 1.0F, 0.0F, 0.0F, 0.5F);
    }
}
