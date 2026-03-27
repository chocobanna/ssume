package com.choco.starsector.editor.logic;

public class ModInfoLogic {

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
    // JSON GENERATION
    // ========================

    public String generateJson(
            String id,
            String name,
            String author,
            int major,
            int minor,
            int patch,
            String gameVersion,
            String description
    ) {
        return String.format("""
        {
          "id": "%s",
          "name": "%s",
          "author": "%s",
          "version": { "major": %d, "minor": %d, "patch": %d },
          "gameVersion": "%s",
          "description": "%s"
        }
        """,
                id,
                name,
                author,
                major,
                minor,
                patch,
                gameVersion,
                escape(description)
        );
    }

    private String escape(String s) {
        return s.replace("\"", "\\\"");
    }

    // ========================
    // BASIC JSON PARSE (still simple)
    // ========================

    public String extract(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return "";

        int start = json.indexOf(":", i) + 1;
        int end = json.indexOf(",", start);

        if (end < 0) end = json.indexOf("}", start);

        return json.substring(start, end)
                .replace("\"", "")
                .trim();
    }
}