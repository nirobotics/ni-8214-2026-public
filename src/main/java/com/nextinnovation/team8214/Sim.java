// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214;

import static edu.wpi.first.units.Units.*;

import com.nextinnovation.team8214.subsystem.shooter.ShooterConfig;
import com.nextinnovation.team8214.subsystem.swerve.SwerveConfig;
import com.nextinnovation.team8214.util.AllianceFlipUtil;
import com.nextinnovation.team8214.util.LoggedTunableNumber;
import com.nextinnovation.team8214.util.LoggerUtil;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.*;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.simulation.RoboRioSim;
import java.util.List;
import java.util.function.DoubleSupplier;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Mass;
import org.dyn4j.geometry.MassType;
import org.ironmaple.simulation.IntakeSimulation;
import org.ironmaple.simulation.SimulatedArena;
import org.ironmaple.simulation.drivesims.SwerveDriveSimulation;
import org.ironmaple.simulation.motorsims.SimulatedBattery;
import org.ironmaple.simulation.seasonspecific.rebuilt2026.Arena2026Rebuilt;
import org.ironmaple.simulation.seasonspecific.rebuilt2026.RebuiltFuelOnField;
import org.ironmaple.simulation.seasonspecific.rebuilt2026.RebuiltFuelOnFly;
import org.ironmaple.utils.mathutils.GeometryConvertor;
import org.littletonrobotics.junction.Logger;

public class Sim {
  private static final LoggedTunableNumber RANDOM_FLYWHEEL_VEL_ERR_METER_PER_SEC =
      new LoggedTunableNumber(
          Config.LiveDebugGroup.SIM.toString(), "sim/randomFlywheelVelErrMeterPerSec", 0.1);

  private static final LoggedTunableNumber RANDOM_YAW_ERR_DEGREE =
      new LoggedTunableNumber(Config.LiveDebugGroup.SIM.toString(), "sim/randomYawErrDegree", 1.5);

  private static final LoggedTunableNumber MULTITRACK_SHOOT_SPACING_METER =
      new LoggedTunableNumber(
          Config.LiveDebugGroup.SIM.toString(), "sim/multiTrackShootSpacingMeter", 0.16);

  private static final LoggedTunableNumber FUEL_STORAGE_MOI_RADIUS_METER =
      new LoggedTunableNumber(
          Config.LiveDebugGroup.SIM.toString(), "sim/load/fuelStorageMoiRadiusMeter", 0.35);

  private static final double FUEL_MASS_KG = 0.227;
  private static final double MAX_SHOOT_INTERVAL_SEC = 1.0 / 8.0;
  static final double CONSTANT_BUS_VOLTAGE_VOLT = 13.5;

  @Getter private final SwerveDriveSimulation swerve;
  @Getter private final IntakeSimulation intake;
  private final Mass baseSwerveMass;
  private final List<?> simulatedBatteryElectricalAppliances;

  private static Sim instance = null;

  @Setter private DoubleSupplier velMeterPerSecSupplier = () -> 0;
  @Setter private DoubleSupplier angleRadSupplier = () -> 0;

  private double lastShootTimestamp = -1;

  private double totalScore = 0;
  private final BumpTerrain bumpTerrain = new BumpTerrain();
  private BumpState bumpState = BumpState.flat();

  public static Sim getInstance() {
    if (instance == null) {
      instance = new Sim();
    }

    return instance;
  }

  private Sim() {
    SimulatedArena.overrideInstance(new FixedTrenchArena2026());

    swerve =
        new SwerveDriveSimulation(
            SwerveConfig.DRIVE_SIMULATION_CONFIG,
            new Pose2d(Field.LENGTH / 2.0, Field.WIDTH / 2.0, new Rotation2d()));

    baseSwerveMass = swerve.getMass().copy();

    intake =
        IntakeSimulation.OverTheBumperIntake(
            "Fuel",
            swerve,
            Meters.of(0.6985),
            Inches.of(8.0),
            IntakeSimulation.IntakeSide.FRONT,
            55);

    simulatedBatteryElectricalAppliances = getSimulatedBatteryElectricalAppliances();
    simulatedBatteryElectricalAppliances.clear();
  }

