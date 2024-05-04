package de.fiereu.ppe.proxy.packets;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class PacketCompression {

  private final Inflater inflater = new Inflater(true);
  private final Deflater deflater = new Deflater(Deflater.BEST_SPEED, true);

  public void inflate(ByteBuffer buffer) throws DataFormatException {
    ByteBuffer deflated = ByteBuffer.allocate(buffer.capacity()).order(ByteOrder.LITTLE_ENDIAN);
    int position = buffer.position();
    deflated.put(buffer);
    buffer.position(position);
    deflated.putInt(-65536);
    inflater.setInput(deflated.array(), deflated.arrayOffset(),
        deflated.arrayOffset() + deflated.position());
    buffer.limit(buffer.position() + inflater.inflate(buffer.array(),
        buffer.arrayOffset() + buffer.position(), buffer.capacity() - buffer.position()));
  }

  public void deflate(ByteBuffer buffer) {
    ByteBuffer inflated = ByteBuffer.allocate(buffer.capacity()).order(ByteOrder.LITTLE_ENDIAN);
    int position = buffer.position();
    inflated.put(buffer);
    buffer.position(position);
    inflated.flip();
    deflater.setInput(inflated);
    buffer.limit(
        buffer.position()
            + deflater.deflate(buffer.array(), buffer.arrayOffset() + buffer.position(),
            buffer.capacity() - buffer.position(), Deflater.SYNC_FLUSH)
            - 4 // remove the sync flush marker (0x0000FFFF)
    );
  }

  public void end() {
    inflater.end();
    deflater.end();
  }
}
