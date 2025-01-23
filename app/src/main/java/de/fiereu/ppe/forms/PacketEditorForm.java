package de.fiereu.ppe.forms;

import de.fiereu.ppe.proxy.Direction;
import de.fiereu.ppe.proxy.ProxyClient;
import de.fiereu.ppe.proxy.ServerType;
import net.miginfocom.swing.MigLayout;
import org.exbin.auxiliary.binary_data.ByteArrayEditableData;
import org.exbin.bined.EditMode;
import org.exbin.bined.swing.basic.CodeArea;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class PacketEditorForm extends JFrame {

    private JComboBox<ServerType> serverCombo;
    private JComboBox<Direction> directionCombo;
    private Map<ServerType, DefaultComboBoxModel<ProxyClient>> clientModels = new HashMap<>();
    private JComboBox<ProxyClient> clientCombo;
    private JSpinner packetIdSpinner;
    private CodeArea packetDataArea;

    public PacketEditorForm(ServerControlPane serverController) {
        initComponents();

        Arrays.stream(ServerType.values()).forEach(server -> {
            DefaultComboBoxModel<ProxyClient> model = clientModels.get(server);
            model.removeAllElements();
            model.addAll(serverController.getClients(server).stream().toList());
        });
    }

    public PacketEditorForm(ServerControlPane serverController, Direction direction, int packetId, byte[] packetData) {
        initComponents();

        Arrays.stream(ServerType.values()).forEach(server -> {
            DefaultComboBoxModel<ProxyClient> model = clientModels.get(server);
            model.removeAllElements();
            model.addAll(serverController.getClients(server).stream().toList());
        });

        directionCombo.setSelectedItem(direction);
        packetIdSpinner.setValue(packetId);
        packetDataArea.setContentData(new ByteArrayEditableData(packetData));
    }

    private void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Packet Editor");
        setLayout(new MigLayout("", "[][grow][][grow]", "[][][][grow][]"));

        add(new JLabel("Server: "), "cell 0 0");
        serverCombo = new JComboBox<>(ServerType.values());
        serverCombo.addActionListener(e -> setCorrectClientModel());
        add(serverCombo, "cell 1 0, growx");

        add(new JLabel("Direction: "), "cell 2 0");
        directionCombo = new JComboBox<>(Direction.values());
        add(directionCombo, "cell 3 0, growx");

        add(new JLabel("Client: "), "cell 0 1");
        Arrays.stream(ServerType.values()).forEach(server -> clientModels.put(server, new DefaultComboBoxModel<>()));
        clientCombo = new JComboBox<>();
        setCorrectClientModel();
        add(clientCombo, "cell 1 1, span 3 1, growx");

        add(new JLabel("Packet ID: "), "cell 0 2");
        packetIdSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) packetIdSpinner.getEditor();
        editor.getTextField().setFormatterFactory(new HexFormatterFactory());
        add(packetIdSpinner, "cell 1 2, span 3 1, growx");

        packetDataArea = new CodeArea();
        packetDataArea.setEditMode(EditMode.EXPANDING);
        packetDataArea.setContentData(new ByteArrayEditableData());

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem clearItem = new JMenuItem("Clear");
        clearItem.addActionListener(e -> packetDataArea.setContentData(new ByteArrayEditableData()));
        popupMenu.add(clearItem);
        JMenuItem pasteItem = new JMenuItem("Paste");
        pasteItem.addActionListener(e -> packetDataArea.paste());
        popupMenu.add(pasteItem);
        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.addActionListener(e -> packetDataArea.copyAsCode());
        popupMenu.add(copyItem);
        JMenuItem copyAllItem = new JMenuItem("Copy All");
        copyAllItem.addActionListener(e -> {packetDataArea.selectAll(); packetDataArea.copyAsCode();});
        popupMenu.add(copyAllItem);

        packetDataArea.setComponentPopupMenu(popupMenu);
        JScrollPane scrollPane = new JScrollPane(packetDataArea);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        add(scrollPane, "cell 0 3, span 4, grow");

        JButton sendBtn = new JButton("Send");
        sendBtn.addActionListener(e -> {
            ProxyClient client = (ProxyClient) clientCombo.getSelectedItem();
            List<ProxyClient> targets = new ArrayList<>();
            if (client != null) {
                targets.add(client);
            } else {
                ListModel<ProxyClient> model = clientCombo.getModel();
                for (int i = 0; i < model.getSize(); i++) {
                    targets.add(model.getElementAt(i));
                }
            }
            targets.forEach(target -> {
                Direction direction = (Direction) directionCombo.getSelectedItem();
                int packetId = (int) packetIdSpinner.getValue();
                byte[] data;
                try {
                    data = packetDataArea.getContentData().getDataInputStream().readAllBytes();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                new PacketInstance(direction, client, packetId, data).send();
            });
        });
        add(sendBtn, "cell 0 4, span 4, grow");

        pack();
    }

    private void setCorrectClientModel() {
        ServerType server = (ServerType) serverCombo.getSelectedItem();
        clientCombo.setModel(clientModels.get(server));
    }
}
