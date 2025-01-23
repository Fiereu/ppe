package de.fiereu.ppe.forms;

import de.fiereu.ppe.ErrorHandler;
import de.fiereu.ppe.PacketHistory;
import de.fiereu.ppe.proxy.ServerType;
import de.fiereu.ppe.util.Finder;

import de.fiereu.ppe.util.Platform;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;


import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainForm extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(MainForm.class);
    private final Configuration config;
    private final PacketHistory packetHistory;
    private final ServerControlPane serverController;
    private JMenuBar menuBar;
    private JTabbedPane serverTabs;

    public MainForm(Configuration config, PacketHistory packetHistory) {
        this.config = config;
        this.packetHistory = packetHistory;
        this.serverController = new ServerControlPane(packetHistory);

        setUncaughtExceptionHandler();
        initComponents();
    }

    private void setUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            ErrorHandler.handle(this, "Uncaught exception in thread " + thread.getName(), exception);
            log.error("Uncaught exception in thread {}", thread.getName(), exception);
        });
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                serverController.close();
                packetHistory.close();
            }
        });

        // Layout
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        // Tabs
        serverTabs = new JTabbedPane();
        serverTabs.addTab(ServerType.LOGIN.name(), new ServerTab(ServerType.LOGIN, serverController, packetHistory));
        serverTabs.addTab(ServerType.GAME.name(), new ServerTab(ServerType.GAME, serverController, packetHistory));
        serverTabs.addTab(ServerType.CHAT.name(), new ServerTab(ServerType.CHAT, serverController, packetHistory));
        contentPane.add(serverTabs, BorderLayout.CENTER);
        contentPane.add(serverController, BorderLayout.WEST);

        // Menu
        menuBar = new JMenuBar();
        createGameMenu();
        createToolsMenu();
        setJMenuBar(menuBar);

        // Window settings
        setSize(800, 450);
        setLocationRelativeTo(null);
    }

    private void createGameMenu() {
        JMenu gameMenu = new JMenu("Game");

        JMenuItem startItem = new JMenuItem("Start");
        startItem.addActionListener(this::runPokeMMO);
        gameMenu.add(startItem);

        menuBar.add(gameMenu);
    }

    private void createToolsMenu() {
        JMenu toolsMenu = new JMenu("Tools");

        JMenuItem repeatItem = new JMenuItem("Repeat");
        repeatItem.addActionListener(this::openRepeaterForm);
        toolsMenu.add(repeatItem);

        JMenuItem chainItem = new JMenuItem("Packet Chainer");
        chainItem.addActionListener(e -> new PacketChainerForm(serverController).setVisible(true));
        toolsMenu.add(chainItem);

        menuBar.add(toolsMenu);
    }

    private void runPokeMMO(ActionEvent e) {
        try {
            String pokeMMOPath = getFilePath("pokeMMO.path", Finder::findPokeMMO, "PokeMMO not found");
            if (pokeMMOPath == null)
                return;

            String agentPath = getFilePath("pokeMMO.agent", Finder::findAgent, "Agent not found");
            if (agentPath == null)
                return;

            List<String> command = new ArrayList<>();
            if (Platform.get() == Platform.WINDOWS) {
                command.add("cmd");
                command.add("/c");
                command.add("start");
            }
            command.addAll(List.of("java", "-javaagent:\"" + agentPath + "\"", "-jar", pokeMMOPath));
            new ProcessBuilder(command)
                    .directory(new File(pokeMMOPath).getParentFile())
                    .start();
        } catch (IOException ex) {
            log.error("Failed to start PokeMMO", ex);
            config.clearProperty("pokeMMO.path");
            config.clearProperty("pokeMMO.agent");
            showErrorDialog("Failed to start PokeMMO");
        }
    }

    private void openRepeaterForm(ActionEvent e) {
        ServerTab selectedTab = (ServerTab) serverTabs.getSelectedComponent();
        if (selectedTab != null) {
            new RepeaterForm(selectedTab.serverType, serverController).setVisible(true);
        }
    }

    private String getFilePath(String configKey, FileFinder finder, String errorMessage) {
        String path = config.getString(configKey);
        if (path == null) {
            path = finder.find(this);
            if (path != null) {
                config.setProperty(configKey, path);
            }
        }
        if (path == null || !new File(path).exists()) {
            showErrorDialog(errorMessage);
            return null;
        }
        return path;
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    private interface FileFinder {
        String find(MainForm mainForm);
    }
}
