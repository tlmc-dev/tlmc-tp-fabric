package cn.timelessmc.teleport.pointer;

import com.mojang.brigadier.CommandDispatcher;
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

public class HomeHandler implements CommandRegistrationCallback {
    private final HomeEntryManager homeEntryManager;
    private final int maxHomeCount;

    public HomeHandler(HomeEntryManager homeManager, int maxHomeCount) {
        this.homeEntryManager = homeManager;
        this.maxHomeCount = maxHomeCount;
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        final var homeCmd = literal("home")
                .requires(CommandSourceStack::isPlayer)
                .then(argument("home", greedyString()).executes(ctx -> {
                    @NotNull final var player = ctx.getSource().getPlayerOrException();
                    final var homeName = ctx.getArgument("home", String.class);
                    homeEntryManager.getHomeEntry(player.getUUID(), homeName).ifPresentOrElse(
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
                                player.sendSystemMessage(Component.literal(String.format("§a成功传送到 §6%s", homeName)));
                            },
                            () -> player.sendSystemMessage(Component.literal(String.format("§c没有这样的个人传送点条目 §6%s", homeName)))
                    );
                    return 0;
                }).suggests((ctx, builder) -> {
                    homeEntryManager.getHomeEntryNames(ctx.getSource().getPlayerOrException().getUUID())
                            .forEach(builder::suggest);
                    return builder.buildFuture();
                }));

        final var setHomeCmd = literal("sethome")
                .requires(CommandSourceStack::isPlayer)
                .then(argument("home", greedyString()).executes(ctx -> {
                    @NotNull final var player = ctx.getSource().getPlayerOrException();

                    final var homeName = ctx.getArgument("home", String.class);
                    if (homeEntryManager.containsHomeEntry(player.getUUID(), homeName)) {
                        player.sendSystemMessage(Component.literal(String.format("§c个人传送点 §6%s §c已经存在!", homeName)));
                        return 1;
                    }

                    if (homeEntryManager.getHomeCount(player.getUUID()) >= maxHomeCount) {
                        player.sendSystemMessage(Component.literal("§c你已达到最大个人传送点数量!"));
                        return 1;
                    }

                    try {
                        homeEntryManager.setHomeEntry(
                                player.getUUID(),
                                homeName,
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
                    player.sendSystemMessage(Component.literal(String.format("§a成功设置个人传送点 §6%s", homeName)));
                    return 0;
                }));

        final var delHomeCmd = literal("delhome")
                .requires(CommandSourceStack::isPlayer)
                .then(argument("home", greedyString()).executes(ctx -> {
                    @NotNull final var player = ctx.getSource().getPlayerOrException();

                    final var homeName = ctx.getArgument("home", String.class);
                    if (!homeEntryManager.containsHomeEntry(player.getUUID(), homeName)) {
                        player.sendSystemMessage(Component.literal(String.format("§c没有这样的个人传送点条目 §6%s", homeName)));
                        return 1;
                    }

                    try {
                        homeEntryManager.removeHomeEntry(player.getUUID(), homeName);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    player.sendSystemMessage(Component.literal(String.format("§a成功删除个人传送点 §6%s", homeName)));
                    return 0;
                }).suggests((ctx, builder) -> {
                    homeEntryManager.getHomeEntryNames(ctx.getSource().getPlayerOrException().getUUID())
                            .forEach(builder::suggest);
                    return builder.buildFuture();
                }));

        dispatcher.register(homeCmd);
        dispatcher.register(setHomeCmd);
        dispatcher.register(delHomeCmd);
    }
}