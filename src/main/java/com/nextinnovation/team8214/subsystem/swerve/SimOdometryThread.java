// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.swerve;

import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.Odometry;
import com.nextinnovation.team8214.util.LoggerUtil;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.wpilibj.Notifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class SimOdometryThread {
  private final Notifier notifier;
  private final List<Supplier<Double>> signals = new ArrayList<>(9);
  private final ArrayBlockingQueue<Odometry.WheeledObservation>
      odometryCachedWheeledObservationQueue = new ArrayBlockingQueue<>(20);
  private final AtomicLong queueOverflowCount;

  SimOdometryThread(
      Supplier<Double> fl_drive_signal,
      Supplier<Double> fl_steer_signal,
      Supplier<Double> bl_drive_signal,
      Supplier<Double> bl_steer_signal,
      Supplier<Double> br_drive_signal,
      Supplier<Double> br_steer_signal,
      Supplier<Double> fr_drive_signal,
      Supplier<Double> fr_steer_signal,
      Supplier<Double> yaw_signal,
      AtomicLong queueOverflowCount) {
    notifier = new Notifier(this::sample);
    notifier.setName("SimOdometryThread");
    this.queueOverflowCount = queueOverflowCount;

    signals.add(0, fl_drive_signal);
    signals.add(1, fl_steer_signal);
    signals.add(2, bl_drive_signal);
    signals.add(3, bl_steer_signal);
    signals.add(4, br_drive_signal);
    signals.add(5, br_steer_signal);
    signals.add(6, fr_drive_signal);
    signals.add(7, fr_steer_signal);
    signals.add(8, yaw_signal);
  }

  ArrayBlockingQueue<Odometry.WheeledObservation> start() {
    notifier.startPeriodic(Config.LOOP_PERIOD_SEC);
    return odometryCachedWheeledObservationQueue;
  }

  private void sample() {
    var observation =
        new Odometry.WheeledObservation(
            LoggerUtil.getTimestampSec(),
            new SwerveModulePosition[] {
              signalValue2SwerveModulePosition(signals.get(0).get(), signals.get(1).get()),
              signalValue2SwerveModulePosition(signals.get(2).get(), signals.get(3).get()),
              signalValue2SwerveModulePosition(signals.get(4).get(), signals.get(5).get()),
              signalValue2SwerveModulePosition(signals.get(6).get(), signals.get(7).get()),
            },
            Rotation2d.fromRadians(signals.get(8).get()),
            true);
    if (!odometryCachedWheeledObservationQueue.offer(observation)) {
      queueOverflowCount.incrementAndGet();
    }
  }

  private SwerveModulePosition signalValue2SwerveModulePosition(
      double rawDrivePosition, double rawSteerPosition) {
    return new SwerveModulePosition(
        rawDrivePosition * SwerveConfig.WHEEL_RADIUS_METER,
        Rotation2d.fromRadians(rawSteerPosition));
  }
}