  public void loadFuelForAuto() {
    loadFuelForTeleop();
  }

  public void loadFuelForTeleop() {
    if (Config.ENABLE_UNLIMITED_SHOOT_IN_SIM) {
      return;
    }
    SimulatedArena.getInstance().clearGamePieces();
    intake.setGamePiecesCount(8);
    addGrid(Field.LENGTH / 2.0, Field.WIDTH / 2.0, 12, 30, 0.16);

    var redDepotCenter = AllianceFlipUtil.forceApply(Field.DEPOT_CENTER);
    addGrid(Field.DEPOT_CENTER.getX(), Field.DEPOT_CENTER.getY(), 4, 6, 0.16);
    addGrid(redDepotCenter.getX(), redDepotCenter.getY(), 4, 6, 0.16);
  }

  void periodic() {
    holdConstantBusVoltage();
    SimulatedArena.getInstance().simulationPeriodic();
    holdConstantBusVoltage();
    updateDynamicLoad();

    Pose3d[] fuelPoses = SimulatedArena.getInstance().getGamePiecesArrayByType("Fuel");
    Logger.recordOutput("sim/fuelPoses", fuelPoses);

    bumpState = bumpTerrain.calculate(swerve.getSimulatedDriveTrainPose());
    Logger.recordOutput("sim/robotPose3d", bumpState.robotPose3d());
    Logger.recordOutput("sim/bump/isOnBump", bumpState.isOnBump());
    Logger.recordOutput("sim/bump/heightMeter", bumpState.heightMeter());
    Logger.recordOutput("sim/bump/rollRad", bumpState.rollRad());
    Logger.recordOutput("sim/bump/pitchRad", bumpState.pitchRad());
    Logger.recordOutput("sim/bump/moduleContactPoses", bumpState.moduleContactPoses());
  }

  private static List<?> getSimulatedBatteryElectricalAppliances() {
    try {
      var appliancesField = SimulatedBattery.class.getDeclaredField("electricalAppliances");
      appliancesField.setAccessible(true);
      Object appliances = appliancesField.get(null);
      if (appliances instanceof List<?> applianceList) {
        return applianceList;
      }
      throw new IllegalStateException("MapleSim battery appliance registry is not a List");
    } catch (ReflectiveOperationException | SecurityException exception) {
      throw new IllegalStateException("Unable to disable the MapleSim battery model", exception);
    }
  }

  private void holdConstantBusVoltage() {
    // MapleSim 0.4.0-beta has no public battery-disable API. Removing its load registry keeps
    // drivetrain motor/current physics active while preventing current draw from reducing voltage.
    simulatedBatteryElectricalAppliances.clear();
    RoboRioSim.setVInVoltage(CONSTANT_BUS_VOLTAGE_VOLT);
  }

  public double getBumpRollRad() {
    return bumpState.rollRad();
  }

  public double getBumpPitchRad() {
    return bumpState.pitchRad();
  }

  public void shoot() {
    var currentTimestamp = LoggerUtil.getTimestampSec();
    if (currentTimestamp - lastShootTimestamp >= MAX_SHOOT_INTERVAL_SEC) {
      lastShootTimestamp = currentTimestamp;
      var spacingMeter = MULTITRACK_SHOOT_SPACING_METER.get();
      addFuelShootProjectile(spacingMeter * 0.5);
      addFuelShootProjectile(spacingMeter * 1.5);
      addFuelShootProjectile(-spacingMeter * 0.5);
      addFuelShootProjectile(-spacingMeter * 1.5);
    }
  }

  private void addBall(double x, double y) {
    SimulatedArena.getInstance().addGamePiece(new RebuiltFuelOnField(new Translation2d(x, y)));
  }

