/*
 * Created by JFormDesigner on Tue May 07 16:42:58 CEST 2024
 */

package de.fiereu.ppe.forms;

import java.awt.event.ActionEvent;
import java.io.Closeable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.github.maltalex.ineter.base.IPAddress;

import de.fiereu.ppe.ErrorHandler;
import de.fiereu.ppe.PacketHistory;
import de.fiereu.ppe.proxy.ProxyClient;
import de.fiereu.ppe.proxy.ProxyServer;
import de.fiereu.ppe.proxy.ServerType;
import net.miginfocom.swing.MigLayout;

public class ServerControlPane extends JPanel implements Closeable {

  public final PacketHistory packetHistory;
  private final Set<ProxyServer> servers = new HashSet<>();
  private JTextField proxyIPTF;
  private JTextField proxyIP6TF;
  private JTextField lsIPTF;
  private JTextField lsPortTF;
  private JTextField gsPortTF;
  private JTextField csPortTF;
  private JButton startServerBtn;
  private JButton stopServerBtn;
  private JTextArea serverLog;

  public ServerControlPane(PacketHistory packetHistory) {
    this.packetHistory = packetHistory;
    initComponents();
  }

  public Set<ProxyClient> getClients(ServerType type) {
    return servers.stream().filter(server -> server.type == type)
        .flatMap(server -> server.getClients().stream()).collect(Collectors.toSet());
  }

  @Override
  public void close() {
    try {
      stopAllServers();
    } catch (InterruptedException ex) {
      ErrorHandler.handle(this, "Could not stop all server.", ex);
    }
  }

  private void resetServerBtns() {
    stopServerBtn.setEnabled(false);
    startServerBtn.setEnabled(true);
  }

  public void log(ServerType type, String msg, Object... args) {
    serverLog.append(String.format("[%s] %s\n", type, String.format(msg, args)));
  }

  public void startServer(ServerType type, IPAddress proxyIP, int proxyPort, IPAddress lsIP,
      int lsPort) {
    ProxyServer server;
    try {
      server = new ProxyServer(
          this,
          type,
          proxyIP,
          proxyPort,
          lsIP,
          lsPort,
          type == ServerType.LOGIN ? this::resetServerBtns : () -> {
          }
      );
    } catch (Exception ex) {
      ErrorHandler.handle(this, "Could not start server.", ex);
      return;
    }
    servers.add(server);
    server.start();
  }

  public void stopAllServers() throws InterruptedException {
    for (var server : servers) {
      server.stop();
    }
    servers.clear();
  }

  public boolean isServerRunning(ServerType type) {
    return servers.stream().anyMatch(s -> s.type == type);
  }

  public IPAddress getProxyIP() {
    return IPAddress.of(proxyIPTF.getText());
  }

  public IPAddress getProxyIP6() {
    return IPAddress.of(proxyIP6TF.getText());
  }

  public int getProxyPort(ServerType type) {
    return switch (type) {
      case LOGIN -> Integer.parseInt(lsPortTF.getText());
      case GAME -> Integer.parseInt(gsPortTF.getText());
      case CHAT -> Integer.parseInt(csPortTF.getText());
    };
  }

  private void startServerBtn(ActionEvent e) {
    try {
      var proxyIP = getProxyIP();
      var proxyIP6 = getProxyIP6();
      var lsIP = IPAddress.of(InetAddress.getByName(lsIPTF.getText()));
      var lsPort = getProxyPort(ServerType.LOGIN);
      startServer(
          ServerType.LOGIN,
          proxyIP,
          lsPort,
          lsIP,
          lsPort
      );
      startServer(
          ServerType.LOGIN,
          proxyIP6,
          lsPort,
          lsIP,
          lsPort
      );
    } catch (IllegalArgumentException | UnknownHostException ex) {
      ErrorHandler.handle(this, "Invalid IP/IP6 address.", ex);
      return;
    }
    stopServerBtn.setEnabled(true);
    startServerBtn.setEnabled(false);
  }

  private void stopServerBtn(ActionEvent e) {
    try {
      stopAllServers();
    } catch (InterruptedException ex) {
      ErrorHandler.handle(this, "Could not stop server.", ex);
      return;
    }
    resetServerBtns();
  }

  private void initComponents() {
    JLabel label1 = new JLabel();
    proxyIPTF = new JTextField();
    JLabel label3 = new JLabel();
    proxyIP6TF = new JTextField();
    JLabel label2 = new JLabel();
    lsIPTF = new JTextField();
    JLabel lsPortLbl = new JLabel();
    lsPortTF = new JTextField();
    JLabel gsPortLbl = new JLabel();
    gsPortTF = new JTextField();
    JLabel csPortLbl = new JLabel();
    csPortTF = new JTextField();
    startServerBtn = new JButton();
    stopServerBtn = new JButton();
    JScrollPane scrollPane1 = new JScrollPane();
    serverLog = new JTextArea();

    setLayout(new MigLayout(
        "filly,hidemode 3",
        "[fill]" +
            "[fill]",
        "[]" +
            "[]" +
            "[]" +
            "[]" +
            "[]" +
            "[]" +
            "[]" +
            "[]" +
            "[grow]"));

    label1.setText("Proxy IP:");
    add(label1, "cell 0 0");

    proxyIPTF.setText("127.0.0.1");
    add(proxyIPTF, "cell 1 0,width 200");

    label3.setText("Proxy IPv6:");
    add(label3, "cell 0 1");

    proxyIP6TF.setText("::1");
    add(proxyIP6TF, "width 200");

    label2.setText("Login server IP:");
    add(label2, "cell 0 2");

    lsIPTF.setText("loginserver.pokemmo.eu");
    add(lsIPTF, "cell 1 2,width 200");

    lsPortLbl.setText("Login server Port:");
    add(lsPortLbl, "cell 0 3");

    lsPortTF.setText("2106");
    add(lsPortTF, "cell 1 3");

    gsPortLbl.setText("Game server Port:");
    add(gsPortLbl, "cell 0 4");

    gsPortTF.setText("7777");
    add(gsPortTF, "cell 1 4");

    csPortLbl.setText("Chat server Port:");
    add(csPortLbl, "cell 0 5");

    csPortTF.setText("7778");
    add(csPortTF, "cell 1 5");

    startServerBtn.setText("Start Server");
    startServerBtn.addActionListener(this::startServerBtn);
    add(startServerBtn, "cell 0 6 2 1,growx");

    stopServerBtn.setText("Stop Server");
    stopServerBtn.setEnabled(false);
    stopServerBtn.addActionListener(this::stopServerBtn);
    add(stopServerBtn, "cell 0 7 2 1,growx");

    serverLog.setWrapStyleWord(true);
    serverLog.setLineWrap(true);
    scrollPane1.setViewportView(serverLog);
    add(scrollPane1, "cell 0 8 2 1,grow");

  }
}
