// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.agent;

import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.util.LoggerUtil;
import edu.wpi.first.hal.AllianceStationID;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import edu.wpi.first.wpilibj2.command.Command;
import java.util.ArrayList;
import java.util.List;
import org.littletonrobotics.junction.Logger;

public final class AgentAutoRunner {
  private static final boolean ENABLED = Boolean.parseBoolean(getEnv("WANT_AGENT_AUTO", "false"));
  private static final String AUTO_NAME = getEnv("AGENT_AUTO_NAME", "Silence").trim();
  private static final double START_DELAY_SEC = 0.25;
  private static final double FINISH_DELAY_SEC = 0.25;
  private static final double TIMEOUT_SEC = getDoubleEnv("AGENT_AUTO_TIMEOUT_SEC", 15.0);

  private static boolean started = false;
  private static boolean autoCommandSeen = false;
  private static boolean finishRequested = false;
  private static double startTimestampSec = 0.0;
  private static double finishTimestampSec = 0.0;
  private static String finishReason = "";

  private AgentAutoRunner() {}

  public static void configureInitialAgentAutoDriverStation() {
    DriverStationSim.setDsAttached(true);
    DriverStationSim.setFmsAttached(false);
    DriverStationSim.setTest(false);
    DriverStationSim.setAutonomous(false);
    DriverStationSim.setEnabled(false);
    DriverStationSim.setMatchTime(TIMEOUT_SEC);
    DriverStationSim.setAllianceStationId(getInitialAgentAutoAllianceStation());
    DriverStationSim.notifyNewData();
  }

  private static AllianceStationID getInitialAgentAutoAllianceStation() {
    var allianceStation = System.getenv("AGENT_AUTO_ALLIANCE");
    if (allianceStation == null || allianceStation.isBlank()) {
      allianceStation = "blue1";
    }

    return parseAllianceStation(allianceStation);
  }

  private static AllianceStationID parseAllianceStation(String allianceStation) {
    return switch (allianceStation.toLowerCase()) {
      case "blue1" -> AllianceStationID.Blue1;
      case "blue2" -> AllianceStationID.Blue2;
      case "blue3" -> AllianceStationID.Blue3;
      case "red1" -> AllianceStationID.Red1;
      case "red2" -> AllianceStationID.Red2;
      case "red3" -> AllianceStationID.Red3;
      default ->
          throw new IllegalArgumentException("Invalid AGENT_AUTO_ALLIANCE: " + allianceStation);
    };
  }

  public static boolean isEnabled() {
    return Config.MODE == Config.Mode.SIM && ENABLED;
  }

  public static void selectAgentAutoMode(AgentAutoModeSelector agentAutoModeSelector) {
    if (!isEnabled()) {
      return;
    }

    agentAutoModeSelector.selectMode(AUTO_NAME, getAutoResponses());
  }

  public static void periodic() {
    if (!isEnabled()) {
      return;
    }

    var nowSec = LoggerUtil.getTimestampSec();
    if (!started && nowSec >= START_DELAY_SEC) {
      started = true;
      startTimestampSec = nowSec;
      System.out.println("[AgentAutoRunner] Auto started: " + AUTO_NAME);
      Logger.recordOutput("agentAutoRunner/autoName", AUTO_NAME);
      Logger.recordOutput("agentAutoRunner/status", "running");
      DriverStationSim.setAutonomous(true);
      DriverStationSim.setEnabled(true);
      DriverStationSim.notifyNewData();
      return;
    }

    if (started && !finishRequested) {
      DriverStationSim.setMatchTime(Math.max(TIMEOUT_SEC - (nowSec - startTimestampSec), 0.0));
      DriverStationSim.notifyNewData();
    }
  }

  public static boolean shouldExit(Command autoCommand) {
    if (!isEnabled()) {
      return false;
    }

    if (finishRequested) {
      return LoggerUtil.getTimestampSec() - finishTimestampSec >= FINISH_DELAY_SEC;
    }

    if (!started) {
      return false;
    }

    if (autoCommand != null) {
      autoCommandSeen = true;
    }

    var elapsedSec = LoggerUtil.getTimestampSec() - startTimestampSec;
    if (autoCommandSeen && autoCommand != null && !autoCommand.isScheduled()) {
      requestFinish("autoFinished", elapsedSec);
    } else if (elapsedSec >= TIMEOUT_SEC) {
      requestFinish("timeout", elapsedSec);
    }

    return false;
  }

  public static boolean wasAutoFinished() {
    return finishRequested && finishReason.equals("autoFinished");
  }

  private static void requestFinish(String reason, double elapsedSec) {
    finishRequested = true;
    finishReason = reason;
    finishTimestampSec = LoggerUtil.getTimestampSec();
    System.out.printf("[AgentAutoRunner] Auto %s in %.2f secs%n", reason, elapsedSec);
    Logger.recordOutput("agentAutoRunner/status", reason);
    Logger.recordOutput("agentAutoRunner/elapsedSec", elapsedSec);
    DriverStationSim.setEnabled(false);
    DriverStationSim.setAutonomous(false);
    DriverStationSim.notifyNewData();
  }

  private static List<String> getAutoResponses() {
    var responses = new ArrayList<String>();
    responses.add(getEnv("AGENT_AUTO_Q1", "").trim());
    responses.add(getEnv("AGENT_AUTO_Q2", "").trim());

    while (!responses.isEmpty() && responses.get(responses.size() - 1).isBlank()) {
      responses.remove(responses.size() - 1);
    }

    return responses;
  }

  private static String getEnv(String name, String defaultValue) {
    var value = System.getenv(name);
    return value == null ? defaultValue : value;
  }

  private static double getDoubleEnv(String name, double defaultValue) {
    var rawValue = getEnv(name, "").trim();
    if (rawValue.isBlank()) {
      return defaultValue;
    }

    try {
      return Double.parseDouble(rawValue);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid " + name + ": " + rawValue, e);
    }
  }
}
