// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214;

import com.nextinnovation.team8214.util.BooleanChooser;
import com.nextinnovation.team8214.util.LoggedTunableNumber;
import java.util.function.BooleanSupplier;
import lombok.RequiredArgsConstructor;

public final class Config {
  public static final double LOOP_PERIOD_SEC = 0.02;

  public static final boolean IS_LIVE_DEBUG = false;
  public static final boolean ENABLE_UNLIMITED_SHOOT_IN_SIM = false;
  public static final boolean WANT_SCORE_BY_HUB_SHIFT = true;

  @RequiredArgsConstructor
  public enum LiveDebugGroup {
    GLOBAL("global", new BooleanChooser("liveDebugGroupChooser/global")),
    SWERVE("swerve", new BooleanChooser("liveDebugGroupChooser/swerve")),
    INTAKE("intake", new BooleanChooser("liveDebugGroupChooser/intake")),
    INDEXER("indexer", new BooleanChooser("liveDebugGroupChooser/indexer")),
    SHOOTER("shooter", new BooleanChooser("liveDebugGroupChooser/shooter")),
    ODOMETRY("odometry", new BooleanChooser("liveDebugGroupChooser/odometry")),
    SIM("sim", new BooleanChooser("liveDebugGroupChooser/sim")),
    ;

    private final String name;
    private final BooleanSupplier groupActiveSupplier;

    public static void updateGroupActive() {
      LoggedTunableNumber.setGroupActive(
          GLOBAL.toString(), GLOBAL.groupActiveSupplier.getAsBoolean());
      LoggedTunableNumber.setGroupActive(
          SWERVE.toString(), SWERVE.groupActiveSupplier.getAsBoolean());
      LoggedTunableNumber.setGroupActive(
          INTAKE.toString(), INTAKE.groupActiveSupplier.getAsBoolean());
      LoggedTunableNumber.setGroupActive(
          INDEXER.toString(), INDEXER.groupActiveSupplier.getAsBoolean());
      LoggedTunableNumber.setGroupActive(
          SHOOTER.toString(), SHOOTER.groupActiveSupplier.getAsBoolean());
      LoggedTunableNumber.setGroupActive(
          ODOMETRY.toString(), ODOMETRY.groupActiveSupplier.getAsBoolean());

      if (MODE == Mode.SIM) {
        LoggedTunableNumber.setGroupActive(SIM.toString(), SIM.groupActiveSupplier.getAsBoolean());
      }
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public static final Mode MODE = Mode.REAL;

  public enum Mode {
    REAL,
    SIM,
    REPLAY;
  }

  /** Checks whether the correct mode is selected when deploying. */
  public static class CheckDeploy {
    public static void main(String... args) {
      if (MODE != Mode.REAL) {
        System.err.println("Cannot deploy, invalid robot selected: " + MODE);
        System.exit(1);
      }
    }
  }

  /** Checks whether the correct mode is selected when simulating. */
  public static class CheckSim {
    public static void main(String... args) {
      if (MODE == Mode.REAL) {
        System.err.println("Cannot sim, invalid robot selected: " + MODE);
        System.exit(1);
      }
    }
  }

  /** Checks whether the correct mode is selected when running agent auto simulation. */
  public static class CheckAgentAuto {
    public static void main(String... args) {
      if (MODE != Mode.SIM) {
        System.err.println("Cannot run agent auto simulation, invalid robot selected: " + MODE);
        System.exit(1);
      }
    }
  }

  /** Checks whether the correct mode is selected when replaying. */
  public static class CheckReplay {
    public static void main(String... args) {
      if (MODE != Mode.REPLAY) {
        System.err.println("Cannot replay, invalid robot selected: " + MODE);
        System.exit(1);
      }
    }
  }

  /** Checks that the release uses real hardware mode with live debug disabled. */
  public static class CheckReleaseConfig {
    public static void main(String... args) {
      if (MODE != Mode.REAL || IS_LIVE_DEBUG) {
        System.err.println("Release configuration requires MODE=REAL and IS_LIVE_DEBUG=false.");
        System.exit(1);
      }
    }
  }
}