  void addGrid(double centerX, double centerY, int cols, int rows, double spacing) {
    double startX = centerX - (cols - 1) * spacing / 2.0;
    double startY = centerY - (rows - 1) * spacing / 2.0;
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        addBall(startX + c * spacing, startY + r * spacing);
      }
    }
  }

  private void updateDynamicLoad() {
    var storedFuelCount = Math.max(0, intake.getGamePiecesAmount());
    var fuelMassKg = storedFuelCount * FUEL_MASS_KG;
    var fuelStorageMoiRadiusMeter = FUEL_STORAGE_MOI_RADIUS_METER.get();
    var robotMassKg = baseSwerveMass.getMass() + fuelMassKg;
    var robotMoiKgM2 =
        baseSwerveMass.getInertia()
            + fuelMassKg * fuelStorageMoiRadiusMeter * fuelStorageMoiRadiusMeter;

    swerve.setMass(new Mass(baseSwerveMass.getCenter().copy(), robotMassKg, robotMoiKgM2));

    Logger.recordOutput("sim/load/storedFuelCount", storedFuelCount);
    Logger.recordOutput("sim/load/fuelMassKg", fuelMassKg);
    Logger.recordOutput("sim/load/baseRobotMassKg", baseSwerveMass.getMass());
    Logger.recordOutput("sim/load/robotMassKg", robotMassKg);
    Logger.recordOutput("sim/load/baseRobotMoiKgM2", baseSwerveMass.getInertia());
    Logger.recordOutput("sim/load/robotMoiKgM2", robotMoiKgM2);
  }

  private double getRandomFlywheelErrMeterPerSec() {
    return getRandomErr(RANDOM_FLYWHEEL_VEL_ERR_METER_PER_SEC.get());
  }

  private double getRandomYawErrDegree() {
    return getRandomErr(RANDOM_YAW_ERR_DEGREE.get());
  }

  private void addFuelFromHub() {
    totalScore++;
    Logger.recordOutput("sim/totalScore", totalScore);

    if (Config.ENABLE_UNLIMITED_SHOOT_IN_SIM) {
      return;
    }

    var normalizedRandom = (int) (4.0 * Math.random());

    Translation2d hubExportPlacement;
    Rotation2d exportDirection;

    switch (normalizedRandom) {
      case 0 -> {
        hubExportPlacement = new Translation2d(0.0, 0.13375 * 3);
        exportDirection = Rotation2d.fromDegrees(45.0);
      }
      case 1 -> {
        hubExportPlacement = new Translation2d(0.0, 0.13375);
        exportDirection = Rotation2d.fromDegrees(0.0);
      }
      case 2 -> {
        hubExportPlacement = new Translation2d(0.0, -0.13375);
        exportDirection = Rotation2d.fromDegrees(0.0);
      }
      default -> {
        hubExportPlacement = new Translation2d(0.0, -0.13375 * 3);
        exportDirection = Rotation2d.fromDegrees(-45.0);
      }
    }

    SimulatedArena.getInstance()
        .addGamePieceProjectile(
            new RebuiltFuelOnFly(
                    AllianceFlipUtil.apply(Field.HUB_EXPORT.plus(hubExportPlacement)),
                    new Translation2d(),
                    new ChassisSpeeds(),
                    AllianceFlipUtil.apply(
                        exportDirection.rotateBy(
                            Rotation2d.fromDegrees(getRandomHubExportAngleErrDegree()))),
                    Meters.of(Field.HUB_EXPORT_HEIGHT),
                    MetersPerSecond.of(3.0 + getRandomHubExportVelErrMeterPerSec()),
                    Degrees.of(-21.0))
                .enableBecomesGamePieceOnFieldAfterTouchGround());
  }

  private double getRandomHubExportVelErrMeterPerSec() {
    return getRandomErr(0.2);
  }

  private double getRandomHubExportAngleErrDegree() {
    return getRandomErr(5.0);
  }

  private double getRandomErr(double scope) {
    var normalizedRandom = 2.0 * Math.random() - 1.0;

    return normalizedRandom * scope;
  }

  private void addFuelShootProjectile(double offsetY) {
    if (intake.getGamePiecesAmount() <= 0) {
      return;
    }

    var robotPose3d = bumpState.robotPose3d();
    var shooterInField =
        robotPose3d.plus(
            new Transform3d(
                ShooterConfig.SHOOTER_IN_ROBOT_POSITION.getTranslation(),
                ShooterConfig.SHOOTER_IN_ROBOT_POSITION.getRotation()));
    var shooterYaw =
        Odometry.getInstance()
            .getShooterInField()
            .getRotation()
            .rotateBy(Rotation2d.fromDegrees(getRandomYawErrDegree()))
            .rotateBy(Rotation2d.k180deg);

    SimulatedArena.getInstance()
        .addGamePieceProjectile(
            new RebuiltFuelOnFly(
                    shooterInField
                        .getTranslation()
                        .toTranslation2d()
                        .plus(
                            new Translation2d(
                                offsetY,
                                Odometry.getInstance()
                                    .getShooterInField()
                                    .getRotation()
                                    .rotateBy(Rotation2d.kCW_90deg))),
                    new Translation2d(),
                    swerve.getDriveTrainSimulatedChassisSpeedsFieldRelative(),
                    shooterYaw,
                    Meters.of(shooterInField.getZ()),
                    MetersPerSecond.of(
                        velMeterPerSecSupplier.getAsDouble() * 0.92
                            + getRandomFlywheelErrMeterPerSec()),
                    Radians.of(
                        angleRadSupplier.getAsDouble()
                            + bumpState.getTiltAlongFieldDirection(
                                shooterYaw, robotPose3d.toPose2d().getRotation())))
                .enableBecomesGamePieceOnFieldAfterTouchGround()
                .withHitTargetCallBack(this::addFuelFromHub));

    if (!Config.ENABLE_UNLIMITED_SHOOT_IN_SIM) {
      intake.setGamePiecesCount(intake.getGamePiecesAmount() - 1);
    }
  }

  public void outpostFeed() {
    intake.setGamePiecesCount(24);
  }

  static final class FixedTrenchArena2026 extends Arena2026Rebuilt {
    FixedTrenchArena2026() {
      super(false);

      // ponytail: MapleSim 0.4.0-beta omits this wall; remove when the vendordep fixes it.
      var obstacle = new Body();
      var fixture =
          obstacle.addFixture(
              Geometry.createRectangle(Inches.of(53).in(Meters), Inches.of(12).in(Meters)));
      fixture.setFriction(0.6);
      fixture.setRestitution(0.3);
      obstacle.setMass(MassType.INFINITE);
      obstacle
          .getTransform()
          .set(
              GeometryConvertor.toDyn4jTransform(
                  new Pose2d(
                      8.27 + Inches.of(143.5).in(Meters),
                      4.035 + Inches.of(102.5).in(Meters),
                      Rotation2d.kZero)));
      physicsWorld.addBody(obstacle);
    }
  }

  record BumpState(
      Pose3d robotPose3d,
      boolean isOnBump,
      double heightMeter,
      double rollRad,
      double pitchRad,
      Pose3d[] moduleContactPoses) {
    static BumpState flat() {
      return new BumpState(new Pose3d(), false, 0.0, 0.0, 0.0, new Pose3d[0]);
    }

    double getTiltAlongFieldDirection(Rotation2d fieldDirection, Rotation2d robotHeading) {
      var robotDirection = fieldDirection.minus(robotHeading);
      var slopeX = -Math.tan(pitchRad);
      var slopeY = Math.tan(rollRad);
      return Math.atan(slopeX * robotDirection.getCos() + slopeY * robotDirection.getSin());
    }
  }

  static final class BumpTerrain {
    private static final double HUB_WIDTH_METER =
        edu.wpi.first.math.util.Units.inchesToMeters(47.0);
    private static final double BUMP_DEPTH_X_METER =
        edu.wpi.first.math.util.Units.inchesToMeters(44.4);
    private static final double BUMP_WIDTH_Y_METER =
        edu.wpi.first.math.util.Units.inchesToMeters(73.0);
    private static final double BUMP_HEIGHT_METER =
        edu.wpi.first.math.util.Units.inchesToMeters(6.513);

    private final BumpRegion[] regions = createRegions();

    BumpState calculate(Pose2d robotPose) {
      var moduleContactPoses = new Pose3d[SwerveConfig.MODULE_TRANSLATIONS.length];
      var sumXHeight = 0.0;
      var sumYHeight = 0.0;
      var sumX2 = 0.0;
      var sumY2 = 0.0;
      var isOnBump = false;

      for (var i = 0; i < SwerveConfig.MODULE_TRANSLATIONS.length; i++) {
        var moduleInRobot = SwerveConfig.MODULE_TRANSLATIONS[i];
        var moduleInField =
            robotPose.getTranslation().plus(moduleInRobot.rotateBy(robotPose.getRotation()));
        var height = getHeight(moduleInField);

        moduleContactPoses[i] =
            new Pose3d(moduleInField.getX(), moduleInField.getY(), height, new Rotation3d());
        sumXHeight += moduleInRobot.getX() * height;
        sumYHeight += moduleInRobot.getY() * height;
        sumX2 += moduleInRobot.getX() * moduleInRobot.getX();
        sumY2 += moduleInRobot.getY() * moduleInRobot.getY();
        isOnBump = isOnBump || height > 0.0;
      }

      var heightMeter = getHeight(robotPose.getTranslation());
      var slopeX = sumX2 > 0.0 ? sumXHeight / sumX2 : 0.0;
      var slopeY = sumY2 > 0.0 ? sumYHeight / sumY2 : 0.0;
      var pitchRad = Math.atan(-slopeX);
      var rollRad = Math.atan(slopeY);
      var robotPose3d =
          new Pose3d(
              robotPose.getX(),
              robotPose.getY(),
              heightMeter,
              new Rotation3d(rollRad, pitchRad, robotPose.getRotation().getRadians()));

      return new BumpState(
          robotPose3d, isOnBump, heightMeter, rollRad, pitchRad, moduleContactPoses);
    }

    private double getHeight(Translation2d position) {
      for (var region : regions) {
        var height = region.getHeight(position);
        if (height > 0.0) {
          return height;
        }
      }
      return 0.0;
    }

    private static BumpRegion[] createRegions() {
      var blueTop = createBlueRegion(true);
      var blueBottom = createBlueRegion(false);

      return new BumpRegion[] {blueTop, blueBottom, forceApply(blueTop), forceApply(blueBottom)};
    }

    private static BumpRegion createBlueRegion(boolean wantTop) {
      var minX = Field.HUB_CENTER.getX() - BUMP_DEPTH_X_METER / 2.0;
      var maxX = Field.HUB_CENTER.getX() + BUMP_DEPTH_X_METER / 2.0;
      var hubHalfWidth = HUB_WIDTH_METER / 2.0;

      if (wantTop) {
        var minY = Field.HUB_CENTER.getY() + hubHalfWidth;
        return new BumpRegion(minX, maxX, minY, minY + BUMP_WIDTH_Y_METER);
      }

      var maxY = Field.HUB_CENTER.getY() - hubHalfWidth;
      return new BumpRegion(minX, maxX, maxY - BUMP_WIDTH_Y_METER, maxY);
    }

    private static BumpRegion forceApply(BumpRegion region) {
      var a = AllianceFlipUtil.forceApply(new Translation2d(region.minX(), region.minY()));
      var b = AllianceFlipUtil.forceApply(new Translation2d(region.maxX(), region.maxY()));

      return new BumpRegion(
          Math.min(a.getX(), b.getX()),
          Math.max(a.getX(), b.getX()),
          Math.min(a.getY(), b.getY()),
          Math.max(a.getY(), b.getY()));
    }

    private record BumpRegion(double minX, double maxX, double minY, double maxY) {
      double getHeight(Translation2d position) {
        if (position.getX() < minX
            || position.getX() > maxX
            || position.getY() < minY
            || position.getY() > maxY) {
          return 0.0;
        }

        var centerX = (minX + maxX) / 2.0;
        var normalizedDistance =
            MathUtil.clamp(
                Math.abs(position.getX() - centerX) / (BUMP_DEPTH_X_METER / 2.0), 0.0, 1.0);
        return BUMP_HEIGHT_METER * (1.0 - normalizedDistance);
      }
    }
  }
}
