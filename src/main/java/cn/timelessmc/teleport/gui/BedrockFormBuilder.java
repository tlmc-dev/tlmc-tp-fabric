package cn.timelessmc.teleport.gui;

import cn.timelessmc.teleport.pointer.HomeEntryManager;
import cn.timelessmc.teleport.pointer.TeleportEntryManager;
import cn.timelessmc.teleport.pointer.WarpHandler;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class BedrockFormBuilder {
    private static BedrockFormBuilder instance = null;

    private final MinecraftServer server;
    private final TeleportEntryManager warpEntryManager;
    private final HomeEntryManager homeEntryManager;

    private BedrockFormBuilder(
            MinecraftServer server,
            TeleportEntryManager warpEntryManager,
            HomeEntryManager homeEntryManager
    ) {
        this.server = server;
        this.warpEntryManager = warpEntryManager;
        this.homeEntryManager = homeEntryManager;
    }

    public static void init(
            MinecraftServer server,
            TeleportEntryManager warpEntryManager,
            HomeEntryManager homeEntryManager
    ) {
        if (instance == null) {
            instance = new BedrockFormBuilder(server, warpEntryManager, homeEntryManager);
        }
    }

    public static @NotNull BedrockFormBuilder getInstance() {
        return instance;
    }

    public Player toVanillaPlayer(@NotNull FloodgatePlayer player) {
        return server.getPlayerList().getPlayer(player.getCorrectUniqueId());
    }

    public Optional<FloodgatePlayer> toFloodgatePlayer(@NotNull Player player) {
        return Optional.ofNullable(FloodgateApi.getInstance().getPlayer(player.getUUID()));
    }

    private void executeCommand(@NotNull Player player, @NotNull String command) {
        try {
            server.getFunctions().getDispatcher()
                    .execute(command, player.createCommandSourceStack());
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Form createRootForm(FloodgatePlayer player) {
        return Permissions.check(
                toVanillaPlayer(player),
                WarpHandler.MODIFY_WARP_PERMISSION,
                2
        ) ? createRootFormOp(player) : SimpleForm.builder()
                .title("TpStick Menu")
                .button("Warp To...")
                .button("Go Home")
                .button("Teleport to Player")
                .button("Modify Home List")
                .button("/back")
                .validResultHandler((response) -> {
                    switch (response.clickedButtonId()) {
                        case 0 -> player.sendForm(createWarpForm(player));
                        case 1 -> player.sendForm(createHomeForm(player));
                        case 2 -> player.sendForm(createTPARequestForm(player));
                        case 3 -> player.sendForm(createHomeModificationForm(player));
                        case 4 -> executeCommand(toVanillaPlayer(player), "back");
                    }
                }).build();
    }

    public Form createRootFormOp(FloodgatePlayer player) {
        return SimpleForm.builder()
                .title("TpStick Menu")
                .button("Warp To...")
                .button("Go Home")
                .button("Teleport to Player")
                .button("Modify Warp List")
                .button("Modify Home List")
                .button("/back")
                .validResultHandler((response) -> {
                    switch (response.clickedButtonId()) {
                        case 0 -> player.sendForm(createWarpForm(player));
                        case 1 -> player.sendForm(createHomeForm(player));
                        case 2 -> player.sendForm(createTPARequestForm(player));
                        case 3 -> player.sendForm(createWarpModificationForm(player));
                        case 4 -> player.sendForm(createHomeModificationForm(player));
                        case 5 -> executeCommand(toVanillaPlayer(player), "back");
                    }
                }).build();
    }

    public Form createTPAConfirmationForm(FloodgatePlayer toPlayer, String fromPlayerName) {
        final var vanillaPlayer = toVanillaPlayer(toPlayer);
        return ModalForm.builder()
                .title("TPA Confirmation")
                .content("Player §6" + fromPlayerName + " §rsent a request to teleport to you.")
                .button1("Accept")
                .button2("Deny").validResultHandler(
                        response -> executeCommand(
                                vanillaPlayer,
                                response.clickedFirst() ? "tpaccept" : "tpdeny"
                        )
                ).build();
    }

    private @NotNull Form createWarpForm(FloodgatePlayer player) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("Warp To...")
                .button("[Back]");
        warpEntryManager.getTeleportEntryNames().forEach(builder::button);

        builder.validResultHandler((response) -> {
            if (response.clickedButtonId() == 0) {
                player.sendForm(createRootForm(player));
            } else {
                executeCommand(
                        toVanillaPlayer(player),
                        "warp " + response.clickedButton().text()
                );
            }
        });
        return builder.build();
    }

    private @NotNull Form createHomeForm(@NotNull FloodgatePlayer player) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("Go Home")
                .button("[Back]");
        homeEntryManager.getHomeEntryNames(player.getCorrectUniqueId()).forEach(builder::button);

        builder.validResultHandler((response) -> {
            if (response.clickedButtonId() == 0) {
                player.sendForm(createRootForm(player));
            } else {
                executeCommand(
                        toVanillaPlayer(player),
                        "home " + response.clickedButton().text()
                );
            }
        });
        return builder.build();
    }

    private @NotNull Form createTPARequestForm(FloodgatePlayer player) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("Teleport to Player")
                .button("[Back]");
        Arrays.stream(server.getPlayerNames()).forEach(builder::button);

        builder.validResultHandler((response) -> {
            if (response.clickedButtonId() == 0) {
                player.sendForm(createRootForm(player));
            } else {
                executeCommand(
                        toVanillaPlayer(player),
                        "tpa " + response.clickedButton().text()
                );
            }

        });
        return builder.build();
    }

    private @NotNull Form createWarpModificationForm(FloodgatePlayer player) {
        return SimpleForm.builder()
                .title("Modify Warp List")
                .button("[Back]")
                .button("Add...")
                .button("Delete...")
                .validResultHandler((response) -> {
                    switch (response.clickedButtonId()) {
                        case 0 -> player.sendForm(createRootForm(player));
                        case 1 -> player.sendForm(createWarpAddingForm(player));
                        case 2 -> player.sendForm(createWarpDeletingForm(player));
                    }
                }).build();
    }

    private @NotNull Form createWarpAddingForm(FloodgatePlayer player) {
        return CustomForm.builder()
                .title("Add...")
                .input("Name")
                .validResultHandler((response) -> {
                    String name = Objects.requireNonNull(response.asInput());
                    executeCommand(
                            toVanillaPlayer(player),
                            "setwarp " + name
                    );
                }).build();
    }

    private @NotNull Form createWarpDeletingForm(FloodgatePlayer player) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("Delete...")
                .button("[Back]");
        warpEntryManager.getTeleportEntryNames().forEach(builder::button);
        builder.validResultHandler((response) -> {
            if (response.clickedButtonId() == 0) {
                player.sendForm(createWarpModificationForm(player));
            } else {
                player.sendForm(ModalForm.builder()
                        .title("Confirm")
                        .content("Do you really want to delete the warp entry '" + response.clickedButton().text() + "'?")
                        .button1("Yes")
                        .button2("No")
                        .validResultHandler((_response) -> {
                            if (_response.clickedFirst()) {
                                executeCommand(
                                        toVanillaPlayer(player),
                                        "delwarp " + response.clickedButton().text()
                                );
                            } else {
                                player.sendForm(createWarpDeletingForm(player));
                            }
                        }).build());
            }
        });
        return builder.build();
    }

    private @NotNull Form createHomeModificationForm(FloodgatePlayer player) {
        return SimpleForm.builder()
                .title("Modify Home List")
                .button("[Back]")
                .button("Add...")
                .button("Delete...")
                .validResultHandler((response) -> {
                    switch (response.clickedButtonId()) {
                        case 0 -> player.sendForm(createRootForm(player));
                        case 1 -> player.sendForm(createHomeAddingForm(player));
                        case 2 -> player.sendForm(createHomeDeletingForm(player));
                    }
                }).build();
    }

    private @NotNull Form createHomeAddingForm(FloodgatePlayer player) {
        return CustomForm.builder()
                .title("Add...")
                .input("Name")
                .validResultHandler((response) -> {
                    String name = Objects.requireNonNull(response.asInput());
                    executeCommand(
                            toVanillaPlayer(player),
                            "sethome " + name
                    );
                }).build();
    }

    private @NotNull Form createHomeDeletingForm(@NotNull FloodgatePlayer player) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("Delete...")
                .button("[Back]");
        homeEntryManager.getHomeEntryNames(player.getCorrectUniqueId()).forEach(builder::button);
        builder.validResultHandler((response) -> {
            if (response.clickedButtonId() == 0) {
                player.sendForm(createHomeModificationForm(player));
            } else {
                player.sendForm(ModalForm.builder()
                        .title("Confirm")
                        .content("Do you really want to delete the home entry '" + response.clickedButton().text() + "'?")
                        .button1("Yes")
                        .button2("No")
                        .validResultHandler((_response) -> {
                            if (_response.clickedFirst()) {
                                executeCommand(
                                        toVanillaPlayer(player),
                                        "delhome " + response.clickedButton().text()
                                );
                            } else {
                                player.sendForm(createHomeDeletingForm(player));
                            }
                        }).build());
            }
        });
        return builder.build();
    }
}
