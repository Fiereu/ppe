package de.fiereu.ppe.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Time {

  private static final DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");

  public static String format(long timestamp) {
    return formatter.format(new Date(timestamp));
  }
}
