// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.indexer;

import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.nextinnovation.cyberpower.EnergyLogger;
import com.nextinnovation.cyberpower.EnergySubsystem;
import com.nextinnovation.cyberpower.MotorType;
import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.Ports;
import com.nextinnovation.team8214.util.*;
import com.nextinnovation.team8214.util.genericsystem.*;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import java.util.function.BooleanSupplier;
import lombok.Getter;
import lombok.Setter;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;

public class Indexer extends SubsystemBase {
  private BooleanSupplier isCoveredByHopperSignalSupplier = () -> false;
  private BooleanSupplier hopperPreloadSignalSupplier = () -> false;

  private final LoggedTunableGains INDEXER_GAINS =
      new LoggedTunableGains(
          IndexerConfig.LOG_GROUP,
          IndexerConfig.LOG_ROOT + "/indexer",
          0.35 * IndexerConfig.GEAR_RATIO,
          0.0,
          0.0,
          12.0 / (5800.0 / 60.0) * IndexerConfig.GEAR_RATIO,
          0.0,
          0.0);

  private final GenericRollerIO indexerIO;
  private final GenericRollerIOInputsAutoLogged indexerInputs =
      new GenericRollerIOInputsAutoLogged();
  private final Alert indexerOfflineAlert =
      new Alert("Indexer motor offline!", Alert.AlertType.WARNING);
  private final EnergySubsystem energySubsystem =
      EnergyLogger.getInstance().createSubsystem("indexer");

  @Getter
  @Setter
  @AutoLogOutput(key = IndexerConfig.LOG_ROOT + "/mode")
  private IndexerGoal goal = IndexerGoal.IDLE;

  public Indexer() {
    switch (Config.MODE) {
      case REAL -> {
        var indexerConfig = new TalonFXConfiguration();
        indexerConfig.MotorOutput.NeutralMode = NeutralModeValue.Brake;
        indexerConfig.MotorOutput.Inverted = InvertedValue.CounterClockwise_Positive;

        indexerConfig.Slot0 =
            new Slot0Configs()
                .withKP(INDEXER_GAINS.getKp())
                .withKD(INDEXER_GAINS.getKd())
                .withKV(INDEXER_GAINS.getKv())
                .withKS(INDEXER_GAINS.getKs());

        indexerConfig.CurrentLimits.StatorCurrentLimit = 110.0;
        indexerConfig.CurrentLimits.StatorCurrentLimitEnable = true;
        indexerConfig.CurrentLimits.SupplyCurrentLimit = 40.0;
        indexerConfig.CurrentLimits.SupplyCurrentLimitEnable = true;

        indexerConfig.Feedback.SensorToMechanismRatio = IndexerConfig.GEAR_RATIO;

        indexerIO =
            new GenericRollerIOKraken("indexer", Ports.Can.INDEXER_FRONT_LEFT, indexerConfig)
                .withFollower(
                    Ports.Can.INDEXER_FRONT_RIGHT, IndexerConfig.INDEXER_FRONT_RIGHT_INVERTED)
                .withFollower(
                    Ports.Can.INDEXER_BACK_RIGHT, IndexerConfig.INDEXER_BACK_RIGHT_INVERTED)
                .withFollower(
                    Ports.Can.INDEXER_BACK_LEFT, IndexerConfig.INDEXER_BACK_LEFT_INVERTED);
      }

      case SIM -> indexerIO = new GenericRollerIOSim(IndexerConfig.GEAR_RATIO);

      default -> indexerIO = new GenericRollerIO() {};
    }
    configureCyberPower();
  }

  private void configureCyberPower() {
    energySubsystem.registerLeaderMotor(
        "frontLeft",
        MotorType.KRAKEN_X60_FOC,
        IndexerConfig.GEAR_RATIO,
        () -> indexerInputs.connected,
        () -> indexerInputs.supplyCurrentAmp,
        () -> indexerInputs.statorCurrentAmp,
        () -> indexerInputs.rotorVelocityRadPerSec);
    energySubsystem.registerFollowerMotor(
        "frontRight",
        MotorType.KRAKEN_X60_FOC,
        IndexerConfig.GEAR_RATIO,
        "frontLeft",
        () -> followerConnected(0),
        () -> followerSupplyCurrent(0));
    energySubsystem.registerFollowerMotor(
        "backRight",
        MotorType.KRAKEN_X60_FOC,
        IndexerConfig.GEAR_RATIO,
        "frontLeft",
        () -> followerConnected(1),
        () -> followerSupplyCurrent(1));
    energySubsystem.registerFollowerMotor(
        "backLeft",
        MotorType.KRAKEN_X60_FOC,
        IndexerConfig.GEAR_RATIO,
        "frontLeft",
        () -> followerConnected(2),
        () -> followerSupplyCurrent(2));
  }

  private boolean followerConnected(int index) {
    if (Config.MODE == Config.Mode.SIM) {
      return indexerInputs.connected;
    }
    return index < indexerInputs.followersConnected.length
        && indexerInputs.followersConnected[index]
        && index < indexerInputs.followersSupplyCurrentAmp.length;
  }

  private double followerSupplyCurrent(int index) {
    if (Config.MODE == Config.Mode.SIM) {
      return indexerInputs.supplyCurrentAmp;
    }
    return index < indexerInputs.followersSupplyCurrentAmp.length
        ? indexerInputs.followersSupplyCurrentAmp[index]
        : Double.NaN;
  }

  @Override
  public void periodic() {
    indexerIO.updateInputs(indexerInputs);
    Logger.processInputs(IndexerConfig.LOG_ROOT + "/indexer", indexerInputs);
    indexerOfflineAlert.set(!indexerInputs.connected);

    LoggedTunableNumber.ifChanged(
        hashCode(),
        () ->
            indexerIO.setPdf(
                INDEXER_GAINS.getKp(),
                INDEXER_GAINS.getKd(),
                INDEXER_GAINS.getKv(),
                INDEXER_GAINS.getKs()),
        INDEXER_GAINS.getEntries());

    goal =
        calculateControlGoal(
            goal,
            isCoveredByHopperSignalSupplier.getAsBoolean(),
            hopperPreloadSignalSupplier.getAsBoolean());
    energySubsystem.periodic(goal);
    var targetVelocityRps = goal.getVelocityRps();
    if (EqualsUtil.epsilonEquals(targetVelocityRps, 0.0, 0.01)) {
      indexerIO.stop();
    } else {
      indexerIO.setVel(Units.rotationsToRadians(targetVelocityRps));
    }
  }

  static IndexerGoal calculateControlGoal(
      IndexerGoal goal, boolean isCoveredByHopper, boolean hopperPreloadRequested) {
    if (isCoveredByHopper) {
      return IndexerGoal.IDLE;
    }
    if (goal == IndexerGoal.IDLE || goal == IndexerGoal.PRELOAD) {
      return hopperPreloadRequested ? IndexerGoal.PRELOAD : IndexerGoal.IDLE;
    }
    return goal;
  }

  public void registerIsCoveredByHopperSignalSupplier(BooleanSupplier signal) {
    isCoveredByHopperSignalSupplier = signal;
  }

  public void registerHopperPreloadSignalSupplier(BooleanSupplier signal) {
    hopperPreloadSignalSupplier = signal;
  }
}
