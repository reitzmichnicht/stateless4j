package com.github.oxo42.stateless4j;

import java.util.function.Predicate;

public abstract class TriggerBehaviour<S, T, C> {

    private final T trigger;

    /**
     * Note that this guard gets called quite often, and sometimes multiple times per fire() call. Thus, it should not be anything performance intensive.
     */
    private final Predicate<C> guard;

    protected TriggerBehaviour(T trigger, Predicate<C> guard) {
        this.trigger = trigger;
        this.guard = guard;
    }

    public T getTrigger() {
        return trigger;
    }

    public abstract void performAction(C context);

    public boolean isInternal() {
        return false;
    }

    public boolean isGuardConditionMet(C context) {
        return guard.test(context);
    }

    public abstract S transitionsTo(S source, C context);
}
