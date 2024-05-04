package de.fiereu.ppe.proxy.hash;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HmacSha256 extends AbstractHash {

  private final Mac mac;
  private int round = 0;

  public HmacSha256(int size, byte[] key) {
    super(size);
    assert size >= 4 && size <= 32;

    try {
      this.mac = Mac.getInstance("HmacSHA256");
      this.mac.init(new SecretKeySpec(key, "HmacSHA256"));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] _toBytes(int value) {
    // Big endian
    return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array();
  }

  @Override
  public byte[] hash(byte[] data) {
    mac.update(data);
    mac.update(_toBytes(round++));
    return Arrays.copyOfRange(mac.doFinal(), 0, size);
  }

  @Override
  public boolean verify(byte[] data, byte[] hash) {
    mac.update(data);
    mac.update(_toBytes(round++));
    byte[] result = Arrays.copyOfRange(mac.doFinal(), 0, size);
    return MessageDigest.isEqual(result, hash);
  }
}
