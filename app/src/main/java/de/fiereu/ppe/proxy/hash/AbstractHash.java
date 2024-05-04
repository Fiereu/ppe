package de.fiereu.ppe.proxy.hash;

public abstract class AbstractHash {

  public final int size;

  public AbstractHash(int size) {
    this.size = size;
  }

  public abstract byte[] hash(byte[] data);

  public abstract boolean verify(byte[] data, byte[] hash);
}
