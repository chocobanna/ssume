package com.choco.starsector.editor.model;

import java.util.List;

public class ModInfo {

    public String id;
    public String name;
    public String author;
    public Version version;
    public String gameVersion;
    public String description;

    public boolean utility = false;
    public boolean totalConversion = false;

    public List<Dependency> dependencies;

    public static class Version {
        public int major;
        public int minor;
        public int patch;
    }

    public static class Dependency {
        public String id;
        public String name;
    }
}