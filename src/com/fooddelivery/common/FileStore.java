package com.fooddelivery.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;

// Shared helper: read/write text. It knows nothing about food or orders.
public class FileStore {
  public final Gson json = new GsonBuilder().setPrettyPrinting().create();
  private final Path folder;

  public FileStore(String folder) throws IOException {
    this.folder = Path.of(folder);
    Files.createDirectories(this.folder);
  }

  public String read(String name) throws IOException {
    Path file = folder.resolve(name);
    if (!Files.exists(file)) return "[]";
    return Files.readString(file);
  }

  public void write(String name, String text) throws IOException {
    Path file = folder.resolve(name);
    Path temporary = Files.createTempFile(folder, "saving-", ".tmp");
    try {
      Files.writeString(temporary, text);
      try {
        Files.move(
            temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  public boolean seeded() {
    return Files.exists(folder.resolve("demo-ready.txt"));
  }

  public void markSeeded() throws IOException {
    write("demo-ready.txt", "Demo data created.\n");
  }
}
