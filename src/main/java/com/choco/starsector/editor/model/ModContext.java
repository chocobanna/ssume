package com.choco.starsector.editor.model;

import java.nio.file.Path;

public class ModContext {

    private Path modFolder;
    private Path modInfoFile;

    public void setModFolder(Path folder) {
        this.modFolder = folder;
        this.modInfoFile = folder.resolve("mod_info.json");
    }

    public Path getModFolder() {
        return modFolder;
    }

    public Path getModInfoFile() {
        return modInfoFile;
    }

    public boolean isLoaded() {
        return modFolder != null;
    }
}