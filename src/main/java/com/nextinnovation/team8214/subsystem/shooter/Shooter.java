// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.shooter;

import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.signals.GravityTypeValue;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.nextinnovation.cyberpower.EnergyLogger;
import com.nextinnovation.cyberpower.EnergySubsystem;
import com.nextinnovation.cyberpower.MotorType;
import com.nextinnovation.team8214.*;
import com.nextinnovation.team8214.subsystem.shooter.controller.*;
import com.nextinnovation.team8214.util.*;
import com.nextinnovation.team8214.util.genericsystem.*;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import lombok.Getter;
import lombok.Setter;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;

public class Shooter extends SubsystemBase {
  private BooleanSupplier isCoveredByHopperSignalSupplier = () -> false;

  private final LoggedTunableGains FLYWHEEL_GAINS =
      new LoggedTunableGains(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT + "/flywheel",
          0.35 * ShooterConfig.FLYWHEEL_GEAR_RATIO,
          0.0,
          0.0,
          12.0 / (5800.0 / 60.0) * ShooterConfig.FLYWHEEL_GEAR_RATIO,
          0.0,
          0.0);

  private final LoggedTunableGains PITCH_GAINS =
      new LoggedTunableGains(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT + "/pitch",
          1800.0,
          0.0,
          90.0,
          0.0,
          0.0,
          0.0);

  private static final LoggedTunableNumber PITCH_UNDER_TRENCH_POSITION_DEGREE =
      new LoggedTunableNumber(
          ShooterConfig.LOG_GROUP,
          ShooterConfig.LOG_ROOT + "pitchUnderTrenchPositionDegree",
          ShooterConfig.START_ANGLE_DEGREE);

  private final LoggedTunableHome HOME =
      new LoggedTunableHome(ShooterConfig.LOG_GROUP, ShooterConfig.LOG_ROOT, 0.3, 5.0, 3.0);

  public enum ControlMode {
    IDLE,
    TRANSPORT,
    SCORE,
    FENCE,
    PRESET
  }

  private final GenericRollerIO flywheelIO;
  private final GenericRollerIOInputsAutoLogged flywheelInputs =
      new GenericRollerIOInputsAutoLogged();
  private final Alert flywheelOfflineAlert =
      new Alert("Shooter flywheel motor offline!", Alert.AlertType.WARNING);

  private final GenericArmIO pitchIO;
  private final GenericArmIOInputsAutoLogged pitchInputs = new GenericArmIOInputsAutoLogged();
  private final Alert pitchOfflineAlert =
      new Alert("Shooter pitch motor offline!", Alert.AlertType.WARNING);
  private final Alert homingTimeoutAlert =
      new Alert("Shooter pitch homing timed out!", Alert.AlertType.ERROR);
  private final EnergySubsystem energySubsystem =
      EnergyLogger.getInstance().createSubsystem("shooter");

  @Getter
  @AutoLogOutput(key = ShooterConfig.LOG_ROOT + "/mode")
  private ControlMode mode = ControlMode.IDLE;

  @Getter
  @AutoLogOutput(key = ShooterConfig.LOG_ROOT + "/flywheelOnTarget")
  private boolean flywheelOnTarget = false;

  @Getter
  @AutoLogOutput(key = ShooterConfig.LOG_ROOT + "/pitchOnTarget")
  private boolean pitchOnTarget = false;

  private final ScoreController scoreController = new ScoreController();
  private final IdleController idleController = new IdleController();
  private final FenceController fenceController = new FenceController();
  private final TransportController transportController = new TransportController();
  private PresetController presetController = null;

  private ShooterControlRequest cachedShooterControlRequest = null;

  @AutoLogOutput(key = ShooterConfig.LOG_ROOT + "/isUnderTrench")
  @Setter
  private boolean isUnderTrench = true;

  @AutoLogOutput(key = ShooterConfig.LOG_ROOT + "/manualScoreDistanceOffset")
  @Getter
  @Setter
  private double manualScoreDistanceOffset = ShooterConfig.DEFAULT_MANUAL_SCORE_DISTANCE_OFFSET;

  private boolean isHoming = false;

  @Setter private boolean enableIdleFlywheel = true;

