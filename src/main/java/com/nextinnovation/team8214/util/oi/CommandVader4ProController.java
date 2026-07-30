// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util.oi;

import edu.wpi.first.wpilibj.event.EventLoop;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.button.CommandGenericHID;
import edu.wpi.first.wpilibj2.command.button.Trigger;

public class CommandVader4ProController extends CommandGenericHID {
  private final Vader4ProController hid;

  /**
   * Construct an instance of a controller.
   *
   * @param port The port index on the Driver Station that the controller is plugged into.
   */
  public CommandVader4ProController(int port) {
    super(port);
    hid = new Vader4ProController(port);
  }

  /**
   * Get the underlying GenericHID object.
   *
   * @return the wrapped GenericHID object
   */
  @Override
  public Vader4ProController getHID() {
    return hid;
  }

  /**
   * Constructs a Trigger instance around the A button's digital signal.
   *
   * @return a Trigger instance representing the A button's digital signal attached to the {@link
   *     CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #a(EventLoop)
   */
  public Trigger a() {
    return a(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the A button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the A button's digital signal attached to the given
   *     loop.
   */
  public Trigger a(EventLoop loop) {
    return button(Vader4ProController.Button.A.value, loop);
  }

  /**
   * Constructs a Trigger instance around the B button's digital signal.
   *
   * @return a Trigger instance representing the B button's digital signal attached to the {@link
   *     CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #b(EventLoop)
   */
  public Trigger b() {
    return b(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the B button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the B button's digital signal attached to the given
   *     loop.
   */
  public Trigger b(EventLoop loop) {
    return button(Vader4ProController.Button.B.value, loop);
  }

  /**
   * Constructs a Trigger instance around the C button's digital signal.
   *
   * @return a Trigger instance representing the C button's digital signal attached to the {@link
   *     CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #c(EventLoop)
   */
  public Trigger c() {
    return c(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the C button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the C button's digital signal attached to the given
   *     loop.
   */
  public Trigger c(EventLoop loop) {
    return button(Vader4ProController.Button.C.value, loop);
  }

  /**
   * Constructs a Trigger instance around the X button's digital signal.
   *
   * @return a Trigger instance representing the X button's digital signal attached to the {@link
   *     CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #x(EventLoop)
   */
  public Trigger x() {
    return x(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the X button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the X button's digital signal attached to the given
   *     loop.
   */
  public Trigger x(EventLoop loop) {
    return button(Vader4ProController.Button.X.value, loop);
  }

  /**
   * Constructs a Trigger instance around the Y button's digital signal.
   *
   * @return a Trigger instance representing the Y button's digital signal attached to the {@link
   *     CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #y(EventLoop)
   */
  public Trigger y() {
    return y(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the Y button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the Y button's digital signal attached to the given
   *     loop.
   */
  public Trigger y(EventLoop loop) {
    return button(Vader4ProController.Button.Y.value, loop);
  }

  /**
   * Constructs a Trigger instance around the Z button's digital signal.
   *
   * @return a Trigger instance representing the Z button's digital signal attached to the {@link
   *     CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #z(EventLoop)
   */
  public Trigger z() {
    return z(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the Z button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the Z button's digital signal attached to the given
   *     loop.
   */
  public Trigger z(EventLoop loop) {
    return button(Vader4ProController.Button.Z.value, loop);
  }

  /**
   * Constructs a Trigger instance around the left bumper button's digital signal.
   *
   * @return a Trigger instance representing the left bumper button's digital signal attached to the
   *     {@link CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #leftBumper(EventLoop)
   */
  public Trigger leftBumper() {
    return leftBumper(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the left bumper button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the left bumper button's digital signal attached to the
   *     given loop.
   */
  public Trigger leftBumper(EventLoop loop) {
    return button(Vader4ProController.Button.LEFT_BUMPER.value, loop);
  }

  /**
   * Constructs a Trigger instance around the right bumper button's digital signal.
   *
   * @return a Trigger instance representing the right bumper button's digital signal attached to
   *     the {@link CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #rightBumper(EventLoop)
   */
  public Trigger rightBumper() {
    return rightBumper(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the right bumper button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the right bumper button's digital signal attached to
   *     the given loop.
   */
  public Trigger rightBumper(EventLoop loop) {
    return button(Vader4ProController.Button.RIGHT_BUMPER.value, loop);
  }

  /**
   * Constructs a Trigger instance around the select button's digital signal.
   *
   * @return a Trigger instance representing the select button's digital signal attached to the
   *     {@link CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #select(EventLoop)
   */
  public Trigger select() {
    return select(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the select button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the select button's digital signal attached to the
   *     given loop.
   */
  public Trigger select(EventLoop loop) {
    return button(Vader4ProController.Button.SELECT.value, loop);
  }

  /**
   * Constructs a Trigger instance around the start button's digital signal.
   *
   * @return a Trigger instance representing the start button's digital signal attached to the
   *     {@link CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #start(EventLoop)
   */
  public Trigger start() {
    return start(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the start button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the start button's digital signal attached to the given
   *     loop.
   */
  public Trigger start(EventLoop loop) {
    return button(Vader4ProController.Button.START.value, loop);
  }

  /**
   * Constructs a Trigger instance around the left stick button's digital signal.
   *
   * @return a Trigger instance representing the left stick button's digital signal attached to the
   *     {@link CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #leftStick(EventLoop)
   */
  public Trigger leftStick() {
    return leftStick(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the left stick button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the left stick button's digital signal attached to the
   *     given loop.
   */
  public Trigger leftStick(EventLoop loop) {
    return button(Vader4ProController.Button.LEFT_STICK.value, loop);
  }

  /**
   * Constructs a Trigger instance around the right stick button's digital signal.
   *
   * @return a Trigger instance representing the right stick button's digital signal attached to the
   *     {@link CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #rightStick(EventLoop)
   */
  public Trigger rightStick() {
    return rightStick(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the right stick button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the right stick button's digital signal attached to the
   *     given loop.
   */
  public Trigger rightStick(EventLoop loop) {
    return button(Vader4ProController.Button.RIGHT_STICK.value, loop);
  }

  /**
   * Constructs a Trigger instance around the back right button's digital signal.
   *
   * @return a Trigger instance representing the back right button's digital signal attached to the
   *     {@link CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #backRight(EventLoop)
   */
  public Trigger backRight() {
    return backRight(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the back right button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the back right button's digital signal attached to the
   *     given loop.
   */
  public Trigger backRight(EventLoop loop) {
    return button(Vader4ProController.Button.BACK_RIGHT.value, loop);
  }

  /**
   * Constructs a Trigger instance around the back left button's digital signal.
   *
   * @return a Trigger instance representing the back left button's digital signal attached to the
   *     {@link CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #backLeft(EventLoop)
   */
  public Trigger backLeft() {
    return backLeft(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the back left button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the back left button's digital signal attached to the
   *     given loop.
   */
  public Trigger backLeft(EventLoop loop) {
    return button(Vader4ProController.Button.BACK_LEFT.value, loop);
  }

  /**
   * Constructs a Trigger instance around the back mid right button's digital signal.
   *
   * @return a Trigger instance representing the back mid right button's digital signal attached to
   *     the {@link CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #backMidRight(EventLoop)
   */
  public Trigger backMidRight() {
    return backMidRight(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the back mid right button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the back mid right button's digital signal attached to
   *     the given loop.
   */
  public Trigger backMidRight(EventLoop loop) {
    return button(Vader4ProController.Button.BACK_MID_RIGHT.value, loop);
  }

  /**
   * Constructs a Trigger instance around the back mid left button's digital signal.
   *
   * @return a Trigger instance representing the back mid left button's digital signal attached to
   *     the {@link CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #backMidLeft(EventLoop)
   */
  public Trigger backMidLeft() {
    return backMidLeft(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the back mid left button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the back mid left button's digital signal attached to
   *     the given loop.
   */
  public Trigger backMidLeft(EventLoop loop) {
    return button(Vader4ProController.Button.BACK_MID_LEFT.value, loop);
  }

  /**
   * Constructs a Trigger instance around the left trigger button's digital signal.
   *
   * @return a Trigger instance representing the left trigger button's digital signal attached to
   *     the {@link CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #leftTrigger(EventLoop)
   */
  public Trigger leftTrigger() {
    return leftTrigger(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the left trigger button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the left trigger button's digital signal attached to
   *     the given loop.
   */
  public Trigger leftTrigger(EventLoop loop) {
    return button(Vader4ProController.Button.LEFT_TRIGGER.value, loop);
  }

  /**
   * Constructs a Trigger instance around the right trigger button's digital signal.
   *
   * @return a Trigger instance representing the right trigger button's digital signal attached to
   *     the {@link CommandScheduler#getDefaultButtonLoop() default scheduler button loop}.
   * @see #rightTrigger(EventLoop)
   */
  public Trigger rightTrigger() {
    return rightTrigger(CommandScheduler.getInstance().getDefaultButtonLoop());
  }

  /**
   * Constructs a Trigger instance around the right trigger button's digital signal.
   *
   * @param loop the event loop instance to attach the event to.
   * @return a Trigger instance representing the right trigger button's digital signal attached to
   *     the given loop.
   */
  public Trigger rightTrigger(EventLoop loop) {
    return button(Vader4ProController.Button.RIGHT_TRIGGER.value, loop);
  }

  /**
   * Get the X axis value of left side of the controller. Right is positive.
   *
   * @return The axis value.
   */
  public double getLeftX() {
    return hid.getLeftX();
  }

  /**
   * Get the X axis value of right side of the controller. Right is positive.
   *
   * @return The axis value.
   */
  public double getRightX() {
    return hid.getRightX();
  }

  /**
   * Get the Y axis value of left side of the controller. Back is positive.
   *
   * @return The axis value.
   */
  public double getLeftY() {
    return hid.getLeftY();
  }

  /**
   * Get the Y axis value of right side of the controller. Back is positive.
   *
   * @return The axis value.
   */
  public double getRightY() {
    return hid.getRightY();
  }
}
