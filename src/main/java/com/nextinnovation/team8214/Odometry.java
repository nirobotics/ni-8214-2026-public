// Copyright (c) 2025 FRC 6328
// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214;

import com.nextinnovation.team8214.subsystem.shooter.ShooterConfig;
import com.nextinnovation.team8214.subsystem.swerve.SwerveConfig;
import com.nextinnovation.team8214.util.AllianceFlipUtil;
import com.nextinnovation.team8214.util.GeomUtil;
import com.nextinnovation.team8214.util.LoggerUtil;
import com.nextinnovation.team8214.util.PolygonUtil;
import com.nextinnovation.team8214.util.VirtualSubsystem;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.math.geometry.*;
import edu.wpi.first.math.interpolation.TimeInterpolatableBuffer;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;

@ExtensionMethod({GeomUtil.class})
public class Odometry extends VirtualSubsystem {
  private static final double CACHED_BUFFER_SIZE_SEC = 2.0;
  private static final Matrix<N3, N1> WHEELED_STD_DEVS =
      new Matrix<>(VecBuilder.fill(0.003, 0.003, 0.0002));

  private static Odometry instance = null;

  public static Odometry getInstance() {
    if (instance == null) {
      instance = new Odometry();
    }

    return instance;
  }

  @AutoLogOutput(key = "odometry/wheeledPose")
  @Getter
  private Pose2d wheeledPose;

  @AutoLogOutput(key = "odometry/estimatedPose")
  @Getter
  private Pose2d estimatedPose;

  @Getter private Twist2d robotCentricVel = new Twist2d();
  private Twist2d trajectoryVel = new Twist2d();
  private final TimeInterpolatableBuffer<Pose2d> poseBuffer =
      TimeInterpolatableBuffer.createBuffer(CACHED_BUFFER_SIZE_SEC);
  private final Matrix<N3, N1> qStdDevs = new Matrix<>(Nat.N3(), Nat.N1());
  private final SwerveDriveKinematics kinematics;
  private SwerveModulePosition[] lastWheelPositions =
      new SwerveModulePosition[] {
        new SwerveModulePosition(),
        new SwerveModulePosition(),
        new SwerveModulePosition(),
        new SwerveModulePosition()
      };
  private Rotation2d lastGyroYaw = new Rotation2d();
  private boolean lastGyroYawValid = false;

  private final Debouncer transportZoneDebouncer = new Debouncer(0.2);
  @Getter private boolean isInTransportZone = false;

  private final Debouncer transportForbiddenZoneDebouncer = new Debouncer(0.1);
  @Getter private boolean isInTransportForbiddenZone = false;

  public record WheeledObservation(
      double timestamp, SwerveModulePosition[] wheelPositions, Rotation2d yaw, boolean yawValid) {}

  public record VisionObservation(double timestamp, Pose2d pose, Matrix<N3, N1> stdDevs) {}

  private final Field2d elasticPose = new Field2d();

  private Odometry() {
    for (int i = 0; i < 3; ++i) {
      qStdDevs.set(i, 0, Math.pow(WHEELED_STD_DEVS.get(i, 0), 2));
    }
    kinematics = SwerveConfig.SWERVE_KINEMATICS;

    resetPose(new Pose2d(Field.LENGTH / 2.0, 0.7, Rotation2d.fromDegrees(0.0)));
  }

  public void addWheeledObservation(WheeledObservation observation) {
    var wheelPositions = observation.wheelPositions();
    var twist = kinematics.toTwist2d(lastWheelPositions, wheelPositions);
    lastWheelPositions = wheelPositions;
    if (observation.yawValid()) {
      if (lastGyroYawValid) {
        twist = new Twist2d(twist.dx, twist.dy, observation.yaw().minus(lastGyroYaw).getRadians());
      }
      lastGyroYaw = observation.yaw();
    }
    lastGyroYawValid = observation.yawValid();
    wheeledPose = wheeledPose.exp(twist);
    poseBuffer.addSample(observation.timestamp, wheeledPose);
    estimatedPose = estimatedPose.exp(twist);
  }

  public Optional<Pose2d> getWheeledPoseByTimestamp(double timestamp) {
    return poseBuffer.getSample(timestamp);
  }

