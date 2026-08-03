package com.xiaomo305mua.draconicarmorrenderfix;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class DARFTransformer implements IClassTransformer {

    private static final String TARGET_CLASS =
            "com.brandon3055.draconicevolution.client.model.ModelRenderOBJ";
    private static final String BIND_OWNER =
            "com/brandon3055/draconicevolution/client/handler/ResourceHandler";
    private static final String BIND_NAME = "bindTexture";
    private static final String BIND_DESC = "(Lnet/minecraft/util/ResourceLocation;)V";
    private static final String FIELD_TEXTURE = "texture";
    private static final String FIELD_TEXTURE_DESC = "Lnet/minecraft/util/ResourceLocation;";
    private static final String METHOD_COMPILE = "compileDisplayList";
    private static final String METHOD_RENDER = "render";
    private static final String METHOD_RENDER_SRG = "func_78785_a";
    private static final String METHOD_RENDER_WITH_ROTATION = "renderWithRotation";
    private static final String METHOD_RENDER_WITH_ROTATION_SRG = "func_78791_b";
    private static final String METHOD_DESC = "(F)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        if (!TARGET_CLASS.equals(transformedName)) {
            return basicClass;
        }

        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean patched = false;
        for (MethodNode method : classNode.methods) {
            if (METHOD_COMPILE.equals(method.name) && METHOD_DESC.equals(method.desc)) {
                patched |= removeBindTexture(method);
            } else if ((METHOD_RENDER.equals(method.name)
                    || METHOD_RENDER_SRG.equals(method.name)
                    || METHOD_RENDER_WITH_ROTATION.equals(method.name)
                    || METHOD_RENDER_WITH_ROTATION_SRG.equals(method.name))
                    && METHOD_DESC.equals(method.desc)) {
                patched |= insertBindTexture(method);
            }
        }

        if (!patched) {
            return basicClass;
        }

        ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static boolean removeBindTexture(MethodNode method) {
        boolean removed = false;
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.INVOKESTATIC && isBindTexture((MethodInsnNode) insn)) {
                AbstractInsnNode field = insn.getPrevious();
                AbstractInsnNode aload = field == null ? null : field.getPrevious();
                if (field instanceof FieldInsnNode
                        && field.getOpcode() == Opcodes.GETFIELD
                        && FIELD_TEXTURE.equals(((FieldInsnNode) field).name)
                        && FIELD_TEXTURE_DESC.equals(((FieldInsnNode) field).desc)
                        && aload instanceof VarInsnNode
                        && aload.getOpcode() == Opcodes.ALOAD
                        && ((VarInsnNode) aload).var == 0) {
                    method.instructions.remove(aload);
                    method.instructions.remove(field);
                    method.instructions.remove(insn);
                    removed = true;
                }
            }
        }
        return removed;
    }

    private static boolean insertBindTexture(MethodNode method) {
        boolean inserted = false;
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            int op = insn.getOpcode();
            if ((op == Opcodes.INVOKESPECIAL || op == Opcodes.INVOKEVIRTUAL)
                    && insn instanceof MethodInsnNode
                    && METHOD_COMPILE.equals(((MethodInsnNode) insn).name)
                    && METHOD_DESC.equals(((MethodInsnNode) insn).desc)) {
                AbstractInsnNode next = insn.getNext();
                if (next != null) {
                    method.instructions.insert(next, buildBindTextureCall());
                } else {
                    method.instructions.add(buildBindTextureCall());
                }
                inserted = true;
            }
        }
        return inserted;
    }

    private static InsnList buildBindTextureCall() {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(
                Opcodes.GETFIELD, TARGET_CLASS.replace('.', '/'), FIELD_TEXTURE, FIELD_TEXTURE_DESC));
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BIND_OWNER, BIND_NAME, BIND_DESC, false));
        return list;
    }

    private static boolean isBindTexture(MethodInsnNode insn) {
        return BIND_OWNER.equals(insn.owner) && BIND_NAME.equals(insn.name) && BIND_DESC.equals(insn.desc);
    }

    private static class SafeClassWriter extends ClassWriter {

        SafeClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            return "java/lang/Object";
        }
    }
}