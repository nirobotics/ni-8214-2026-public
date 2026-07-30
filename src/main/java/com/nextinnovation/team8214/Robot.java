// Copyright (c) 2025 FRC 6328
// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214;

import com.ctre.phoenix6.SignalLogger;
import com.nextinnovation.cyberpower.EnergyLogger;
import com.nextinnovation.team8214.agent.AgentAutoRunner;
import com.nextinnovation.team8214.subsystem.shooter.ShootingCalculator;
import com.nextinnovation.team8214.util.AdvantageKitLogSink;
import com.nextinnovation.team8214.util.LoggerUtil;
import com.nextinnovation.team8214.util.VirtualSubsystem;
import edu.wpi.first.hal.AllianceStationID;
import edu.wpi.first.math.MathShared;
import edu.wpi.first.math.MathSharedStore;
import edu.wpi.first.math.MathUsageId;
import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import edu.wpi.first.wpilibj.simulation.RoboRioSim;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.littletonrobotics.junction.AutoLog;
import org.littletonrobotics.junction.LogFileUtil;
import org.littletonrobotics.junction.LoggedRobot;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.NT4Publisher;
import org.littletonrobotics.junction.wpilog.WPILOGReader;
import org.littletonrobotics.junction.wpilog.WPILOGWriter;

public class Robot extends LoggedRobot {
  private final RobotContainer robotContainer;
  private Command autoCmd;

  private boolean autoMessagePrinted;
  private double autoStart;

  private final BatteryIOInputsAutoLogged batteryInputs = new BatteryIOInputsAutoLogged();

  private static final Debouncer allianceConfirmDebouncer =
      new Debouncer(Config.LOOP_PERIOD_SEC * 2.0, Debouncer.DebounceType.kRising);
  private static DriverStation.Alliance lastAlliance = null;
  private static boolean hasAllianceConfirmed = false;

  public Robot() {
    super(Config.LOOP_PERIOD_SEC);

    ctreLoggerInit();
    advantageKitLoggerInit();

    TrajectoryLoader.getInstance().lazyLoadTrajectorySet();
    ShootingCalculator.getInstance();
    cyberPowerInit();

    if (Config.MODE == Config.Mode.SIM) {
      // Configure Driver Station for sim
      RoboRioSim.setTeamNumber(8214);
      RoboRioSim.setBrownoutVoltage(0.0);
      RoboRioSim.setVInVoltage(Sim.CONSTANT_BUS_VOLTAGE_VOLT);
      RobotController.setBrownoutVoltage(0.0);
      if (AgentAutoRunner.isEnabled()) {
        AgentAutoRunner.configureInitialAgentAutoDriverStation();
      } else {
        DriverStationSim.setAllianceStationId(AllianceStationID.Red1);
        DriverStationSim.notifyNewData();
      }
    } else {
      RobotController.setBrownoutVoltage(5.5);
    }

    robotContainer = new RobotContainer();
  }

  private void cyberPowerInit() {
    EnergyLogger.getInstance()
        .registerLogSink(new AdvantageKitLogSink())
        .registerTimeSource(Logger::getTimestamp)
        .registerBatteryVoltageSource(() -> batteryInputs.batteryVoltageVolt);
  }

  private void ctreLoggerInit() {
    SignalLogger.enableAutoLogging(false);
    SignalLogger.stop();
  }

