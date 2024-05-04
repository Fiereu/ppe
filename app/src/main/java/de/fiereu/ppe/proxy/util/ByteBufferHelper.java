package de.fiereu.ppe.proxy.util;

import com.github.maltalex.ineter.base.IPAddress;
import com.github.maltalex.ineter.base.IPv4Address;
import com.github.maltalex.ineter.base.IPv6Address;
import java.nio.ByteBuffer;

public class ByteBufferHelper {

  public static String readString(ByteBuffer buffer) {
    StringBuilder builder = new StringBuilder();
    char c;
    while ((c = buffer.getChar()) != 0) {
      builder.append(c);
    }
    return builder.toString();
  }

  public static IPAddress readIP(ByteBuffer buffer) {
    byte type = buffer.get();
    if (type == 0x04) {
      return new IPv4Address(buffer.getInt());
    } else {
      return new IPv6Address(buffer.getLong(), buffer.getLong());
    }
  }
}
