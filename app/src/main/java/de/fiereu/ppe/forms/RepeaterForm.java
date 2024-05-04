/*
 * Created by JFormDesigner on Fri May 10 11:09:39 CEST 2024
 */

package de.fiereu.ppe.forms;

import de.fiereu.ppe.ErrorHandler;
import de.fiereu.ppe.PacketHistory;
import de.fiereu.ppe.proxy.Direction;
import de.fiereu.ppe.proxy.ProxyClient;
import de.fiereu.ppe.proxy.ServerType;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import net.miginfocom.swing.MigLayout;
import org.exbin.auxiliary.binary_data.ByteArrayEditableData;
import org.exbin.bined.swing.basic.CodeArea;

public class RepeaterForm extends JFrame {

  private final ServerControlPane serverController;
  private final JSpinner packetIdSpinner;
  private final JComboBox<Direction> packetDirectionCombo;
  private final JComboBox<ServerType> serverCombo;
  private final JComboBox<ProxyClient> targetCombo;
  private final CodeArea packetDataArea;
  private JCheckBox doSpam;
  private JSpinner spamSP;
  private JCheckBox useDelay;
  private JSpinner delaySP;
  private JButton sendBtn;

  public RepeaterForm(ServerType type, PacketHistory.PacketEntry pe,
      ServerControlPane serverController) {
    this.serverController = serverController;
    packetIdSpinner = new JSpinner(new SpinnerNumberModel(pe.id(), 0, 255, 1));
    packetDirectionCombo = new JComboBox<>(Direction.values());
    packetDirectionCombo.setSelectedItem(pe.direction());
    serverCombo = new JComboBox<>(ServerType.values());
    serverCombo.setSelectedItem(type);
    targetCombo = new JComboBox<>();
    packetDataArea = new CodeArea();
    packetDataArea.setContentData(new ByteArrayEditableData(pe.data()));
    updateClients();
    initComponents();
  }

  public RepeaterForm(ServerType type, ServerControlPane serverController) {
    this.serverController = serverController;
    packetIdSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
    packetDirectionCombo = new JComboBox<>(Direction.values());
    serverCombo = new JComboBox<>(ServerType.values());
    serverCombo.setSelectedItem(type);
    targetCombo = new JComboBox<>();
    packetDataArea = new CodeArea();
    updateClients();
    initComponents();
  }

  private void updateClients() {
    var type = (ServerType) serverCombo.getSelectedItem();
    var clients = serverController.getClients(type);
    if (clients.isEmpty()) {
      ErrorHandler.handle(this, "No client running on %s server.".formatted(type));
      targetCombo.setModel(new DefaultComboBoxModel<>());
      return;
    }
    clients.add(null);
    targetCombo.setModel(new DefaultComboBoxModel<>(clients.toArray(new ProxyClient[0])));
  }

  private void sendBtn(ActionEvent e) {
    Set<ProxyClient> clients = targetCombo.getSelectedItem() != null ?
        Set.of((ProxyClient) targetCombo.getSelectedItem())
        :
            serverController.getClients((ServerType) serverCombo.getSelectedItem());

    int packetID = (int) packetIdSpinner.getValue();
    Direction packetDirection = (Direction) packetDirectionCombo.getSelectedItem();
    if (packetDirection == null) {
      ErrorHandler.handle(this, "No packet direction selected.");
      return;
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      packetDataArea.getContentData().saveToStream(baos);
    } catch (IOException ex) {
      ErrorHandler.handle(this, "Could not save packet data.", ex);
      return;
    }
    byte[] packetData = baos.toByteArray();
    if (doSpam.isSelected()) {
      int spam = (int) spamSP.getValue();
      for (int i = 0; i < spam; i++) {
        clients.forEach(proxyClient -> {
          proxyClient.sendPacket(
              packetID,
              packetDirection,
              packetData
          );
        });
        if (useDelay.isSelected()) {
          try {
            Thread.sleep((long) delaySP.getValue());
          } catch (InterruptedException ex) {
            ErrorHandler.handle(this, "Thread sleep interrupted.", ex);
          }
        }
      }
    } else {
      clients.forEach(proxyClient -> {
        proxyClient.sendPacket(
            packetID,
            packetDirection,
            packetData
        );
      });
    }
  }

  private void serverComboChanged(ActionEvent e) {
    updateClients();
  }

  private void initComponents() {
    JLabel label1 = new JLabel();
    JLabel label2 = new JLabel();
    JLabel label4 = new JLabel();
    JLabel label3 = new JLabel();
    doSpam = new JCheckBox();
    spamSP = new JSpinner();
    useDelay = new JCheckBox();
    delaySP = new JSpinner();
    sendBtn = new JButton();

    var contentPane = getContentPane();
    contentPane.setLayout(new MigLayout(
        "fill,hidemode 3",
        // columns
        "[fill]" +
            "[fill]",
        // rows
        "[]" +
            "[]" +
            "[]" +
            "[]" +
            "[grow]" +
            "[]" +
            "[]" +
            "[]"));

    label1.setText("Packet ID:");
    contentPane.add(label1, "cell 0 0");
    contentPane.add(packetIdSpinner, "cell 1 0");

    label2.setText("Packet Direction:");
    contentPane.add(label2, "cell 0 1");
    contentPane.add(packetDirectionCombo, "cell 1 1");

    label4.setText("Server:");
    contentPane.add(label4, "cell 0 2");

    serverCombo.addActionListener(this::serverComboChanged);
    contentPane.add(serverCombo, "cell 1 2");

    label3.setText("Client:");
    contentPane.add(label3, "cell 0 3");
    contentPane.add(targetCombo, "cell 1 3");
    contentPane.add(packetDataArea, "cell 0 4 2 1,grow");

    doSpam.setText("Spam");
    contentPane.add(doSpam, "cell 0 5");
    contentPane.add(spamSP, "cell 1 5");

    useDelay.setText("Delay");
    contentPane.add(useDelay, "cell 0 6");

    delaySP.setModel(new SpinnerNumberModel(0, null, null, 100));
    contentPane.add(delaySP, "cell 1 6");

    sendBtn.setText("Send");
    sendBtn.addActionListener(this::sendBtn);
    contentPane.add(sendBtn, "cell 0 7 2 1");
    setSize(760, 400);
    setLocationRelativeTo(getOwner());
  }
}
