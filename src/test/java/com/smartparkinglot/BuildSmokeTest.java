package com.smartparkinglot;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BuildSmokeTest {
    @Test
    void runtimeSupportsJava17() {
        assertTrue(Runtime.version().feature() >= 17);
    }
}
