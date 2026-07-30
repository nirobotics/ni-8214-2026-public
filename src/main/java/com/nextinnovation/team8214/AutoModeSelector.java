// Copyright (c) 2025-2026 Littleton Robotics
// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214;

import com.nextinnovation.team8214.util.SwitchableChooser;
import com.nextinnovation.team8214.util.VirtualSubsystem;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StringPublisher;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.LoggedDashboardChooser;

public class AutoModeSelector extends VirtualSubsystem {
  private static final int maxQuestions = 2;
  private static final AutoMode defaultMode =
      new AutoMode("Silence", List.of(), null, rs -> Commands.none());

  private record AutoMode(
      String name,
      List<AutoQuestion> questions,
      Function<List<String>, Pose2d> initialPoseBuilder,
      Function<List<String>, Command> modeBuilder) {}

  public record AutoQuestion(String question, List<String> responses) {}

  private final LoggedDashboardChooser<AutoMode> modeChooser;
  private final List<StringPublisher> questionPublishers;
  private final List<SwitchableChooser> questionChoosers;

  private AutoMode lastMode = defaultMode;
  private List<String> lastResponses = List.of();

  public AutoModeSelector(String key) {
    modeChooser = new LoggedDashboardChooser<>(key + "/Mode");
    modeChooser.addDefaultOption(defaultMode.name, defaultMode);

    // Publish questions and choosers
    questionPublishers = new ArrayList<>();
    questionChoosers = new ArrayList<>();
    for (int i = 0; i < maxQuestions; i++) {
      var publisher =
          NetworkTableInstance.getDefault()
              .getStringTopic("/SmartDashboard/" + key + "/Question #" + (i + 1))
              .publish();
      publisher.set("NA");
      questionPublishers.add(publisher);
      questionChoosers.add(new SwitchableChooser(key + "/Question #" + (i + 1) + " Chooser"));
    }
  }

  @Override
  public void periodic() {
    if (DriverStation.isAutonomousEnabled() && lastMode != null && lastResponses != null) {
      return;
    }

    var selectedMode = modeChooser.get();
    if (selectedMode == null) {
      return;
    }

    var modeChanged = !selectedMode.equals(lastMode);
    if (modeChanged) {
      System.out.println("Auto mode switched -> " + selectedMode.name);
      var questions = selectedMode.questions();
      for (int i = 0; i < maxQuestions; i++) {
        if (i < questions.size()) {
          questionPublishers.get(i).set(questions.get(i).question());
          questionChoosers.get(i).setOptions(questions.get(i).responses().toArray(String[]::new));
        } else {
          questionPublishers.get(i).set("");
          questionChoosers.get(i).setOptions(new String[] {});
        }
      }
    }

    var responses = new ArrayList<String>();
    for (int i = 0; i < selectedMode.questions().size(); i++) {
      var response = questionChoosers.get(i).get();
      responses.add(
          response == null ? selectedMode.questions().get(i).responses().get(0) : response);
    }

    if (shouldResetPoseForSelectedMode(
        Config.MODE,
        modeChanged,
        DriverStation.isDisabled(),
        selectedMode.initialPoseBuilder != null)) {
      Odometry.getInstance().resetPose(selectedMode.initialPoseBuilder.apply(responses));
    }

    lastMode = selectedMode;
    lastResponses = responses;
    Logger.recordOutput("AutoModeSelector/Responses", lastResponses.toArray(String[]::new));
  }

  public void addMode(String name, Command mode) {
    addMode(name, rs -> mode);
  }

  public void addMode(String name, Function<List<String>, Command> modeBuilder) {
    addMode(name, List.of(), modeBuilder);
  }

  public void addMode(
      String name, List<AutoQuestion> questions, Function<List<String>, Command> modeBuilder) {
    addMode(name, questions, null, modeBuilder);
  }

  public void addMode(
      String name,
      List<AutoQuestion> questions,
      Function<List<String>, Pose2d> initialPoseBuilder,
      Function<List<String>, Command> modeBuilder) {
    modeChooser.addOption(name, new AutoMode(name, questions, initialPoseBuilder, modeBuilder));
  }

  public Command getCmd() {
    System.out.println("Auto mode built " + lastMode.name);
    return lastMode.modeBuilder.apply(lastResponses);
  }

  static boolean shouldResetPoseForSelectedMode(
      Config.Mode mode, boolean modeChanged, boolean disabled, boolean hasInitialPose) {
    return mode == Config.Mode.SIM && modeChanged && disabled && hasInitialPose;
  }
}
