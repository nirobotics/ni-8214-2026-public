// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.indexer;

import com.nextinnovation.team8214.util.LoggedTunableNumber;
import java.util.function.DoubleSupplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum IndexerGoal {
  IDLE(
      new LoggedTunableNumber(
          IndexerConfig.LOG_GROUP, IndexerConfig.LOG_ROOT + "/goal/idle/indexerVelocityRps", 0.0)),
  PRELOAD(
      new LoggedTunableNumber(
          IndexerConfig.LOG_GROUP,
          IndexerConfig.LOG_ROOT + "/goal/preload/indexerVelocityRps",
          8.0)),
  EJECT(
      new LoggedTunableNumber(
          IndexerConfig.LOG_GROUP, IndexerConfig.LOG_ROOT + "/goal/eject/indexerVelocityRps", 0.0)),
  TRANSPORT(
      new LoggedTunableNumber(
          IndexerConfig.LOG_GROUP,
          IndexerConfig.LOG_ROOT + "/goal/transport/indexerVelocityRps",
          IndexerConfig.FEED_VELOCITY_RPS)),
  FENCE(
      new LoggedTunableNumber(
          IndexerConfig.LOG_GROUP,
          IndexerConfig.LOG_ROOT + "/goal/fence/indexerVelocityRps",
          IndexerConfig.FEED_VELOCITY_RPS)),
  SCORE(
      new LoggedTunableNumber(
          IndexerConfig.LOG_GROUP,
          IndexerConfig.LOG_ROOT + "/goal/score/indexerVelocityRps",
          IndexerConfig.FEED_VELOCITY_RPS));

  private final DoubleSupplier indexerVelocityRps;

  double getVelocityRps() {
    return indexerVelocityRps.getAsDouble();
  }
}
