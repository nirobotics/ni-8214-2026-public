// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.intake;

import com.nextinnovation.team8214.util.LoggedTunableNumber;
import java.util.function.DoubleSupplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum IntakeGoal {
  START(
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/goal/start/pivotPositionDegree", 120.0),
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/goal/start/rollerVoltageVolt", 0.0)),
  IDLE(
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/goal/idle/pivotPositionDegree", 0.0),
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/goal/idle/rollerVoltageVolt", 0.0)),
  SAFE_IDLE(
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP,
          IntakeConfig.LOG_ROOT + "/goal/safeIdle/pivotPositionDegree",
          100.0),
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/goal/safeIdle/rollerVoltageVolt", 0.0)),
  COLLECT(
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/goal/collect/pivotPositionDegree", 0.0),
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/goal/collect/rollerVoltageVolt", 8.0)),
  EJECT(
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/goal/eject/pivotPositionDegree", 0.0),
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/goal/eject/rollerVoltageVolt", -4.0)),
  SCORE(
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/goal/score/pivotPositionDegree", 0.0),
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/goal/score/rollerVoltageVolt", 2.0)),
  TRANSPORT(
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP,
          IntakeConfig.LOG_ROOT + "/goal/transport/pivotPositionDegree",
          0.0),
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP,
          IntakeConfig.LOG_ROOT + "/goal/transport/rollerVoltageVolt",
          2.0)),
  FULL_COMPRESS(
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP,
          IntakeConfig.LOG_ROOT + "/goal/fullCompress/pivotPositionDegree",
          104.5),
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP,
          IntakeConfig.LOG_ROOT + "/goal/fullCompress/rollerVoltageVolt",
          0.8)),
  HOME(
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/goal/home/pivotPositionDegree", 0.0),
      new LoggedTunableNumber(
          IntakeConfig.LOG_GROUP, IntakeConfig.LOG_ROOT + "/goal/home/rollerVoltageVolt", 0.0));

  private final DoubleSupplier pivotPositionDegree;
  private final DoubleSupplier rollerVoltageVolt;

  double getPivotPositionDegree() {
    return pivotPositionDegree.getAsDouble();
  }

  double getRollerVoltageVolt() {
    return rollerVoltageVolt.getAsDouble();
  }
}
