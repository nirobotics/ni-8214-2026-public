// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util.oi;

import edu.wpi.first.hal.FRCNetComm;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.event.BooleanEvent;
import edu.wpi.first.wpilibj.event.EventLoop;

public class Vader4ProController extends GenericHID {
  public enum Button {
    A(2),
    B(3),
    C(4),
    X(5),
    Y(6),
    Z(7),
    LEFT_BUMPER(8),
    RIGHT_BUMPER(9),
    LEFT_TRIGGER(10),
    RIGHT_TRIGGER(11),
    SELECT(12),
    START(13),
    LEFT_STICK(14),
    RIGHT_STICK(15),
    BACK_RIGHT(16),
    BACK_LEFT(17),
    BACK_MID_RIGHT(18),
    BACK_MID_LEFT(19);

    public final int value;

    Button(int value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.name().substring(1) + "Button";
    }
  }

  public enum Axis {
    LEFT_X(0),
    RIGHT_X(3),
    LEFT_Y(1),
    RIGHT_Y(4);

    public final int value;

    Axis(int value) {
      this.value = value;
    }

    @Override
    public String toString() {
      var name = this.name().substring(1);
      if (name.endsWith("Trigger")) {
        return name + "Axis";
      }
      return name;
    }
  }

  public Vader4ProController(final int port) {
    super(port);
    HAL.report(FRCNetComm.tResourceType.kResourceType_XboxController, port + 1);
  }

  public double getLeftX() {
    return getRawAxis(Axis.LEFT_X.value);
  }

  public double getRightX() {
    return getRawAxis(Axis.RIGHT_X.value);
  }

  public double getLeftY() {
    return getRawAxis(Axis.LEFT_Y.value);
  }

  public double getRightY() {
    return getRawAxis(Axis.RIGHT_Y.value);
  }

  public boolean getLeftTriggerButton() {
    return getRawButton(Button.LEFT_TRIGGER.value);
  }

  public boolean getLeftTriggerButtonPressed() {
    return getRawButtonPressed(Button.LEFT_TRIGGER.value);
  }

  public boolean getLeftTriggerButtonReleased() {
    return getRawButtonReleased(Button.LEFT_TRIGGER.value);
  }

  public BooleanEvent leftTrigger(EventLoop loop) {
    return button(Button.LEFT_TRIGGER.value, loop);
  }

  public boolean getRightTriggerButton() {
    return getRawButton(Button.RIGHT_TRIGGER.value);
  }

  public boolean getRightTriggerButtonPressed() {
    return getRawButtonPressed(Button.RIGHT_TRIGGER.value);
  }

  public boolean getRightTriggerButtonReleased() {
    return getRawButtonReleased(Button.RIGHT_TRIGGER.value);
  }

  public BooleanEvent rightTrigger(EventLoop loop) {
    return button(Button.RIGHT_TRIGGER.value, loop);
  }

  public boolean getAButton() {
    return getRawButton(Button.A.value);
  }

  public boolean getAButtonPressed() {
    return getRawButtonPressed(Button.A.value);
  }

  public boolean getAButtonReleased() {
    return getRawButtonReleased(Button.A.value);
  }

  public BooleanEvent a(EventLoop loop) {
    return button(Button.A.value, loop);
  }

  public boolean getBButton() {
    return getRawButton(Button.B.value);
  }

  public boolean getBButtonPressed() {
    return getRawButtonPressed(Button.B.value);
  }

  public boolean getBButtonReleased() {
    return getRawButtonReleased(Button.B.value);
  }

  public BooleanEvent b(EventLoop loop) {
    return button(Button.B.value, loop);
  }

  public boolean getXButton() {
    return getRawButton(Button.X.value);
  }

  public boolean getXButtonPressed() {
    return getRawButtonPressed(Button.X.value);
  }

  public boolean getXButtonReleased() {
    return getRawButtonReleased(Button.X.value);
  }

  public BooleanEvent x(EventLoop loop) {
    return button(Button.X.value, loop);
  }

  public boolean getYButton() {
    return getRawButton(Button.Y.value);
  }

  public boolean getYButtonPressed() {
    return getRawButtonPressed(Button.Y.value);
  }

  public boolean getYButtonReleased() {
    return getRawButtonReleased(Button.Y.value);
  }

  public BooleanEvent y(EventLoop loop) {
    return button(Button.Y.value, loop);
  }

  public boolean getCButton() {
    return getRawButton(Button.C.value);
  }

  public boolean getCButtonPressed() {
    return getRawButtonPressed(Button.C.value);
  }

  public boolean getCButtonReleased() {
    return getRawButtonReleased(Button.C.value);
  }

  public BooleanEvent c(EventLoop loop) {
    return button(Button.C.value, loop);
  }

  public boolean getZButton() {
    return getRawButton(Button.Z.value);
  }

  public boolean getZButtonPressed() {
    return getRawButtonPressed(Button.Z.value);
  }

