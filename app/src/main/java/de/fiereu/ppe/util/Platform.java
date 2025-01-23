package de.fiereu.ppe.util;

public enum Platform {
    WINDOWS, LINUX, MAC, UNKNOWN;

    public static Platform get() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return WINDOWS;
        } else if (os.contains("nix") || os.contains("nux")) {
            return LINUX;
        } else if (os.contains("mac")) {
            return MAC;
        } else {
            return UNKNOWN;
        }
    }
}
