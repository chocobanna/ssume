package com.choco.starsector.editor.logic;

import com.choco.starsector.editor.model.ModInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ModInfoLogic {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    // ========================
    // ID GENERATION
    // ========================
    public String generateId(String author, String name) {
        String a = sanitize(author);
        String n = sanitize(name);

        if (a.isEmpty() || n.isEmpty()) return "";
        return a + "." + n;
    }

    private String sanitize(String input) {
        if (input == null) return "";

        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\s_]", "")
                .trim()
                .replaceAll("\\s+", "_");
    }

    // ========================
    // JSON → OBJECT
    // ========================
    public ModInfo parse(String json) throws Exception {
        return mapper.readValue(json, ModInfo.class);
    }

    // ========================
    // OBJECT → JSON
    // ========================
    public String toJson(ModInfo mod) throws Exception {
        return mapper.writeValueAsString(mod);
    }
}