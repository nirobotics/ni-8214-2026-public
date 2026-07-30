// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve;

import static edu.wpi.first.units.Units.*;

import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.signals.StaticFeedforwardSignValue;
import com.nextinnovation.team8214.Config;
import edu.wpi.first.math.geometry.*;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.util.Units;
import org.ironmaple.simulation.drivesims.COTS;
import org.ironmaple.simulation.drivesims.configs.DriveTrainSimulationConfig;
import org.ironmaple.simulation.drivesims.configs.SwerveModuleSimulationConfig;

public class SwerveConfig {
  public static final String LOG_GROUP = Config.LiveDebugGroup.SWERVE.toString();
  public static final String LOG_ROOT = "subsystem/swerve";

  static final String FL_MODULE_NAME = "FL";
  static final String BL_MODULE_NAME = "BL";
  static final String BR_MODULE_NAME = "BR";
  static final String FR_MODULE_NAME = "FR";

  static final double ODOMETRY_FREQUENCY_HZ = 250.0;

  public static final double WHEELBASE_LENGTH_METER = 0.278525 * 2.0;
  public static final double WHEELBASE_WIDTH_METER = 0.278525 * 2.0;
  public static final double WHEELBASE_DIAGONAL_METER =
      Math.hypot(WHEELBASE_LENGTH_METER, WHEELBASE_WIDTH_METER);

  static final double ROBOT_HALF_WIDTH_METER = 0.850 / 2.0;
  static final double SHOOTER_SIDE_X_METER = -0.850 / 2.0;
  static final double INTAKE_SIDE_X_METER = 0.6458;

  static final Translation2d[] SCORE_CENTER_OF_ROTATIONS = {
    new Translation2d(SHOOTER_SIDE_X_METER, ROBOT_HALF_WIDTH_METER),
    new Translation2d(SHOOTER_SIDE_X_METER, -ROBOT_HALF_WIDTH_METER),
    new Translation2d(INTAKE_SIDE_X_METER, ROBOT_HALF_WIDTH_METER),
    new Translation2d(INTAKE_SIDE_X_METER, -ROBOT_HALF_WIDTH_METER),
  };

  public static final double MAX_TRANSLATION_VEL_METER_PER_SEC = 4.38912;
  public static final double MAX_ANGULAR_VEL_RAD_PER_SEC =
      MAX_TRANSLATION_VEL_METER_PER_SEC / (WHEELBASE_DIAGONAL_METER / 2.0);

  public static final Translation2d[] MODULE_TRANSLATIONS =
      new Translation2d[] {
        new Translation2d(WHEELBASE_LENGTH_METER / 2.0, WHEELBASE_WIDTH_METER / 2.0),
        new Translation2d(-WHEELBASE_LENGTH_METER / 2.0, WHEELBASE_WIDTH_METER / 2.0),
        new Translation2d(-WHEELBASE_LENGTH_METER / 2.0, -WHEELBASE_WIDTH_METER / 2.0),
        new Translation2d(WHEELBASE_LENGTH_METER / 2.0, -WHEELBASE_WIDTH_METER / 2.0)
      };

  public static final SwerveDriveKinematics SWERVE_KINEMATICS =
      new SwerveDriveKinematics(MODULE_TRANSLATIONS);

  static final double WHEEL_RADIUS_METER = Units.inchesToMeters(2.0);
  static final ModuleConfig FL_MODULE_CONFIG =
      new ModuleConfig(
          getDriveTalonConfig(),
          getSteerTalonNoEncoderConfig(),
          getCancoderConfig(0.5 + 0.44970703125));
  static final ModuleConfig BL_MODULE_CONFIG =
      new ModuleConfig(
          getDriveTalonConfig(),
          getSteerTalonNoEncoderConfig(),
          getCancoderConfig(0.5 - 0.096435546875));
  static final ModuleConfig BR_MODULE_CONFIG =
      new ModuleConfig(
          getDriveTalonConfig(),
          getSteerTalonNoEncoderConfig(),
          getCancoderConfig(0.5 - 0.24755859375));
  static final ModuleConfig FR_MODULE_CONFIG =
      new ModuleConfig(
          getDriveTalonConfig(),
          getSteerTalonNoEncoderConfig(),
          getCancoderConfig(0.5 + 0.010498046875));

