package cn.timelessmc.teleport.pointer;

import cn.timelessmc.teleport.FabricMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HomeEntryManager {
    private final Path root;
    private final Map<UUID, TeleportEntryManager> entries;

    public HomeEntryManager(Path root) throws IOException {
        this.root = root;
        if (Files.notExists(root)) {
            Files.createDirectories(root);
        }
        try (var files = Files.list(root)) {
            entries = files.filter(Files::isDirectory)
                    .map(playerDirectory -> {
                        try {
                            return Map.entry(
                                    UUID.fromString(playerDirectory.getFileName().toString()),
                                    new TeleportEntryManager(playerDirectory)
                            );
                        } catch (IOException e) {
                            FabricMod.LOGGER.error("Failed to read home entries from {}", playerDirectory);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));
        }
    }

    public Optional<TeleportEntry> getHomeEntry(UUID player, String name) {
        return entries.containsKey(player) ?
                entries.get(player).getTeleportEntry(name) :
                Optional.empty();
    }

    public void setHomeEntry(UUID player, String name, TeleportEntry entry) throws IOException {
        Objects.requireNonNull(entries.computeIfAbsent(player, playerUUID -> {
            try {
                return new TeleportEntryManager(root.resolve(playerUUID.toString()));
            } catch (IOException e) {
                FabricMod.LOGGER.error("Failed to create home entry manager for {}", playerUUID);
                return null;
            }
        })).setTeleportEntry(name, entry);
    }

    public boolean containsHomeEntry(UUID player, String name) {
        return entries.containsKey(player) && entries.get(player).containsTeleportEntry(name);
    }

    public void removeHomeEntry(UUID player, String name) throws IOException {
        if (entries.containsKey(player)) {
            entries.get(player).removeTeleportEntry(name);
        }
    }

    public int getHomeCount(UUID player) {
        return entries.containsKey(player) ? entries.get(player).getTeleportEntryCount() : 0;
    }

    public Stream<String> getHomeEntryNames(UUID player) {
        return entries.containsKey(player) ? entries.get(player).getTeleportEntryNames() : Stream.empty();
    }
}
