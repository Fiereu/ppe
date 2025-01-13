package de.fiereu.ppe.forms;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.exbin.auxiliary.binary_data.ByteArrayEditableData;
import org.exbin.bined.swing.basic.CodeArea;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.fiereu.ppe.ErrorHandler;
import de.fiereu.ppe.proxy.Direction;
import de.fiereu.ppe.proxy.ProxyClient;
import de.fiereu.ppe.proxy.ServerType;
import net.miginfocom.swing.MigLayout;

public class PacketChainerForm extends JFrame {

    private final JTable packetTable;
    private final DefaultTableModel tableModel;

    private final JButton addPacketButton;
    private final JButton removePacketButton;

    private final JButton saveButton;
    private final JButton loadButton;

    private final JButton executeButton;

    private final ServerControlPane serverController;

    public PacketChainerForm(ServerControlPane serverController) {
        this.serverController = serverController;

        setTitle("Packet Chainer");
        setLayout(new MigLayout("fill, wrap 2", "[grow, fill] [grow, fill]", "[grow, fill] [] []"));

        tableModel = new DefaultTableModel(new Object[]{"Label", "ID", "Delay (ms)", "Data"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) // ID column
                {
                    return Integer.class;
                }
                if (columnIndex == 2) // Delay column
                {
                    return Integer.class;
                }
                if (columnIndex == 3) // Data column
                {
                    return byte[].class;
                }
                return String.class; // Label column
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 3; // Make sure Data Col is not editable
            }

            public void moveRow(int from, int to) {
                if (from < 0 || from >= getRowCount() || to < 0 || to >= getRowCount()) {
                    return;
                }

                Object[] movedRow = new Object[getColumnCount()];
                for (int i = 0; i < getColumnCount(); i++) {
                    movedRow[i] = getValueAt(from, i);
                }

                removeRow(from);
                insertRow(to, movedRow);
            }
        };

        packetTable = new JTable(tableModel);
        packetTable.getColumnModel().getColumn(2).setCellRenderer(new HexDataRenderer());

        JScrollPane tableScrollPane = new JScrollPane(packetTable);
        add(tableScrollPane, "span 2, grow");

        // Move rows w/ DnD
        enableDragAndDrop();

        // Buttons
        addPacketButton = new JButton("Add Packet");
        addPacketButton.addActionListener(this::addPacket);
        add(addPacketButton, "split 2");

        removePacketButton = new JButton("Remove Selected Packet");
        removePacketButton.addActionListener(this::removeSelectedPacket);
        add(removePacketButton);

        saveButton = new JButton("Save to JSON");
        saveButton.addActionListener(this::saveToJson);
        add(saveButton, "split 2");

        loadButton = new JButton("Load from JSON");
        loadButton.addActionListener(this::loadFromJson);
        add(loadButton);

        executeButton = new JButton("Execute Chain");
        executeButton.addActionListener(this::executeChain);
        add(executeButton, "span 2, growx");

        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    private void enableDragAndDrop() {
        packetTable.setDragEnabled(true);
        packetTable.setDropMode(DropMode.INSERT_ROWS);

        packetTable.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation();
                int dropRow = dropLocation.getRow();

                try {
                    String draggedRowString = (String) support.getTransferable()
                            .getTransferData(DataFlavor.stringFlavor);
                    int draggedRow = Integer.parseInt(draggedRowString);

                    tableModel.moveRow(draggedRow, draggedRow, dropRow);
                    return true;
                } catch (UnsupportedFlavorException | IOException | NumberFormatException e) {
                    ErrorHandler.handle(PacketChainerForm.this, "Failed to reorder rows.", e);
                }

                return false;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                int selectedRow = packetTable.getSelectedRow();
                return new StringSelection(String.valueOf(selectedRow));
            }

