package com.dylibso.chicory.simd;

import com.dylibso.chicory.runtime.BitOps;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.runtime.MStack;
import com.dylibso.chicory.runtime.OpcodeImpl;
import com.dylibso.chicory.runtime.StackFrame;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import java.util.Deque;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorOperators;

public final class SimdInterpreterMachine extends InterpreterMachine {

    public SimdInterpreterMachine(Instance instance) {
        super(instance);
    }

    @Override
    protected void evalDefault(
            MStack stack,
            Instance instance,
            Deque<StackFrame> callStack,
            Instruction instruction,
            Operands operands)
            throws ChicoryException {
        switch (instruction.opcode()) {
            case OpCode.V128_CONST:
                V128_CONST(stack, operands);
                break;
            case OpCode.V128_LOAD:
                V128_LOAD(stack, instance, operands);
                break;
            case OpCode.I8x16_EXTRACT_LANE_S:
                I8x16_EXTRACT_LANE_S(stack, operands);
                break;
            case OpCode.V128_NOT:
                V128_NOT(stack);
                break;
            case OpCode.V128_BITSELECT:
                V128_BITSELECT(stack);
                break;
            case OpCode.I8x16_EQ:
                I8x16_EQ(stack);
                break;
            case OpCode.I8x16_SUB:
                I8x16_SUB(stack);
                break;
            case OpCode.I8x16_ADD:
                I8x16_ADD(stack);
                break;
            case OpCode.I8x16_SWIZZLE:
                I8x16_SWIZZLE(stack);
                break;
            case OpCode.I8x16_ALL_TRUE:
                I8x16_ALL_TRUE(stack);
                break;
            case OpCode.I8x16_SHL:
                I8x16_SHL(stack);
                break;
            case OpCode.F32x4_MUL:
                F32x4_MUL(stack);
                break;
            case OpCode.F32x4_ABS:
                F32x4_ABS(stack);
                break;
            case OpCode.F32x4_MIN:
                F32x4_MIN(stack);
                break;
            case OpCode.I32x4_TRUNC_SAT_F32X4_S:
                I32x4_TRUNC_SAT_F32x4_S(stack);
                break;
            case OpCode.F32x4_CONVERT_I32x4_U:
                F32x4_CONVERT_I32x4_U(stack);
                break;
            default:
                super.evalDefault(stack, instance, callStack, instruction, operands);
                break;
        }
    }

    private static void V128_CONST(MStack stack, Operands operands) {
        stack.push(operands.get(0));
        stack.push(operands.get(1));
    }

    private static void V128_LOAD(MStack stack, Instance instance, Operands operands) {
        var ptr = readMemPtr(stack, operands);
        var valHigh = instance.memory().readLong(ptr);
        var valLow = instance.memory().readLong(ptr + 8);
        stack.push(valHigh);
        stack.push(valLow);
    }

    private static void I8x16_EXTRACT_LANE_S(MStack stack, Operands operands) {
        var laneIdx = operands.get(0);
        var offset = stack.size() - 2;
        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes();

        var result = v1.lane((int) laneIdx);
        stack.array()[stack.size() - 1] = result;
    }

    private static void I8x16_EQ(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        int offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsBytes();

        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes();

        var result = v1.eq(v2).toVector().reinterpretAsLongs().toArray();

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I8x16_ADD(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, offset)
                        .reinterpretAsBytes();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes();

        var result = v1.add(v2).reinterpretAsLongs().toArray();

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I8x16_SUB(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        var offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, offset)
                        .reinterpretAsBytes();
        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes();

        var result = v2.sub(v1).reinterpretAsLongs().toArray();

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I8x16_SHL(MStack stack) {
        var s = stack.pop();

        var offset = stack.size() - 2;

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsBytes();

        var result = v.lanewise(VectorOperators.LSHL, s).reinterpretAsLongs().toArray();
        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I8x16_ALL_TRUE(MStack stack) {
        var v =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), stack.size() - 2)
                        .reinterpretAsBytes();

        // Compare the vector against zero to create a mask of non-zero elements
        if (v.compare(VectorOperators.NE, 0).allTrue()) {
            stack.push(BitOps.TRUE);
        } else {
            stack.push(BitOps.FALSE);
        }
    }

    private static void V128_NOT(MStack stack) {
        var offset = stack.size() - 2;
        var not = LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset).not();
        var res = not.toArray();