  public Shooter() {
    switch (Config.MODE) {
      case REAL -> {
        var flywheelConfig = new TalonFXConfiguration();
        flywheelConfig.MotorOutput.NeutralMode = NeutralModeValue.Coast;
        flywheelConfig.MotorOutput.Inverted = InvertedValue.Clockwise_Positive;
        flywheelConfig.Slot0 =
            new Slot0Configs()
                .withKP(FLYWHEEL_GAINS.getKp())
                .withKD(FLYWHEEL_GAINS.getKd())
                .withKV(FLYWHEEL_GAINS.getKv())
                .withKS(FLYWHEEL_GAINS.getKs());
        flywheelConfig.CurrentLimits.StatorCurrentLimit = 60.0;
        flywheelConfig.CurrentLimits.StatorCurrentLimitEnable = true;
        flywheelConfig.CurrentLimits.SupplyCurrentLimit = 35.0;
        flywheelConfig.CurrentLimits.SupplyCurrentLimitEnable = true;
        flywheelConfig.Feedback.SensorToMechanismRatio = ShooterConfig.FLYWHEEL_GEAR_RATIO;

        flywheelIO =
            new GenericRollerIOKraken(
                    "shooter/flywheel", Ports.Can.SHOOTER_FLYWHEEL_UP_LEFT_MASTER, flywheelConfig)
                .withFollower(Ports.Can.SHOOTER_FLYWHEEL_DOWN_LEFT_SALVE, false)
                .withFollower(Ports.Can.SHOOTER_FLYWHEEL_DOWN_RIGHT_SALVE, true)
                .withFollower(Ports.Can.SHOOTER_FLYWHEEL_UP_RIGHT_SALVE, true);

        var pitchConfig = new TalonFXConfiguration();
        pitchConfig.MotorOutput.NeutralMode = NeutralModeValue.Brake;
        pitchConfig.MotorOutput.Inverted = InvertedValue.CounterClockwise_Positive;
        pitchConfig.Slot0 =
            new Slot0Configs()
                .withKP(PITCH_GAINS.getKp())
                .withKD(PITCH_GAINS.getKd())
                .withKS(PITCH_GAINS.getKs())
                .withKG(PITCH_GAINS.getKg())
                .withGravityType(GravityTypeValue.Arm_Cosine);
        pitchConfig.Feedback.SensorToMechanismRatio =
            ShooterConfig.PITCH_FEEDBACK_SENSOR_TO_MECHANISM_RATIO;
        pitchConfig.CurrentLimits.StatorCurrentLimit = 60.0;
        pitchConfig.CurrentLimits.StatorCurrentLimitEnable = true;
        pitchConfig.CurrentLimits.SupplyCurrentLimit = 20.0;
        pitchConfig.CurrentLimits.SupplyCurrentLimitEnable = true;

        pitchIO =
            new GenericArmIOKraken(
                "shooter/pitch",
                Ports.Can.SHOOTER_PITCH,
                pitchConfig,
                Units.degreesToRadians(ShooterConfig.START_ANGLE_DEGREE));
      }

      case SIM -> {
        flywheelIO = new GenericRollerIOSim(ShooterConfig.FLYWHEEL_GEAR_RATIO);
        pitchIO =
            new GenericArmIOSim(
                Units.degreesToRadians(ShooterConfig.START_ANGLE_DEGREE),
                Units.degreesToRadians(45),
                Units.degreesToRadians(80));
      }

      default -> {
        flywheelIO = new GenericRollerIO() {};
        pitchIO = new GenericArmIO() {};
      }
    }
    configureCyberPower();
  }

  private void configureCyberPower() {
    energySubsystem.registerLeaderMotor(
        "flywheelUpLeft",
        MotorType.KRAKEN_X60_FOC,
        ShooterConfig.FLYWHEEL_GEAR_RATIO,
        () -> flywheelInputs.connected,
        () -> flywheelInputs.supplyCurrentAmp,
        () -> flywheelInputs.statorCurrentAmp,
        () -> flywheelInputs.rotorVelocityRadPerSec);
    registerFlywheelFollower("flywheelDownLeft", 0);
    registerFlywheelFollower("flywheelDownRight", 1);
    registerFlywheelFollower("flywheelUpRight", 2);
    energySubsystem.registerLeaderMotor(
        "pitch",
        MotorType.KRAKEN_X44_FOC,
        ShooterConfig.PITCH_ANALYSIS_REDUCTION,
        () -> pitchInputs.motorConnected,
        () -> pitchInputs.supplyCurrentAmp,
        () -> pitchInputs.statorCurrentAmp,
        () -> pitchInputs.rotorVelocityRadPerSec);
  }

