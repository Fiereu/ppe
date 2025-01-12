package de.fiereu.ppe.forms;

import de.fiereu.ppe.ErrorHandler;
import de.fiereu.ppe.PacketHistory;
import de.fiereu.ppe.proxy.ServerType;
import de.fiereu.ppe.util.Time;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerTab extends JPanel {

    public final ServerType serverType;

    private final ServerControlPane serverController;
    private final PacketHistory packetHistory;
    private JScrollPane scrollPane;
    private JTable packetTable;
    private JPopupMenu tablePopupMenu;
    private PacketChainerForm activeChainerForm = null;

    public ServerTab(ServerType type, ServerControlPane serverController, PacketHistory packetHistory) {
        this.serverType = type;
        this.serverController = serverController;
        this.packetHistory = packetHistory;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        scrollPane = new JScrollPane();
        packetTable = new JTable();
        tablePopupMenu = new JPopupMenu();

        packetTable.setModel(new PacketTableModel());
        packetTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        packetTable.setComponentPopupMenu(tablePopupMenu);
        scrollPane.setViewportView(packetTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JMenu menuItemSendTo = new JMenu("Send to");
        tablePopupMenu.add(menuItemSendTo);

        JMenuItem menuItemRepeater = new JMenuItem("Repeater");
        menuItemRepeater.addActionListener(this::sendToRepeaterBtn);
        menuItemSendTo.add(menuItemRepeater);

        JMenuItem menuItemSendToChainer = new JMenuItem("Send to Packet Chainer");
        menuItemSendToChainer.addActionListener(this::sendToChainerBtn);
        menuItemSendTo.add(menuItemSendToChainer);

        JMenuItem menuItemDelete = new JMenuItem("Delete");
        menuItemDelete.addActionListener(this::deleteSelectedBtn);
        tablePopupMenu.add(menuItemDelete);

        tablePopupMenu.addSeparator();

        JMenuItem menuItemDeleteAll = new JMenuItem("Delete all");
        menuItemDeleteAll.addActionListener(this::deleteAllBtn);
        tablePopupMenu.add(menuItemDeleteAll);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void deleteSelectedBtn(ActionEvent e) {
        ((PacketTableModel) packetTable.getModel())
                .getSelectedRows()
                .forEach(packet -> {
                    try {
                        packetHistory.deletePacket(serverType, packet.uid());
                    } catch (SQLException ex) {
                        ErrorHandler.handle(this, "Could not delete row in packet history.", ex);
                    }
                });
    }

    private void deleteAllBtn(ActionEvent e) {
        try {
            packetHistory.clear(serverType);
        } catch (SQLException ex) {
            ErrorHandler.handle(this, "Could not delete rows in packet history.", ex);
        }
    }

    private void sendToRepeaterBtn(ActionEvent e) {
        ((PacketTableModel) packetTable.getModel())
                .getSelectedRows()
                .forEach(packet -> new RepeaterForm(serverType, packet, serverController).setVisible(true));
    }

    private void sendToChainerBtn(ActionEvent e) {
        List<PacketHistory.PacketEntry> selectedPackets = ((PacketTableModel) packetTable.getModel()).getSelectedRows();
        if (selectedPackets.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No packets selected to send.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (activeChainerForm == null || !activeChainerForm.isVisible()) {
            // Create a new PacketChainerForm if none exists or the previous one was closed
            activeChainerForm = new PacketChainerForm(serverController);
            activeChainerForm.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            activeChainerForm.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    activeChainerForm = null; // Clear reference when the form is closed
                }
            });
            activeChainerForm.setVisible(true);
        }

        // Add the selected packets to the active form
        for (PacketHistory.PacketEntry packet : selectedPackets) {
            byte[] data = packet.data(); // Retrieve packet data
            activeChainerForm.addPacketToTable(String.valueOf(packet.uid()), 0, data); // Add to the chainer form with 0
                                                                                       // delay by default
        }

        // Bring the active form to the front if it's already open
        activeChainerForm.toFront();
    }

    private class PacketTableModel extends AbstractTableModel {

        private static final Logger log = LoggerFactory.getLogger(PacketTableModel.class);
        private static final String[] COLUMN_NAMES = { "Time", "Direction", "Id", "Data" };
        private final List<PacketHistory.PacketEntry> cache = new ArrayList<>();
        private final Timer updateTimer = new Timer();

        protected PacketTableModel() {
            updateTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    update();
                }
            }, 0, 500);
        }

        private void update() {
            List<Long> selectedUids = Arrays.stream(packetTable.getSelectedRows())
                    .mapToObj(this::getRowUid)
                    .filter(Objects::nonNull)
                    .toList();

            synchronized (cache) {
                cache.clear();
                try {
                    cache.addAll(packetHistory.getPackets(serverType));
                } catch (SQLException e) {
                    log.error("Failed to update packet table", e);
                }
            }

            fireTableDataChanged();
            restoreSelection(selectedUids);
        }

        private Long getRowUid(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= cache.size()) {
                return null;
            }
            return cache.get(cache.size() - rowIndex - 1).uid();
        }

        private void restoreSelection(List<Long> selectedUids) {
            for (int i = 0; i < cache.size(); i++) {
                if (selectedUids.contains(cache.get(i).uid())) {
                    packetTable.getSelectionModel()
                            .addSelectionInterval(cache.size() - i - 1, cache.size() - i - 1);
                }
            }
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public int getRowCount() {
            return cache.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= cache.size()) {
                return null;
            }
            rowIndex = cache.size() - rowIndex - 1;
            synchronized (cache) {
                PacketHistory.PacketEntry entry = cache.get(rowIndex);
                return switch (columnIndex) {
                    case 0 ->
                        Time.format(entry.timestamp());
                    case 1 ->
                        entry.direction();
                    case 2 ->
                        String.format("0x%02X (%d)", entry.id() & 0xFF, entry.id());
                    case 3 ->
                        HexFormat.of().formatHex(entry.data());
                    default ->
                        null;
                };
            }
        }

        public PacketHistory.PacketEntry getRow(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= cache.size()) {
                return null;
            }
            rowIndex = cache.size() - rowIndex - 1;
            synchronized (cache) {
                return cache.get(rowIndex);
            }
        }

        public List<PacketHistory.PacketEntry> getSelectedRows() {
            return Arrays.stream(packetTable.getSelectedRows())
                    .mapToObj(this::getRow)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }
}
