package cn.timelessmc.teleport.tpa;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import java.util.Optional;
import java.util.UUID;

public class TpaSessionHolder {
    private final long expiryTimeInMillis;
    private final BiMap<UUID, UUID> requests = Maps.synchronizedBiMap(HashBiMap.create());

    public TpaSessionHolder(long expiryTimeInMillis) {
        this.expiryTimeInMillis = expiryTimeInMillis;
    }

    public void subscribe(UUID from, UUID to, Runnable expiryCallback) {
        Thread.startVirtualThread(() -> {
            requests.put(from, to);
            try {
                Thread.sleep(expiryTimeInMillis);
            } catch (InterruptedException ignored) {}
            resolve(to).ifPresent(uuid -> expiryCallback.run());
        });
    }

    public boolean existsFromUUID(UUID from) {
        return requests.containsKey(from);
    }

    public boolean existsToUUID(UUID to) {
        return requests.inverse().containsKey(to);
    }

    public Optional<UUID> resolve(UUID to) {
        return Optional.ofNullable(requests.inverse().remove(to));
    }

    public long getExpiryTimeInMillis() {
        return expiryTimeInMillis;
    }
}
