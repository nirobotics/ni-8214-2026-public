// Copyright (c) 2025 FRC 6328
// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import java.util.function.BooleanSupplier;

public class TriggerUtil {
  /**
   * The given command will never stop or interrupted while the button is held.
   *
   * <p>{@link Command#schedule()} will be started while the trigger is active, and will be canceled
   * only when button is not holding anymore.
   *
   * @param otherStartCondition other condition that decide whether command will be started, will
   *     not affect command ending
   * @param command the command to start
   * @return this trigger, so calls can be chained
   */
  public static Trigger whileHoldingNeverEnd(
      Trigger buttonTrigger, BooleanSupplier otherStartCondition, final Command command) {
    return buttonTrigger
        .and(otherStartCondition)
        .onTrue(
            command
                .until(() -> !buttonTrigger.getAsBoolean())
                .andThen(Commands.waitUntil(() -> !buttonTrigger.getAsBoolean()))
                .withInterruptBehavior(Command.InterruptionBehavior.kCancelIncoming));
  }
}
