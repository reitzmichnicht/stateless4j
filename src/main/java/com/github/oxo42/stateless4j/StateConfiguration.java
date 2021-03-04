package com.github.oxo42.stateless4j;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class StateConfiguration<S, T, C> {

    private static final String GUARD_IS_NULL = "guard must not be null";
    private static final String ENTRY_ACTION_IS_NULL = "entryAction must not be null";
    private static final String EXIT_ACTION_IS_NULL = "exitAction must not be null";
    private static final String ACTION_IS_NULL = "action must not be null";
    private static final String TRIGGER_IS_NULL = "trigger must not be null";
    private static final String DESTINATION_STATE_SELECTOR_IS_NULL = "destinationStateSelector must not be null";

    private final StateRepresentation<S, T, C> representation;
    private final Function<S, StateRepresentation<S, T, C>> lookup;

    public StateConfiguration(final StateRepresentation<S, T, C> representation, final Function<S, StateRepresentation<S, T, C>> lookup) {
        Objects.requireNonNull(representation, "representation must not be null");
        Objects.requireNonNull(lookup, "lookup must not be null");
        this.representation = representation;
        this.lookup = lookup;
    }

    /**
     * Accept the specified trigger and transition to the destination state
     *
     * @param trigger The accepted trigger
     * @param destinationState The state that the trigger will cause a transition to
     * @return The receiver
     */
    public StateConfiguration<S, T, C> permit(T trigger, S destinationState) {
        enforceNotIdentityTransition(destinationState);
        return publicPermit(trigger, destinationState);
    }

    /**
     * Accept the specified trigger and transition to the destination state.
     * <p>
     * Additionally a given action is performed when transitioning. This action will be called after
     * the onExit action of the current state and before the onEntry action of
     * the destination state.
     *
     * @param trigger          The accepted trigger
     * @param destinationState The state that the trigger will cause a transition to
     * @param action           The action to be performed "during" transition
     * @return The receiver
     */
    public StateConfiguration<S, T, C> permit(T trigger, S destinationState, final Consumer<C> action) {
        enforceNotIdentityTransition(destinationState);
        return publicPermit(trigger, destinationState, action);
    }

    /**
     * Accept the specified trigger and transition to the destination state if guard is true
     *
     * @param trigger          The accepted trigger
     * @param destinationState The state that the trigger will cause a transition to
     * @param guard            Function that must return true in order for the trigger to be accepted
     * @return The receiver
     */
    public StateConfiguration<S, T, C> permitIf(T trigger, S destinationState, Predicate<C> guard) {
        enforceNotIdentityTransition(destinationState);
        return publicPermitIf(trigger, destinationState, guard);
    }

    /**
     * Accept the specified trigger and transition to the destination state if guard is true
     * <p>
     * Additionally a given action is performed when transitioning. This action will be called after
     * the onExit action of the current state and before the onEntry action of
     * the destination state.
     *
     * @param trigger          The accepted trigger
     * @param destinationState The state that the trigger will cause a transition to
     * @param guard            Function that must return true in order for the trigger to be accepted
     * @param action           The action to be performed "during" transition
     * @return The receiver
     */
    public StateConfiguration<S, T, C> permitIf(T trigger, S destinationState, Predicate<C> guard, Consumer<C> action) {
        enforceNotIdentityTransition(destinationState);
        return publicPermitIf(trigger, destinationState, guard, action);
    }

    /**
     * Accept the specified trigger and transition to the destination state if guard true, otherwise ignore
     *
     * @param trigger          The accepted trigger
     * @param destinationState The state that the trigger will cause a transition to
     * @param guard            Function that must return true in order for the trigger to be accepted
     * @return The receiver
     */
    public StateConfiguration<S, T, C> permitIfElseIgnore(T trigger, S destinationState, final Predicate<C> guard) {
        enforceNotIdentityTransition(destinationState);
        ignoreIf(trigger, guard.negate());
        return publicPermitIf(trigger, destinationState, guard);
    }

    /**
     * Accept the specified trigger and transition to the destination state if guard true, otherwise ignore
     * <p>
     * Additionally a given action is performed when transitioning. This action will be called after
     * the onExit action of the current state and before the onEntry action of
     * the destination state.
     *
     * @param trigger          The accepted trigger
     * @param destinationState The state that the trigger will cause a transition to
     * @param guard            Function that must return true in order for the trigger to be accepted
     * @param action           The action to be performed "during" transition
     * @return The receiver
     */
    public StateConfiguration<S, T, C> permitIfElseIgnore(T trigger, S destinationState, final Predicate<C> guard, Consumer<C> action) {
        enforceNotIdentityTransition(destinationState);
        ignoreIf(trigger, guard.negate());
        return publicPermitIf(trigger, destinationState, guard, action);
    }

    /**
     * Accept the specified trigger, execute action and stay in state
     * <p>
     * Applies to the current state only. No exit or entry actions will be
     * executed and the state will not change. The only thing that happens is
     * the execution of a given action.
     *
     * @param trigger The accepted trigger
     * @param action  The action to be performed
     * @return The receiver
     */
    public StateConfiguration<S, T, C> permitInternal(T trigger, Consumer<C> action) {
        return permitInternalIf(trigger, x -> true, action);
    }

    /**
     * Accept the specified trigger, execute action and stay in state
     * <p>
     * Applies to the current state only. No exit or entry actions will be
     * executed and the state will not change. The only thing that happens is
     * the execution of a given action.
     * <p>
     * The action is only executed if the given guard returns true. Otherwise
     * this transition will not be taken into account (so it does not count
     * as 'ignore', then).
     *
     * @param trigger The accepted trigger
     * @param guard   Function that must return true in order for the trigger to be accepted
     * @param action  The action to be performed
     * @return The receiver
     */
    public StateConfiguration<S, T, C> permitInternalIf(T trigger, Predicate<C> guard, Consumer<C> action) {
        Objects.requireNonNull(guard, GUARD_IS_NULL);
        Objects.requireNonNull(action, ACTION_IS_NULL);
        representation.addTriggerBehaviour(new InternalTriggerBehaviour<>(
            trigger, guard, action));
        return this;
    }

    /**
     * Accept the specified trigger, execute exit actions and re-execute entry actions. Reentry behaves as though the
     * configured state transitions to an identical sibling state
     * <p>
     * Applies to the current state only. Will not re-execute superstate actions, or  cause actions to execute
     * transitioning between super- and sub-states
     *
     * @param trigger The accepted trigger
     * @return The receiver
     */
    public StateConfiguration<S, T, C> permitReentry(T trigger) {
        return publicPermit(trigger, representation.getUnderlyingState());
    }

    /**
     * Accept the specified trigger, execute exit actions and re-execute entry actions. Reentry behaves as though the
     * configured state transitions to an identical sibling state
     * <p>
     * Applies to the current state only. Will not re-execute superstate actions, or  cause actions to execute
     * transitioning between super- and sub-states
     * <p>
     * Additionally a given action is performed when transitioning. This action will be called after
     * the onExit action and before the onEntry action (of the re-entered state).
     *
     * @param trigger The accepted trigger
     * @param action  The action to be performed "during" transition
     * @return The receiver
     */
    public StateConfiguration<S, T, C> permitReentry(T trigger, Consumer<C> action) {
        return publicPermit(trigger, representation.getUnderlyingState(), action);
    }

    /**
     * Accept the specified trigger, execute exit actions and re-execute entry actions. Reentry behaves as though the
     * configured state transitions to an identical sibling state
     * <p>
     * Applies to the current state only. Will not re-execute superstate actions, or  cause actions to execute
     * transitioning between super- and sub-states
     *
     * @param trigger The accepted trigger
     * @param guard   Function that must return true in order for the trigger to be accepted
     * @return The receiver
     */
    public StateConfiguration<S, T, C> permitReentryIf(T trigger, Predicate<C> guard) {
        return publicPermitIf(trigger, representation.getUnderlyingState(), guard);
    }

    /**
     * Accept the specified trigger, execute exit actions and re-execute entry actions. Reentry behaves as though the
     * configured state transitions to an identical sibling state
     * <p>
     * Applies to the current state only. Will not re-execute superstate actions, or  cause actions to execute
     * transitioning between super- and sub-states
     * <p>
     * Additionally a given action is performed when transitioning. This action will be called after
     * the onExit action and before the onEntry action (of the re-entered state).
     *
     * @param trigger The accepted trigger
     * @param guard   Function that must return true in order for the trigger to be accepted
     * @return The receiver
     */
    public StateConfiguration<S, T, C> permitReentryIf(T trigger, Predicate<C> guard, Consumer<C> action) {
        return publicPermitIf(trigger, representation.getUnderlyingState(), guard, action);
    }

    /**
     * ignore the specified trigger when in the configured state
     *
     * @param trigger The trigger to ignore
     * @return The receiver
     */
    public StateConfiguration<S, T, C> ignore(T trigger) {
        return ignoreIf(trigger, x -> true);
    }

    /**
     * ignore the specified trigger when in the configured state, if the guard returns true
     *
     * @param trigger The trigger to ignore
     * @param guard   Function that must return true in order for the trigger to be ignored
     * @return The receiver
     */
    public StateConfiguration<S, T, C> ignoreIf(T trigger, Predicate<C> guard) {
        Objects.requireNonNull(guard, GUARD_IS_NULL);
        representation.addTriggerBehaviour(new InternalTriggerBehaviour<>(trigger, guard, x -> {
        }));
        return this;
    }

    /**
     * Specify an action that will execute when transitioning into the configured state
     *
     * @param entryAction Action to execute
     * @return The receiver
     */
    public StateConfiguration<S, T, C> onEntry(final Consumer<C> entryAction) {
        Objects.requireNonNull(entryAction, ENTRY_ACTION_IS_NULL);
        return onEntry((t, c) -> entryAction.accept(c));
    }

    /**
     * Specify an action that will execute when transitioning into the configured state
     *
     * @param entryAction Action to execute, providing details of the transition
     * @return The receiver
     */
    public StateConfiguration<S, T, C> onEntry(final BiConsumer<Transition<S, T>, C> entryAction) {
        Objects.requireNonNull(entryAction, ENTRY_ACTION_IS_NULL);
        representation.addEntryAction(entryAction);
        return this;
    }

    /**
     * Specify an action that will execute when transitioning into the configured state
     *
     * @param trigger     The trigger by which the state must be entered in order for the action to execute
     * @param entryAction Action to execute
     * @return The receiver
     */
    public StateConfiguration<S, T, C> onEntryFrom(T trigger, final Consumer<C> entryAction) {
        Objects.requireNonNull(entryAction, ENTRY_ACTION_IS_NULL);
        return onEntryFrom(trigger, (t, c) -> entryAction.accept(c));
    }

    /**
     * Specify an action that will execute when transitioning into the configured state
     *
     * @param trigger     The trigger by which the state must be entered in order for the action to execute
     * @param entryAction Action to execute, providing details of the transition
     * @return The receiver
     */
    public StateConfiguration<S, T, C> onEntryFrom(T trigger, final BiConsumer<Transition<S, T>, C> entryAction) {
        Objects.requireNonNull(entryAction, ENTRY_ACTION_IS_NULL);
        representation.addEntryAction(trigger, entryAction);
        return this;
    }

    /**
     * Specify an action that will execute when transitioning from the configured state
     *
     * @param exitAction Action to execute
     * @return The receiver
     */
    public StateConfiguration<S, T, C> onExit(final Consumer<C> exitAction) {
        Objects.requireNonNull(exitAction, EXIT_ACTION_IS_NULL);
        return onExit((t, c) -> exitAction.accept(c));
    }

    /**
     * Specify an action that will execute when transitioning from the configured state
     *
     * @param exitAction Action to execute
     * @return The receiver
     */
    public StateConfiguration<S, T, C> onExit(BiConsumer<Transition<S, T>, C> exitAction) {
        Objects.requireNonNull(exitAction, EXIT_ACTION_IS_NULL);
        representation.addExitAction(exitAction);
        return this;
    }

    /**
     * Sets the superstate that the configured state is a substate of
     * <p>
     * Substates inherit the allowed transitions of their superstate.
     * When entering directly into a substate from outside of the superstate,
     * entry actions for the superstate are executed.
     * Likewise when leaving from the substate to outside the supserstate,
     * exit actions for the superstate will execute.
     *
     * @param superstate The superstate
     * @return The receiver
     */
    public StateConfiguration<S, T, C> substateOf(S superstate) {
        StateRepresentation<S, T, C> superRepresentation = lookup.apply(superstate);
        representation.setSuperstate(superRepresentation);
        superRepresentation.addSubstate(representation);
        return this;
    }

    /**
     * Accept the specified trigger and transition to the destination state, calculated dynamically by the supplied
     * function
     *
     * @param trigger                  The accepted trigger
     * @param destinationStateSelector Function to calculate the state that the trigger will cause a transition to
     * @return The receiver
     */
    //public StateConfiguration<S, T, C> permitDynamic(T trigger, final Func<S> destinationStateSelector) {
    //    return permitDynamicIf(trigger, destinationStateSelector, x -> true);
    //}

    /**
     * Accept the specified trigger and transition to the destination state, calculated dynamically by the supplied
     * function
     * <p>
     * Additionally a given action is performed when transitioning. This action will be called after
     * the onExit action and before the onEntry action (of the re-entered state).
     *
     * @param trigger                  The accepted trigger
     * @param destinationStateSelector Function to calculate the state that the trigger will cause a transition to
     * @param action                   The action to be performed "during" transition
     * @return The receiver
     */
    //public StateConfiguration<S, T, C> permitDynamic(T trigger, final Func<S> destinationStateSelector, Consumer<C> action) {
    //    return permitDynamicIf(trigger, destinationStateSelector, x -> true, action);
    //}

    /**
     * Accept the specified trigger and transition to the destination state, calculated dynamically by the supplied
     * function
     *
     * @param trigger                  The accepted trigger
     * @param destinationStateSelector Function to calculate the state that the trigger will cause a transition to
     * @param guard                    Function that must return true in order for the  trigger to be accepted
     * @return The receiver
     */
    //public StateConfiguration<S, T, C> permitDynamicIf(T trigger, final Func<S> destinationStateSelector, Predicate<C> guard) {
    //    Objects.requireNonNull(destinationStateSelector, DESTINATION_STATE_SELECTOR_IS_NULL);
    //    return publicPermitDynamicIf(trigger, arg0 -> destinationStateSelector.get(), guard, (c) -> {});
    //}

    /**
     * Accept the specified trigger and transition to the destination state, calculated dynamically by the supplied function
     * <p>
     * Additionally a given action is performed when transitioning. This action will be called after the onExit action of the current state and before the onEntry action of the destination state.
     *
     * @param trigger The accepted trigger
     * @param destinationStateSelector Function to calculate the state that the trigger will cause a transition to
     * @param guard Function that must return true in order for the  trigger to be accepted
     * @param action The action to be performed "during" transition
     * @return The receiver
     */
    //public StateConfiguration<S, T, C> permitDynamicIf(T trigger, final Func<S> destinationStateSelector, Predicate<C> guard,
    //    final Consumer<C> action) {
    //    Objects.requireNonNull(destinationStateSelector, DESTINATION_STATE_SELECTOR_IS_NULL);
    //    return publicPermitDynamicIf(trigger, arg0 -> destinationStateSelector.get(), guard, args -> action.accept());
    //}

    void enforceNotIdentityTransition(S destination) {
        if (destination.equals(representation.getUnderlyingState())) {
            throw new IllegalStateException(
                "Permit() (and PermitIf()) require that the destination state is not equal to the source state. To accept a trigger without changing state, use either ignore(), permitInternal() or "
                    + "permitReentry().");
        }
    }

    StateConfiguration<S, T, C> publicPermit(T trigger, S destinationState) {
        return publicPermitIf(trigger, destinationState, x -> true, x -> {
        });
    }

    StateConfiguration<S, T, C> publicPermit(T trigger, S destinationState, Consumer<C> action) {
        return publicPermitIf(trigger, destinationState, x -> true, action);
    }

    StateConfiguration<S, T, C> publicPermitIf(T trigger, S destinationState, Predicate<C> guard) {
        return publicPermitIf(trigger, destinationState, guard, x -> {
        });
    }

    StateConfiguration<S, T, C> publicPermitIf(T trigger, S destinationState, Predicate<C> guard, Consumer<C> action) {
        Objects.requireNonNull(action, ACTION_IS_NULL);
        Objects.requireNonNull(guard, GUARD_IS_NULL);
        representation.addTriggerBehaviour(new TransitioningTriggerBehaviour<>(trigger, destinationState, guard, action));
        return this;
    }

    //StateConfiguration<S, T, C> publicPermitDynamic(T trigger, Func2<Object[], S> destinationStateSelector) {
    //    return publicPermitDynamicIf(trigger, destinationStateSelector, x -> true, (c) -> {});
    //}

    //StateConfiguration<S, T, C> publicPermitDynamicIf(T trigger, Func2<Object[], S> destinationStateSelector, Predicate<C> guard) {
    //    Objects.requireNonNull(destinationStateSelector, DESTINATION_STATE_SELECTOR_IS_NULL);
    //    Objects.requireNonNull(guard, GUARD_IS_NULL);
    //    representation.addTriggerBehaviour(new DynamicTriggerBehaviour<>(trigger, destinationStateSelector, guard, (c) -> {}));
    //    return this;
    //}

    //StateConfiguration<S, T, C> publicPermitDynamicIf(T trigger, Func2<Object[], S> destinationStateSelector, Predicate<C> guard, Consumer<C> action) {
    //    Objects.requireNonNull(destinationStateSelector, DESTINATION_STATE_SELECTOR_IS_NULL);
    //    Objects.requireNonNull(guard, GUARD_IS_NULL);
    //    representation.addTriggerBehaviour(new DynamicTriggerBehaviour<>(trigger, destinationStateSelector, guard, action));
    //    return this;
    //}
}
