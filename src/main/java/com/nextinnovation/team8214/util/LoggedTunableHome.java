// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util;

import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.FunctionalCommand;
import edu.wpi.first.wpilibj2.command.Subsystem;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class LoggedTunableHome {
  private final LoggedTunableNumber stallTimeSec;
  private final LoggedTunableNumber currentAmp;
  private final LoggedTunableNumber maxTimeSec;

  public LoggedTunableHome(
      String logRoot, double stallTimeSec, double currentAmp, double maxTimeSec) {
    this("Default", logRoot, stallTimeSec, currentAmp, maxTimeSec);
  }

  public LoggedTunableHome(
      String logGroup, String logRoot, double stallTimeSec, double currentAmp, double maxTimeSec) {
    var cleanLogGroup = logGroup.replaceFirst("/$", "");
    var cleanLogRoot = logRoot.replaceFirst("/$", "") + "/home";

    this.stallTimeSec =
        new LoggedTunableNumber(cleanLogGroup, cleanLogRoot + "/stallTimeSec", stallTimeSec);
    this.currentAmp =
        new LoggedTunableNumber(cleanLogGroup, cleanLogRoot + "/currentAmp", currentAmp);
    this.maxTimeSec =
        new LoggedTunableNumber(cleanLogGroup, cleanLogRoot + "/maxTimeSec", maxTimeSec);
  }

  public double getStallTimeSec() {
    return stallTimeSec.getAsDouble();
  }

  public double getCurrentAmp() {
    return currentAmp.getAsDouble();
  }

  public double getMaxTimeSec() {
    return maxTimeSec.getAsDouble();
  }

  public Command createCommand(
      Subsystem requirement,
      Runnable initialize,
      Runnable applyCurrent,
      BooleanSupplier isStopped,
      Runnable home,
      Runnable cleanup,
      Consumer<Boolean> timeoutAlert) {
    var timer = new Timer();
    var debouncer = new AtomicReference<Debouncer>();
    var stalled = new AtomicBoolean();
    var timedOut = new AtomicBoolean();

    return new FunctionalCommand(
        () -> {
          stalled.set(false);
          timedOut.set(false);
          timeoutAlert.accept(false);
          debouncer.set(new Debouncer(getStallTimeSec(), Debouncer.DebounceType.kRising));
          debouncer.get().calculate(false);
          timer.restart();
          initialize.run();
        },
        () -> {
          applyCurrent.run();
          stalled.set(debouncer.get().calculate(isStopped.getAsBoolean()));
          timedOut.set(!stalled.get() && timer.hasElapsed(getMaxTimeSec()));
        },
        interrupted -> {
          try {
            if (!interrupted && stalled.get()) {
              home.run();
            } else if (!interrupted && timedOut.get()) {
              timeoutAlert.accept(true);
            }
          } finally {
            timer.stop();
            cleanup.run();
          }
        },
        () -> stalled.get() || timedOut.get(),
        requirement);
  }
}