        System.arraycopy(res, 0, stack.array(), offset, 2);
    }

    private static void V128_BITSELECT(MStack stack) {
        // https://github.com/tetratelabs/wazero/blob/58488880a334e8bda5be0d715a24d8ddeb34c725/internal/engine/interpreter/interpreter.go#L2378C7-L2384
        var cHi = stack.pop();
        var cLo = stack.pop();
        var x2Hi = stack.pop();
        var x2Lo = stack.pop();
        var x1Hi = stack.pop();
        var x1Lo = stack.pop();
        // v128.or(v128.and(v1, c), v128.and(v2, v128.not(c)))
        stack.push((x1Lo & cLo) | (x2Lo & (cLo ^ 0xFFFFFFFFFFFFFFFFL)));
        stack.push((x1Hi & cHi) | (x2Hi & (cHi ^ 0xFFFFFFFFFFFFFFFFL)));
    }

    private static void F32x4_MUL(MStack stack) {
        var val1High = stack.pop();
        var val1Low = stack.pop();
        var val2High = stack.pop();
        var val2Low = stack.pop();

        long resultLow = 0L;
        long resultHigh = 0L;

        for (int i = 0; i < 2; i++) {
            var shift = i * 32L;
            var floatHigh =
                    Float.intBitsToFloat((int) ((val1High >> shift) & 0xFFFFFFFFL))
                            * Float.intBitsToFloat((int) ((val2High >> shift) & 0xFFFFFFFFL));
            resultHigh |= (Float.floatToIntBits(floatHigh) & 0xFFFFFFFFL) << shift;
            var floatLow =
                    Float.intBitsToFloat((int) ((val1Low >> shift) & 0xFFFFFFFFL))
                            * Float.intBitsToFloat((int) ((val2Low >> shift) & 0xFFFFFFFFL));
            resultLow |= (Float.floatToIntBits(floatLow) & 0xFFFFFFFFL) << shift;
        }

        stack.push(resultLow);
        stack.push(resultHigh);
    }

    private static void F32x4_ABS(MStack stack) {
        var offset = stack.size() - 2;

        var v =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsFloats();

        var result = v.abs().reinterpretAsLongs().toArray();

        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void F32x4_MIN(MStack stack) {
        var v1High = stack.pop();
        var v1Low = stack.pop();

        int offset = stack.size() - 2;

        var v1 =
                LongVector.fromArray(LongVector.SPECIES_128, new long[] {v1Low, v1High}, 0)
                        .reinterpretAsFloats();

        var v2 =
                LongVector.fromArray(LongVector.SPECIES_128, stack.array(), offset)
                        .reinterpretAsFloats();

        var result = v1.min(v2).reinterpretAsLongs().toArray();
        System.arraycopy(result, 0, stack.array(), offset, 2);
    }

    private static void I32x4_TRUNC_SAT_F32x4_S(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        long resultLow = 0L;
        long resultHigh = 0L;

        for (int i = 0; i < 2; i++) {
            var shift = i * 32L;
            resultHigh |=
                    (OpcodeImpl.I32_TRUNC_SAT_F32_S(
                                            Float.intBitsToFloat(
                                                    (int) ((valHigh >> shift) & 0xFFFFFFFFL)))
                                    & 0xFFFFFFFFL)
                            << shift;
            resultLow |=
                    (OpcodeImpl.I32_TRUNC_SAT_F32_S(
                                            Float.intBitsToFloat(
                                                    (int) ((valLow >> shift) & 0xFFFFFFFFL)))
                                    & 0xFFFFFFFFL)
                            << shift;
        }

        stack.push(resultLow);
        stack.push(resultHigh);
    }

    private static void F32x4_CONVERT_I32x4_U(MStack stack) {
        var valHigh = stack.pop();
        var valLow = stack.pop();

        long resultLow = 0L;
        long resultHigh = 0L;

        for (int i = 0; i < 2; i++) {
            var shift = i * 32L;
            resultHigh |=
                    (Float.floatToIntBits(
                                            OpcodeImpl.F32_CONVERT_I32_U(
                                                    (int) ((valHigh >> shift) & 0xFFFFFFFFL)))
                                    & 0xFFFFFFFFL)
                            << shift;
            resultLow |=
                    (Float.floatToIntBits(
                                            OpcodeImpl.F32_CONVERT_I32_U(
                                                    (int) ((valLow >> shift) & 0xFFFFFFFFL)))
                                    & 0xFFFFFFFFL)
                            << shift;
        }

        stack.push(resultLow);
        stack.push(resultHigh);
    }

    private static void I8x16_SWIZZLE(MStack stack) {
        var idxHigh = stack.pop();
        var idxLow = stack.pop();
        var baseHigh = stack.pop();
        var baseLow = stack.pop();

        long resultLow = 0L;
        long resultHigh = 0L;

        for (int i = 0; i < 16; i++) {
            long id = 0;
            if (i < 8) {
                id = (idxLow >> (i * 8)) & 0xFFL;
            } else {
                id = (idxHigh >> ((i - 8) * 8)) & 0xFFL;
            }

            long base;
            if (id < 8) {
                base = (baseLow >> (id * 8)) & 0xFFL;
            } else if (id < 16) {
                base = (baseHigh >> (id * 8)) & 0xFFL;
            } else {
                base = 0x00L;
            }

            if (i < 8) {
                resultLow |= base << (i * 8);
            } else {
                resultHigh |= base << ((i - 8) * 8);
            }
        }

        stack.push(resultLow);
        stack.push(resultHigh);
    }
}
