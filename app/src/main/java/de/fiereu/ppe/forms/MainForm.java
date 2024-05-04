/*
 * Created by JFormDesigner on Fri Apr 26 21:45:33 CEST 2024
 */

package de.fiereu.ppe.forms;

import de.fiereu.ppe.ErrorHandler;
import de.fiereu.ppe.PacketHistory;
import de.fiereu.ppe.proxy.ServerType;
import de.fiereu.ppe.util.Finder;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainForm extends JFrame {

  private static final Logger log = LoggerFactory.getLogger(MainForm.class);
  private final Configuration config;
  private final PacketHistory packetHistory;
  private JMenuBar menuBar1;
  private JMenuItem menuItem1;
  private JMenuItem menuItem2;
  private JMenu menu1;
  private JMenu menu2;
  private JTabbedPane serverTabs;
  private ServerControlPane serverController;

  public MainForm(Configuration config, PacketHistory packetHistory) {
    this.config = config;
    this.packetHistory = packetHistory;
    setUncaughtExceptionHandler();
    initComponents();
  }

  private void setUncaughtExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
      ErrorHandler.handle(this, "Uncaught exception in thread %s".formatted(t.getName()), e);
      log.error("Uncaught exception in thread %s".formatted(t.getName()), e);
    });
  }

  private void thisWindowClosing(WindowEvent e) {
    serverController.close();
    packetHistory.close();
  }

  private void createUIComponents() {
  }

  private void runPokeMMO(ActionEvent e) {
    String path = config.getString("pokeMMO.path");
    if (path == null) {
      path = Finder.findPokeMMO(this);
      if (path != null) {
        config.setProperty("pokeMMO.path", path);
      }
    }
      if (path == null) {
          return;
      }
    File pokeMMO = new File(path);
    if (!pokeMMO.exists()) {
      JOptionPane.showMessageDialog(this, "PokeMMO not found", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    path = config.getString("pokeMMO.agent");
    if (path == null) {
      path = Finder.findAgent(this);
      if (path != null) {
        config.setProperty("pokeMMO.agent", path);
      }
    }
      if (path == null) {
          return;
      }
    File agent = new File(path);
    if (!agent.exists()) {
      JOptionPane.showMessageDialog(this, "Agent not found", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    try {
      new ProcessBuilder("java", "-javaagent:\"" + agent.getAbsolutePath() + "\"", "-jar",
          pokeMMO.getAbsolutePath())
          .directory(pokeMMO.getParentFile())
          .start();
    } catch (Exception ex) {
      config.clearProperty("pokeMMO.path");
      config.clearProperty("pokeMMO.agent");
      JOptionPane.showMessageDialog(this, "Failed to start PokeMMO", "Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private void openRepeaterBtn(ActionEvent e) {
    new RepeaterForm(
        ((ServerTab) serverTabs.getSelectedComponent()).serverType,
        serverController
    ).setVisible(true);
  }

  private void initComponents() {
    menuBar1 = new JMenuBar();
    menu1 = new JMenu();
    menuItem1 = new JMenuItem();
    menu2 = new JMenu();
    menuItem2 = new JMenuItem();
    serverController = new ServerControlPane(packetHistory);
    serverTabs = new JTabbedPane();
    serverTabs.addTab(ServerType.LOGIN.name(),
        new ServerTab(ServerType.LOGIN, serverController, packetHistory));
    serverTabs.addTab(ServerType.GAME.name(),
        new ServerTab(ServerType.GAME, serverController, packetHistory));
    serverTabs.addTab(ServerType.CHAT.name(),
        new ServerTab(ServerType.CHAT, serverController, packetHistory));

    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        thisWindowClosing(e);
      }
    });
    var contentPane = getContentPane();
    contentPane.setLayout(new BorderLayout());

    menu1.setText("Game");

    menuItem1.setText("Start");
    menuItem1.addActionListener(this::runPokeMMO);
    menu1.add(menuItem1);

    menuItem2.setText("Repeat");
    menuItem2.addActionListener(this::openRepeaterBtn);
    menu2.add(menuItem2);

    menu2.setText("Tools");
    menuBar1.add(menu1);
    menuBar1.add(menu2);

    setJMenuBar(menuBar1);
    contentPane.add(serverTabs, BorderLayout.CENTER);
    contentPane.add(serverController, BorderLayout.WEST);
    setSize(800, 450);
    setLocationRelativeTo(null);
  }
}