  public boolean getZButtonReleased() {
    return getRawButtonReleased(Button.Z.value);
  }

  public BooleanEvent z(EventLoop loop) {
    return button(Button.Z.value, loop);
  }

  public boolean getLeftBumperButton() {
    return getRawButton(Button.LEFT_BUMPER.value);
  }

  public boolean getLeftBumperButtonPressed() {
    return getRawButtonPressed(Button.LEFT_BUMPER.value);
  }

  public boolean getLeftBumperButtonReleased() {
    return getRawButtonReleased(Button.LEFT_BUMPER.value);
  }

  public BooleanEvent leftBumper(EventLoop loop) {
    return button(Button.LEFT_BUMPER.value, loop);
  }

  public boolean getRightBumperButton() {
    return getRawButton(Button.RIGHT_BUMPER.value);
  }

  public boolean getRightBumperButtonPressed() {
    return getRawButtonPressed(Button.RIGHT_BUMPER.value);
  }

  public boolean getRightBumperButtonReleased() {
    return getRawButtonReleased(Button.RIGHT_BUMPER.value);
  }

  public BooleanEvent rightBumper(EventLoop loop) {
    return button(Button.RIGHT_BUMPER.value, loop);
  }

  public boolean getSelectButton() {
    return getRawButton(Button.SELECT.value);
  }

  public boolean getSelectButtonPressed() {
    return getRawButtonPressed(Button.SELECT.value);
  }

  public boolean getSelectButtonReleased() {
    return getRawButtonReleased(Button.SELECT.value);
  }

  public BooleanEvent select(EventLoop loop) {
    return button(Button.SELECT.value, loop);
  }

  public boolean getStartButton() {
    return getRawButton(Button.START.value);
  }

  public boolean getStartButtonPressed() {
    return getRawButtonPressed(Button.START.value);
  }

  public boolean getStartButtonReleased() {
    return getRawButtonReleased(Button.START.value);
  }

  public BooleanEvent start(EventLoop loop) {
    return button(Button.START.value, loop);
  }

  public boolean getLeftStickButton() {
    return getRawButton(Button.LEFT_STICK.value);
  }

  public boolean getLeftStickButtonPressed() {
    return getRawButtonPressed(Button.LEFT_STICK.value);
  }

  public boolean getLeftStickButtonReleased() {
    return getRawButtonReleased(Button.LEFT_STICK.value);
  }

  public BooleanEvent leftStick(EventLoop loop) {
    return button(Button.LEFT_STICK.value, loop);
  }

  public boolean getRightStickButton() {
    return getRawButton(Button.RIGHT_STICK.value);
  }

  public boolean getRightStickButtonPressed() {
    return getRawButtonPressed(Button.RIGHT_STICK.value);
  }

  public boolean getRightStickButtonReleased() {
    return getRawButtonReleased(Button.RIGHT_STICK.value);
  }

  public BooleanEvent rightStick(EventLoop loop) {
    return button(Button.RIGHT_STICK.value, loop);
  }

  public boolean getBackRightButton() {
    return getRawButton(Button.BACK_RIGHT.value);
  }

  public boolean getBackRightButtonPressed() {
    return getRawButtonPressed(Button.BACK_RIGHT.value);
  }

  public boolean getBackRightButtonReleased() {
    return getRawButtonReleased(Button.BACK_RIGHT.value);
  }

  public BooleanEvent backRight(EventLoop loop) {
    return button(Button.BACK_RIGHT.value, loop);
  }

  public boolean getBackLeftButton() {
    return getRawButton(Button.BACK_LEFT.value);
  }

  public boolean getBackLeftButtonPressed() {
    return getRawButtonPressed(Button.BACK_LEFT.value);
  }

  public boolean getBackLeftButtonReleased() {
    return getRawButtonReleased(Button.BACK_LEFT.value);
  }

  public BooleanEvent backLeft(EventLoop loop) {
    return button(Button.BACK_LEFT.value, loop);
  }

  public boolean getBackMidRightButton() {
    return getRawButton(Button.BACK_MID_RIGHT.value);
  }

  public boolean getBackMidRightButtonPressed() {
    return getRawButtonPressed(Button.BACK_MID_RIGHT.value);
  }

  public boolean getBackMidRightButtonReleased() {
    return getRawButtonReleased(Button.BACK_MID_RIGHT.value);
  }

  public BooleanEvent backMidRight(EventLoop loop) {
    return button(Button.BACK_MID_RIGHT.value, loop);
  }

  public boolean getBackMidLeftButton() {
    return getRawButton(Button.BACK_MID_LEFT.value);
  }

  public boolean getBackMidLeftButtonPressed() {
    return getRawButtonPressed(Button.BACK_MID_LEFT.value);
  }

  public boolean getBackMidLeftButtonReleased() {
    return getRawButtonReleased(Button.BACK_MID_LEFT.value);
  }

  public BooleanEvent backMidLeft(EventLoop loop) {
    return button(Button.BACK_MID_LEFT.value, loop);
  }
}
