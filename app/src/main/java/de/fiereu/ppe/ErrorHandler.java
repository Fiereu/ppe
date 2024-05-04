package de.fiereu.ppe;

import java.awt.Component;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandler {

  public static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

  public static void handle(Component parent, String message, Throwable t) {
    log.error(message, t);
    JOptionPane.showMessageDialog(parent, t.getMessage(), message, JOptionPane.ERROR_MESSAGE);
  }

  public static void handle(Component parent, String message) {
    log.error(message);
    JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
  }
}
