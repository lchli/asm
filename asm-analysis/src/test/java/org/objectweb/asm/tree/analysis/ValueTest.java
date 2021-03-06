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
package org.objectweb.asm.tree.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnNode;

/**
 * BasicValue and SourceValue tests.
 *
 * @author Eric Bruneton
 */
public class ValueTest {

    @Test
    public void testBasicValue() {
        assertTrue(BasicValue.UNINITIALIZED_VALUE.equals(new BasicValue(null)));
        assertTrue(BasicValue.INT_VALUE.equals(new BasicValue(Type.INT_TYPE)));
        assertTrue(BasicValue.INT_VALUE.equals(BasicValue.INT_VALUE));
        assertFalse(BasicValue.INT_VALUE.equals(new Object()));

        assertTrue(BasicValue.REFERENCE_VALUE.isReference());
        assertTrue(new BasicValue(Type.getObjectType("[I")).isReference());
        assertFalse(BasicValue.UNINITIALIZED_VALUE.isReference());
        assertFalse(BasicValue.INT_VALUE.isReference());

        assertEquals(0, BasicValue.UNINITIALIZED_VALUE.hashCode());
        assertNotEquals(0, BasicValue.INT_VALUE.hashCode());

        assertEquals(".", BasicValue.UNINITIALIZED_VALUE.toString());
        assertEquals("A", BasicValue.RETURNADDRESS_VALUE.toString());
        assertEquals("R", BasicValue.REFERENCE_VALUE.toString());
        assertEquals("LI;", new BasicValue(Type.getObjectType("I")).toString());
    }

    @Test
    public void testSourceValue() {
        assertEquals(2, new SourceValue(2).getSize());

        assertTrue(new SourceValue(1).equals(new SourceValue(1)));
        assertFalse(new SourceValue(1).equals(new SourceValue(1, new InsnNode(Opcodes.NOP))));
        assertFalse(new SourceValue(1).equals(new SourceValue(2)));
        assertFalse(new SourceValue(1).equals(null));

        assertEquals(0, new SourceValue(1).hashCode());
        assertNotEquals(0, new SourceValue(1, new InsnNode(Opcodes.NOP)).hashCode());
    }
}
