package com.github.oxo42.stateless4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

public class StateRepresentation<S, T, C> {

    private static final String ACTION_IS_NULL = "action must not be null";
    private static final String TRANSITION_IS_NULL = "transition must not be null";
    private final S state;

    private final Map<T, List<TriggerBehaviour<S, T, C>>> triggerBehaviours = new HashMap<>();
    private final List<BiConsumer<Transition<S, T>, C>> entryActions = new ArrayList<>();
    private final List<BiConsumer<Transition<S, T>, C>> exitActions = new ArrayList<>();
    private final List<StateRepresentation<S, T, C>> substates = new ArrayList<>();
    private StateRepresentation<S, T, C> superstate; // null

    public StateRepresentation(S state) {
        this.state = state;
    }

    protected Map<T, List<TriggerBehaviour<S, T, C>>> getTriggerBehaviours() {
        return triggerBehaviours;
    }

    public Boolean canHandle(T trigger, C context) {
        return tryFindHandler(trigger, context) != null;
    }

    public TriggerBehaviour<S, T, C> tryFindHandler(T trigger, C context) {
        TriggerBehaviour<S, T, C> result = tryFindLocalHandler(trigger, context);
        if (result == null && superstate != null) {
            result = superstate.tryFindHandler(trigger, context);
        }
        return result;
    }

    TriggerBehaviour<S, T, C> tryFindLocalHandler(T trigger, C context) {
        List<TriggerBehaviour<S, T, C>> possible = triggerBehaviours.get(trigger);
        if (possible == null) {
            return null;
        }

        List<TriggerBehaviour<S, T, C>> actual = new ArrayList<>();
        for (TriggerBehaviour<S, T, C> triggerBehaviour : possible) {
            if (triggerBehaviour.isGuardConditionMet(context)) {
                actual.add(triggerBehaviour);
            }
        }

        if (actual.size() > 1) {
            throw new IllegalStateException("Multiple permitted exit transitions are configured from state '" + state + "' for trigger '" + trigger + "'. Guard clauses must be mutually exclusive.");
        }

        return actual.isEmpty() ? null : actual.get(0);
    }

    public void addEntryAction(final T trigger, final BiConsumer<Transition<S, T>, C> action) {
        Objects.requireNonNull(action, ACTION_IS_NULL);

        entryActions.add((t, c) -> {
            T trans_trigger = t.getTrigger();
            if (trans_trigger != null && trans_trigger.equals(trigger)) {
                action.accept(t, c);
            }
        });
    }

    public void addEntryAction(BiConsumer<Transition<S, T>, C> action) {
        Objects.requireNonNull(action, ACTION_IS_NULL);
        entryActions.add(action);
    }

    public void insertEntryAction(BiConsumer<Transition<S, T>, C> action) {
        Objects.requireNonNull(action, ACTION_IS_NULL);
        entryActions.add(0, action);
    }

    public void addExitAction(BiConsumer<Transition<S, T>, C> action) {
        Objects.requireNonNull(action, ACTION_IS_NULL);
        exitActions.add(action);
    }

    public void enter(Transition<S, T> transition, C context) {
        Objects.requireNonNull(transition, TRANSITION_IS_NULL);

        if (transition.isReentry()) {
            executeEntryActions(transition, context);
        } else if (!includes(transition.getSource())) {
            if (superstate != null) {
                superstate.enter(transition, context);
            }

            executeEntryActions(transition, context);
        }
    }

    public void exit(Transition<S, T> transition, C context) {
        Objects.requireNonNull(transition, TRANSITION_IS_NULL);

        if (transition.isReentry()) {
            executeExitActions(transition, context);
        } else if (!includes(transition.getDestination())) {
            executeExitActions(transition, context);
            if (superstate != null) {
                superstate.exit(transition, context);
            }
        }
    }

    void executeEntryActions(Transition<S, T> transition, C context) {
        Objects.requireNonNull(transition, TRANSITION_IS_NULL);
        for (BiConsumer<Transition<S, T>, C> action : entryActions) {
            action.accept(transition, context);
        }
    }

    void executeExitActions(Transition<S, T> transition, C context) {
        Objects.requireNonNull(transition, TRANSITION_IS_NULL);
        for (BiConsumer<Transition<S, T>, C> action : exitActions) {
            action.accept(transition, context);
        }
    }

    public void addTriggerBehaviour(TriggerBehaviour<S, T, C> triggerBehaviour) {
        List<TriggerBehaviour<S, T, C>> allowed;
        if (!triggerBehaviours.containsKey(triggerBehaviour.getTrigger())) {
            allowed = new ArrayList<>();
            triggerBehaviours.put(triggerBehaviour.getTrigger(), allowed);
        }
        allowed = triggerBehaviours.get(triggerBehaviour.getTrigger());
        allowed.add(triggerBehaviour);
    }

    public StateRepresentation<S, T, C> getSuperstate() {
        return superstate;
    }

    public void setSuperstate(StateRepresentation<S, T, C> value) {
        superstate = value;
    }

    public S getUnderlyingState() {
        return state;
    }

    public void addSubstate(StateRepresentation<S, T, C> substate) {
        Objects.requireNonNull(substate, "substate must not be null");
        substates.add(substate);
    }

    public boolean includes(S stateToCheck) {
        for (StateRepresentation<S, T, C> s : substates) {
            if (s.includes(stateToCheck)) {
                return true;
            }
        }
        return this.state.equals(stateToCheck);
    }

    public boolean isIncludedIn(S stateToCheck) {
        return this.state.equals(stateToCheck) || (superstate != null && superstate.isIncludedIn(stateToCheck));
    }

    public List<T> getPermittedTriggers(C context) {
        Set<T> result = new HashSet<>();

        for (T t : triggerBehaviours.keySet()) {
            for (TriggerBehaviour<S, T, C> v : triggerBehaviours.get(t)) {
                if (v.isGuardConditionMet(context)) {
                    result.add(t);
                    break;
                }
            }
        }

        if (getSuperstate() != null) {
            result.addAll(getSuperstate().getPermittedTriggers(context));
        }

        return new ArrayList<>(result);
    }
}
