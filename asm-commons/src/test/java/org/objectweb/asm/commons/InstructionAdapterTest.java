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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.test.AsmTest;

/**
 * InstructionAdapter tests.
 *
 * @author Eric Bruneton
 */
public class InstructionAdapterTest extends AsmTest {

    /**
     * Tests that classes transformed with an InstructionAdapter are unchanged.
     */
    @ParameterizedTest
    @MethodSource(ALL_CLASSES_AND_ALL_APIS)
    public void testAdaptInstructions(final PrecompiledClass classParameter, final Api apiParameter) {
        byte[] classFile = classParameter.getBytes();
        ClassReader classReader = new ClassReader(classFile);
        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor classVisitor =
                new ClassVisitor(apiParameter.value(), classWriter) {

                    @Override
                    public MethodVisitor visitMethod(
                            final int access,
                            final String name,
                            final String descriptor,
                            final String signature,
                            final String[] exceptions) {
                        return new InstructionAdapter(
                                api, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                        };
                    }
                };
        if (classParameter.isMoreRecentThan(apiParameter)) {
            assertThrows(RuntimeException.class, () -> classReader.accept(classVisitor, attributes(), 0));
        } else {
            classReader.accept(classVisitor, attributes(), 0);
            assertThatClass(classWriter.toByteArray()).isEqualTo(classFile);
        }
    }

    private static Attribute[] attributes() {
        return new Attribute[]{new Comment(), new CodeComment()};
    }
}
