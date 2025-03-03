package com.dylibso.chicory.wasm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class WasmModuleTest {

    @Test
    public void shouldHaveTheSameHashCode() {
        var mod1 =
                Parser.parse(
                        WasmModuleTest.class.getResourceAsStream("/compiled/count_vowels.rs.wasm"));
        var mod2 =
                Parser.parse(
                        WasmModuleTest.class.getResourceAsStream("/compiled/count_vowels.rs.wasm"));

        assertEquals(mod1.hashCode(), mod2.hashCode());
    }

    @Test
    public void shouldBeEquals() {
        var mod1 =
                Parser.parse(
                        WasmModuleTest.class.getResourceAsStream("/compiled/count_vowels.rs.wasm"));
        var mod2 =
                Parser.parse(
                        WasmModuleTest.class.getResourceAsStream("/compiled/count_vowels.rs.wasm"));

        assertTrue(mod1.equals(mod2));
    }
}
