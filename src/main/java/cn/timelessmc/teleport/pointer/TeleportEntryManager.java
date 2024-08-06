package cn.timelessmc.teleport.pointer;

import cn.timelessmc.teleport.FabricMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static cn.timelessmc.teleport.pointer.Serializer.gson;

public class TeleportEntryManager {
    private final Path root;
    protected final Map<String, TeleportEntry> entries;

    public TeleportEntryManager(Path path) throws IOException {
        this.root = path;
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
        try (var files = Files.list(path)) {
            entries = files.filter(Files::isRegularFile)
                    .map(file -> {
                        try {
                            return Map.entry(
                                    file.getFileName().toString(),
                                    gson.fromJson(Files.readString(file), TeleportEntry.class)
                            );
                        } catch (IOException e) {
                            FabricMod.LOGGER.error("Failed to read warp entry from {}", file);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(
                            ConcurrentHashMap::new,
                            (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                            ConcurrentHashMap::putAll
                    );
        }
    }

    public Optional<TeleportEntry> getTeleportEntry(String name) {
        return Optional.ofNullable(entries.get(name));
    }

    public void setTeleportEntry(String name, TeleportEntry entry) throws IOException {
        Files.writeString(root.resolve(name), gson.toJson(entry));
        entries.put(name, entry);
    }

    public boolean containsTeleportEntry(String name) {
        return entries.containsKey(name);
    }

    public Optional<TeleportEntry> removeTeleportEntry(String name) throws IOException {
        Files.delete(root.resolve(name));
        return Optional.ofNullable(entries.remove(name));
    }

    public int getTeleportEntryCount() {
        return entries.size();
    }

    public Stream<String> getTeleportEntryNames() {
        return entries.keySet().stream();
    }
}
