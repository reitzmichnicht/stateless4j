package com.github.oxo42.stateless4j.delegates;

import java.util.function.Function;

/**
 * Represents a function that accepts an input and produces a result
 *
 * @param <T1> Input argument type
 * @param <R> Result type
 */
@FunctionalInterface
public interface Func2<T1, R> extends Function<T1, R> {

}
