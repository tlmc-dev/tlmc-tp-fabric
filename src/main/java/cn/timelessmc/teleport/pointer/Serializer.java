package cn.timelessmc.teleport.pointer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class Serializer {
    /**
     * The Gson instance for serialization and deserialization
     * with type adapter for {@link net.minecraft.resources.ResourceKey}
     */
    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(TeleportEntry.class, new TypeAdapter<TeleportEntry>() {
                @Override
                public void write(JsonWriter out, TeleportEntry value) throws IOException {
                    out.beginObject()
                            .name("world").value(value.world().location().toString())
                            .name("x").value(value.x())
                            .name("y").value(value.y())
                            .name("z").value(value.z())
                    .endObject();
                }

                @Override
                public @Nullable TeleportEntry read(JsonReader in) throws IOException {
                    in.beginObject();
                    var x = 0d;
                    var y = 0d;
                    var z = 0d;
                    var world = Level.OVERWORLD;
                    while (in.hasNext()) {
                        switch (in.nextName()) {
                            case "world" -> world = switch (in.nextString()) {
                                case "minecraft:overworld" -> Level.OVERWORLD;
                                case "minecraft:the_nether" -> Level.NETHER;
                                case "minecraft:the_end" -> Level.END;
                                default -> null;
                            };
                            case "x" -> x = in.nextDouble();
                            case "y" -> y = in.nextDouble();
                            case "z" -> z = in.nextDouble();
                        }
                    }
                    in.endObject();
                    if (world == null) {
                        return null;
                    } else {
                        return new TeleportEntry(world, x, y, z);
                    }
                }
            })
            .create();

}
