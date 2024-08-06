package cn.timelessmc.teleport;

import cn.timelessmc.teleport.back.BackHandler;
import cn.timelessmc.teleport.gui.BedrockFormBuilder;
import cn.timelessmc.teleport.gui.StickHandler;
import cn.timelessmc.teleport.pointer.*;
import cn.timelessmc.teleport.tpa.TpaHandler;
import cn.timelessmc.teleport.tpa.TpaSessionHolder;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class FabricMod implements DedicatedServerModInitializer {
    public static final String MOD_ID = "teleport";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeServer() {
        try {
            ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                @Override
                public void onResourceManagerReload(ResourceManager resourceManager) {

                }

                @Override
                public ResourceLocation getFabricId() {
                    return ResourceLocation.fromNamespaceAndPath(MOD_ID, "resources");
                }
            });

            final var expiryTimeInMillis = 30000L;
            final var maxHomeCount = 16;

            final var tpaSessionHolder = new TpaSessionHolder(expiryTimeInMillis);
            final var warpEntryManager = new WarpEntryManager(Path.of("world", "tp-data", "warps"));
            final var homeEntryManager = new HomeEntryManager(Path.of("world", "tp-data", "homes"));

            CommandRegistrationCallback.EVENT.register(new StickHandler());
            CommandRegistrationCallback.EVENT.register(new BackHandler());
            CommandRegistrationCallback.EVENT.register(new TpaHandler(tpaSessionHolder));
            CommandRegistrationCallback.EVENT.register(new WarpHandler(warpEntryManager));
            CommandRegistrationCallback.EVENT.register(new HomeHandler(homeEntryManager, maxHomeCount));

            ServerLifecycleEvents.SERVER_STARTED.register(server -> BedrockFormBuilder.init(server, warpEntryManager, homeEntryManager));
        } catch (Exception e) {
            LOGGER.error("An error occurred while initializing the plugin: {}", e.getMessage());
        }
    }
}
