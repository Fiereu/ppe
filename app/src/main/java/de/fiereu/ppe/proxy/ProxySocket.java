package de.fiereu.ppe.proxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ProxySocket {
  public static final int BUFFER_SIZE = 0xFFFF;
  public final SocketChannel socket;
  public final ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
  public final ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
  public final IConsumer<ByteBuffer> onDataRead;
  public final IConsumer<ByteBuffer> onDataWrite;
  public ProxySocket(SocketChannel socket, Selector selector, IConsumer<ByteBuffer> onDataRead,
      IConsumer<ByteBuffer> onDataWrite) throws IOException {
    this.socket = socket;
    this.onDataRead = onDataRead;
    this.onDataWrite = onDataWrite;
    socket.configureBlocking(false);
    socket.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
  }

  public void disconnect() throws IOException {
    socket.close();
  }

  public void read() throws IOException {
    var bytesRead = socket.read(rBuffer);
    if (bytesRead == -1) {
      disconnect();
    } else if (bytesRead == 0) {
      return;
    }
    rBuffer.flip();
    this.onDataRead.accept(rBuffer);
    if (rBuffer.hasRemaining()) {
      rBuffer.compact();
    } else {
      rBuffer.clear();
    }
  }

  public void write() throws IOException {
    this.onDataWrite.accept(wBuffer);
    if (wBuffer.position() == 0) {
      return; // Nothing to write
    }
    wBuffer.flip();
    socket.write(wBuffer);
    if (wBuffer.hasRemaining()) {
      wBuffer.compact();
    } else {
      wBuffer.clear();
    }
  }

  @Override
  public String toString() {
    return "ProxySocket{" +
        "socket=" + socket +
        '}';
  }

  public String getRemoteAddress() {
    try {
      return socket.getRemoteAddress().toString();
    } catch (IOException e) {
      return "<unknown>";
    }
  }

  public interface IConsumer<T> {

    void accept(T t);
  }
}
