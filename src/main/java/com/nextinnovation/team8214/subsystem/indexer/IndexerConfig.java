// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.indexer;

import com.nextinnovation.team8214.Config;

class IndexerConfig {
  static final String LOG_GROUP = Config.LiveDebugGroup.INDEXER.toString();
  static final String LOG_ROOT = "subsystem/indexer";
  static final double GEAR_RATIO = 1.0 / 1.1;
  static final double FEED_VELOCITY_RPS = 5800.0 / 60.0 / GEAR_RATIO * 0.5;

  static final boolean INDEXER_FRONT_RIGHT_INVERTED = true;
  static final boolean INDEXER_BACK_RIGHT_INVERTED = true;
  static final boolean INDEXER_BACK_LEFT_INVERTED = false;
}
