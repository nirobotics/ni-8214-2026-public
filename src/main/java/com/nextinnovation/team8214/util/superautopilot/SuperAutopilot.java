// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util.superautopilot;

import static edu.wpi.first.units.Units.Meters;
import static edu.wpi.first.units.Units.MetersPerSecond;

import com.nextinnovation.team8214.util.EqualsUtil;
import com.therekrab.autopilot.APProfile;
import com.therekrab.autopilot.APTarget;
import com.therekrab.autopilot.Autopilot;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;

public class SuperAutopilot {
  private final SAPProfile profile;
  private final Autopilot autopilot;

  private Pose2d lastCurrentPose = new Pose2d();
  private APTarget lastTarget = null;

  public SuperAutopilot(SAPProfile profile) {
    this.profile = profile;
    this.autopilot =
        new Autopilot(
            new APProfile(profile.toAPConstraints()).withBeelineRadius(profile.beelineRadius()));
  }

  /**
   * Calculates autopilot result for the given target. CRITICAL: APTarget MUST have entryAngle set,
   * otherwise program will crash.
   *
   * @param current The robot's current pose
   * @param velocity The robot's current robot relative velocity
   * @param target The target the robot should drive towards (MUST have entryAngle)
   * @return The calculated autopilot result
   * @throws RuntimeException if target.getEntryAngle() is empty
   */
  public Autopilot.APResult calculate(Pose2d current, Twist2d velocity, APTarget target) {
    if (target.getEntryAngle().isEmpty()) {
      throw new RuntimeException(
          "FATAL ERROR: APTarget.entryAngle is null! SuperAutopilot requires entryAngle to be set!");
    }

    var currentPos = current.getTranslation();
    var targetPos = target.getReference().getTranslation();
    var distanceToTarget = currentPos.getDistance(targetPos);

    lastCurrentPose = current;
    lastTarget = target;

    if (shouldUseOriginalAutopilot(distanceToTarget, currentPos, targetPos, target)) {
      return applyToleranceClamping(
          autopilot.calculate(
              current, new ChassisSpeeds(velocity.dx, velocity.dy, velocity.dtheta), target));
    } else {
      return calculateTransitionVelocity(currentPos, targetPos, target);
    }
  }

  private boolean shouldUseOriginalAutopilot(
      double distanceToTarget, Translation2d currentPos, Translation2d targetPos, APTarget target) {
    if (distanceToTarget >= profile.autopilotRadius().in(Meters)) {
      return false;
    }

    var targetToCurrent = currentPos.minus(targetPos);
    if (targetToCurrent.equals(Translation2d.kZero)) {
      return true;
    }

    var oppositeEntryAngle = target.getEntryAngle().get().plus(Rotation2d.fromRadians(Math.PI));
    var angleTowardsRobot = new Rotation2d(targetToCurrent.getX(), targetToCurrent.getY());

    return EqualsUtil.GeomExtensions.epsilonEquals(
        angleTowardsRobot, oppositeEntryAngle, Math.PI / 2, true);
  }

  private Autopilot.APResult calculateTransitionVelocity(
      Translation2d currentPos, Translation2d targetPos, APTarget target) {
    var transitionPoint = calculateTransitionPoint(targetPos, target);
    var toTransitionPoint = transitionPoint.minus(currentPos);

    if (toTransitionPoint.equals(Translation2d.kZero)) {
      return new Autopilot.APResult(
          MetersPerSecond.zero(), MetersPerSecond.zero(), target.getReference().getRotation());
    }

    var velocity = new Translation2d(profile.velocityMeterPerSec(), toTransitionPoint.getAngle());

    return new Autopilot.APResult(
        MetersPerSecond.of(velocity.getX()),
        MetersPerSecond.of(velocity.getY()),
        target.getReference().getRotation());
  }

  private Translation2d calculateTransitionPoint(Translation2d targetPos, APTarget target) {
    var entryAngle = target.getEntryAngle().get();
    var oppositeDirection = entryAngle.plus(Rotation2d.fromRadians(Math.PI));

    var shiftVector =
        new Translation2d(profile.transitionPointShiftingX().in(Meters), oppositeDirection);

    return targetPos.plus(shiftVector);
  }

  private Autopilot.APResult applyToleranceClamping(Autopilot.APResult result) {
    if (lastTarget == null
        || !atPos(lastCurrentPose, lastTarget.getReference(), lastTarget.getEntryAngle().get())) {
      return result;
    }

    return new Autopilot.APResult(
        MetersPerSecond.zero(), MetersPerSecond.zero(), result.targetAngle());
  }

  private Translation2d transformToEntryAngleFrame(Translation2d vector, Rotation2d entryAngle) {
    return vector.rotateBy(entryAngle.unaryMinus());
  }

  private Translation2d getPositionErrInEntryAngleFrame(
      Pose2d current, Pose2d goal, Rotation2d entryAngle) {
    var positionError = current.getTranslation().minus(goal.getTranslation());
    return transformToEntryAngleFrame(positionError, entryAngle);
  }

  public boolean atX(Pose2d current, Pose2d goal, Rotation2d entryAngle) {
    return EqualsUtil.epsilonEquals(
        getPositionErrInEntryAngleFrame(current, goal, entryAngle).getX(),
        0.0,
        profile.errorX().in(Meters));
  }

  public boolean atY(Pose2d current, Pose2d goal, Rotation2d entryAngle) {
    return EqualsUtil.epsilonEquals(
        getPositionErrInEntryAngleFrame(current, goal, entryAngle).getY(),
        0.0,
        profile.errorY().in(Meters));
  }

  public boolean atPos() {
    return atPos(lastCurrentPose, lastTarget.getReference(), lastTarget.getEntryAngle().get());
  }

  public boolean atPos(Pose2d current, Pose2d goal, Rotation2d entryAngle) {
    var positionError = getPositionErrInEntryAngleFrame(current, goal, entryAngle);

    return EqualsUtil.epsilonEquals(positionError.getX(), 0.0, profile.errorX().in(Meters))
        && EqualsUtil.epsilonEquals(positionError.getY(), 0.0, profile.errorY().in(Meters));
  }
}
