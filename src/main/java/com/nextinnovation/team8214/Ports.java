// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214;

import com.ctre.phoenix6.CANBus;
import com.nextinnovation.team8214.util.driver.CanId;

public final class Ports {
  public static final class Can {
    public static final CANBus CHASSIS_CANIVORE_BUS = new CANBus("chassis");

    // IMU
    public static final CanId CHASSIS_PIGEON = new CanId(0, CHASSIS_CANIVORE_BUS);

    // Swerve
    public static final CanId FL_DRIVE_MOTOR = new CanId(1, CHASSIS_CANIVORE_BUS);
    public static final CanId FL_STEER_MOTOR = new CanId(2, CHASSIS_CANIVORE_BUS);
    public static final CanId FL_STEER_SENSOR = new CanId(3, CHASSIS_CANIVORE_BUS);

    public static final CanId BL_DRIVE_MOTOR = new CanId(4, CHASSIS_CANIVORE_BUS);
    public static final CanId BL_STEER_MOTOR = new CanId(5, CHASSIS_CANIVORE_BUS);
    public static final CanId BL_STEER_SENSOR = new CanId(6, CHASSIS_CANIVORE_BUS);

    public static final CanId BR_DRIVE_MOTOR = new CanId(7, CHASSIS_CANIVORE_BUS);
    public static final CanId BR_STEER_MOTOR = new CanId(8, CHASSIS_CANIVORE_BUS);
    public static final CanId BR_STEER_SENSOR = new CanId(9, CHASSIS_CANIVORE_BUS);

    public static final CanId FR_DRIVE_MOTOR = new CanId(10, CHASSIS_CANIVORE_BUS);
    public static final CanId FR_STEER_MOTOR = new CanId(11, CHASSIS_CANIVORE_BUS);
    public static final CanId FR_STEER_SENSOR = new CanId(12, CHASSIS_CANIVORE_BUS);

    // Intake
    public static final CanId INTAKE_ROLLER_LEFT_MASTER = new CanId(13, CHASSIS_CANIVORE_BUS);
    public static final CanId INTAKE_ROLLER_RIGHT_SLAVE = new CanId(14, CHASSIS_CANIVORE_BUS);
    public static final CanId INTAKE_PIVOT = new CanId(15, CHASSIS_CANIVORE_BUS);

    // Indexer
    public static final CanId INDEXER_FRONT_LEFT = new CanId(16, CHASSIS_CANIVORE_BUS);
    public static final CanId INDEXER_FRONT_RIGHT = new CanId(17, CHASSIS_CANIVORE_BUS);
    public static final CanId INDEXER_BACK_RIGHT = new CanId(18, CHASSIS_CANIVORE_BUS);
    public static final CanId INDEXER_BACK_LEFT = new CanId(19, CHASSIS_CANIVORE_BUS);
    public static final CanId FLOOR_SENSOR_CANRANGE = new CanId(26, CHASSIS_CANIVORE_BUS);

    // Shooter
    public static final CanId SHOOTER_PITCH = new CanId(20, CHASSIS_CANIVORE_BUS);
    public static final CanId SHOOTER_FLYWHEEL_UP_LEFT_MASTER = new CanId(21, CHASSIS_CANIVORE_BUS);
    public static final CanId SHOOTER_FLYWHEEL_DOWN_LEFT_SALVE =
        new CanId(22, CHASSIS_CANIVORE_BUS);
    public static final CanId SHOOTER_FLYWHEEL_DOWN_RIGHT_SALVE =
        new CanId(23, CHASSIS_CANIVORE_BUS);
    public static final CanId SHOOTER_FLYWHEEL_UP_RIGHT_SALVE = new CanId(24, CHASSIS_CANIVORE_BUS);
    public static final CanId FEEDER_SENSOR_CANRANGE = new CanId(27, CHASSIS_CANIVORE_BUS);
  }

  public static final class Joystick {
    public static final int DRIVER = 0;
    public static final int CODRIVER = 1;
  }
}
