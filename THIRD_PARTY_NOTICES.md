# Third-Party Notices

Team 8214-authored code is licensed under the root MIT `LICENSE`. Adapted files retain
their upstream copyright and license notices as identified below. The project MIT license
does not relicense third-party dependencies, source files, assets, or binaries.

This file is an attribution and provenance index, not a replacement for license texts
embedded in upstream artifacts. Binary redistributions must preserve the applicable
upstream license and notice files.

## Source and build dependencies

| Component | Version | Upstream | License | Repository paths / notes |
| --- | --- | --- | --- | --- |
| WPILib | 2026.2.1 | [wpilibsuite/allwpilib](https://github.com/wpilibsuite/allwpilib) | [BSD-3-Clause](THIRD_PARTY_LICENSES/WPILib-BSD-3-Clause.txt) | Robot framework; `Main.java` retains the generated WPILib entry-point notice. |
| WPILib New Commands | 1.0.0 | [wpilibsuite/allwpilib](https://github.com/wpilibsuite/allwpilib) | BSD-3-Clause | `vendordeps/WPILibNewCommands.json`. |
| AdvantageKit | 26.0.2 | [Mechanical-Advantage/AdvantageKit](https://github.com/Mechanical-Advantage/AdvantageKit/tree/v26.0.2) | [BSD-3-Clause](THIRD_PARTY_LICENSES/AdvantageKit-BSD-3-Clause.txt) | `vendordeps/AdvantageKit.json`; two adapted gyro IO files are identified below. |
| Autopilot | 1.6.1 | [therekrab/autopilot](https://github.com/therekrab/autopilot) | MIT | `vendordeps/Autopilot.json`. |
| ChoreoLib | 2026.0.2 | [SleipnirGroup/Choreo](https://github.com/SleipnirGroup/Choreo) | BSD-3-Clause | `vendordeps/ChoreoLib.json`. |
| MapleSim | 0.4.0-beta | [Shenzhen-Robotics-Alliance/maple-sim](https://github.com/Shenzhen-Robotics-Alliance/maple-sim/tree/v0.4.0-beta) | Apache-2.0 | `vendordeps/maple-sim.json`. |
| Gson | 2.11.0 | [google/gson](https://github.com/google/gson/tree/gson-parent-2.11.0) | Apache-2.0 | Explicit Java dependency declared by `vendordeps/ChoreoLib.json`. |
| dyn4j | 5.0.2 | [dyn4j/dyn4j](https://github.com/dyn4j/dyn4j/tree/v5.0.2) | BSD-3-Clause | Explicit Java dependency declared by `vendordeps/maple-sim.json`. |
| CTRE Phoenix 6 | 26.1.3 | [Cross The Road Electronics](https://v6.docs.ctr-electronics.com/) | CTRE vendor license | `vendordeps/Phoenix6.json`; not relicensed by this project. |
| PhotonLib | v2026.3.4 | [PhotonVision/photonvision](https://github.com/PhotonVision/photonvision/tree/v2026.3.4) | MIT for `photonlib-java`; GPLv3 for `photontargeting-java` and `photontargeting-cpp` | `vendordeps/photonlib.json`, `libs/photonvision/maven`; each artifact contains its upstream license. |
| Cyber Power | 2026.2.2 | `https://power.team8214.com/vendordep/CyberPower.json` | No public license yet | `vendordeps/CyberPower.json`. The source snapshot references but does not bundle this separately distributed dependency. Do not redistribute generated fat JARs or describe the complete dependency stack as open source until the matching Cyber Power source is published under an explicit license. |
| Javalin | 6.4.0 | [javalin/javalin](https://github.com/javalin/javalin) | Apache-2.0 | Direct runtime dependency; retained intentionally. |
| SLF4J | 2.0.16 | [qos-ch/slf4j](https://github.com/qos-ch/slf4j) | MIT | `slf4j-nop` direct runtime dependency. |
| Jackson | 2.19.2 resolved | [FasterXML/jackson](https://github.com/FasterXML/jackson) | Apache-2.0 | Runtime dependency used by ShootingCalculator and Javalin. |
The robot JAR is a fat JAR. License and notice files embedded by runtime dependencies must
remain present in released binaries.

## Adapted Java source

| Upstream | License | Repository paths |
| --- | --- | --- |
| FRC 6328 RobotCode 2024/2025 | MIT | `Robot.java`, `Odometry.java`, `util/Alert.java`, `util/VirtualSubsystem.java`, `util/SwitchableChooser.java`, `util/EqualsUtil.java`, `util/GeomUtil.java`, `util/LoggedTunableNumber.java`, `util/AllianceFlipUtil.java`, `util/TriggerUtil.java`, `subsystem/swerve/controller/HeadingController.java`, and `subsystem/swerve/controller/TrajectoryController.java` |
| Littleton Robotics RobotCode 2026 | MIT | `AutoModeSelector.java` and `HubShiftUtil.java` |
| AdvantageKit 26.0.2 talonfx swerve template | BSD-3-Clause with Team 8214 modifications under MIT | `subsystem/swerve/GyroIO.java` and `subsystem/swerve/GyroIOPigeon2.java` |

Paths in this table are relative to
`src/main/java/com/nextinnovation/team8214/`. Copyright and SPDX headers in
these files are part of their license notices and must be retained.

## Bundled web source

| Component | Version / source | License | Repository path | Local modification |
| --- | --- | --- | --- | --- |
| Chart.js | 4.5.1, [chartjs/Chart.js](https://github.com/chartjs/Chart.js/tree/v4.5.1) | MIT | `src/main/deploy/shootingcalculator/chart.min.js` | No |
| chartjs-plugin-dragdata | 2.2.5, [chrispahm/chartjs-plugin-dragdata](https://github.com/chrispahm/chartjs-plugin-dragdata/tree/v2.2.5) | MIT | `src/main/deploy/shootingcalculator/chartjs-plugin-dragdata.min.js` | No |
| NT4 browser client | FRC 6328 public robot code | MIT | `src/main/deploy/shootingcalculator/nt4.js` | Adapted for this dashboard |
| MessagePack browser client | Yves Goergen `msgpack.js`, adapted by FRC 6328 | MIT | `src/main/deploy/shootingcalculator/msgpack.js` | Adapted for this dashboard |

Current SHA-256 values:

```text
chart.min.js                    48444a82d4edcb5bec0f1965faacdde18d9c17db3063d042abada2f705c9f54a
chartjs-plugin-dragdata.min.js  3195fc0d88241f85ef4e5c494b7015b4ab977555f4823ef000680e4d4a7b9223
msgpack.js                      58e9af95580bea4111b8de72994bfa872ae5c01991de1bc6a4a97bfcecbc2bbb
nt4.js                          dbd710773751aab3aeb8d58d9ef1a818c88fca13c6aa3b96508394dc09c38baa
```

The hashes above are calculated from the repository's canonical LF content, enforced by
`.gitattributes`.

## Bundled applications and binary artifacts

| Component | Version | License | Repository path / source |
| --- | --- | --- | --- |
| Elastic Dashboard | 2026.1.2 | MIT plus bundled third-party licenses | `.elastic`; based on upstream `Elastic-Web.zip` (SHA-256 `6865537c1e2281b73a54aba475e9fa0d30fd270cc2f2cab62b7949e3578e3f47`). Retain `.elastic/assets/NOTICES` and the license files under `.elastic/assets/assets/third_party_licenses`. |
| PhotonVision offline Maven artifacts | v2026.3.4 | MIT and GPLv3 as mapped above | `libs/photonvision/maven`; the release archive checksum and source tag/commit are recorded in `libs/photonvision/README.md`. |
| PhotonVision ARM64 updater | v2026.3.4 | GPLv3 plus bundled third-party notices, including AGPLv3 model notices | `assets/photonvision/photonvision-v2026.3.4-linuxarm64.jar`; upstream release SHA-256 `ccaf5e862a4427c90cb063953903e4967e2041747b1d3f9d0f04b68e1cd975dc`. |

The PhotonVision GPLv3/AGPLv3 artifacts are separately licensed third-party components
and are not covered by the project MIT license. Do not remove or replace their
embedded `LICENSE` and `NOTICE` files when redistributing them.
