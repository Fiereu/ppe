package de.fiereu.ppe.proxy;

import de.fiereu.ppe.proxy.packets.Packet;
import de.fiereu.ppe.proxy.packets.PacketCompression;
import de.fiereu.ppe.proxy.packets.PacketCypher;
import de.fiereu.ppe.proxy.packets.PacketDissector;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyClient implements Runnable {

  public final ProxyServer proxyServer;
  private final Logger log = LoggerFactory.getLogger(ProxyClient.class);

  private final PacketCypher targetCypher = new PacketCypher(this, ProxyHandler.TARGET);
  private final PacketCypher clientCypher = new PacketCypher(this, ProxyHandler.CLIENT);
  private final PacketCompression targetCompression = new PacketCompression();
  private final PacketCompression clientCompression = new PacketCompression();
  private final DecryptionBootstrap targetCypherBootstrap = new DecryptionBootstrap(targetCypher);
  private final DecryptionBootstrap clientCypherBootstrap = new DecryptionBootstrap(clientCypher);
  private final ProxySocket clientSocket;
  private final ProxySocket targetSocket;
  private final Selector selector;
  private final Deque<Packet> clientInboundPackets = new ArrayDeque<>();
  private final Deque<Packet> clientOutboundPackets = new ArrayDeque<>();
  public ProxyClient(ProxyServer proxyServer, SocketChannel clientSocket,
      SocketChannel targetSocket) throws IOException {
    this.proxyServer = proxyServer;
    this.selector = Selector.open();
    this.clientSocket = new ProxySocket(clientSocket, selector, this::onClientDataRead,
        this::onClientDataWrite);
    this.targetSocket = new ProxySocket(targetSocket, selector, this::onTargetDataRead,
        this::onTargetDataWrite);
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        this.selector.select();
        for (var key : this.selector.selectedKeys()) {
          ProxySocket proxySocket = (ProxySocket) key.attachment();
          if (key.isReadable()) {
            proxySocket.read();
          }
          if (key.isWritable()) {
            proxySocket.write();
          }
        }
      } catch (CancelledKeyException e) {
        break;
      } catch (IOException e) {
        log.error("An error occurred while processing data.", e);
        break;
      }
    }
    log.info("Stopping {}.", this);
    disconnect();
  }

  private void onTargetDataWrite(ByteBuffer byteBuffer) {
      if (clientOutboundPackets.isEmpty()) {
          return;
      }
    while (byteBuffer.hasRemaining() && !clientOutboundPackets.isEmpty()) {
      do {
        var packet = clientOutboundPackets.peek();
        if (packet == null) {
          clientOutboundPackets.poll();
          break;
        }
        targetCypherBootstrap.intercept(packet);
        log.trace("Sending packet to target: {}", packet);
        try {
          proxyServer.serverControlPane.packetHistory.addPacket(proxyServer.type,
              targetSocket.socket.getRemoteAddress(), clientSocket.socket.getRemoteAddress(),
              packet);
        } catch (SQLException | IOException e) {
          log.error("An error occurred while saving packet to database.", e);
        }
        if (!PacketDissector.assemble(byteBuffer, packet, targetCypher, null)) {
          break;
        }
        clientOutboundPackets.poll();
      } while (byteBuffer.hasRemaining());
    }
    clientCypherBootstrap.trySecureConnection();
    targetCypherBootstrap.trySecureConnection();
  }

  private void onTargetDataRead(ByteBuffer byteBuffer) {
    List<Packet> packets = PacketDissector.dissect(byteBuffer, Direction.SERVER_TO_CLIENT,
        targetCypher, canCompress() ? targetCompression : null);
    for (var packet : packets) {
      targetCypherBootstrap.intercept(packet);
      log.trace("Received packet from target: {}", packet);
    }
    clientInboundPackets.addAll(packets);
  }

  private void onClientDataWrite(ByteBuffer byteBuffer) {
      if (clientInboundPackets.isEmpty()) {
          return;
      }
    while (byteBuffer.hasRemaining() && !clientInboundPackets.isEmpty()) {
      do {
        var packet = clientInboundPackets.peek();
        if (packet == null) {
          clientInboundPackets.poll();
          break;
        }
        clientCypherBootstrap.intercept(packet);
        ServerBootstrap.interceptLoginPacket(proxyServer,
            packet); // intercepts game server data and replaces it with proxy server data
        ServerBootstrap.interceptGamePacket(proxyServer,
            packet); // intercepts chat server data and replaces it with proxy server data
        log.trace("Sending packet to client: {}", packet);
        try {
          proxyServer.serverControlPane.packetHistory.addPacket(proxyServer.type,
              targetSocket.socket.getRemoteAddress(), clientSocket.socket.getRemoteAddress(),
              packet);
        } catch (SQLException | IOException e) {
          log.error("An error occurred while saving packet to database.", e);
        }
        if (!PacketDissector.assemble(byteBuffer, packet, clientCypher,
            canCompress() ? clientCompression : null)) {
          break;
        }
        clientInboundPackets.poll();
      } while (byteBuffer.hasRemaining());
    }
  }

  private void onClientDataRead(ByteBuffer byteBuffer) {
    // Packets from client are not compressed
    List<Packet> packets = PacketDissector.dissect(byteBuffer, Direction.CLIENT_TO_SERVER,
        clientCypher, null);
    for (var packet : packets) {
      clientCypherBootstrap.intercept(packet);
      log.trace("Received packet from client: {}", packet);
    }
    clientOutboundPackets.addAll(packets);
  }

  private void disconnect() {
    log.info("Disconnecting {}.", this);
    try {
      clientSocket.disconnect();
      targetSocket.disconnect();
    } catch (IOException e) {
      log.error("An error occurred while disconnecting the client.", e);
    }
    targetCompression.end();
    clientCompression.end();
  }

  @Override
  public String toString() {
    return "Client{" +
        proxyServer.type + ", " +
        clientSocket.getRemoteAddress() + "}";
  }

  private boolean canCompress() {
    return proxyServer.type != ServerType.LOGIN &&
        clientCypher.state == PacketCypher.DecryptionState.SECURED &&
        targetCypher.state == PacketCypher.DecryptionState.SECURED;
  }

  public void sendPacket(int packetID, Direction packetDirection, byte[] packetData) {
    Packet packet = new Packet(packetDirection, (byte) packetID, packetData, false);
    switch (packetDirection) {
      case SERVER_TO_CLIENT -> {
        clientInboundPackets.add(packet);
      }
      case CLIENT_TO_SERVER -> {
        clientOutboundPackets.add(packet);
      }
    }
  }

  public boolean isClosed() {
    return clientSocket.isClosed() || targetSocket.isClosed();
  }

  public enum ProxyHandler {
    CLIENT,
    TARGET,
  }
}
