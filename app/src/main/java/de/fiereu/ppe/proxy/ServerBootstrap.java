package de.fiereu.ppe.proxy;

import de.fiereu.ppe.proxy.packets.Packet;
import de.fiereu.ppe.proxy.util.ByteBufferHelper;
import com.github.maltalex.ineter.base.IPAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ServerBootstrap {

  public static void interceptLoginPacket(ProxyServer server, Packet packet) {
      if (server.type != ServerType.LOGIN) {
          return;
      }
      if (packet.direction != Direction.SERVER_TO_CLIENT) {
          return;
      }
      if (packet.id == 0x03) {
          interceptPacket03(server, packet);
      }
      if (packet.id == 0x26) {
          interceptPacket26(server, packet); // reconnect packet
      }
    return;
  }

  private static void patchConnectionData(ByteBuffer buffer, ProxyServer server, ServerType type) {
    buffer.get();
    // we know that its always a ipv4 and ipv6 address
    buffer.mark();
    IPAddress ipv4 = ByteBufferHelper.readIP(buffer);
    IPAddress ipv6 = ByteBufferHelper.readIP(buffer);
    int port = buffer.getShort();
    buffer.get();
    int proxyPort = server.serverControlPane.getProxyPort(type);
    if (!server.serverControlPane.isServerRunning(type)) {
      server.serverControlPane.startServer(type, server.serverControlPane.getProxyIP(), proxyPort,
          ipv4, port);
      server.serverControlPane.startServer(type, server.serverControlPane.getProxyIP6(), proxyPort,
          ipv6, port);
    }
    buffer.reset();
    buffer.put((byte) 0x04);
    buffer.put(server.serverControlPane.getProxyIP().toLittleEndianArray());
    buffer.put((byte) 0x06);
    buffer.put(server.serverControlPane.getProxyIP6().toLittleEndianArray());
    buffer.putShort((short) (proxyPort & 0xFFFF));
    buffer.get();
  }

  private static void interceptPacket03(ProxyServer server, Packet packet) {
    ByteBuffer buffer = ByteBuffer.wrap(packet.data).order(ByteOrder.LITTLE_ENDIAN);
    byte loginResult = buffer.get();
      if (loginResult != 0) {
          return;
      }
    buffer.getInt();
    byte[] sessionKey = new byte[buffer.get()];
    buffer.get(sessionKey);
    buffer.get();
    byte[] publicIp = new byte[buffer.get()];
    buffer.get(publicIp);
    ByteBufferHelper.readString(buffer);
    buffer.getInt();
    byte connectionDataLength = buffer.get();
    for (int i = 0; i < connectionDataLength; i++) {
      patchConnectionData(buffer, server, ServerType.GAME);
    }
  }

  private static void interceptPacket26(ProxyServer server, Packet packet) {
    ByteBuffer buffer = ByteBuffer.wrap(packet.data).order(ByteOrder.LITTLE_ENDIAN);
    buffer.getInt();
    buffer.get(new byte[buffer.get()]);
    buffer.get();
    ByteBufferHelper.readString(buffer);
    buffer.get(new byte[buffer.get()]);
    ByteBufferHelper.readString(buffer);
    buffer.getInt();
    buffer.getShort();
    buffer.getShort();
    buffer.get();
    byte connectionDataLength = buffer.get();
    for (int i = 0; i < connectionDataLength; i++) {
      patchConnectionData(buffer, server, ServerType.GAME);
    }
  }

  public static void interceptGamePacket(ProxyServer server, Packet packet) {
      if (server.type != ServerType.GAME) {
          return;
      }
      if (packet.direction != Direction.SERVER_TO_CLIENT) {
          return;
      }
      if ((packet.id & 0xFF) != 0xFC) {
          return;
      }
    ByteBuffer buffer = ByteBuffer.wrap(packet.data).order(ByteOrder.LITTLE_ENDIAN);
    buffer.get(new byte[buffer.get()]);
    byte connectionDataLength = buffer.get();
    for (int i = 0; i < connectionDataLength; i++) {
      patchConnectionData(buffer, server, ServerType.CHAT);
    }
  }
}
