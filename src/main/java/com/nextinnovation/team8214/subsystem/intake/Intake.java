// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.intake;

import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.signals.GravityTypeValue;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.nextinnovation.cyberpower.EnergyLogger;
import com.nextinnovation.cyberpower.EnergySubsystem;
import com.nextinnovation.cyberpower.MotorType;
import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.Ports;
import com.nextinnovation.team8214.Sim;
import com.nextinnovation.team8214.util.*;
import com.nextinnovation.team8214.util.genericsystem.*;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import lombok.Getter;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;

public class Intake extends SubsystemBase {
  private final LoggedTunableGains PIVOT_GAINS =
      new LoggedTunableGains(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT, 750.0, 0.0, 50.0, 0.0, 0.0, 12.0);
  private final LoggedTunableMotionMagic.Spin PIVOT_START_MM =
      new LoggedTunableMotionMagic.Spin(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/start", 825.0, 4000.0);
  private final LoggedTunableMotionMagic.Spin PIVOT_COMPRESS_MM =
      new LoggedTunableMotionMagic.Spin(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/compress", 825.0, 4000.0);
  private final LoggedTunableMotionMagic.Spin PIVOT_UP_MM =
      new LoggedTunableMotionMagic.Spin(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/up", 720.0, 720.0);
  private final LoggedTunableMotionMagic.Spin PIVOT_DOWN_MM =
      new LoggedTunableMotionMagic.Spin(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/down", 825.0, 4000.0);

  private final LoggedTunableHome HOME =
      new LoggedTunableHome(IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT, 0.3, -28.0, 3.0);
  private final LoggedTunableTolerance.Spin TOLERANCE =
      new LoggedTunableTolerance.Spin(IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT, 0.03, 0.02);

  private final GenericRollerIO rollerIO;
  private final GenericRollerIOInputsAutoLogged rollerInputs =
      new GenericRollerIOInputsAutoLogged();
  private final Alert rollerOfflineAlert =
      new Alert("Intake roller motor offline!", Alert.AlertType.WARNING);

  private final GenericArmIO pivotIO;
  private final GenericArmIOInputsAutoLogged pivotInputs = new GenericArmIOInputsAutoLogged();
  private final Alert pivotMotorOfflineAlert =
      new Alert("Intake pivot motor offline!", Alert.AlertType.WARNING);
  private final Alert homingTimeoutAlert =
      new Alert("Intake pivot homing timed out!", Alert.AlertType.ERROR);
  private final EnergySubsystem energySubsystem =
      EnergyLogger.getInstance().createSubsystem("intake");

  @Getter
  @AutoLogOutput(key = IntakeConfig.LOG_ROOT + "/goal")
  private IntakeGoal goal = IntakeConfig.INIT_GOAL;

  private boolean useStartMm = false;
  private boolean useUpMm = true;

  private boolean isHoming = false;

  public Intake() {
    switch (Config.MODE) {
      case REAL -> {
        var rollerTalonConfig = new TalonFXConfiguration();
        rollerTalonConfig.MotorOutput.NeutralMode = NeutralModeValue.Coast;
        rollerTalonConfig.MotorOutput.Inverted = InvertedValue.Clockwise_Positive;

        rollerTalonConfig.CurrentLimits.StatorCurrentLimit = 80.0;
        rollerTalonConfig.CurrentLimits.StatorCurrentLimitEnable = true;
        rollerTalonConfig.CurrentLimits.SupplyCurrentLimit = 37.0;
        rollerTalonConfig.CurrentLimits.SupplyCurrentLowerLimit = 25.0;
        rollerTalonConfig.CurrentLimits.SupplyCurrentLimitEnable = true;

        rollerIO =
            new GenericRollerIOKraken(
                    "intake/roller", Ports.Can.INTAKE_ROLLER_LEFT_MASTER, rollerTalonConfig)
                .withFollower(Ports.Can.INTAKE_ROLLER_RIGHT_SLAVE, true);

        var pivotConfig = new TalonFXConfiguration();
        pivotConfig.MotorOutput.NeutralMode = NeutralModeValue.Brake;
        pivotConfig.MotorOutput.Inverted = InvertedValue.Clockwise_Positive;
        pivotConfig.Slot0 =
            new Slot0Configs()
                .withKP(PIVOT_GAINS.getKp())
                .withKD(PIVOT_GAINS.getKd())
                .withKS(PIVOT_GAINS.getKs())
                .withKG(PIVOT_GAINS.getKg())
                .withGravityType(GravityTypeValue.Arm_Cosine);

        pivotConfig.CurrentLimits.StatorCurrentLimit = 75.0;
        pivotConfig.CurrentLimits.StatorCurrentLimitEnable = true;
        pivotConfig.CurrentLimits.SupplyCurrentLimit = 30.0;
        pivotConfig.CurrentLimits.SupplyCurrentLimitEnable = true;

        pivotConfig.Feedback.SensorToMechanismRatio = IntakeConfig.PIVOT_GEAR_RATIO;

        pivotIO =
            new GenericArmIOKraken(
                "intake/pivot",
                Ports.Can.INTAKE_PIVOT,
                pivotConfig,
                Units.degreesToRadians(IntakeConfig.INIT_GOAL.getPivotPositionDegree()));
      }

      case SIM -> {
        rollerIO = new GenericRollerIOSim();
        pivotIO =
            new GenericArmIOSim(
                Units.degreesToRadians(IntakeConfig.INIT_GOAL.getPivotPositionDegree()),
                Units.degreesToRadians(0.0),
                Units.degreesToRadians(120.0));
      }

      default -> {
        rollerIO = new GenericRollerIO() {};
        pivotIO = new GenericArmIO() {};
      }
    }
    configureCyberPower();
  }

  private void configureCyberPower() {
    energySubsystem.registerLeaderMotor(
        "rollerLeft",
        MotorType.KRAKEN_X60_FOC,
        1.0,
        () -> rollerInputs.connected,
        () -> rollerInputs.supplyCurrentAmp,
        () -> rollerInputs.statorCurrentAmp,
        () -> rollerInputs.rotorVelocityRadPerSec);
    energySubsystem.registerFollowerMotor(
        "rollerRight",
        MotorType.KRAKEN_X60_FOC,
        1.0,
        "rollerLeft",
        () ->
            Config.MODE == Config.Mode.SIM
                ? rollerInputs.connected
                : rollerInputs.followersConnected.length > 0
                    && rollerInputs.followersConnected[0]
                    && rollerInputs.followersSupplyCurrentAmp.length > 0,
        () ->
            Config.MODE == Config.Mode.SIM
                ? rollerInputs.supplyCurrentAmp
                : rollerInputs.followersSupplyCurrentAmp.length > 0
                    ? rollerInputs.followersSupplyCurrentAmp[0]
                    : Double.NaN);
    energySubsystem.registerLeaderMotor(
        "pivot",
        MotorType.KRAKEN_X60_FOC,
        IntakeConfig.PIVOT_GEAR_RATIO,
        () -> pivotInputs.motorConnected,
        () -> pivotInputs.supplyCurrentAmp,
        () -> pivotInputs.statorCurrentAmp,
        () -> pivotInputs.rotorVelocityRadPerSec);
  }

  @Override
  public void periodic() {
    rollerIO.updateInputs(rollerInputs);
    Logger.processInputs(IntakeConfig.LOG_ROOT + "/roller", rollerInputs);
    rollerOfflineAlert.set(!rollerInputs.connected);

    pivotIO.updateInputs(pivotInputs);
    Logger.processInputs(IntakeConfig.LOG_ROOT + "/pivot", pivotInputs);
    pivotMotorOfflineAlert.set(!pivotInputs.motorConnected);
    energySubsystem.periodic(isHoming ? "HOMING" : goal.name());
    LoggedTunableNumber.ifChanged(
        hashCode(),
        () ->
            pivotIO.setPdf(
                PIVOT_GAINS.getKp(), PIVOT_GAINS.getKd(), PIVOT_GAINS.getKs(), PIVOT_GAINS.getKg()),
        PIVOT_GAINS.getEntries());

    if (Config.MODE == Config.Mode.SIM) {
      var sim = Sim.getInstance().getIntake();

      if (goal == IntakeGoal.COLLECT || goal == IntakeGoal.SCORE || goal == IntakeGoal.TRANSPORT) {
        sim.startIntake();
      } else {
        sim.stopIntake();
      }
    }

    if (!isHoming) {
      double maxVelocityRadPerSec;
      double maxAccelRadPerSec2;
      if (useStartMm) {
        maxVelocityRadPerSec = PIVOT_START_MM.getMaxVelocityRadPerSec();
        maxAccelRadPerSec2 = PIVOT_START_MM.getMaxAccelRadPerSec2();
      } else if (goal == IntakeGoal.FULL_COMPRESS) {
        maxVelocityRadPerSec = PIVOT_COMPRESS_MM.getMaxVelocityRadPerSec();
        maxAccelRadPerSec2 = PIVOT_COMPRESS_MM.getMaxAccelRadPerSec2();
      } else if (useUpMm) {
        maxVelocityRadPerSec = PIVOT_UP_MM.getMaxVelocityRadPerSec();
        maxAccelRadPerSec2 = PIVOT_UP_MM.getMaxAccelRadPerSec2();
      } else {
        maxVelocityRadPerSec = PIVOT_DOWN_MM.getMaxVelocityRadPerSec();
        maxAccelRadPerSec2 = PIVOT_DOWN_MM.getMaxAccelRadPerSec2();
      }

      rollerIO.setVoltage(
          goal == IntakeGoal.COLLECT && DriverStation.isAutonomousEnabled()
              ? 9.0
              : goal.getRollerVoltageVolt());
      pivotIO.setPosition(
          Units.degreesToRadians(goal.getPivotPositionDegree()),
          maxVelocityRadPerSec,
          maxAccelRadPerSec2,
          0.0);
    }
  }

  public Transform3d getSwerveToIntakeTransform() {
    return new Transform3d(0, 0.14, 0.28, new Rotation3d())
        .plus(new Transform3d(0.0, 0.0, 0.0, new Rotation3d(-pivotInputs.positionRad, 0.0, 0.0)));
  }

  public void setGoal(IntakeGoal goal) {
    if (this.goal != goal) {
      if (this.goal == IntakeGoal.START) {
        useStartMm = true;
        useUpMm = false;
      } else {
        useStartMm = false;
        useUpMm = goal.getPivotPositionDegree() > this.goal.getPivotPositionDegree();
      }
      this.goal = goal;
    }
  }

  public Command getHomeCmd() {
    return HOME.createCommand(
        this,
        () -> {
          setGoal(IntakeGoal.IDLE);
          isHoming = true;
          rollerIO.stop();
        },
        () -> pivotIO.setCurrent(HOME.getCurrentAmp()),
        () -> pivotInputs.motorConnected && hasStop(),
        () -> {
          pivotIO.home(Units.degreesToRadians(IntakeGoal.HOME.getPivotPositionDegree()));
          isHoming = false;
        },
        pivotIO::stop,
        homingTimeoutAlert::set);
  }

  public boolean pivotAtGoal() {
    return EqualsUtil.epsilonEquals(
        pivotInputs.positionRad,
        Units.degreesToRadians(goal.getPivotPositionDegree()),
        TOLERANCE.getAtGoalPositionToleranceRad());
  }

  public boolean hasStop() {
    return EqualsUtil.epsilonEquals(
        pivotInputs.velRadPerSec, 0.0, TOLERANCE.getStopVelocityToleranceRadPerSec());
  }

  public void forceHomeAtStart() {
    pivotIO.home(Units.degreesToRadians(IntakeGoal.START.getPivotPositionDegree()));
  }

  public Command singleCompressCmd() {
    return Commands.sequence(
        runOnce(() -> setGoal(IntakeGoal.SCORE)), run(() -> setGoal(IntakeGoal.FULL_COMPRESS)));
  }

  public void setHoming(boolean wantHoming) {
    isHoming = wantHoming;

    if (wantHoming) {
      pivotIO.stop();
    }
  }
}
