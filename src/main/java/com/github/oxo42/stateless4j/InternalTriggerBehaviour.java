package com.github.oxo42.stateless4j;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class InternalTriggerBehaviour<S, T, C> extends TriggerBehaviour<S, T, C> {

  private final Consumer<C> action;

  public InternalTriggerBehaviour(T trigger, Predicate<C> guard, Consumer<C> action) {
    super(trigger, guard);
    this.action = action;
  }

  @Override
  public void performAction(C context) {
    action.accept(context);
  }

  @Override
  public boolean isInternal() {
    return true;
  }

  @Override
  public S transitionsTo(S source, C context) {
    return source;
  }
}
