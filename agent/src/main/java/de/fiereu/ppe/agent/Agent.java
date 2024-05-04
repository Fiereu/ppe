package de.fiereu.ppe.agent;

import java.lang.instrument.Instrumentation;

public class Agent {

  private static Instrumentation instrumentation;

  public static void premain(String args, Instrumentation inst) {
    instrumentation = inst;
    instrumentation.addTransformer(new PpeClassFileTransformer());
  }

}
