package com.nilambar.math;

/**
 * Provides methods to compute the factorial of a non-negative integer
 * using iterative, recursive, and stream-based approaches.
 */
public class Factorial {

    private Factorial() {
        // utility class
    }

    /**
     * Computes factorial iteratively.
     *
     * @param n a non-negative integer (0 ≤ n ≤ 20)
     * @return n!
     * @throws IllegalArgumentException if n is negative or greater than 20
     */
    public static long iterative(int n) {
        validateInput(n);
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    /**
     * Computes factorial recursively.
     *
     * @param n a non-negative integer (0 ≤ n ≤ 20)
     * @return n!
     * @throws IllegalArgumentException if n is negative or greater than 20
     */
    public static long recursive(int n) {
        validateInput(n);
        return recursiveHelper(n);
    }

    private static long recursiveHelper(int n) {
        if (n <= 1) {
            return 1;
        }
        return n * recursiveHelper(n - 1);
    }

    /**
     * Computes factorial using Java streams.
     *
     * @param n a non-negative integer (0 ≤ n ≤ 20)
     * @return n!
     * @throws IllegalArgumentException if n is negative or greater than 20
     */
    public static long stream(int n) {
        validateInput(n);
        if (n <= 1) {
            return 1;
        }
        return java.util.stream.LongStream.rangeClosed(2, n).reduce(1, (a, b) -> a * b);
    }

    private static void validateInput(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative, got: " + n);
        }
        if (n > 20) {
            throw new IllegalArgumentException("n must be ≤ 20 to avoid long overflow, got: " + n);
        }
    }
}
