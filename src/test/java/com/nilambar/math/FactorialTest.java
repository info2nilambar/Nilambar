package com.nilambar.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Factorial")
class FactorialTest {

    // ── Iterative ────────────────────────────────────────────────

    @Nested
    @DisplayName("iterative()")
    class Iterative {

        @Test
        @DisplayName("0! = 1")
        void zeroReturnsOne() {
            assertEquals(1, Factorial.iterative(0));
        }

        @Test
        @DisplayName("1! = 1")
        void oneReturnsOne() {
            assertEquals(1, Factorial.iterative(1));
        }

        @ParameterizedTest(name = "{0}! = {1}")
        @CsvSource({
            "2, 2",
            "3, 6",
            "4, 24",
            "5, 120",
            "10, 3628800",
            "15, 1307674368000",
            "20, 2432902008176640000"
        })
        @DisplayName("known values")
        void knownValues(int n, long expected) {
            assertEquals(expected, Factorial.iterative(n));
        }

        @ParameterizedTest(name = "negative input {0}")
        @ValueSource(ints = {-1, -5, Integer.MIN_VALUE})
        @DisplayName("throws on negative input")
        void throwsOnNegative(int n) {
            IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> Factorial.iterative(n));
            assertEquals("n must be non-negative, got: " + n, ex.getMessage());
        }

        @ParameterizedTest(name = "overflow input {0}")
        @ValueSource(ints = {21, 100, Integer.MAX_VALUE})
        @DisplayName("throws on overflow input")
        void throwsOnOverflow(int n) {
            IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> Factorial.iterative(n));
            assertEquals("n must be ≤ 20 to avoid long overflow, got: " + n, ex.getMessage());
        }
    }

    // ── Recursive ────────────────────────────────────────────────

    @Nested
    @DisplayName("recursive()")
    class Recursive {

        @Test
        @DisplayName("0! = 1")
        void zeroReturnsOne() {
            assertEquals(1, Factorial.recursive(0));
        }

        @Test
        @DisplayName("1! = 1")
        void oneReturnsOne() {
            assertEquals(1, Factorial.recursive(1));
        }

        @ParameterizedTest(name = "{0}! = {1}")
        @CsvSource({
            "2, 2",
            "3, 6",
            "4, 24",
            "5, 120",
            "10, 3628800",
            "15, 1307674368000",
            "20, 2432902008176640000"
        })
        @DisplayName("known values")
        void knownValues(int n, long expected) {
            assertEquals(expected, Factorial.recursive(n));
        }

        @ParameterizedTest(name = "negative input {0}")
        @ValueSource(ints = {-1, -5, Integer.MIN_VALUE})
        @DisplayName("throws on negative input")
        void throwsOnNegative(int n) {
            IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> Factorial.recursive(n));
            assertEquals("n must be non-negative, got: " + n, ex.getMessage());
        }

        @ParameterizedTest(name = "overflow input {0}")
        @ValueSource(ints = {21, 100, Integer.MAX_VALUE})
        @DisplayName("throws on overflow input")
        void throwsOnOverflow(int n) {
            IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> Factorial.recursive(n));
            assertEquals("n must be ≤ 20 to avoid long overflow, got: " + n, ex.getMessage());
        }
    }

    // ── Stream ───────────────────────────────────────────────────

    @Nested
    @DisplayName("stream()")
    class Stream {

        @Test
        @DisplayName("0! = 1")
        void zeroReturnsOne() {
            assertEquals(1, Factorial.stream(0));
        }

        @Test
        @DisplayName("1! = 1")
        void oneReturnsOne() {
            assertEquals(1, Factorial.stream(1));
        }

        @ParameterizedTest(name = "{0}! = {1}")
        @CsvSource({
            "2, 2",
            "3, 6",
            "4, 24",
            "5, 120",
            "10, 3628800",
            "15, 1307674368000",
            "20, 2432902008176640000"
        })
        @DisplayName("known values")
        void knownValues(int n, long expected) {
            assertEquals(expected, Factorial.stream(n));
        }

        @ParameterizedTest(name = "negative input {0}")
        @ValueSource(ints = {-1, -5, Integer.MIN_VALUE})
        @DisplayName("throws on negative input")
        void throwsOnNegative(int n) {
            IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> Factorial.stream(n));
            assertEquals("n must be non-negative, got: " + n, ex.getMessage());
        }

        @ParameterizedTest(name = "overflow input {0}")
        @ValueSource(ints = {21, 100, Integer.MAX_VALUE})
        @DisplayName("throws on overflow input")
        void throwsOnOverflow(int n) {
            IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> Factorial.stream(n));
            assertEquals("n must be ≤ 20 to avoid long overflow, got: " + n, ex.getMessage());
        }
    }

    // ── Cross-method consistency ─────────────────────────────────

    @ParameterizedTest(name = "all methods agree for n={0}")
    @ValueSource(ints = {0, 1, 2, 5, 10, 15, 20})
    @DisplayName("all three implementations return the same result")
    void allMethodsAgree(int n) {
        long iterative = Factorial.iterative(n);
        long recursive = Factorial.recursive(n);
        long stream = Factorial.stream(n);
        assertEquals(iterative, recursive, "iterative vs recursive for n=" + n);
        assertEquals(iterative, stream, "iterative vs stream for n=" + n);
    }
}
