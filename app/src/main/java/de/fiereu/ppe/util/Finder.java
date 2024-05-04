package de.fiereu.ppe.util;

import java.awt.Component;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class Finder {

  public static String findPokeMMO(Component parent) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setDialogTitle("Select the PokeMMO executable");
    fileChooser.setFileFilter(new FileFilter() {
      @Override
      public boolean accept(File f) {
        return f.getName().equals("PokeMMO.exe") || f.isDirectory();
      }

      @Override
      public String getDescription() {
        return "PokeMMO Executable (*.exe)";
      }
    });
    int result = fileChooser.showOpenDialog(parent);
    if (result == JFileChooser.APPROVE_OPTION) {
      return fileChooser.getSelectedFile().getAbsolutePath();
    } else {
      return null;
    }
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
