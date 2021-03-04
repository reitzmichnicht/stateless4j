package com.github.oxo42.stateless4j;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The state machine configuration. Reusable.
 */
public class StateMachineConfig<S, T, C> {

  private final Map<S, StateRepresentation<S, T, C>> stateConfiguration = new HashMap<>();

  /**
   * Return StateRepresentation for the specified state. May return null.
   *
   * @param state The state
   * @return StateRepresentation for the specified state, or null.
   */
  public StateRepresentation<S, T, C> getRepresentation(S state) {
    return stateConfiguration.get(state);
  }

  /**
   * Return StateRepresentation for the specified state. Creates representation if it does not exist.
   *
   * @param state The state
   * @return StateRepresentation for the specified state.
   */
  private StateRepresentation<S, T, C> getOrCreateRepresentation(S state) {
    return stateConfiguration.computeIfAbsent(state, StateRepresentation::new);
  }

  /**
   * Begin configuration of the entry/exit actions and allowed transitions when the state machine is in a particular state
   *
   * @param state The state to configure
   * @return A configuration object through which the state can be configured
   */
  public StateConfiguration<S, T, C> configure(S state) {
    return new StateConfiguration<>(getOrCreateRepresentation(state), this::getOrCreateRepresentation);
  }

  public void generateDotFileInto(final OutputStream dotFile) throws IOException {
    generateDotFileInto(dotFile, false);
  }

  public void generateDotFileInto(final OutputStream dotFile, boolean printLabels) throws IOException {
    try (OutputStreamWriter w = new OutputStreamWriter(dotFile, StandardCharsets.UTF_8)) {
      PrintWriter writer = new PrintWriter(w);
      writer.write("digraph G {\n");
      for (Map.Entry<S, StateRepresentation<S, T, C>> entry : this.stateConfiguration.entrySet()) {
        Map<T, List<TriggerBehaviour<S, T, C>>> behaviours = entry.getValue().getTriggerBehaviours();
        for (Map.Entry<T, List<TriggerBehaviour<S, T, C>>> behaviour : behaviours.entrySet()) {
          for (TriggerBehaviour<S, T, C> triggerBehaviour : behaviour.getValue()) {
            if (triggerBehaviour instanceof TransitioningTriggerBehaviour) {
              S destination = triggerBehaviour.transitionsTo(null, null);
              if (printLabels) {
                writer.write(String.format("\t%s -> %s [label = \"%s\" ];\n", entry.getKey(), destination, triggerBehaviour.getTrigger()));
              } else {
                writer.write(String.format("\t%s -> %s;\n", entry.getKey(), destination));
              }
            }
          }
        }
      }
      writer.write("}");
    }
    }

}