  private void advantageKitLoggerInit() {
    Logger.recordMetadata("IsLiveDebug", Boolean.toString(Config.IS_LIVE_DEBUG));
    Logger.recordMetadata("RuntimeType", getRuntimeType().toString());
    Logger.recordMetadata("ProjectName", BuildConstants.MAVEN_NAME);
    Logger.recordMetadata("BuildDate", BuildConstants.BUILD_DATE);
    Logger.recordMetadata("GitSHA", BuildConstants.GIT_SHA);
    Logger.recordMetadata("GitDate", BuildConstants.GIT_DATE);
    Logger.recordMetadata("GitBranch", BuildConstants.GIT_BRANCH);
    switch (BuildConstants.DIRTY) {
      case 0:
        Logger.recordMetadata("GitDirty", "All changes committed");
        break;
      case 1:
        Logger.recordMetadata("GitDirty", "Uncomitted changes");
        break;
      default:
        Logger.recordMetadata("GitDirty", "Unknown");
        break;
    }

    switch (Config.MODE) {
      case REAL -> {
        Logger.addDataReceiver(new WPILOGWriter());
        if (Config.IS_LIVE_DEBUG) {
          Logger.addDataReceiver(new NT4Publisher());
        }
      }

      case SIM -> {
        if (AgentAutoRunner.isEnabled()) {
          var simLogDirectory = Path.of("log", "sim");
          try {
            Files.createDirectories(simLogDirectory);
          } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to create sim log directory: " + simLogDirectory.toAbsolutePath(), e);
          }
          Logger.addDataReceiver(new WPILOGWriter(simLogDirectory.toString()));
        }
        Logger.addDataReceiver(new NT4Publisher());
      }

      case REPLAY -> {
        var inPath = LogFileUtil.findReplayLog();
        var replayLogDirectory = Path.of("log", "replay");
        try {
          Files.createDirectories(replayLogDirectory);
        } catch (IOException e) {
          throw new IllegalStateException(
              "Failed to create replay log directory: " + replayLogDirectory.toAbsolutePath(), e);
        }

        final var replayFileName = Path.of(LogFileUtil.addPathSuffix(inPath, "_sim")).getFileName();
        var outPath = replayLogDirectory.resolve(replayFileName).toString();
        Logger.setReplaySource(new WPILOGReader(inPath));
        Logger.addDataReceiver(new WPILOGWriter(outPath));
      }
    }

    setUseTiming(Config.MODE != Config.Mode.REPLAY);

    Logger.start();

    Map<String, Integer> commandCounts = new HashMap<>();
    BiConsumer<Command, Boolean> logCommandFunction =
        (Command command, Boolean active) -> {
          String name = command.getName();
          int count = commandCounts.getOrDefault(name, 0) + (active ? 1 : -1);
          commandCounts.put(name, count);
          Logger.recordOutput(
              "CommandsUnique/" + name + "_" + Integer.toHexString(command.hashCode()), active);
          Logger.recordOutput("CommandsAll/" + name, count > 0);
        };
    CommandScheduler.getInstance()
        .onCommandInitialize((Command command) -> logCommandFunction.accept(command, true));
    CommandScheduler.getInstance()
        .onCommandFinish((Command command) -> logCommandFunction.accept(command, false));
    CommandScheduler.getInstance()
        .onCommandInterrupt((Command command) -> logCommandFunction.accept(command, false));

    // Adjust loop overrun warning timeout
    try {
      Field watchdogField = IterativeRobotBase.class.getDeclaredField("m_watchdog");
      watchdogField.setAccessible(true);
      Watchdog watchdog = (Watchdog) watchdogField.get(this);
      watchdog.setTimeout(Config.LOOP_PERIOD_SEC * 10);
    } catch (Exception e) {
      DriverStation.reportWarning("Failed to disable loop overrun warnings.", false);
    }
    CommandScheduler.getInstance().setPeriod(Config.LOOP_PERIOD_SEC * 10);

    // Silence joystick alerts
    DriverStation.silenceJoystickConnectionWarning(true);