  /// WCP X2i Reductions
  /// https://docs.wcproducts.com/welcome/gearboxes/wcp-swerve-x2/general-info/ratio-options
  /// - X1T10: (54.0 / 10.0) * (18.0 / 38.0) * (45.0 / 15.0) = 7.67
  /// - X1T11: (54.0 / 11.0) * (18.0 / 38.0) * (45.0 / 15.0) = 6.98
  /// - X1T12: (54.0 / 12.0) * (18.0 / 38.0) * (45.0 / 15.0) = 6.39
  /// - X2T10: (54.0 / 10.0) * (16.0 / 38.0) * (45.0 / 15.0) = 6.82
  /// - X2T11: (54.0 / 11.0) * (16.0 / 38.0) * (45.0 / 15.0) = 6.20
  /// - X2T12: (54.0 / 12.0) * (16.0 / 38.0) * (45.0 / 15.0) = 5.68
  /// - X3T10: (54.0 / 10.0) * (16.0 / 40.0) * (45.0 / 15.0) = 6.48
  /// - X3T11: (54.0 / 11.0) * (16.0 / 40.0) * (45.0 / 15.0) = 5.89
  /// - X3T12: (54.0 / 12.0) * (16.0 / 40.0) * (45.0 / 15.0) = 5.40
  /// - X4T10: (54.0 / 10.0) * (14.0 / 40.0) * (45.0 / 15.0) = 5.67
  /// - X4T11: (54.0 / 11.0) * (14.0 / 40.0) * (45.0 / 15.0) = 5.15
  /// - X4T12: (54.0 / 12.0) * (14.0 / 40.0) * (45.0 / 15.0) = 4.73
  /// - TURN: (88.0 / 16.0) * (22.0 / 27.0) * (27.0 / 10.0) = 12.1
  ///
  /// SDS MK5n Reductions
  /// https://www.swervedrivespecialties.com/products/mk5n-swerve-module
  /// - R1: (54.0 / 12.0) * (25.0 / 32.0) * (30.0 / 15.0)
  /// - R2: (54.0 / 14.0) * (25.0 / 32.0) * (30.0 / 15.0)
  /// - R3: (54.0 / 16.0) * (25.0 / 32.0) * (30.0 / 15.0)
  /// - TURN: 287.0 / 11.0
  static final double DRIVE_REDUCTION = (54.0 / 12.0) * (25.0 / 32.0) * (30.0 / 15.0);
  static final double STEER_REDUCTION = 287.0 / 11.0;

  record ModuleConfig(
      TalonFXConfiguration driveTalonConfig,
      TalonFXConfiguration steerTalonConfig,
      CANcoderConfiguration cancoderConfig) {}

  static Gains getDriveGains() {
    // Kv unit should be rotations/s
    return switch (Config.MODE) {
      case REAL -> new Gains(2.5, 0.0, 0.124 * DRIVE_REDUCTION, 0.0);
      case SIM, REPLAY -> new Gains(0.2, 0.0, 1.0 / Units.rotationsToRadians(1.0 / 0.91035), 0.03);
    };
  }

  static Gains getSteerGains() {
    return switch (Config.MODE) {
      case REAL -> new Gains(100.0, 0.5, 0.0, 0.1);
      case SIM, REPLAY -> new Gains(4.0, 0.0, 0.0, 0.0);
    };
  }

  record Gains(double kp, double kd, double kv, double ks) {}

  static TalonFXConfiguration getDriveTalonConfig() {
    var config = new TalonFXConfiguration();

    config.MotorOutput.NeutralMode = NeutralModeValue.Brake;
    config.MotorOutput.Inverted = InvertedValue.Clockwise_Positive;

    var gains = getDriveGains();
    config.Slot0 =
        new Slot0Configs()
            .withKP(gains.kp())
            .withKD(gains.kd())
            .withKV(gains.kv())
            .withKS(gains.ks());

    config.CurrentLimits.StatorCurrentLimit = 300.0;
    config.CurrentLimits.StatorCurrentLimitEnable = true;

    config.CurrentLimits.SupplyCurrentLimitEnable = false;

    config.Feedback.SensorToMechanismRatio = DRIVE_REDUCTION;

    return config;
  }

  static TalonFXConfiguration getSteerTalonNoEncoderConfig() {
    var config = new TalonFXConfiguration();

    config.MotorOutput.NeutralMode = NeutralModeValue.Brake;
    config.MotorOutput.Inverted = InvertedValue.CounterClockwise_Positive;

    var gains = getSteerGains();
    config.Slot0 =
        new Slot0Configs()
            .withKP(gains.kp())
            .withKD(gains.kd())
            .withKS(gains.ks())
            .withStaticFeedforwardSign(StaticFeedforwardSignValue.UseClosedLoopSign);

    config.CurrentLimits.StatorCurrentLimit = 70.0;
    config.CurrentLimits.StatorCurrentLimitEnable = true;
    config.CurrentLimits.SupplyCurrentLimit = 20.0;
    config.CurrentLimits.SupplyCurrentLimitEnable = true;

    config.Feedback.RotorToSensorRatio = STEER_REDUCTION;

    config.ClosedLoopGeneral.ContinuousWrap = true;

    return config;
  }

  private static CANcoderConfiguration getCancoderConfig(double magnetOffset) {
    var config = new CANcoderConfiguration();
    config.MagnetSensor.MagnetOffset = magnetOffset;

    return config;
  }

  public static final DriveTrainSimulationConfig DRIVE_SIMULATION_CONFIG =
      DriveTrainSimulationConfig.Default()
          .withRobotMass(Pounds.of(125))
          .withCustomModuleTranslations(MODULE_TRANSLATIONS)
          .withBumperSize(Meters.of(0.8585), Meters.of(0.8585))
          .withGyro(COTS.ofPigeon2())
          .withSwerveModule(
              new SwerveModuleSimulationConfig(
                  DCMotor.getKrakenX60Foc(1),
                  DCMotor.getKrakenX44Foc(1),
                  DRIVE_REDUCTION,
                  STEER_REDUCTION,
                  Volts.of(0.02),
                  Volts.of(0.02),
                  Inches.of(2),
                  KilogramSquareMeters.of(0.01),
                  1.5));
}
