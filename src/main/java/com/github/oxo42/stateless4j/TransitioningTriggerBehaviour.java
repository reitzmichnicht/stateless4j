package com.github.oxo42.stateless4j;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class TransitioningTriggerBehaviour<S, T, C> extends TriggerBehaviour<S, T, C> {

    private final S destination;
    private final Consumer<C> action;

    public TransitioningTriggerBehaviour(T trigger, S destination, Predicate<C> guard, Consumer<C> action) {
        super(trigger, guard);
        this.destination = destination;
        this.action = action;
    }

    @Override
    public void performAction(C context) {
        action.accept(context);
    }

    @Override
    public S transitionsTo(S source, C context) {
        return destination;
    }
}
