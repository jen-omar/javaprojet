package com.example.mythoriadesktop.data;

import com.example.mythoriadesktop.model.World;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WorldRepository {
    private static final Logger LOG = Logger.getLogger(WorldRepository.class.getName());

    private static final Type WORLD_LIST_TYPE = TypeToken.getParameterized(List.class, World.class).getType();

    private final Gson gson;
    private final Path storageFile;
    private final ObservableList<World> worlds;

    public WorldRepository() {
        this(defaultStorageFile());
    }

    public WorldRepository(Path storageFile) {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new com.google.gson.TypeAdapter<Instant>() {
                    @Override
                    public void write(com.google.gson.stream.JsonWriter out, Instant value) throws IOException {
                        if (value == null) {
                            out.nullValue();
                        } else {
                            out.value(value.toString());
                        }
                    }

                    @Override
                    public Instant read(com.google.gson.stream.JsonReader in) throws IOException {
                        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                            in.nextNull();
                            return null;
                        }
                        return Instant.parse(in.nextString());
                    }
                })
                .setPrettyPrinting()
                .create();
        this.storageFile = storageFile;
        this.worlds = FXCollections.observableArrayList();
        loadFromDisk();
    }

    public ObservableList<World> worlds() {
        return worlds;
    }

    public World create(String title, String description, String loreSnapshot) {
        World world = World.createNew(title, description, loreSnapshot);
        worlds.add(0, world);
        saveToDisk();
        return world;
    }

    public World update(UUID id, String title, String description, String loreSnapshot) {
        int idx = indexOf(id);
        if (idx < 0) {
            throw new IllegalArgumentException("World not found: " + id);
        }

        World current = worlds.get(idx);
        World updated = new World(
                current.id(),
                Optional.ofNullable(title).orElse("").trim(),
                Optional.ofNullable(description).orElse("").trim(),
                Optional.ofNullable(loreSnapshot).orElse("").trim(),
                current.createdAt() == null ? Instant.now() : current.createdAt(),
                current.books() == null ? new ArrayList<>() : current.books()
        );
        worlds.set(idx, updated);
        saveToDisk();
        return updated;
    }

    public void delete(UUID id) {
        int idx = indexOf(id);
        if (idx >= 0) {
            worlds.remove(idx);
            saveToDisk();
        }
    }

    public void addBookToWorld(UUID worldId, com.example.mythoriadesktop.model.Book book) {
        int idx = indexOf(worldId);
        if (idx >= 0) {
            World w = worlds.get(idx);
            List<com.example.mythoriadesktop.model.Book> newBooks = new ArrayList<>(w.books());
            newBooks.add(book);
            World updated = w.withBooks(newBooks);
            worlds.set(idx, updated);
            saveToDisk();
        }
    }

    public Optional<World> getWorld(UUID id) {
        int idx = indexOf(id);
        return idx >= 0 ? Optional.of(worlds.get(idx)) : Optional.empty();
    }

    private int indexOf(UUID id) {
        for (int i = 0; i < worlds.size(); i++) {
            World w = worlds.get(i);
            if (w != null && id != null && id.equals(w.id())) {
                return i;
            }
        }
        return -1;
    }

    private void loadFromDisk() {
        if (Files.notExists(storageFile)) {
            seedIfEmpty();
            saveToDisk();
            return;
        }

        try {
            String json = Files.readString(storageFile, StandardCharsets.UTF_8);
            List<World> loaded = gson.fromJson(json, WORLD_LIST_TYPE);
            worlds.setAll(loaded == null ? List.of() : loaded);
            if (worlds.isEmpty()) {
                seedIfEmpty();
                saveToDisk();
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to load worlds, starting empty");
            worlds.clear();
            seedIfEmpty();
            saveToDisk();
        }
    }

    private void saveToDisk() {
        try {
            Files.createDirectories(storageFile.getParent());
            String json = gson.toJson(new ArrayList<>(worlds), WORLD_LIST_TYPE);
            Files.writeString(
                    storageFile,
                    json,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to save worlds");
        }
    }

    private void seedIfEmpty() {
        if (!worlds.isEmpty()) {
            return;
        }
        worlds.add(World.createNew(
                "Mythoria Prime",
                "Le monde d'origine : archives, guildes et chroniques.",
                "Scribes, Oracles et Grand Archives au coeur d'une cité sombre et dorée."
        ));
        worlds.add(World.createNew(
                "The Ashen Coast",
                "Une côte volcanique où les chroniques se paient en cendres.",
                "Le vent porte des fragments de mémoires et des noms oubliés."
        ));
        worlds.add(World.createNew(
                "Gilded Ruins",
                "Des ruines arcanepunk aux mécanismes d'or.",
                "La lumière dorée réagit aux serments et aux mensonges."
        ));
    }

    private static Path defaultStorageFile() {
        return Path.of(System.getProperty("user.dir"), ".mythoria", "worlds.json");
    }
}
