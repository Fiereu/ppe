package de.fiereu.ppe.proxy;

import de.fiereu.ppe.pokemmo.Certificates;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyService {

  private static final Logger log = LoggerFactory.getLogger(KeyService.class);
  private static final HashMap<ServerType, PublicKey> pokemmoKeys = new HashMap<>();
  private static final KeyPair proxyKeyPair;

  static {
    try {
      proxyKeyPair = new KeyPair(
          pubFromBase64(Certificates.proxyPubKey),
          fromDER(Objects.requireNonNull(KeyService.class.getResource("/cert/proxy.priv.der"))
              .openStream())
      );
    } catch (Exception e) {
      log.error("Failed to load proxy key pair", e);
      throw new RuntimeException(e);
    }
    pokemmoKeys.put(ServerType.LOGIN, pubFromBase64(Certificates.lsPubKey));
    pokemmoKeys.put(ServerType.GAME, pubFromBase64(Certificates.gsPubKey));
    pokemmoKeys.put(ServerType.CHAT, pubFromBase64(Certificates.csPubKey));
  }

  public static PublicKey getPublicKey(ServerType serverType) {
    return pokemmoKeys.get(serverType);
  }

  public static KeyPair getProxyKeyPair() {
    return proxyKeyPair;
  }

  private static PublicKey pubFromBase64(String base64) {
    try {
      var keyFactory = KeyFactory.getInstance("EC");
      return keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      log.error("Failed to generate PublicKey", e);
      throw new RuntimeException(e);
    }
  }

  private static PrivateKey fromDER(InputStream privKeyStream)
      throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privKeyStream.readAllBytes());
    KeyFactory keyFactory = KeyFactory.getInstance("EC");
    return keyFactory.generatePrivate(privKeySpec);
  }
}
