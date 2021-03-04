package com.github.oxo42.stateless4j.delegates;

import java.util.function.Supplier;

/**
 * Represents a function that accepts no input and produces a result
 *
 * @param <R> Result type
 */
@FunctionalInterface
public interface Func<R> extends Supplier<R> {

}
