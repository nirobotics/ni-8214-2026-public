// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.agent;

import com.nextinnovation.team8214.Field;
import com.nextinnovation.team8214.subsystem.hopper.Hopper;
import com.nextinnovation.team8214.subsystem.indexer.Indexer;
import com.nextinnovation.team8214.subsystem.intake.Intake;
import com.nextinnovation.team8214.subsystem.shooter.Shooter;
import com.nextinnovation.team8214.subsystem.swerve.Swerve;
import java.util.List;

public final class AgentAutoModes {
  private final AgentAutoCommands agentAutoCommands;

  public AgentAutoModes(
      Swerve swerve, Intake intake, Indexer indexer, Hopper hopper, Shooter shooter) {
    agentAutoCommands = new AgentAutoCommands(swerve, intake, indexer, hopper, shooter);
  }

  public void addDefaultModes(AgentAutoModeSelector agentAutoModeSelector) {
    agentAutoModeSelector.addMode(
        "1LB.left2DoubleSweepBump",
        List.of(
            new AgentAutoModeSelector.AgentAutoQuestion(
                "1st sweep?", List.of("short", "long", "scrum")),
            new AgentAutoModeSelector.AgentAutoQuestion("2nd sweep?", List.of("long", "short"))),
        responses ->
            agentAutoCommands
                .left2DoubleSweepBump(responses)
                .withName("Agent 1LB Left Double Sweep Bump"));

    agentAutoModeSelector.addMode(
        "1RB.right2DoubleSweepBump",
        List.of(
            new AgentAutoModeSelector.AgentAutoQuestion(
                "1st sweep?", List.of("short", "long", "scrum")),
            new AgentAutoModeSelector.AgentAutoQuestion("2nd sweep?", List.of("long", "short"))),
        responses ->
            agentAutoCommands
                .right2DoubleSweepBump(responses)
                .withName("Agent 1RB Right Double Sweep Bump"));

    agentAutoModeSelector.addMode(
        "2.left2Sweep2Depot",
        agentAutoCommands.left2Sweep2Depot().withName("Agent 2 Left Sweep To Depot"));

    agentAutoModeSelector.addMode(
        "3.leftClose2Depot",
        agentAutoCommands.leftCloseStart2Depot().withName("Agent 3 Left Close To Depot"));

    agentAutoModeSelector.addMode(
        "4L.leftClose2Sweep2BumpShoot2Depot",
        List.of(new AgentAutoModeSelector.AgentAutoQuestion("End dash?", List.of("no", "yes"))),
        responses ->
            agentAutoCommands
                .leftCloseStart2Sweep2BumpShoot2Depot(responses)
                .withName("Agent 4L Left Close Sweep Bump Depot"));

    agentAutoModeSelector.addMode(
        "4R.rightClose2Sweep2BumpShoot",
        agentAutoCommands
            .rightCloseStart2Sweep2BumpShoot()
            .withName("Agent 4R Right Close Sweep Bump Shoot"));

    agentAutoModeSelector.addMode(
        "agent.scoreFromLeftCloseStart",
        agentAutoCommands
            .scoreFromPose(Field.LEFT_CLOSE_START, 4.0)
            .withName("Agent Score From Left Close Start"));

    agentAutoModeSelector.addMode(
        "agent.leftClose2Depot",
        agentAutoCommands.leftClose2Depot().withName("Agent Left Close To Depot"));

    agentAutoModeSelector.addMode(
        "agent.bumpThrough", agentAutoCommands.bumpThrough().withName("Agent Bump Through"));

    agentAutoModeSelector.addMode(
        "agent.bumpThroughAfterStick",
        agentAutoCommands.bumpThroughAfterStick().withName("Agent Bump Through After Stick"));

    agentAutoModeSelector.addMode(
        "agent.longestTrajectory",
        agentAutoCommands.longestTrajectory().withName("Agent Longest Trajectory"));
  }
}
