package com.github.oxo42.stateless4j;

import com.github.oxo42.stateless4j.delegates.Func2;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DynamicTriggerBehaviour<S, T, C> extends TriggerBehaviour<S, T, C> {

    private final Func2<C, S> destination;
    private final Consumer<C> action;

    public DynamicTriggerBehaviour(T trigger, Func2<C, S> destination, Predicate<C> guard, Consumer<C> action) {
        super(trigger, guard);
        Objects.requireNonNull(destination, "destination most not be null");
        this.destination = destination;
        this.action = action;
    }

    @Override
    public void performAction(C context) {
        action.accept(context);
    }

    @Override
    public S transitionsTo(S source, C context) {
        return destination.apply(context);
    }
}
