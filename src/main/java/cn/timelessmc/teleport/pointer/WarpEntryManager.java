package cn.timelessmc.teleport.pointer;

import com.github.promeg.pinyinhelper.Pinyin;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public class WarpEntryManager extends TeleportEntryManager {
    private final Multimap<String, String> pinyinToNameMap = HashMultimap.create();

    public WarpEntryManager(Path path) throws IOException {
        super(path);
        entries.keySet().forEach(name -> pinyinToNameMap.put(
                Pinyin.toPinyin(name, "").toLowerCase(), name
        ));
    }

    @Override
    public void setTeleportEntry(String name, TeleportEntry entry) throws IOException {
        super.setTeleportEntry(name, entry);
        pinyinToNameMap.put(Pinyin.toPinyin(name, "").toLowerCase(), name);
    }

    @Override
    public boolean containsTeleportEntry(String name) {
        return super.containsTeleportEntry(name);
    }

    @Override
    public Optional<TeleportEntry> removeTeleportEntry(String name) throws IOException {
        pinyinToNameMap.remove(Pinyin.toPinyin(name, "").toLowerCase(), name);
        return super.removeTeleportEntry(name);
    }

    @Override
    public Optional<TeleportEntry> getTeleportEntry(String name) {
        return super.getTeleportEntry(name);
    }

    public Stream<String> getPossibleMatches(String pinyinPrefix) {
        return pinyinToNameMap.keySet().stream()
                .filter(pinyin -> pinyin.startsWith(pinyinPrefix))
                .flatMap(pinyin -> pinyinToNameMap.get(pinyin).stream());
    }
}
