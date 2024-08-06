package cn.timelessmc.teleport.pointer;

import com.mojang.brigadier.CommandDispatcher;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class WarpHandler implements CommandRegistrationCallback {
    public static final String MODIFY_WARP_PERMISSION = "teleport.warp.modify";

    private final WarpEntryManager warpEntryManager;

    public WarpHandler(WarpEntryManager warpEntryManager) {
        this.warpEntryManager = warpEntryManager;
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        final var warpCmd = literal("warp")
                .requires(CommandSourceStack::isPlayer)
                .then(argument("warp", greedyString()).executes(ctx -> {
                    @NotNull final var player = ctx.getSource().getPlayerOrException();
                    final var warpName = ctx.getArgument("warp", String.class);
                    warpEntryManager.getTeleportEntry(warpName).ifPresentOrElse(
                            teleportEntry -> {
                                player.teleportTo(
                                        Objects.requireNonNull(player.getServer())
                                                .getLevel(teleportEntry.world()),
                                        teleportEntry.x(),
                                        teleportEntry.y(),
                                        teleportEntry.z(),
                                        player.getYRot(),
                                        player.getXRot()
                                );
                                player.sendSystemMessage(Component.literal(String.format("§a成功传送到 §6%s", warpName)));
                            },
                            () -> player.sendSystemMessage(Component.literal(String.format("§c没有这样的传送点条目 §6%s", warpName)))
                    );
                    return 0;
                }).suggests((ctx, builder) -> {
                    (ctx.getNodes().size() <= 1 ?
                            warpEntryManager.getTeleportEntryNames() :
                            warpEntryManager.getPossibleMatches(ctx.getArgument("warp", String.class))
                    ).forEach(builder::suggest);
                    return builder.buildFuture();
                }));

        final var setWarpCmd = literal("setwarp")
                .requires(source -> source.isPlayer() && Permissions.check(source, MODIFY_WARP_PERMISSION, 2))
                .then(argument("warp", greedyString()).executes(ctx -> {
                    @NotNull final var player = ctx.getSource().getPlayerOrException();

                    final var warpName = ctx.getArgument("warp", String.class);
                    if (warpEntryManager.containsTeleportEntry(warpName)) {
                        player.sendSystemMessage(Component.literal(String.format("§c传送点 §6%s §c已经存在!", warpName)));
                        return 1;
                    }

                    try {
                        warpEntryManager.setTeleportEntry(
                                warpName,
                                new TeleportEntry(
                                        player.level().dimension(),
                                        player.getX(),
                                        player.getY(),
                                        player.getZ()
                                )
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    player.sendSystemMessage(Component.literal(String.format("§a成功设置传送点 §6%s", warpName)));
                    return 0;
                }));

        final var delWarpCmd = literal("delwarp")
                .requires(source -> source.isPlayer() && Permissions.check(source, MODIFY_WARP_PERMISSION, 2))
                .then(argument("warp", greedyString()).executes(ctx -> {
                    @NotNull final var player = ctx.getSource().getPlayerOrException();

                    final var warpName = ctx.getArgument("warp", String.class);
                    try {
                        warpEntryManager.removeTeleportEntry(warpName).ifPresentOrElse(
                                teleportEntry -> player.sendSystemMessage(Component.literal(String.format("§a成功删除传送点 §6%s", warpName))),
                                () -> player.sendSystemMessage(Component.literal(String.format("§c没有这样的传送点条目 §6%s", warpName)))
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return 0;
                }).suggests((ctx, builder) -> {
                    warpEntryManager.getTeleportEntryNames().forEach(builder::suggest);
                    return builder.buildFuture();
                }));

        dispatcher.register(warpCmd);
        dispatcher.register(setWarpCmd);
        dispatcher.register(delWarpCmd);
    }
}