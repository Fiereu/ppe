package de.fiereu.ppe.forms;

import de.fiereu.ppe.ErrorHandler;
import de.fiereu.ppe.PacketHistory;
import de.fiereu.ppe.proxy.ServerType;
import de.fiereu.ppe.util.Time;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerTab extends JPanel {

  public final ServerType serverType;
  private final ServerControlPane serverController;
  private final PacketHistory packetHistory;
  private JScrollPane scrollPane1;
  private JTable packetTable;
  private JPopupMenu tablePM;
  private JMenu menu1;
  private JMenuItem menuItem1;
  private JMenuItem menuItem2;
  private JMenuItem menuItem3;
  public ServerTab(ServerType type, ServerControlPane serverController,
      PacketHistory packetHistory) {
    this.serverType = type;
    this.serverController = serverController;
    this.packetHistory = packetHistory;
    initComponents();
  }

  private void deleteSelectedBtn(ActionEvent e) {
    ((PacketTableModel) packetTable.getModel())
        .getSelectedRows()
        .forEach(pe -> {
          try {
            packetHistory.deletePacket(serverType, pe.uid());
          } catch (SQLException ex) {
            ErrorHandler.handle(this, "Could not delete row in packet history.", ex);
          }
        });
  }

  private void deleteAllBtn(ActionEvent e) {
    try {
      packetHistory.clear(serverType);
    } catch (SQLException ex) {
      ErrorHandler.handle(this, "Could not delete row in packet history.", ex);
    }
  }

  private void sendToRepeaterBtn(ActionEvent e) {
    ((PacketTableModel) packetTable.getModel())
        .getSelectedRows()
        .forEach(pe -> new RepeaterForm(serverType, pe, serverController).setVisible(true));
  }

  private void initComponents() {
    scrollPane1 = new JScrollPane();
    tablePM = new JPopupMenu();
    menu1 = new JMenu();
    menuItem3 = new JMenuItem();
    menuItem1 = new JMenuItem();
    menuItem2 = new JMenuItem();
    packetTable = new JTable();

    packetTable.setModel(new PacketTableModel());
    packetTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    setLayout(new BorderLayout());
    packetTable.setComponentPopupMenu(tablePM);
    scrollPane1.setViewportView(packetTable);
    scrollPane1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    add(scrollPane1, BorderLayout.CENTER);
    menu1.setText("Send to");

    menuItem3.setText("Repeater");
    menuItem3.addActionListener(this::sendToRepeaterBtn);
    menu1.add(menuItem3);
    tablePM.add(menu1);

    menuItem1.setText("Delete");
    menuItem1.addActionListener(this::deleteSelectedBtn);
    tablePM.add(menuItem1);
    tablePM.addSeparator();

    menuItem2.setText("Delete all");
    menuItem2.addActionListener(this::deleteAllBtn);
    tablePM.add(menuItem2);
  }

  private class PacketTableModel extends AbstractTableModel {

    private static final Logger log = LoggerFactory.getLogger(PacketTableModel.class);
    private static final String[] columnNames = new String[]{"Time", "Direction", "Id", "Data"};
    private final ArrayList<PacketHistory.PacketEntry> cache = new ArrayList<>();
    private final Timer updateTimer = new Timer();

    protected PacketTableModel() {
      super();
      updateTimer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
          update();
        }
      }, 0, 500);
    }

    public void update() {
      ArrayList<Long> selected = new ArrayList<>();
      for (int s : packetTable.getSelectedRows()) {
        selected.add(cache.get(cache.size() - s - 1).uid());
      }

      synchronized (cache) {
        cache.clear();
        try {
          cache.addAll(packetHistory.getPackets(serverType));
        } catch (SQLException e) {
          log.error("Failed to update packet table", e);
        }
      }

      fireTableDataChanged();
      for (int i = 0; i < cache.size(); i++) {
        if (selected.contains(cache.get(i).uid())) {
          packetTable.getSelectionModel()
              .addSelectionInterval(cache.size() - i - 1, cache.size() - i - 1);
        }
      }
    }

    @Override
    public String getColumnName(int column) {
      return columnNames[column];
    }

    @Override
    public int getRowCount() {
      return cache.size();
    }

    @Override
    public int getColumnCount() {
      return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      if (rowIndex < 0 || rowIndex >= cache.size()) {
        return null;
      }
      rowIndex = cache.size() - rowIndex - 1;
      synchronized (cache) {
        return switch (columnIndex) {
          case 0 -> Time.format(cache.get(rowIndex).timestamp());
          case 1 -> cache.get(rowIndex).direction();
          case 2 ->
              String.format("0x%02X (%d)", cache.get(rowIndex).id() & 0xFF, cache.get(rowIndex).id());
          case 3 -> HexFormat.of().formatHex(cache.get(rowIndex).data());
          default -> null;
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
