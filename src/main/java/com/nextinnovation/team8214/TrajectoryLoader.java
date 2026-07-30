// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214;

import choreo.Choreo;
import choreo.trajectory.SwerveSample;
import choreo.trajectory.Trajectory;
import com.nextinnovation.team8214.util.AllianceValue;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Synchronized;

@Getter
public class TrajectoryLoader {
  private static TrajectoryLoader instance = null;

  public static TrajectoryLoader getInstance() {
    if (instance == null) {
      instance = new TrajectoryLoader();
    }
    return instance;
  }

  private TrajectorySet trajectorySet = null;

  private TrajectoryLoader() {
    lazyLoadTrajectorySet();
  }

  @Synchronized
  public void lazyLoadTrajectorySet() {
    if (trajectorySet == null) {
      System.out.println("Lazy loading all trajectories...");
      trajectorySet = new TrajectorySet();
      System.out.println("Trajectories finished loading!");
    }
  }

  public static final class TrajectorySet {
    public AllianceValue<Trajectory<SwerveSample>> leftTrenchStart2NeutralZoneWander2BumpShoot;
    public AllianceValue<Trajectory<SwerveSample>> rightTrenchStart2NeutralZoneWander2BumpShoot;
    public AllianceValue<Trajectory<SwerveSample>> leftTrenchStart2NeutralZoneWanderShort2BumpShoot;
    public AllianceValue<Trajectory<SwerveSample>>
        rightTrenchStart2NeutralZoneWanderShort2BumpShoot;
    public AllianceValue<Trajectory<SwerveSample>> leftTrenchStart2NeutralZoneScrum2BumpShoot;
    public AllianceValue<Trajectory<SwerveSample>> rightTrenchStart2NeutralZoneScrum2BumpShoot;

    public AllianceValue<Trajectory<SwerveSample>> leftTrenchReady2NeutralZoneDrunk2BumpShoot;
    public AllianceValue<Trajectory<SwerveSample>> rightTrenchReady2NeutralZoneDrunk2BumpShoot;
    public AllianceValue<Trajectory<SwerveSample>> leftTrenchReady2NeutralZoneDrunkShort2BumpShoot;
    public AllianceValue<Trajectory<SwerveSample>> rightTrenchReady2NeutralZoneDrunkShort2BumpShoot;

    public AllianceValue<Trajectory<SwerveSample>> leftTrenchReady2NeutralZoneDash;
    public AllianceValue<Trajectory<SwerveSample>> rightTrenchReady2NeutralZoneDash;

    public AllianceValue<Trajectory<SwerveSample>> depotSweep2SideShoot;
    public AllianceValue<Trajectory<SwerveSample>> leftCloseTrenchStart2NeutralZoneWander2BumpShoot;
    public AllianceValue<Trajectory<SwerveSample>>
        rightCloseTrenchStart2NeutralZoneWander2BumpShoot;

    public AllianceValue<Trajectory<SwerveSample>> leftBumpShoot2Depot;
    public AllianceValue<Trajectory<SwerveSample>> rightBumpShoot2MidShoot;

    public TrajectorySet() {
      leftTrenchStart2NeutralZoneWander2BumpShoot =
          loadTrajectory("leftTrenchStart2NeutralZoneWander2BumpShoot", false);
      rightTrenchStart2NeutralZoneWander2BumpShoot =
          loadTrajectory("leftTrenchStart2NeutralZoneWander2BumpShoot", true);

      leftTrenchStart2NeutralZoneWanderShort2BumpShoot =
          loadTrajectory("leftTrenchStart2NeutralZoneWanderShort2BumpShoot", false);
      rightTrenchStart2NeutralZoneWanderShort2BumpShoot =
          loadTrajectory("leftTrenchStart2NeutralZoneWanderShort2BumpShoot", true);

      leftTrenchReady2NeutralZoneDrunk2BumpShoot =
          loadTrajectory("leftTrenchReady2NeutralZoneDrunk2BumpShoot", false);
      rightTrenchReady2NeutralZoneDrunk2BumpShoot =
          loadTrajectory("leftTrenchReady2NeutralZoneDrunk2BumpShoot", true);

      leftTrenchReady2NeutralZoneDrunkShort2BumpShoot =
          loadTrajectory("leftTrenchReady2NeutralZoneDrunkShort2BumpShoot", false);
      rightTrenchReady2NeutralZoneDrunkShort2BumpShoot =
          loadTrajectory("leftTrenchReady2NeutralZoneDrunkShort2BumpShoot", true);

      leftTrenchStart2NeutralZoneScrum2BumpShoot =
          loadTrajectory("leftTrenchStart2NeutralZoneScrum2BumpShoot", false);
      rightTrenchStart2NeutralZoneScrum2BumpShoot =
          loadTrajectory("leftTrenchStart2NeutralZoneScrum2BumpShoot", true);

      leftTrenchReady2NeutralZoneDash = loadTrajectory("leftTrenchReady2NeutralZoneDash", false);
      rightTrenchReady2NeutralZoneDash = loadTrajectory("leftTrenchReady2NeutralZoneDash", true);

      depotSweep2SideShoot = loadTrajectory("depotSweep2SideShoot", false);

      leftCloseTrenchStart2NeutralZoneWander2BumpShoot =
          loadTrajectory("leftCloseTrenchStart2NeutralZoneWander2BumpShoot", false);
      rightCloseTrenchStart2NeutralZoneWander2BumpShoot =
          loadTrajectory("leftCloseTrenchStart2NeutralZoneWander2BumpShoot", true);

      leftBumpShoot2Depot = loadTrajectory("leftBumpShoot2Depot", false);

      rightBumpShoot2MidShoot = loadTrajectory("leftBumpShoot2MidShoot", true);
    }

    @SuppressWarnings("unchecked")
    private AllianceValue<Trajectory<SwerveSample>> loadTrajectory(
        String name, boolean wantMirrorByXMidline) {
      var trajectory = Choreo.loadTrajectory(name);

      if (trajectory.isEmpty()) {
        throw new NullPointerException("[TrajectoryLoader]: " + name + ".traj not found");
      }

      var blue = (Trajectory<SwerveSample>) trajectory.get();

      if (wantMirrorByXMidline) {
        name = name + "_xMirrored";
        blue = mirrorTrajectoryByXMidline(blue);
      }

      System.out.println("[TrajectoryLoader]: " + name + ".traj loaded");

      return new AllianceValue<>(blue, blue.flipped());
    }

    @SuppressWarnings("unchecked")
    private Trajectory<SwerveSample> mirrorTrajectoryByXMidline(Trajectory<SwerveSample> original) {
      List<SwerveSample> mirroredSamples =
          original.samples().stream()
              .map(
                  sample -> {
                    var mirroredFx = sample.moduleForcesX();
                    var mirroredFy = new double[sample.moduleForcesY().length];
                    for (var i = 0; i < mirroredFy.length; i++) {
                      mirroredFy[i] = -sample.moduleForcesY()[i];
                    }

                    return new SwerveSample(
                        sample.t,
                        sample.x,
                        Field.WIDTH - sample.y,
                        -sample.heading,
                        sample.vx,
                        -sample.vy,
                        -sample.omega,
                        sample.ax,
                        -sample.ay,
                        -sample.alpha,
                        mirroredFx,
                        mirroredFy);
                  })
              .collect(Collectors.toList());

      return new Trajectory<>(
          original.name() + "_xMirrored", mirroredSamples, original.splits(), original.events());
    }
  }
}
