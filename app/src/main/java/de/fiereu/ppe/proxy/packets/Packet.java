package de.fiereu.ppe.proxy.packets;

import de.fiereu.ppe.proxy.Direction;
import ch.qos.logback.core.encoder.ByteArrayUtil;

public class Packet {

  public final Direction direction;
  public final byte id;
  public byte[] data;
  public boolean wasCompressed = false;

  public Packet(Direction direction, byte id, byte[] data, boolean wasCompressed) {
    this.direction = direction;
    this.id = id;
    this.data = data;
    this.wasCompressed = wasCompressed;
  }

  @Override
  public String toString() {
    return "Packet{" +
        "id=" + Integer.toHexString(id & 0xFF) +
        ", direction=" + direction +
        ", wasCompressed=" + wasCompressed +
        ", data=" + ByteArrayUtil.toHexString(data) +
        '}';
  }
}