            @Override
            public int getSourceActions(JComponent c) {
                return MOVE;
            }
        });
    }

    private void addPacket(ActionEvent e) {
        PacketEditorForm editor = new PacketEditorForm(packet -> {
            byte[] packetData;
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                packet.data.saveToStream(outputStream);
                packetData = outputStream.toByteArray(); // Extract data as byte array
            } catch (IOException ex) {
                ErrorHandler.handle(this, "Failed to retrieve packet data.", ex);
                return;
            }

            tableModel.addRow(new Object[]{
                packet.label,
                packet.id,
                packet.delay,
                packetData
            });
        });
        editor.setVisible(true);
    }

    public void addPacketToTable(String label, int id, int delay, byte[] data) {
        tableModel.addRow(new Object[]{label, id, delay, data});
    }

    private void removeSelectedPacket(ActionEvent e) {
        int selectedRow = packetTable.getSelectedRow();
        if (selectedRow != -1) {
            tableModel.removeRow(selectedRow);
        } else {
            JOptionPane.showMessageDialog(this, "No packet selected to remove.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveToJson(ActionEvent e) {
        List<PacketData> packets = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String label = (String) tableModel.getValueAt(i, 0);
            int packetID = (Integer) tableModel.getValueAt(i, 1);
            int delay = (Integer) tableModel.getValueAt(i, 2);
            byte[] data = (byte[]) tableModel.getValueAt(i, 3);
            packets.add(new PacketData(label, packetID, delay, new ByteArrayEditableData(data)));
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter("packets.json")) {
            gson.toJson(packets, writer);
            JOptionPane.showMessageDialog(this, "Packets saved to packets.json.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            ErrorHandler.handle(this, "Failed to save packets to JSON.", ex);
        }
    }

    private void loadFromJson(ActionEvent e) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader("packets.json")) {
            PacketData[] packets = gson.fromJson(reader, PacketData[].class);
            tableModel.setRowCount(0); // Clear table
            for (PacketData packet : packets) {
                byte[] data;
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    packet.data.saveToStream(outputStream);
                    data = outputStream.toByteArray();
                }
                tableModel.addRow(new Object[]{packet.label, packet.delay, data});
            }
            JOptionPane.showMessageDialog(this, "Packets loaded from packets.json.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            ErrorHandler.handle(this, "Failed to load packets from JSON.", ex);
        }
    }

    private void executeChain(ActionEvent e) {
        new Thread(() -> {
            try {
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    String label = (String) tableModel.getValueAt(i, 0);
                    int packetID = (Integer) tableModel.getValueAt(i, 1);
                    int delay = (Integer) tableModel.getValueAt(i, 2);
                    byte[] data = (byte[]) tableModel.getValueAt(i, 3);

                    sendPacket(label, packetID, data);

                    if (delay > 0) {
                        Thread.sleep(delay);
                    }
                }

                // Notify when chain has executed
                SwingUtilities
                        .invokeLater(() -> JOptionPane.showMessageDialog(this, "Packet chain executed successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE));
            } catch (Exception ex) {
                ErrorHandler.handle(this, "Error executing packet chain.", ex);
            }
        }).start();
    }

    private void sendPacket(String label, int packetID, byte[] data) {
        // TODO: Allow chaining LOGIN & CHAT commands
        ServerType serverType = ServerType.GAME;
        Set<ProxyClient> clients = serverController.getClients(serverType);

        if (clients.isEmpty()) {
            ErrorHandler.handle(this, "No clients connected to the server: " + serverType);
            return;
        }

        Direction packetDirection = Direction.CLIENT_TO_SERVER;
        clients.forEach(proxyClient -> {
            try {
                proxyClient.sendPacket(packetID, packetDirection, data);
                System.out.println("Sending packet: " + label + " with data: " + HexFormat.of().formatHex(data));
            } catch (Exception ex) {
                ErrorHandler.handle(this, "Failed to send packet to client: " + proxyClient, ex);
            }
        });
    }

    private static class PacketEditorForm extends JDialog {

        private final JTextField labelField;
        private final JSpinner idSpinner;
        private final JSpinner delaySpinner;
        private final CodeArea dataEditor;
        private final JButton saveButton;

        public PacketEditorForm(PacketCallback callback) {
            setTitle("Edit Packet");
            setLayout(new MigLayout("fill, wrap 2", "[][grow, fill]", "[][][grow, fill][]"));

            add(new JLabel("Label:"));
            labelField = new JTextField();
            add(labelField);

            add(new JLabel("Packet ID:"));
            idSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
            add(idSpinner);

            add(new JLabel("Delay (ms):"));
            delaySpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 100));
            add(delaySpinner);

            add(new JLabel("Data:"));
            dataEditor = new CodeArea();
            dataEditor.setContentData(new ByteArrayEditableData());
            add(new JScrollPane(dataEditor), "span 2, grow");

            saveButton = new JButton("Save");
            saveButton.addActionListener(e -> {
                String label = labelField.getText();
                int id = (int) idSpinner.getValue();
                int delay = (int) delaySpinner.getValue();

                ByteArrayEditableData data = new ByteArrayEditableData();
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    dataEditor.getContentData().saveToStream(outputStream);
                    data.insert(0, outputStream.toByteArray(), 0, outputStream.size());
                } catch (IOException ex) {
                    ErrorHandler.handle(this, "Failed to save packet data.", ex);
                    return;
                }

                callback.onSave(new PacketData(label, id, delay, data));
                dispose();
            });
            add(saveButton, "span 2, align center");

            setSize(400, 300);
            setLocationRelativeTo(null);
        }
    }

    @FunctionalInterface
    private interface PacketCallback {

        void onSave(PacketData packet);
    }

    private static class PacketData {

        final String label;
        final int id;
        final int delay;
        final ByteArrayEditableData data;

        public PacketData(String label, int id, int delay, ByteArrayEditableData data) {
            this.label = label;
            this.id = id;
            this.delay = delay;
            this.data = data;
        }
    }

    private static class HexDataRenderer extends DefaultTableCellRenderer {

        @Override
        protected void setValue(Object value) {
            if (value instanceof byte[] data) {
                setText(HexFormat.of().formatHex(data));
            } else {
                super.setValue(value);
            }
        }
    }
}
