package cn.timelessmc.teleport.pointer;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record TeleportEntry(
        @NotNull ResourceKey<Level> world,
        double x,
        double y,
        double z
) {}
