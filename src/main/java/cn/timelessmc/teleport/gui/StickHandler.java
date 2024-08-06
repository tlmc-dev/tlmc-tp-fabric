package cn.timelessmc.teleport.gui;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.commands.Commands.literal;

public class StickHandler implements CommandRegistrationCallback {
    public StickHandler() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            final var itemStack = player.getItemInHand(hand);
            if (itemStack.getItem() == Items.STICK && itemStack.isEnchanted()) {
                BedrockFormBuilder.getInstance()
                        .toFloodgatePlayer(player)
                        .ifPresent(floodgatePlayer -> floodgatePlayer.sendForm(
                                BedrockFormBuilder.getInstance().createRootForm(floodgatePlayer)
                        ));
            }
            return InteractionResultHolder.pass(itemStack);
        });
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        final var snowballCmd = literal("tpstick")
                .requires(CommandSourceStack::isPlayer)
                .executes(ctx -> {
                    @NotNull final var player = ctx.getSource().getPlayerOrException();
                    final var stickItemStack = new ItemStack(Items.STICK);
                    stickItemStack.set(DataComponents.ITEM_NAME, Component.literal("§b传送棒"));
                    player.level().registryAccess()
                            .registryOrThrow(Registries.ENCHANTMENT)
                            .getHolder(Enchantments.FROST_WALKER)
                            .ifPresent(enchantment -> stickItemStack.enchant(enchantment, 1));
                    player.getInventory().add(stickItemStack);
                    return 0;
                });

        dispatcher.register(snowballCmd);
    }
}
