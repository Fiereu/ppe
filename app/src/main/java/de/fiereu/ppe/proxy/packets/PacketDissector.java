package de.fiereu.ppe.proxy.packets;

import static de.fiereu.ppe.proxy.ProxySocket.BUFFER_SIZE;

import de.fiereu.ppe.proxy.Direction;
import ch.qos.logback.core.encoder.ByteArrayUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;
import javax.crypto.ShortBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketDissector {

  private static final Logger log = LoggerFactory.getLogger(PacketDissector.class);

  public static List<Packet> dissect(ByteBuffer buffer, Direction direction, PacketCypher cypher,
      PacketCompression compressor) {
    ByteBuffer packetBuffer = ByteBuffer.allocate(BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    List<Packet> packets = new ArrayList<>();
    int readLimit = buffer.limit();
    while (buffer.remaining() > 2) {
      int length = buffer.getShort() - 2;
      if (length > buffer.remaining()) {
        buffer.position(buffer.position() - 2);
        break;
      }
      buffer.limit(buffer.position() + length);
      log.trace("Dissecting packet: {}", ByteArrayUtil.toHexString(
          Arrays.copyOfRange(buffer.array(), buffer.arrayOffset() + buffer.position() - 2,
              buffer.arrayOffset() + buffer.limit())));
      packetBuffer.clear();
      packetBuffer.put(buffer);
      packetBuffer.flip();
      buffer.limit(readLimit); // Reset limit to the end of read buffer
      try {
        cypher.decrypt(packetBuffer);
      } catch (Exception e) {
        throw new RuntimeException("Failed to decrypt packet", e);
      }
      byte id = packetBuffer.get();
      boolean wasCompressed = false;
      if (compressor != null && cypher.state == PacketCypher.DecryptionState.SECURED) {
        if (packetBuffer.get() == 0x01) {
          try {
            compressor.inflate(packetBuffer);
          } catch (DataFormatException e) {
            throw new RuntimeException("Failed to decompress packet", e);
          }
          wasCompressed = true;
        }
      }
      byte[] data = new byte[packetBuffer.remaining()];
      packetBuffer.get(data);
      var packet = new Packet(direction, id, data, wasCompressed);
      packets.add(packet);
    }
    return packets;
  }

  public static boolean assemble(ByteBuffer buffer, Packet packet, PacketCypher decryptor,
      PacketCompression compressor) {
    var length = packet.data.length + 2 + 1 + decryptor.getHashSize();
    if (buffer.remaining() < length) {
      return false;
    }
    int start = buffer.position();
    buffer.putShort((short) 0);
    buffer.put(packet.id);
    if (compressor != null) {
      buffer.put((byte) (packet.wasCompressed ? 0x01 : 0x00));
    }
    int postDataPos = buffer.position();
    buffer.put(packet.data);
    buffer.flip();
    if (packet.wasCompressed) {
      assert compressor != null;
      try {
        buffer.position(postDataPos);
        compressor.deflate(buffer);
      } catch (Exception e) {
        throw new RuntimeException("Failed to compress packet", e);
      }
    }
    buffer.position(start);
    buffer.putShort((short) 0);
    try {
      decryptor.encrypt(buffer);
    } catch (ShortBufferException e) {
      buffer.reset();
      return false;
    }
    buffer.flip();
    buffer.position(start);
    buffer.putShort((short) buffer.remaining());
    buffer.position(buffer.limit());
    buffer.limit(buffer.capacity());
    log.trace("Assembled packet ({}): {}", Byte.toUnsignedInt(packet.id), ByteArrayUtil.toHexString(
        Arrays.copyOfRange(buffer.array(), buffer.arrayOffset() + start,
            buffer.arrayOffset() + buffer.position())));
    return true;
  }
}
