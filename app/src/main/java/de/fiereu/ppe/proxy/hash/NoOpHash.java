package de.fiereu.ppe.proxy.hash;

public class NoOpHash extends AbstractHash {

  public NoOpHash() {
    super(0);
  }

  @Override
  public byte[] hash(byte[] data) {
    return new byte[0];
  }

  @Override
  public boolean verify(byte[] data, byte[] hash) {
    return true;
  }
}
