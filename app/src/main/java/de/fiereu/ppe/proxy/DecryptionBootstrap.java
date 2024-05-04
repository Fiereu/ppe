package de.fiereu.ppe.proxy;

import de.fiereu.ppe.proxy.packets.Packet;
import de.fiereu.ppe.proxy.packets.PacketCypher;
import de.fiereu.ppe.proxy.util.EC;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecryptionBootstrap {

  private static final Logger log = LoggerFactory.getLogger(DecryptionBootstrap.class);
  private final PacketCypher packetCypher;
  private byte hashSize;

  public DecryptionBootstrap(PacketCypher packetCypher) {
    this.packetCypher = packetCypher;
  }

  public void intercept(Packet packet) {
    if (packetCypher.state == PacketCypher.DecryptionState.SECURED) {
      return;
    }
    if (packet.direction == Direction.SERVER_TO_CLIENT) {
      if (packet.id == 0x01) {
        handshake01(packet);
      }
    } else {
      if (packet.id == 0x02) {
        handshake02(packet);
      }
    }
  }

  /**
   * The server sends the client its public key, a signature of the public key and the size of the
   * hash.
   *
   * @param packet The packet.
   */
  private void handshake01(Packet packet) {
    ByteBuffer buffer = ByteBuffer.wrap(packet.data).order(ByteOrder.LITTLE_ENDIAN);
    byte[] uncompressedPubKey = new byte[buffer.getShort()];
    buffer.get(uncompressedPubKey);
    byte[] signature = new byte[buffer.getShort()];
    buffer.get(signature);
    this.hashSize = buffer.get();
    // If this decryptor is between the client and the proxy replace the targets public key with the proxy's public key.
    if (packetCypher.handler == ProxyClient.ProxyHandler.CLIENT) {
      log.trace("Replacing target's public key with proxy's public key.");
      uncompressedPubKey = EC.toUncompressedPoint(
          (ECPublicKey) packetCypher.proxyKeyPair.getPublic());
      signature = EC.sign(uncompressedPubKey, KeyService.getProxyKeyPair().getPrivate());
      packet.data = ByteBuffer.allocate(uncompressedPubKey.length + 2 + signature.length + 2 + 1)
          .order(ByteOrder.LITTLE_ENDIAN)
          .putShort((short) uncompressedPubKey.length).put(uncompressedPubKey)
          .putShort((short) signature.length).put(signature)
          .put(hashSize).array();
    } else {
      packetCypher.initialize(uncompressedPubKey, hashSize);
      PublicKey serverCert = KeyService.getPublicKey(packetCypher.proxyClient.proxyServer.type);

      if (!EC.verify(signature, uncompressedPubKey, serverCert)) {
        log.error("Failed to verify server signature");
        log.debug("This only happens when the official server certificates got changed.");
        throw new RuntimeException("Failed to verify server signature");
      }
    }
  }

  /**
   * The client send the server its public key. So every packet from now on will be encrypted.
   *
   * @param packet The packet.
   */
  private void handshake02(Packet packet) {
    // If this decryptor is between the target and the proxy replace the client's public key with the proxy's public key.
    if (packetCypher.handler == ProxyClient.ProxyHandler.TARGET) {
      log.trace("Replacing client's public key with proxy's public key.");
      byte[] uncompressedPubKey = EC.toUncompressedPoint(
          (ECPublicKey) packetCypher.proxyKeyPair.getPublic());
      packet.data = ByteBuffer.allocate(2 + uncompressedPubKey.length)
          .order(ByteOrder.LITTLE_ENDIAN)
          .putShort((short) uncompressedPubKey.length).put(uncompressedPubKey).array();
    } else {
      ByteBuffer buffer = ByteBuffer.wrap(packet.data).order(ByteOrder.LITTLE_ENDIAN);
      short size = buffer.getShort();
      byte[] uncompressedPubKey = new byte[size];
      buffer.get(uncompressedPubKey);
      packetCypher.initialize(uncompressedPubKey, hashSize);
    }
  }

  public void trySecureConnection() {
    if (
        packetCypher.state != PacketCypher.DecryptionState.SECURED
            && packetCypher.encryptionCipher != null
            && packetCypher.decryptionCipher != null
    ) {
      log.trace("Switched to SECURED connection state");
      packetCypher.state = PacketCypher.DecryptionState.SECURED;
    }
  }
}