    // Silence Rotation2d warnings
    var mathShared = MathSharedStore.getMathShared();
    MathSharedStore.setMathShared(
        new MathShared() {
          @Override
          public void reportError(String error, StackTraceElement[] stackTrace) {
            if (error.startsWith("x and y components of Rotation2d are zero")) {
              return;
            }
            mathShared.reportError(error, stackTrace);
          }

          @Override
          public void reportUsage(MathUsageId id, int count) {
            mathShared.reportUsage(id, count);
          }

          @Override
          public double getTimestamp() {
            return mathShared.getTimestamp();
          }
        });
  }

  @Override
  public void robotPeriodic() {
    Threads.setCurrentThreadPriority(true, 99);

    var alliance = DriverStation.getAlliance();
    hasAllianceConfirmed =
        allianceConfirmDebouncer.calculate(alliance.isPresent() && lastAlliance == alliance.get());
    lastAlliance = alliance.orElse(null);

    if (autoCmd != null) {
      if (!autoCmd.isScheduled() && !autoMessagePrinted) {
        if (DriverStation.isAutonomousEnabled() || AgentAutoRunner.wasAutoFinished()) {
          System.out.printf(
              "*** Auto finished in %.2f secs ***%n", LoggerUtil.getTimestampSec() - autoStart);
        } else {
          System.out.printf(
              "*** Auto cancelled in %.2f secs ***%n", LoggerUtil.getTimestampSec() - autoStart);
        }
        autoMessagePrinted = true;
      }
    }

    if (Config.IS_LIVE_DEBUG && Config.MODE != Config.Mode.REPLAY) {
      Config.LiveDebugGroup.updateGroupActive();
    }

    // Update battery inputs
    batteryInputs.batteryVoltageVolt = RobotController.getBatteryVoltage();
    batteryInputs.rioCurrentAmp = RobotController.getInputCurrent();
    Logger.processInputs("energyLogger", batteryInputs);

    var startTime = LoggerUtil.getTimestampSec();
    VirtualSubsystem.periodicAll();
    Logger.recordOutput(
        "performance/VirtualSubsystem/periodicAll", LoggerUtil.getTimestampSec() - startTime);

    startTime = LoggerUtil.getTimestampSec();
    CommandScheduler.getInstance().run();
    Logger.recordOutput(
        "performance/CommandScheduler/run", LoggerUtil.getTimestampSec() - startTime);

    robotContainer.updateHopperCover();

    Odometry.getInstance().updateElasticPoses();

    robotContainer.updateJoysticksDisconnectedAlert();

    // Publish match time
    SmartDashboard.putNumber("Match Time", DriverStation.getMatchTime());

    // Update from HubShiftUtil
    SmartDashboard.putString(
        "Shifts/Remaining Shift Time",
        String.format("%.1f", Math.max(HubShiftUtil.getShiftedShiftInfo().remainingTime(), 0.0)));
    SmartDashboard.putBoolean("Shifts/Shift Active", HubShiftUtil.getShiftedShiftInfo().active());
    SmartDashboard.putString(
        "Shifts/Game State", HubShiftUtil.getShiftedShiftInfo().currentShift().toString());

    var hasAutoWinData =
        !DriverStation.getGameSpecificMessage().isEmpty()
            || HubShiftUtil.getAllianceWinOverride().isPresent();

    var activeFirst =
        DriverStation.getAlliance().orElse(DriverStation.Alliance.Blue)
            == HubShiftUtil.getFirstActiveAlliance();

    SmartDashboard.putString(
        "Shifts/Active First?", hasAutoWinData ? activeFirst ? "√" : "X" : "-");
    SmartDashboard.putString("Shifts/Win Auto?", hasAutoWinData ? activeFirst ? "X" : "√" : "-");

    SmartDashboard.putNumber("FPGA Timestamp", LoggerUtil.getTimestampSec());

    EnergyLogger.getInstance().periodicRobot();

    Threads.setCurrentThreadPriority(true, 10);
  }

  @Override
  public void autonomousInit() {
    if (Config.MODE == Config.Mode.SIM) {
      Sim.getInstance().loadFuelForAuto();
    }

    robotContainer.setRobotRebootDuringTeleop(false);
    robotContainer.flushSwerveDriveCurrentLimit(true);
    robotContainer.resetHopperCover();

    autoStart = LoggerUtil.getTimestampSec();
    autoMessagePrinted = false;
    autoCmd = robotContainer.getAutoCmd();

    if (autoCmd != null) {
      CommandScheduler.getInstance().schedule(autoCmd);
    }
  }

  @Override
  public void autonomousPeriodic() {}

  @Override
  public void teleopInit() {
    if (Config.MODE == Config.Mode.SIM) {
      Sim.getInstance().loadFuelForTeleop();
    }
    if (autoCmd != null) {
      autoCmd.cancel();
      autoCmd = null;
      robotContainer.setSwerveDriveBrakeMode();
    }
    robotContainer.flushSwerveDriveCurrentLimit(false);

    HubShiftUtil.restartShiftTimer();
  }

  @Override
  public void teleopPeriodic() {}

  @Override
  public void disabledInit() {}

  @Override
  public void disabledPeriodic() {}

  @Override
  public void testInit() {}

  @Override
  public void testPeriodic() {}

  @Override
  public void simulationInit() {
    if (Config.MODE == Config.Mode.SIM) {
      Sim.getInstance();
    }
  }

  @Override
  public void simulationPeriodic() {
    if (Config.MODE == Config.Mode.SIM) {
      AgentAutoRunner.periodic();
      Sim.getInstance().periodic();

      if (AgentAutoRunner.shouldExit(autoCmd)) {
        Logger.end();
        endCompetition();
      }
    }
  }

  @AutoLog
  public static class BatteryIOInputs {
    double batteryVoltageVolt = 12.0;
    double rioCurrentAmp = 0.0;
  }

  public static boolean hasAllianceConfirmed() {
    return hasAllianceConfirmed;
  }
}
