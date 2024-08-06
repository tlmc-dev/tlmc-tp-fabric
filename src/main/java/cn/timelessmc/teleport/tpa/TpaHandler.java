package cn.timelessmc.teleport.tpa;

import cn.timelessmc.teleport.gui.BedrockFormBuilder;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class TpaHandler implements CommandRegistrationCallback {
    private final TpaSessionHolder tpaSessionHolder;
    private final long expiryTimeInMillis;

    public TpaHandler(@NotNull TpaSessionHolder holder) {
        this.tpaSessionHolder = holder;
        this.expiryTimeInMillis = holder.getExpiryTimeInMillis();
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        final var tpaCmd = literal("tpa")
                .requires(CommandSourceStack::isPlayer)
                .then(argument("player", greedyString()).executes(ctx -> {
                    @NotNull final var fromPlayer = ctx.getSource().getPlayerOrException();
                    @Nullable final var toPlayer = ctx.getSource().getServer().getPlayerList()
                            .getPlayerByName(ctx.getArgument("player", String.class));
                    if (toPlayer == null) {
                        fromPlayer.sendSystemMessage(Component.literal(String.format("§c没有名为 §6%s §c的玩家", ctx.getArgument("player", String.class))));
                        return 1;
                    }
                    if (tpaSessionHolder.existsFromUUID(fromPlayer.getUUID())) {
                        fromPlayer.sendSystemMessage(Component.literal("§c你已有一个待对方处理的请求."));
                        return 1;
                    }

                    if (tpaSessionHolder.existsToUUID(toPlayer.getUUID())) {
                        fromPlayer.sendSystemMessage(Component.literal("§c该玩家已被其他玩家请求."));
                        return 1;
                    }

                    tpaSessionHolder.subscribe(
                            fromPlayer.getUUID(),
                            toPlayer.getUUID(),
                            () -> fromPlayer.sendSystemMessage(Component.literal("§c请求已过期!"))
                    );
                    fromPlayer.sendSystemMessage(Component.literal(String.format("§e传送请求已发送给 §6%s", toPlayer.getGameProfile().getName())));
                    toPlayer.sendSystemMessage(Component.literal(String.format("§e收到来自 §6%s 的传送请求", fromPlayer.getGameProfile().getName())));
                    toPlayer.sendSystemMessage(Component.literal(String.format("§e在 %d 秒内输入 §a/tpaccept §e以同意请求.", expiryTimeInMillis / 1000L)));
                    toPlayer.sendSystemMessage(Component.literal("§e输入 §c/tpdeny §e以拒绝请求."));

                    BedrockFormBuilder.getInstance()
                            .toFloodgatePlayer(toPlayer)
                            .ifPresent(floodgatePlayer -> floodgatePlayer.sendForm(
                                    BedrockFormBuilder.getInstance().createTPAConfirmationForm(
                                            floodgatePlayer,
                                            fromPlayer.getGameProfile().getName()
                                    )
                            ));

                    return 0;
                }).suggests((ctx, builder) -> {
                    Arrays.stream(ctx.getSource().getServer().getPlayerNames()).forEach(builder::suggest);
                    return builder.buildFuture();
                }));

        final var tpAcceptCmd = literal("tpaccept")
                .requires(CommandSourceStack::isPlayer)
                .executes(ctx -> {
                    @NotNull final var toPlayer = ctx.getSource().getPlayerOrException();
                    tpaSessionHolder.resolve(toPlayer.getUUID()).ifPresentOrElse(
                            uuid -> {
                                @Nullable final var fromPlayer = ctx.getSource().getServer().getPlayerList()
                                        .getPlayer(uuid);
                                if (fromPlayer == null) {
                                    toPlayer.sendSystemMessage(Component.literal("§c玩家已下线"));
                                    return;
                                }

                                fromPlayer.teleportTo(
                                        (ServerLevel) toPlayer.getCommandSenderWorld(),
                                        toPlayer.getX(),
                                        toPlayer.getY(),
                                        toPlayer.getZ(),
                                        toPlayer.getYRot(),
                                        toPlayer.getXRot()
                                );
                                fromPlayer.sendSystemMessage(Component.literal("§a传送请求已接受"));
                                toPlayer.sendSystemMessage(Component.literal("§a传送请求已接受"));
                            },
                            () -> toPlayer.sendSystemMessage(Component.literal("§c你没有待处理的请求!"))
                    );
                    return 0;
                });

        final var tpDenyCmd = literal("tpdeny")
                .requires(CommandSourceStack::isPlayer)
                .executes(ctx -> {
                    @NotNull final var player = ctx.getSource().getPlayerOrException();
                    tpaSessionHolder.resolve(player.getUUID()).ifPresentOrElse(
                            uuid -> {
                                @Nullable final var fromPlayer = ctx.getSource().getServer()
                                        .getPlayerList().getPlayer(uuid);
                                if (fromPlayer == null) {
                                    player.sendSystemMessage(Component.literal("§c玩家已下线"));
                                    return;
                                }

                                player.sendSystemMessage(Component.literal("§c传送请求已拒绝"));
                                fromPlayer.sendSystemMessage(Component.literal("§c传送请求已拒绝"));
                            },
                            () -> player.sendSystemMessage(Component.literal("§c你没有待处理的请求!"))
                    );
                    return 0;
                });

        dispatcher.register(tpaCmd);
        dispatcher.register(tpAcceptCmd);
        dispatcher.register(tpDenyCmd);
    }
}