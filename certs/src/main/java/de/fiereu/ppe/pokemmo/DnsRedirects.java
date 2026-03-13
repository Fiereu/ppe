package de.fiereu.ppe.pokemmo;

import java.util.Collections;
import java.util.Map;

public final class DnsRedirects {

  private static final String PROP_REGION = "ppe.dns.region";
  private static final String PROP_CUSTOM_HOSTNAME = "ppe.dns.custom.hostname";

  private DnsRedirects() {}

  public static Region getRegion() {
    String regionStr = System.getProperty(PROP_REGION);
    if (regionStr != null) {
      try {
        return Region.valueOf(regionStr.toUpperCase());
      } catch (IllegalArgumentException ignored) {
      }
    }
    return Region.EU;
  }

  public static String getCustomHostname() {
    return System.getProperty(PROP_CUSTOM_HOSTNAME);
  }

  public static Map<String, String> getRedirects() {
    Region region = getRegion();
    if (region == Region.CUSTOM) {
      String custom = getCustomHostname();
      if (custom != null && !custom.isBlank()) {
        return Map.of(custom.toLowerCase(), "127.0.0.1");
      }
      return Collections.emptyMap();
    }
    return Map.of(region.hostname.toLowerCase(), "127.0.0.1");
  }

  public static String redirect(String hostname) {
    if (hostname == null) {
      return null;
    }
    return getRedirects().getOrDefault(hostname.toLowerCase(), hostname);
  }

  public static String getTargetIp() {
    Region region = getRegion();
    if (region == Region.CUSTOM) {
      String custom = getCustomHostname();
      return custom != null && !custom.isBlank() ? custom : null;
    }
    return region.realIp;
  }
}
