package com.choco.starsector.editor.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModFileService {

    public String load(Path file) throws IOException {
        return Files.readString(file);
    }

    public void save(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    public boolean validate(String json) {
        return json.contains("\"id\"")
                && json.contains("\"name\"")
                && json.contains("\"version\"")
                && json.contains("\"gameVersion\"");
    }

    public String defaultTemplate() {
        return """
        {
          "id": "my_mod",
          "name": "My Mod",
          "author": "You",
          "version": { "major": 1, "minor": 0, "patch": 0 },
          "gameVersion": "0.97a",
          "description": "Describe your mod"
        }
        """;
    }
}