  private void registerFlywheelFollower(String name, int index) {
    energySubsystem.registerFollowerMotor(
        name,
        MotorType.KRAKEN_X60_FOC,
        ShooterConfig.FLYWHEEL_GEAR_RATIO,
        "flywheelUpLeft",
        () ->
            Config.MODE == Config.Mode.SIM
                ? flywheelInputs.connected
                : index < flywheelInputs.followersConnected.length
                    && flywheelInputs.followersConnected[index]
                    && index < flywheelInputs.followersSupplyCurrentAmp.length,
        () ->
            Config.MODE == Config.Mode.SIM
                ? flywheelInputs.supplyCurrentAmp
                : index < flywheelInputs.followersSupplyCurrentAmp.length
                    ? flywheelInputs.followersSupplyCurrentAmp[index]
                    : Double.NaN);
  }

  @Override
  public void periodic() {
    flywheelIO.updateInputs(flywheelInputs);
    Logger.processInputs(ShooterConfig.LOG_ROOT + "/flywheel", flywheelInputs);
    flywheelOfflineAlert.set(!flywheelInputs.connected);

    LoggedTunableNumber.ifChanged(
        hashCode(),
        () ->
            flywheelIO.setPdf(
                FLYWHEEL_GAINS.getKp(),
                FLYWHEEL_GAINS.getKd(),
                FLYWHEEL_GAINS.getKv(),
                FLYWHEEL_GAINS.getKs()),
        FLYWHEEL_GAINS.getEntries());

    pitchIO.updateInputs(pitchInputs);
    Logger.processInputs(ShooterConfig.LOG_ROOT + "/pitch", pitchInputs);
    pitchOfflineAlert.set(!pitchInputs.motorConnected);
    energySubsystem.periodic(getEnergyState());

    LoggedTunableNumber.ifChanged(
        hashCode(),
        () ->
            pitchIO.setPdf(
                PITCH_GAINS.getKp(), PITCH_GAINS.getKd(), PITCH_GAINS.getKs(), PITCH_GAINS.getKg()),
        PITCH_GAINS.getEntries());

    SmartDashboard.putString("Dist Offset(m)", String.format("%.2f", manualScoreDistanceOffset));

    var shootingCalculator = ShootingCalculator.getInstance();
    var currentFlywheelVelMeterPerSec =
        ShooterControlRequest.flywheelRad2Meter(flywheelInputs.velRadPerSec);
    shootingCalculator.publishMainCurrentXYVel(
        currentFlywheelVelMeterPerSec * Math.cos(pitchInputs.positionRad),
        currentFlywheelVelMeterPerSec * Math.sin(pitchInputs.positionRad));

    if (ShooterConfig.ENABLE_AUTO_DODGE_TRENCH && mode == ControlMode.IDLE) {
      var currentShooterInFieldForTrench = Odometry.getInstance().getShooterInField();

      Logger.recordOutput(
          ShooterConfig.LOG_ROOT + "/currentShooterInFieldForTrench",
          currentShooterInFieldForTrench);

      var maybeFlippedCurrentX = AllianceFlipUtil.apply(currentShooterInFieldForTrench).getX();

      var predictedShooterInFieldForTrench = Odometry.getInstance().getPredictedShooterInField(0.5);

      Logger.recordOutput(
          ShooterConfig.LOG_ROOT + "/predictedShooterInFieldForTrench",
          predictedShooterInFieldForTrench);

      var maybeFlippedPredictedShooterInFieldX =
          AllianceFlipUtil.apply(predictedShooterInFieldForTrench).getX();

      if (isInTrenchDangerZone(currentShooterInFieldForTrench.getY())
          && ((maybeFlippedCurrentX >= Field.TRENCH_AWAY_X_CLOSE
                  != maybeFlippedPredictedShooterInFieldX >= Field.TRENCH_AWAY_X_CLOSE)
              || (maybeFlippedCurrentX >= Field.TRENCH_AWAY_X_FAR
                  != maybeFlippedPredictedShooterInFieldX >= Field.TRENCH_AWAY_X_FAR)
              || (maybeFlippedCurrentX <= Field.TRENCH_BACK_X_CLOSE
                  != maybeFlippedPredictedShooterInFieldX <= Field.TRENCH_BACK_X_CLOSE)
              || (maybeFlippedCurrentX <= Field.TRENCH_BACK_X_FAR
                  != maybeFlippedPredictedShooterInFieldX <= Field.TRENCH_BACK_X_FAR))) {
        setUnderTrench(true);
      }
    }

    SmartDashboard.putBoolean("isUnderTrench", isUnderTrench);

    if (DriverStation.isDisabled()) {
      flywheelIO.stop();
      flywheelOnTarget = false;
      pitchOnTarget = false;
      return;
    }

    if (isHoming) {
      flywheelIO.stop();
      flywheelOnTarget = false;
      pitchOnTarget = false;
      cachedShooterControlRequest = ShooterControlRequest.idle();
      return;
    }

    var controlMode = isCoveredByHopperSignalSupplier.getAsBoolean() ? ControlMode.IDLE : mode;
    ShooterControlRequest targetState =
        switch (controlMode) {
          case IDLE ->
              !DriverStation.isAutonomous() && enableIdleFlywheel
                  ? idleController.update(manualScoreDistanceOffset)
                  : ShooterControlRequest.idle();
          case FENCE -> fenceController.update();
          case TRANSPORT -> transportController.update();
          case SCORE -> scoreController.update(manualScoreDistanceOffset);
          case PRESET -> {
            if (presetController != null) {
              yield presetController.update(manualScoreDistanceOffset);
            } else {
              yield ShooterControlRequest.idle();
            }
          }
        };

    cachedShooterControlRequest = targetState;

    var flywheelSetpoint = targetState.flywheelSetpoint();
    double flywheelVelocityRadPerSec =
        flywheelSetpoint.map(ShooterControlRequest.FlywheelSetpoint::velocityRadPerSec).orElse(0.0);
    Logger.recordOutput(
        ShooterConfig.LOG_ROOT + "/targetState/flywheelVelocityRadPerSec",
        flywheelVelocityRadPerSec);

    if (flywheelSetpoint.isEmpty() || EqualsUtil.epsilonEquals(flywheelVelocityRadPerSec, 0.0)) {
      flywheelIO.stop();
      flywheelOnTarget = false;
    } else {
      var presentFlywheelSetpoint = flywheelSetpoint.get();
      if (targetState.flywheelConstraints().isPresent()) {
        var flywheelConstraints = targetState.flywheelConstraints().get();
        flywheelIO.setVel(
            presentFlywheelSetpoint.velocityRadPerSec(),
            flywheelConstraints.maxAccelerationRadPerSec2());
      } else {
        flywheelIO.setVel(presentFlywheelSetpoint.velocityRadPerSec());
      }

      if (!flywheelOnTarget) {
        if (mode != ControlMode.TRANSPORT && mode != ControlMode.FENCE) {
          flywheelOnTarget =
              EqualsUtil.epsilonEquals(
                  flywheelInputs.velRadPerSec,
                  presentFlywheelSetpoint.velocityRadPerSec(),
                  presentFlywheelSetpoint.toleranceRadPerSec());
        } else {
          flywheelOnTarget =
              flywheelInputs.velRadPerSec
                  >= presentFlywheelSetpoint.velocityRadPerSec()
                      - presentFlywheelSetpoint.toleranceRadPerSec();
        }
      }
    }

    var pitchSetpoint = targetState.resolvedPitchSetpoint();
    Logger.recordOutput(
        ShooterConfig.LOG_ROOT + "/targetState/pitchPositionRad", pitchSetpoint.positionRad());

    if (controlMode == ControlMode.IDLE) {
      setPitchUnderTrench();
      pitchOnTarget = false;
      return;
    }

    if (isUnderTrench) {
      setPitchUnderTrench();

      pitchOnTarget = false;
    } else {
      if (targetState.pitchConstraints().isPresent()) {
        var pitchConstraints = targetState.pitchConstraints().get();
        pitchIO.setPosition(
            pitchSetpoint.positionRad(),
            pitchConstraints.maxVelocityRadPerSec(),
            pitchConstraints.maxAccelerationRadPerSec2(),
            0.0);
      } else {
        pitchIO.setPosition(pitchSetpoint.positionRad(), 0.0);
      }
      pitchOnTarget =
          EqualsUtil.epsilonEquals(
              pitchInputs.positionRad, pitchSetpoint.positionRad(), pitchSetpoint.toleranceRad());
    }
  }

