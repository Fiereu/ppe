package de.fiereu.ppe.agent;

import de.fiereu.ppe.pokemmo.Certificates;
import java.util.List;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class CertificatePatch extends ClassVisitor {

  private static final List<String> certificates = List.of(
      Certificates.lsPubKey,
      Certificates.gsPubKey,
      Certificates.csPubKey
  );

  CertificatePatch(int api, ClassVisitor classVisitor) {
    super(api, classVisitor);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
      String[] exceptions) {
    return new MethodVisitor(api,
        super.visitMethod(access, name, descriptor, signature, exceptions)) {
      @Override
      public void visitLdcInsn(Object value) {
        if (value instanceof String str) {
          if (certificates.contains(str)) {
            super.visitLdcInsn(Certificates.proxyPubKey);
            System.out.printf("Patched certificate: %s -> %s%n", str, Certificates.proxyPubKey);
            return;
          }
        }
        super.visitLdcInsn(value);
      }
    };
  }
}
