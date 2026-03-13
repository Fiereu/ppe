package de.fiereu.ppe.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class DnsPatch extends ClassVisitor {

  private static final String DNS_REDIRECTS_OWNER = "de/fiereu/ppe/pokemmo/DnsRedirects";
  private static final String DNS_REDIRECTS_METHOD = "redirect";
  private static final String DNS_REDIRECTS_DESC = "(Ljava/lang/String;)Ljava/lang/String;";

  DnsPatch(int api, ClassVisitor classVisitor) {
    super(api, classVisitor);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
      String[] exceptions) {
    return new MethodVisitor(api,
        super.visitMethod(access, name, descriptor, signature, exceptions)) {
      @Override
      public void visitMethodInsn(int opcode, String owner, String mName, String mDescriptor,
          boolean isInterface) {
        if (owner.equals("java/net/InetAddress")
            && mName.equals("getByName")
            && mDescriptor.equals("(Ljava/lang/String;)Ljava/net/InetAddress;")) {
          super.visitMethodInsn(Opcodes.INVOKESTATIC,
              DNS_REDIRECTS_OWNER,
              DNS_REDIRECTS_METHOD,
              DNS_REDIRECTS_DESC,
              false);
        } else if (owner.equals("java/net/InetAddress")
            && mName.equals("getAllByName")
            && mDescriptor.equals("(Ljava/lang/String;)[Ljava/net/InetAddress;")) {
          super.visitMethodInsn(Opcodes.INVOKESTATIC,
              DNS_REDIRECTS_OWNER,
              DNS_REDIRECTS_METHOD,
              DNS_REDIRECTS_DESC,
              false);
        }
        super.visitMethodInsn(opcode, owner, mName, mDescriptor, isInterface);
      }
    };
  }
}