  public void setFence() {
    this.mode = ControlMode.FENCE;
    flywheelOnTarget = false;
  }

  public void setScore() {
    this.mode = ControlMode.SCORE;
    flywheelOnTarget = false;
  }

  public void setTransport() {
    this.mode = ControlMode.TRANSPORT;
    flywheelOnTarget = false;
  }

  public void setPreset(Pose2d robotInField, double flywheelMaxAccelMeterPerSec2) {
    this.mode = ControlMode.PRESET;
    presetController = new PresetController(robotInField, flywheelMaxAccelMeterPerSec2);
    flywheelOnTarget = false;
  }

  public void setIdle() {
    this.mode = ControlMode.IDLE;
    flywheelOnTarget = false;
  }

  public boolean onTarget() {
    return (mode == ControlMode.SCORE || mode == ControlMode.TRANSPORT || mode == ControlMode.FENCE)
        && isPitchOnTarget()
        && isFlywheelOnTarget();
  }

  public double getFlywheelVelMeterPerSec() {
    return ShooterControlRequest.flywheelRad2Meter(flywheelInputs.velRadPerSec);
  }

  public double getPitchAngleRad() {
    return pitchInputs.positionRad;
  }

  public Transform3d getSwerveToShooterTransform() {
    return new Transform3d(0.0, 0.430, -0.27500, new Rotation3d())
        .plus(
            new Transform3d(
                0.0,
                0.0,
                0.0,
                new Rotation3d(Units.degreesToRadians(-80.0) + pitchInputs.positionRad, 0.0, 0.0)));
  }

