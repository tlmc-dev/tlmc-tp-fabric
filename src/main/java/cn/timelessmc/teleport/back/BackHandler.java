package cn.timelessmc.teleport.back;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Objects;

import static net.minecraft.commands.Commands.literal;

public class BackHandler implements CommandRegistrationCallback {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        final var backCmd = literal("back")
                .requires(CommandSourceStack::isPlayer)
                .executes(ctx -> {
                    final var player = ctx.getSource().getPlayerOrException();
                    player.getLastDeathLocation().ifPresentOrElse(
                            location -> {
                                player.teleportTo(
                                        Objects.requireNonNull(player.getServer()).getLevel(location.dimension()),
                                        location.pos().getX(),
                                        location.pos().getY(),
                                        location.pos().getZ(),
                                        player.getYRot(),
                                        player.getXRot()
                                );
                                player.sendSystemMessage(Component.literal("§a传送成功!"));
                            },
                            () -> player.sendSystemMessage(Component.literal("§c没有找到上一个死亡位置"))
                    );
                    return 0;
                });
        dispatcher.register(backCmd);
    }
}
