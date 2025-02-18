package red.mohist.remapnms;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.provider.JointProvider;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ListIterator;

public class ReflectionTransformer {
    public static final String DESC_ReflectionMethods = Type.getInternalName(ReflectionMethods.class);

    public static JarMapping jarMapping;
    public static JarRemapper remapper;

    public static final HashMap<String, String> classDeMapping = Maps.newHashMap();
    public static final Multimap<String, String> methodDeMapping = ArrayListMultimap.create();
    public static final Multimap<String, String> fieldDeMapping = ArrayListMultimap.create();
    public static final Multimap<String, String> methodFastMapping = ArrayListMultimap.create();

    private static boolean disable = false;

    public static void init() {
        try {
            MohistRemapUtils.getCallerClassloader();
        } catch (Throwable e) {
            new RuntimeException("Unsupported Java version, disabled reflection remap!", e).printStackTrace();
            disable = true;
        }
        jarMapping = MappingLoader.loadMapping();
        JointProvider provider = new JointProvider();
        provider.add(new ClassInheritanceProvider());
        jarMapping.setFallbackInheritanceProvider(provider);
        remapper = new JarRemapper(jarMapping);

        jarMapping.classes.forEach((k, v) -> classDeMapping.put(v, k));
        jarMapping.methods.forEach((k, v) -> methodDeMapping.put(v, k));
        jarMapping.fields.forEach((k, v) -> fieldDeMapping.put(v, k));
        jarMapping.methods.forEach((k, v) -> methodFastMapping.put(k.split("\\s+")[0], k));
    }

    public static byte[] transform(byte[] code) {
        ClassReader reader = new ClassReader(code); // Turn from bytes into visitor
        ClassNode node = new ClassNode();
        reader.accept(node, 0); // Visit using ClassNode
        boolean remapCL = false;
        if (node.superName.equals("java/net/URLClassLoader")) {
            node.superName = CustomURLClassLoader.url;
            remapCL = true;
        }

        for (MethodNode method : node.methods) { // Taken from SpecialSource
            ListIterator<AbstractInsnNode> insnIterator = method.instructions.iterator();
            while (insnIterator.hasNext()) {
                AbstractInsnNode next = insnIterator.next();

                if (next instanceof TypeInsnNode) {
                    TypeInsnNode insn = (TypeInsnNode) next;
                    if (insn.getOpcode() == Opcodes.NEW && insn.desc.equals("java/net/URLClassLoader")) { // remap new URLClassLoader
                        insn.desc = CustomURLClassLoader.url;
                        remapCL = true;
                    }
                }

                if (!(next instanceof MethodInsnNode)) continue;
                MethodInsnNode insn = (MethodInsnNode) next;
                switch (insn.getOpcode()) {
                    case Opcodes.INVOKEVIRTUAL:
                        remapVirtual(insn);
                        break;
                    case Opcodes.INVOKESTATIC:
                        remapForName(insn);
                        break;
                    case Opcodes.INVOKESPECIAL:
                        if (remapCL) remapURLClassLoader(insn);
                        break;
                }

                if(insn.owner.equals("javax/script/ScriptEngineManager") && insn.desc.equals("()V") && insn.name.equals("<init>")){
                    insn.desc="(Ljava/lang/ClassLoader;)V";
                    method.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;"));
                    method.maxStack++;
                }
            }
        }

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    public static void remapForName(AbstractInsnNode insn) {
        MethodInsnNode method = (MethodInsnNode) insn;
        if (disable || !method.owner.equals("java/lang/Class") || !method.name.equals("forName")) return;
        method.owner = DESC_ReflectionMethods;
    }

    public static void remapVirtual(AbstractInsnNode insn) {
        MethodInsnNode method = (MethodInsnNode) insn;

        if (!(
                (method.owner.equals("java/lang/Class") && (
                    method.name.equals("getField") ||
                    method.name.equals("getDeclaredField") ||
                    method.name.equals("getMethod") ||
                    method.name.equals("getDeclaredMethod") ||
                    method.name.equals("getSimpleName"))
                )
            ||
                (method.name.equals("getName") && (
                    method.owner.equals("java/lang/reflect/Field") ||
                    method.owner.equals("java/lang/reflect/Method"))
                )
            ||
                (method.owner.equals("java/lang/ClassLoader") && method.name.equals("loadClass"))
            )) return;

        Type returnType = Type.getReturnType(method.desc);

        ArrayList<Type> args = new ArrayList<Type>();
        args.add(Type.getObjectType(method.owner));
        args.addAll(Arrays.asList(Type.getArgumentTypes(method.desc)));

        method.setOpcode(Opcodes.INVOKESTATIC);
        method.owner = DESC_ReflectionMethods;
        method.desc = Type.getMethodDescriptor(returnType, args.toArray(new Type[args.size()]));
    }

    public static void remapURLClassLoader(MethodInsnNode method) {
        if (!(method.owner.equals("java/net/URLClassLoader") && method.name.equals("<init>"))) return;
        method.owner = CustomURLClassLoader.url;
    }
}