  public Command getHomeCmd() {
    return HOME.createCommand(
        this,
        () -> {
          setIdle();
          isHoming = true;
          flywheelIO.stop();
        },
        () -> pitchIO.setCurrent(HOME.getCurrentAmp()),
        () -> pitchInputs.motorConnected && hasPitchStop(),
        () -> {
          pitchIO.home(Units.degreesToRadians(ShooterConfig.START_ANGLE_DEGREE));
          isHoming = false;
        },
        pitchIO::stop,
        homingTimeoutAlert::set);
  }

  public boolean hasPitchStop() {
    return EqualsUtil.epsilonEquals(pitchInputs.velRadPerSec, 0.0, Units.degreesToRadians(5.0));
  }

  public Optional<Rotation2d> getTargetFieldCentricYaw() {
    return cachedShooterControlRequest == null
        ? Optional.empty()
        : cachedShooterControlRequest.targetFieldCentricYaw();
  }

  private void setPitchUnderTrench() {
    pitchIO.setPosition(
        Units.degreesToRadians(PITCH_UNDER_TRENCH_POSITION_DEGREE.get()),
        Units.degreesToRadians(2160.0),
        Units.degreesToRadians(4320.0),
        0.0);
  }

  static boolean isInTrenchDangerZone(double y) {
    return y >= Field.TRENCH_DANGER_ZONE_Y
        || y <= AllianceFlipUtil.forceApplyY(Field.TRENCH_DANGER_ZONE_Y);
  }

  private String getEnergyState() {
    if (DriverStation.isDisabled()) {
      return "DISABLED";
    }
    if (isHoming) {
      return "HOMING";
    }
    return (isCoveredByHopperSignalSupplier.getAsBoolean() ? ControlMode.IDLE : mode).name();
  }

  public void forceHomeAtStart() {
    pitchIO.home(Units.degreesToRadians(ShooterConfig.START_ANGLE_DEGREE));
  }

  public void setHoming(boolean wantHoming) {
    isHoming = wantHoming;

    if (wantHoming) {
      pitchIO.stop();
    }
  }

  public void registerIsCoveredByHopperSignalSupplier(BooleanSupplier signal) {
    isCoveredByHopperSignalSupplier = signal;
  }

  public void resetToDefaultScoreManualDistanceOffset() {
    setManualScoreDistanceOffset(ShooterConfig.DEFAULT_MANUAL_SCORE_DISTANCE_OFFSET);
  }
}
