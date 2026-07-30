// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util;

import edu.wpi.first.math.MathUtil;
import lombok.Getter;

public class IdealSimMotor {
  // Here is tricky, because we want the motor can not only spin but also do linear movement
  private static final double MAX_VELOCITY = 600.0;
  private static final double KV = MAX_VELOCITY / 12.0;
  private static final double KA = MAX_VELOCITY / 0.1 / 300.0;

  private final double minPosition;
  private final double maxPosition;

  @Getter private double voltageVolt = 0.0;
  @Getter private double currentAmp = 0.0;
  @Getter private double velocity = 0.0;
  @Getter private double position;

  private double lastPosition = 0.0;

  private enum ControlMode {
    IDLE,
    CURRENT,
    VOLTAGE,
    VELOCITY,
    POSITION
  }

  private ControlMode controlMode = ControlMode.IDLE;

  public IdealSimMotor() {
    this(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
  }

  public IdealSimMotor(double initPosition) {
    this(initPosition, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
  }

  public IdealSimMotor(double initPosition, double minPosition, double maxPosition) {
    position = initPosition;
    this.minPosition = minPosition;
    this.maxPosition = maxPosition;
  }

  public void setVoltageVolt(double voltageVolt) {
    controlMode = ControlMode.VOLTAGE;
    this.voltageVolt = voltageVolt;
  }

  public void setCurrentAmp(double currentAmp) {
    controlMode = ControlMode.CURRENT;
    this.currentAmp = currentAmp;
  }

  public void setVelocity(double velocity) {
    controlMode = ControlMode.VELOCITY;
    this.velocity = velocity;
    lastPosition = position;
  }

  public void setPosition(double position) {
    controlMode = ControlMode.POSITION;
    this.position = MathUtil.clamp(position, minPosition, maxPosition);

    if (position <= minPosition || position >= maxPosition) {
      velocity = 0.0;
    }
  }

  public void update(double periodSec) {
    switch (controlMode) {
      case IDLE -> {
        voltageVolt = 0.0;
        currentAmp = 0.0;
        velocity = 0.0;
      }

      case CURRENT -> {
        if (currentAmp == 0) {
          voltageVolt = 0.0;
          velocity = 0.0;
        } else {
          velocity =
              MathUtil.clamp(velocity + currentAmp * KA * periodSec, -MAX_VELOCITY, MAX_VELOCITY);
        }
        position = MathUtil.clamp(position + velocity * periodSec, minPosition, maxPosition);

        if (position <= minPosition || position >= maxPosition) {
          velocity = 0.0;
        }
      }

      case VOLTAGE -> {
        velocity = MathUtil.clamp(voltageVolt * KV, -MAX_VELOCITY, MAX_VELOCITY);
        position = MathUtil.clamp(position + velocity * periodSec, minPosition, maxPosition);

        if (position <= minPosition || position >= maxPosition) {
          velocity = 0.0;
        }
      }

      case VELOCITY -> {
        position = MathUtil.clamp(position + velocity * periodSec, minPosition, maxPosition);
        velocity = (position - lastPosition) / periodSec;
        lastPosition = position;
        if (position <= minPosition || position >= maxPosition) {
          velocity = 0.0;
        }
      }

      case POSITION -> {}
    }
  }
}
