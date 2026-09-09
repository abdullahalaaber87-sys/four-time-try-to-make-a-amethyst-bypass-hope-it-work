package com.radar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;

public class DeepslateAmethystRadar implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;

            World world = client.world;
            BlockPos playerPos = client.player.getBlockPos();
            MatrixStack matrices = context.matrixStack();

            int radius = 2;
            ChunkPos centerChunk = new ChunkPos(playerPos);

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    int chunkX = centerChunk.x + x;
                    int chunkZ = centerChunk.z + z;

                    boolean foundAmethyst = false;
                    int minX = chunkX * 16;
                    int minZ = chunkZ * 16;

                    for (int y = -64; y <= 32; y++) {
                        for (int bx = 0; bx < 16; bx++) {
                            for (int bz = 0; bz < 16; bz++) {
                                BlockPos pos = new BlockPos(minX + bx, y, minZ + bz);
                                BlockState state = world.getBlockState(pos);
                                if (state.isOf(Blocks.AMETHYST_CLUSTER) || state.isOf(Blocks.BUDDING_AMETHYST)) {
                                    foundAmethyst = true;
                                    break;
                                }
                            }
                            if (foundAmethyst) break;
                        }
                        if (foundAmethyst) break;
                    }

                    if (foundAmethyst) {
                        matrices.push();
                        Box box = new Box(minX, -64, minZ, minX + 16, 32, minZ + 16);
                        WorldRenderer.drawBox(matrices, context.consumers(), box, 1.0F, 0.0F, 0.0F, 0.6F);
                        matrices.pop();
                    }
                }
            }
        });
    }
}
