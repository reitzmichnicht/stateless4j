package com.github.oxo42.stateless4j;

import com.github.oxo42.stateless4j.delegates.Trace;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Models behaviour as transitions between a finite set of states
 *
 * @param <S> The type used to represent the states
 * @param <T> The type used to represent the triggers that cause state transitions
 */
public class StateMachine<S, T, C> {

    protected final StateMachineConfig<S, T, C> config;
    protected final Supplier<S> stateAccessor;
    protected final Consumer<S> stateMutator;

    protected final C context;
    private Trace<S, T> trace = null;
    private boolean isStarted = false;
    private S initialState;

    protected BiConsumer<S, T> unhandledTriggerAction = (state, trigger) -> {
        throw new IllegalStateException(
            String.format(
                "No valid leaving transitions are permitted from state '%s' for trigger '%s'. Consider ignoring the trigger.",
                state, trigger)
        );
    };

    /**
     * Construct a state machine
     *
     * @param initialState The initial state
     */
    public StateMachine(S initialState, C context) {
        this(initialState, context, new StateMachineConfig<>());
    }

    /**
     * Construct a state machine
     *
     * @param initialState The initial state
     * @param config       State machine configuration
     */
    public StateMachine(S initialState, C context, StateMachineConfig<S, T, C> config) {
        this.initialState = initialState;
        this.config = config;
        this.context = context;
        final StateReference<S> reference = new StateReference<>();
        reference.setState(initialState);
        stateAccessor = reference::getState;
        stateMutator = reference::setState;
    }

    /**
     * Construct a state machine with external state storage.
     *
     * @param initialState  The initial state
     * @param stateAccessor State accessor
     * @param stateMutator  State mutator
     */
    public StateMachine(S initialState, C context, Supplier<S> stateAccessor, Consumer<S> stateMutator, StateMachineConfig<S, T, C> config) {
        this.config = config;
        this.context = context;
        this.stateAccessor = stateAccessor;
        this.stateMutator = stateMutator;
        stateMutator.accept(initialState);
    }

    /**
     * Fire initial transition into the initial state.
     * All super-states are entered too.
     *
     * This method can be called only once, before state machine is used.
     */
    public void fireInitialTransition() {
        S currentState = getCurrentRepresentation().getUnderlyingState();
        if (isStarted || !currentState.equals(initialState)) {
            throw new IllegalStateException("Firing initial transition after state machine has been started");
        }
        isStarted = true;
        Transition<S, T> initialTransition = new Transition<>(null, currentState, null);
        getCurrentRepresentation().enter(initialTransition, context);
    }

    public StateConfiguration<S, T, C> configure(S state) {
        return config.configure(state);
    }

    public StateMachineConfig<S, T, C> configuration() {
        return config;
    }

    public C getContext() {
        return context;
    }

    /**
     * The current state
     *
     * @return The current state
     */
    public S getState() {
        return stateAccessor.get();
    }

    private void setState(S value) {
        stateMutator.accept(value);
    }

    /**
     * The currently-permissible trigger values
     *
     * @return The currently-permissible trigger values
     */
    public List<T> getPermittedTriggers() {
        return getCurrentRepresentation().getPermittedTriggers(context);
    }

    StateRepresentation<S, T, C> getCurrentRepresentation() {
        StateRepresentation<S, T, C> representation = config.getRepresentation(getState());
        return representation == null ? new StateRepresentation<>(getState()) : representation;
    }

    /**
     * Transition from the current state via the specified trigger. The target state is determined by the configuration of the current state. Actions associated with leaving the current state and
     * entering the new one will be invoked
     *
     * @param trigger The trigger to fire
     */
    public void fire(T trigger) {
        publicFire(trigger);
    }

    protected void publicFire(T trigger) {
        isStarted = true;
        if (trace != null) {
            trace.trigger(trigger);
        }

        TriggerBehaviour<S, T, C> triggerBehaviour = getCurrentRepresentation().tryFindHandler(trigger, context);
        if (triggerBehaviour == null) {
            unhandledTriggerAction.accept(getCurrentRepresentation().getUnderlyingState(), trigger);
            return;
        }

        if (triggerBehaviour.isInternal()) {
            triggerBehaviour.performAction(context);
        } else {
            S source = getState();
            S destination = triggerBehaviour.transitionsTo(source, context);
            Transition<S, T> transition = new Transition<>(source, destination, trigger);

            getCurrentRepresentation().exit(transition, context);
            triggerBehaviour.performAction(context);
            setState(destination);
            getCurrentRepresentation().enter(transition, context);
            if (trace != null) {
                trace.transition(trigger, source, destination);
            }
        }
    }

    /**
     * Override the default behaviour of throwing an exception when an unhandled trigger is fired
     *
     * @param unhandledTriggerAction An action to call when an unhandled trigger is fired
     */
    public void onUnhandledTrigger(final BiConsumer<S, T> unhandledTriggerAction) {
        Objects.requireNonNull(unhandledTriggerAction, "unhandledTriggerAction must not be null");
        this.unhandledTriggerAction = unhandledTriggerAction::accept;
    }

    /**
     * Determine if the state machine is in the supplied state
     *
     * @param state The state to test for
     * @return True if the current state is equal to, or a substate of, the supplied state
     */
    public boolean isInState(S state) {
        return getCurrentRepresentation().isIncludedIn(state);
    }

    /**
     * Returns true if {@code trigger} can be fired  in the current state
     *
     * @param trigger Trigger to test
     * @return True if the trigger can be fired, false otherwise
     */
    public boolean canFire(T trigger) {
        return getCurrentRepresentation().canHandle(trigger, context);
    }

    /**
     * Set tracer delegate. Set trace delegate to investigate what the state machine is doing
     * at runtime. Trace delegate will be called on {@link #fire(Object)} and on transition.
     *
     * @param trace Trace delegate or null, if trace should be disabled
     */
    public void setTrace(Trace<S, T> trace) {
        this.trace = trace;
    }

    /**
     * A human-readable representation of the state machine
     *
     * @return A description of the current state and permitted triggers
     */
    @Override
    public String toString() {
        List<String> parameters = new ArrayList<>();

        for (T trigger : getPermittedTriggers()) {
            parameters.add(trigger.toString());
        }

        return String.format(
            "StateMachine {{ State = %s, PermittedTriggers = {{ %s }}}}",
            getState(),
            String.join(",", parameters));
    }
}
