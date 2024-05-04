package de.fiereu.ppe.proxy.packets;

import de.fiereu.ppe.proxy.ProxyClient;
import de.fiereu.ppe.proxy.hash.AbstractHash;
import de.fiereu.ppe.proxy.hash.CRC16;
import de.fiereu.ppe.proxy.hash.HmacSha256;
import de.fiereu.ppe.proxy.hash.NoOpHash;
import de.fiereu.ppe.proxy.util.EC;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketCypher {

  private static final Logger log = LoggerFactory.getLogger(PacketCypher.class);
  private static final byte[] ivSeed = new byte[]{73, 86, 68, 69, 82, 73, 86};
  private static final byte[] serverSeed = new byte[]{75, 101, 121, 83, 97, 108, 116, 1};
  private static final byte[] clientSeed = new byte[]{75, 101, 121, 83, 97, 108, 116, 2};
  public final KeyPair proxyKeyPair = EC.keyPair();
  public final ProxyClient proxyClient;
  public DecryptionState state = DecryptionState.UNSECURED;
  public ProxyClient.ProxyHandler handler;
  public Cipher encryptionCipher;
  public Cipher decryptionCipher;
  private AbstractHash encryptionHash = new NoOpHash();
  private AbstractHash decryptionHash = new NoOpHash();
  private ECPublicKey remotePublicKey;
  private byte[] sharedSecret;
  private byte[] encryptionKey;
  private byte[] decryptionKey;
  public PacketCypher(ProxyClient proxyClient, ProxyClient.ProxyHandler handler) {
    this.proxyClient = proxyClient;
    this.handler = handler;
  }

  public int getHashSize() {
    if (state == DecryptionState.UNSECURED) {
      return 0; // Still using NoOpHash
    }
    return encryptionHash.size; // Math.max(encryptionHash.size, decryptionHash.size);
  }

  public void decrypt(ByteBuffer buffer) throws ShortBufferException {
    if (state == DecryptionState.UNSECURED) {
      return;
    }
    log.trace("Decrypting packet");
    int dataLength = buffer.remaining();
    int position = buffer.position() + buffer.arrayOffset();
    byte[] hash = Arrays.copyOfRange(buffer.array(), position + dataLength - decryptionHash.size,
        position + dataLength);
    byte[] data = Arrays.copyOfRange(buffer.array(), position,
        position + dataLength - decryptionHash.size);
    if (!decryptionHash.verify(data, hash)) {
      log.error("Failed to verify packet hash");
      throw new RuntimeException("Failed to verify packet hash");
    }
    int extra = decryptionCipher.update(data, 0, data.length, buffer.array(), 0);
    buffer.position(extra);
    buffer.flip();
    log.trace("Decrypted packet: {}", HexFormat.of().formatHex(
        Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(),
            buffer.arrayOffset() + buffer.limit())));
  }

  public void encrypt(ByteBuffer buffer) throws ShortBufferException {
    if (state == DecryptionState.UNSECURED) {
      buffer.position(buffer.limit());
      return;
    }
    log.trace("Encrypting packet");
    int start = buffer.position();
    int dataLength = buffer.remaining();
    int position = buffer.position() + buffer.arrayOffset();
    int extra = encryptionCipher.update(buffer.array(), position, dataLength, buffer.array(),
        position);
    buffer.limit(start + extra);
    byte[] hash = encryptionHash.hash(
        Arrays.copyOfRange(buffer.array(), position, position + extra));
    buffer.position(buffer.limit());
    buffer.limit(buffer.limit() + encryptionHash.size);
    buffer.put(hash);
  }

  public void initialize(byte[] uncompressedPubKey, byte hashSize) {
    remotePublicKey = EC.fromUncompressedPoint(uncompressedPubKey);
    sharedSecret = EC.keyAgreement(proxyKeyPair.getPrivate(), remotePublicKey);

    // Depending on the handler the encryption and decryption key are different.
    // In one position we play the client and in the other position we play the server.
    encryptionKey = byteShuffler(sharedSecret,
        handler == ProxyClient.ProxyHandler.CLIENT ? clientSeed : serverSeed);
    decryptionKey = byteShuffler(sharedSecret,
        handler == ProxyClient.ProxyHandler.CLIENT ? serverSeed : clientSeed);

    try {
      encryptionCipher = Cipher.getInstance("AES/CTR/NoPadding");
      encryptionCipher.init(
          Cipher.ENCRYPT_MODE,
          new SecretKeySpec(encryptionKey, "AES"),
          new IvParameterSpec(byteShuffler(encryptionKey, ivSeed))
      );
      decryptionCipher = Cipher.getInstance("AES/CTR/NoPadding");
      decryptionCipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(decryptionKey, "AES"),
          new IvParameterSpec(byteShuffler(decryptionKey, ivSeed))
      );
    } catch (Exception e) {
      log.error("Failed to initialize encryption/decryption cipher", e);
      throw new RuntimeException(e);
    }

    this.encryptionHash = switch (hashSize) {
      case 0 -> new NoOpHash();
      case 2 -> new CRC16();
      default -> new HmacSha256(hashSize, encryptionKey);
    };
    this.decryptionHash = switch (hashSize) {
      case 0 -> new NoOpHash();
      case 2 -> new CRC16();
      default -> new HmacSha256(hashSize, decryptionKey);
    };
  }

  private byte[] byteShuffler(byte[] arr1, byte[] arr2) {
    MessageDigest sha256;
    try {
      sha256 = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      log.error("Failed to initialize SHA-256", e);
      throw new RuntimeException(e);
    }
    sha256.update(arr2);
    sha256.update(arr1);
    sha256.update(arr2);
    return Arrays.copyOfRange(sha256.digest(), 0, 16);
  }

  public enum DecryptionState {
    UNSECURED,
    SECURED,
  }
}
