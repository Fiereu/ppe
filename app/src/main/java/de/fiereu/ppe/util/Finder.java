package de.fiereu.ppe.util;

import java.awt.Component;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class Finder {

  public static String findPokeMMO(Component parent) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fileChooser.setDialogTitle("Select the PokeMMO installation");

    Platform platform = Platform.get();
    fileChooser.setFileFilter(new FileFilter() {
      @Override
      public boolean accept(File f) {
        if (f.isDirectory()) return true;
        String name = f.getName().toLowerCase();
        return switch (platform) {
          case WINDOWS -> name.equals("pokemmo.exe");
          case MAC -> name.equals("pokemmo.exe");
          case LINUX -> name.equals("pokemmo.exe");
          default -> name.endsWith(".jar") || name.endsWith(".exe") || name.endsWith(".app");
        };
      }

      @Override
      public String getDescription() {
        return switch (platform) {
          case WINDOWS -> "PokeMMO (PokeMMO.exe)";
          case MAC -> "PokeMMO (~/Library/Application Support/com.pokeemu.macos/pokemmo-client-live/PokeMMO.exe)";
          case LINUX -> "PokeMMO (PokeMMO.exe - ~/.local/share/pokemmo/ or portable/)";
          default -> "PokeMMO Installation";
        };
      }
    });

    int result = fileChooser.showOpenDialog(parent);
    return result == JFileChooser.APPROVE_OPTION
        ? fileChooser.getSelectedFile().getAbsolutePath()
        : null;
  }

  public static String findAgent(Component parent) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setDialogTitle("Select the agent.jar");
    fileChooser.setCurrentDirectory(new File("."));
    fileChooser.setFileFilter(new FileFilter() {
      @Override
      public boolean accept(File f) {
        return f.getName().endsWith(".jar") || f.isDirectory();
      }

      @Override
      public String getDescription() {
        return "Agent Jar (*.jar)";
      }
    });
    int result = fileChooser.showOpenDialog(parent);
    if (result == JFileChooser.APPROVE_OPTION) {
      return fileChooser.getSelectedFile().getAbsolutePath();
    } else {
      return null;
    }
  }
}
