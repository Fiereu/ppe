package de.fiereu.ppe.pokemmo;

public enum Region {
  EU("loginserver.pokemmo.eu", "185.152.67.99"),
  GLOBAL("loginserver.pokemmo.com", "185.180.13.145"),
  CUSTOM(null, null);

  public final String hostname;
  public final String realIp;

  Region(String hostname, String realIp) {
    this.hostname = hostname;
    this.realIp = realIp;
  }

  @Override
  public String toString() {
    if (this == CUSTOM) {
      return "CUSTOM";
    }
    return name() + " [" + hostname + "]";
  }
}
