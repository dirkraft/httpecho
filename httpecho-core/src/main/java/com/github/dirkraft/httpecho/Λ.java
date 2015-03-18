package com.github.dirkraft.httpecho;

import java.util.function.BinaryOperator;

public class Λ {

    public static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u)); };
    }
}
