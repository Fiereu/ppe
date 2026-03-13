package de.fiereu.ppe.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class PpeClassFileTransformer implements ClassFileTransformer {

  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain, byte[] classfileBuffer) {
    if (className == null
        || className.startsWith("java/")
        || className.startsWith("javax/")
        || className.startsWith("sun/")
        || className.startsWith("de/fiereu/ppe/")) {
      return null;
    }
    try {
      ClassReader classReader = new ClassReader(classfileBuffer);
      ClassWriter classWriter = new ClassWriter(classReader, 0);
      ClassVisitor certPatch = new CertificatePatch(Opcodes.ASM9, classWriter);
      ClassVisitor dnsPatch = new DnsPatch(Opcodes.ASM9, certPatch);
      classReader.accept(dnsPatch, 0);
      return classWriter.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return classfileBuffer;
  }
}