  public void addVisionObservation(VisionObservation observation) {
    try {
      if (poseBuffer.getInternalBuffer().lastKey() - CACHED_BUFFER_SIZE_SEC
          > observation.timestamp()) {
        return;
      }
    } catch (NoSuchElementException ex) {
      return;
    }

    var sample = poseBuffer.getSample(observation.timestamp());
    if (sample.isEmpty()) {
      return;
    }

    var old2NowWheeledPoseTransform = new Transform2d(sample.get(), wheeledPose);
    var now2OldWheeledPoseTransform = new Transform2d(wheeledPose, sample.get());
    var oldEstimatedPose = estimatedPose.plus(now2OldWheeledPoseTransform);

    var r = new double[3];
    for (int i = 0; i < 3; ++i) {
      r[i] = observation.stdDevs().get(i, 0) * observation.stdDevs().get(i, 0);
    }
    var visionK = new Matrix<>(Nat.N3(), Nat.N3());
    for (int row = 0; row < 3; ++row) {
      double stdDev = qStdDevs.get(row, 0);
      if (stdDev == 0.0) {
        visionK.set(row, row, 0.0);
      } else {
        visionK.set(row, row, stdDev / (stdDev + Math.sqrt(stdDev * r[row])));
      }
    }

    var transform = new Transform2d(oldEstimatedPose, observation.pose());
    var kTimesTransform =
        visionK.times(
            VecBuilder.fill(
                transform.getX(), transform.getY(), transform.getRotation().getRadians()));
    var scaledTransform =
        new Transform2d(
            kTimesTransform.get(0, 0),
            kTimesTransform.get(1, 0),
            Rotation2d.fromRadians(kTimesTransform.get(2, 0)));
    estimatedPose = oldEstimatedPose.plus(scaledTransform).plus(old2NowWheeledPoseTransform);
  }

  public void addRobotCentricVel(Twist2d vel) {
    robotCentricVel = vel;
  }

  public void addTrajectoryVel(Twist2d vel) {
    trajectoryVel = vel;
  }

  @AutoLogOutput(key = "odometry/fieldCentricVel")
  public Twist2d getFieldCentricVel() {
    var fieldCentricTranslationVel =
        new Translation2d(robotCentricVel.dx, robotCentricVel.dy)
            .rotateBy(estimatedPose.getRotation());

    return new Twist2d(
        fieldCentricTranslationVel.getX(),
        fieldCentricTranslationVel.getY(),
        robotCentricVel.dtheta);
  }

  @AutoLogOutput(key = "odometry/velNorm")
  public double getLinerVelNorm() {
    var vel = getFieldCentricVel();
    return Math.hypot(vel.dx, vel.dy);
  }

  public void resetPose(Pose2d initialPose) {
    estimatedPose = initialPose;
    wheeledPose = initialPose;
    poseBuffer.clear();
    Logger.recordOutput("odometry/resetPose", initialPose);
    Logger.recordOutput("odometry/resetPoseTimestampSec", LoggerUtil.getTimestampSec());

    if (Config.MODE == Config.Mode.SIM) {
      Sim.getInstance().getSwerve().setSimulationWorldPose(initialPose);
    }
  }

  public Pose2d getShooterInField() {
    return getEstimatedPose()
        .transformBy(ShooterConfig.SHOOTER_IN_ROBOT_POSITION.toPose2d().toTransform2d());
  }

  public Pose2d getPredictedShooterInField(double lookaheadSec) {
    return getPredictedPose(lookaheadSec)
        .transformBy(ShooterConfig.SHOOTER_IN_ROBOT_POSITION.toPose2d().toTransform2d());
  }

  public Pose2d getPredictedPose(double lookaheadSec) {
    var orgVel = getFieldCentricVel();
    var orgPose = getEstimatedPose();
    return new Pose2d(
        orgPose.getX() + orgVel.dx * lookaheadSec,
        orgPose.getY() + orgVel.dy * lookaheadSec,
        orgPose.getRotation().rotateBy(Rotation2d.fromRadians(orgVel.dtheta * lookaheadSec)));
  }

  public void setElasticObjectPoses(String objectName, Pose2d[] poses) {
    elasticPose.getObject(objectName).setPoses(poses);
  }

  public void updateElasticPoses() {
    elasticPose.setRobotPose(Odometry.getInstance().getEstimatedPose());
    SmartDashboard.putData("elasticPose", elasticPose);
  }

  @Override
  public void periodic() {
    isInTransportZone =
        transportZoneDebouncer.calculate(
            AllianceFlipUtil.applyX(getEstimatedPose().getX()) >= Field.TRANSPORT_LINE_X);

    isInTransportForbiddenZone =
        transportForbiddenZoneDebouncer.calculate(
            PolygonUtil.isInPolygon(
                    getEstimatedPose().getTranslation(),
                    AllianceFlipUtil.apply(Field.TRANSPORT_FORBIDDEN_ZONE_CLOSE))
                || PolygonUtil.isInPolygon(
                    getEstimatedPose().getTranslation(),
                    AllianceFlipUtil.apply(Field.TRANSPORT_FORBIDDEN_ZONE_FAR)));

    if (Config.MODE == Config.Mode.SIM) {
      Logger.recordOutput("odometry/simPose", getSimPose());
    }
  }

  public Pose2d getSimPose() {
    return Sim.getInstance().getSwerve().getSimulatedDriveTrainPose();
  }
}
