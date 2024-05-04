package de.fiereu.ppe.proxy.util;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import javax.crypto.KeyAgreement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EC {

  private static final Logger log = LoggerFactory.getLogger(EC.class);
  private static final byte uncompressedPointIndicator = 0x04;
  private static final ECParameterSpec ecParameterSpec;

  static {
    try {
      var algorithmSpec = AlgorithmParameters.getInstance("EC");
      algorithmSpec.init(new ECGenParameterSpec("secp256r1"));
      ecParameterSpec = algorithmSpec.getParameterSpec(ECParameterSpec.class);
    } catch (InvalidParameterSpecException | NoSuchAlgorithmException e) {
      log.error("Failed to initialize ECParameterSpec", e);
      throw new RuntimeException(e);
    }
  }

  public static ECPublicKey fromUncompressedPoint(byte[] data) {
    if (data[0] != uncompressedPointIndicator) {
      throw new IllegalArgumentException("Invalid uncompressed point indicator");
    }
    int keyLength = ((ecParameterSpec.getOrder().bitLength() + Byte.SIZE - 1) / Byte.SIZE);
    if (data.length != 1 + 2 * keyLength) {
      throw new IllegalArgumentException("Invalid data length");
    }
    int offset = 1;
    BigInteger x = new BigInteger(1, Arrays.copyOfRange(data, offset, offset + keyLength));
    offset += keyLength;
    BigInteger y = new BigInteger(1, Arrays.copyOfRange(data, offset, offset + keyLength));
    ECPoint point = new ECPoint(x, y);
    try {
      var keyFactory = KeyFactory.getInstance("EC");
      return (ECPublicKey) keyFactory.generatePublic(new ECPublicKeySpec(point, ecParameterSpec));
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      log.error("Failed to generate ECPublicKey", e);
      throw new RuntimeException(e);
    }
  }

  public static byte[] toUncompressedPoint(ECPublicKey key) {
    ECPoint point = key.getW();
    int keyLength = ((ecParameterSpec.getOrder().bitLength() + Byte.SIZE - 1) / Byte.SIZE);
    int offset = 0;
    byte[] data = new byte[1 + 2 * keyLength];
    data[offset++] = uncompressedPointIndicator;
    byte[] x = point.getAffineX().toByteArray();
    byte[] y = point.getAffineY().toByteArray();
    if (x.length <= keyLength) {
      System.arraycopy(x, 0, data, offset + keyLength - x.length, x.length);
    } else if (x.length == keyLength + 1 && x[0] == 0) {
      System.arraycopy(x, 1, data, offset, keyLength);
    } else {
      throw new IllegalStateException("x value is too large");
    }
    offset += keyLength;
    if (y.length <= keyLength) {
      System.arraycopy(y, 0, data, offset + keyLength - y.length, y.length);
    } else if (y.length == keyLength + 1 && y[0] == 0) {
      System.arraycopy(y, 1, data, offset, keyLength);
    } else {
      throw new IllegalStateException("y value is too large");
    }
    return data;
  }

  public static KeyPair keyPair() {
    try {
      var keyPairGenerator = KeyPairGenerator.getInstance("EC");
      keyPairGenerator.initialize(ecParameterSpec);
      return keyPairGenerator.generateKeyPair();
    } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
      log.error("Failed to generate KeyPair", e);
      throw new RuntimeException(e);
    }
  }

  public static byte[] keyAgreement(PrivateKey privateKey, PublicKey publicKey) {
    try {
      var keyAgreement = KeyAgreement.getInstance("ECDH");
      keyAgreement.init(privateKey);
      keyAgreement.doPhase(publicKey, true);
      return keyAgreement.generateSecret();
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      log.error("Failed to generate shared secret", e);
      throw new RuntimeException(e);
    }
  }

  public static byte[] sign(byte[] data, PrivateKey privateKey) {
    try {
      var signature = Signature.getInstance("SHA256withECDSA");
      signature.initSign(privateKey);
      signature.update(data);
      return signature.sign();
    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      log.error("Failed to sign data", e);
      throw new RuntimeException(e);
    }
  }

  public static boolean verify(byte[] signature, byte[] data, PublicKey publicKey) {
    try {
      var signatureVerification = Signature.getInstance("SHA256withECDSA");
      signatureVerification.initVerify(publicKey);
      signatureVerification.update(data);
      return signatureVerification.verify(signature);
    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      log.error("Failed to verify signature", e);
      throw new RuntimeException(e);
    }
  }
}
