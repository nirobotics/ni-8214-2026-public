// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.agent;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.littletonrobotics.junction.Logger;

public final class AgentAutoModeSelector {
  private static final AgentAutoMode DEFAULT_MODE =
      new AgentAutoMode("Silence", List.of(), responses -> Commands.none());

  private record AgentAutoMode(
      String name,
      List<AgentAutoQuestion> questions,
      Function<List<String>, Command> modeBuilder) {}

  public record AgentAutoQuestion(String question, List<String> responses) {}

  private final List<AgentAutoMode> modes = new ArrayList<>();

  private AgentAutoMode selectedMode = DEFAULT_MODE;
  private List<String> selectedResponses = List.of();

  public AgentAutoModeSelector() {
    modes.add(DEFAULT_MODE);
  }

  public void addMode(String name, Command mode) {
    addMode(name, responses -> mode);
  }

  public void addMode(String name, Function<List<String>, Command> modeBuilder) {
    addMode(name, List.of(), modeBuilder);
  }

  public void addMode(
      String name, List<AgentAutoQuestion> questions, Function<List<String>, Command> modeBuilder) {
    modes.add(new AgentAutoMode(name, questions, modeBuilder));
  }

  public void selectMode(String name, List<String> responses) {
    var nextMode =
        modes.stream()
            .filter(mode -> mode.name().equals(name))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Unknown agent auto mode: "
                            + name
                            + ". Available modes: "
                            + modes.stream().map(AgentAutoMode::name).toList()));

    selectedMode = nextMode;
    selectedResponses = selectResponses(nextMode, responses);

    Logger.recordOutput("agentAutoModeSelector/autoName", selectedMode.name());
    Logger.recordOutput(
        "agentAutoModeSelector/responses", selectedResponses.toArray(String[]::new));
    System.out.println("Agent auto mode selected -> " + selectedMode.name());
  }

  public Command getCmd() {
    Logger.recordOutput("agentAutoModeSelector/autoName", selectedMode.name());
    Logger.recordOutput(
        "agentAutoModeSelector/responses", selectedResponses.toArray(String[]::new));
    System.out.println("Agent auto mode built " + selectedMode.name());
    return selectedMode.modeBuilder().apply(selectedResponses);
  }

  private static List<String> selectResponses(AgentAutoMode mode, List<String> responses) {
    var selected = new ArrayList<String>();

    for (var i = 0; i < mode.questions().size(); i++) {
      var question = mode.questions().get(i);
      var response =
          i < responses.size() && !responses.get(i).isBlank()
              ? responses.get(i)
              : question.responses().get(0);
      if (!question.responses().contains(response)) {
        throw new IllegalArgumentException(
            "Invalid response for "
                + question.question()
                + ": "
                + response
                + ". Available responses: "
                + question.responses());
      }
      selected.add(response);
    }

    return List.copyOf(selected);
  }
}
