// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.objectweb.asm.commons;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.test.AsmTest;

/**
 * AdviceAdapter tests.
 *
 * @author Eugene Kuleshov
 */
public class AdviceAdapterTest extends AsmTest {

    @Test
    public void test() throws Exception {
        Class<?> c = getClass();
        String name = c.getName();
        AdvisingClassLoader cl = new AdvisingClassLoader(name + "$");
        Class<?> cc = cl.loadClass(name + "$B");
        Method m = cc.getMethod("run", new Class<?>[]{Integer.TYPE});
        try {
            m.invoke(null, new Object[]{new Integer(0)});
        } catch (InvocationTargetException e) {
            throw (Exception) e.getTargetException();
        }
    }

    private static final class AdvisingClassLoader extends ClassLoader {
        private String prefix;

        public AdvisingClassLoader(final String prefix) throws IOException {
            this.prefix = prefix;
        }

        @Override
        public Class<?> loadClass(final String name) throws ClassNotFoundException {
            if (name.startsWith(prefix)) {
                try {
                    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    ClassReader cr =
                            new ClassReader(
                                    getClass().getResourceAsStream("/" + name.replace('.', '/') + ".class"));
                    cr.accept(new AdviceClassAdapter(cw), ClassReader.EXPAND_FRAMES);
                    byte[] bytecode = cw.toByteArray();
                    return super.defineClass(name, bytecode, 0, bytecode.length);
                } catch (IOException ex) {
                    throw new ClassNotFoundException("Load error: " + ex.toString(), ex);
                }
            }
            return super.loadClass(name);
        }
    }

    // test callback
    private static int n = 0;

    public static void enter(final String msg) {
        n++;
    }

    public static void exit(final String msg) {
        n--;
    }

    private static StringBuilder off() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append("  ");
        }
        return sb;
    }

    static class AdviceClassAdapter extends ClassVisitor implements Opcodes {
        String cname;

        public AdviceClassAdapter(final ClassVisitor cv) {
            super(Opcodes.ASM5, cv);
        }

        @Override
        public void visit(
                final int version,
                final int access,
                final String name,
                final String signature,
                final String superName,
                final String[] interfaces) {
            this.cname = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(
                final int access,
                final String name,
                final String desc,
                final String signature,
                final String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

            if (mv == null || (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) > 0) {
                return mv;
            }

            return new AdviceAdapter(Opcodes.ASM5, mv, access, name, desc) {

                @Override
                protected void onMethodEnter() {
                    mv.visitLdcInsn(cname + "." + name + desc);
                    mv.visitMethodInsn(
                            INVOKESTATIC,
                            "org/objectweb/asm/commons/AdviceAdapterTest",
                            "enter",
                            "(Ljava/lang/String;)V",
                            false);
                }

                @Override
                protected void onMethodExit(final int opcode) {
                    mv.visitLdcInsn(cname + "." + name + desc);
                    mv.visitMethodInsn(
                            INVOKESTATIC,
                            "org/objectweb/asm/commons/AdviceAdapterTest",
                            "exit",
                            "(Ljava/lang/String;)V",
                            false);
                }
            };
        }
    }

    // TEST CLASSES

    public static class A {
        final String s;

        public A(final String s) {
            this.s = s;
        }

        public A(final A a) {
            this.s = a.s;
        }
    }

    public static class B extends A {

        public B() {
            super(new B(""));
            test(this);
        }

        public B(final A a) {
            super(a);
            test(this);
        }

        public B(final String s) {
            super(s == null ? new A("") : new A(s));
            test(this);
        }

        private static A aa;

        public B(final String s, final A a) {
            this(s == null ? aa = new A(s) : a);
            A aa = new A("");
            test(aa);
        }

        public B(final String s, final String s1) {
            super(s != null ? new A(getA(s1).s) : new A(s));
            test(this);
        }

        private void test(final Object b) {
        }

        private static A getA(final String s) {
            return new A(s);
        }

        // execute all
        public static void run(final int n) {
            new B();
            new B(new A(""));
            new B(new B());
            new B("", new A(""));
            new B("", "");
        }
    }

    @ParameterizedTest
    @MethodSource(ALL_CLASSES_AND_ALL_APIS)
    public void testEmptyAdviceAdapter(PrecompiledClass classParameter, Api apiParameter)
            throws Exception {
        ClassReader classReader = new ClassReader(classParameter.getBytes());
        ClassWriter expectedClassWriter = new ClassWriter(0);
        ClassWriter actualClassWriter = new ClassWriter(0);
        ClassVisitor expectedClassVisitor =
                new ReferenceClassAdapter(apiParameter.value(), expectedClassWriter);
        ClassVisitor actualClassVisitor =
                new EmptyAdviceClassAdapter(apiParameter.value(), actualClassWriter);
        if (classParameter.isMoreRecentThan(apiParameter)) {
            assertThrows(
                    RuntimeException.class,
                    () -> classReader.accept(expectedClassVisitor, ClassReader.EXPAND_FRAMES));
            assertThrows(
                    RuntimeException.class,
                    () -> classReader.accept(actualClassVisitor, ClassReader.EXPAND_FRAMES));
            return;
        }
        classReader.accept(expectedClassVisitor, ClassReader.EXPAND_FRAMES);
        classReader.accept(actualClassVisitor, ClassReader.EXPAND_FRAMES);
        assertThatClass(actualClassWriter.toByteArray()).isEqualTo(expectedClassWriter.toByteArray());
    }

    private static class ReferenceClassAdapter extends ClassVisitor {

        ReferenceClassAdapter(final int api, final ClassVisitor cv) {
            super(api, cv);
        }

        @Override
        public MethodVisitor visitMethod(
                final int access,
                final String name,
                final String desc,
                final String signature,
                final String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (mv == null || (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) > 0) {
                return mv;
            }
            return new LocalVariablesSorter(api, access, desc, mv);
        }
    }

    private static class EmptyAdviceClassAdapter extends ClassVisitor {

        EmptyAdviceClassAdapter(final int api, final ClassVisitor cv) {
            super(api, cv);
        }

        @Override
        public MethodVisitor visitMethod(
                final int access,
                final String name,
                final String desc,
                final String signature,
                final String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (mv == null || (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) > 0) {
                return mv;
            }
            return new AdviceAdapter(api, mv, access, name, desc) {
            };
        }
    }
}
