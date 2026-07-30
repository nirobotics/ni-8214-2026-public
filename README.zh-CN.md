# FRC Team 8214 — 2026 机器人代码

![Next Innovation](https://img.shields.io/badge/Next-Innovation-8A2BE2?labelColor=555555&style=flat)
![Lang zh-CN](https://img.shields.io/badge/Lang-zh--CN-2DBA4E?labelColor=555555&style=flat)

[English](README.md) | 简体中文

![MACK，Team 8214 的 2026 休赛季机器人](assets/mack.jpg)

**MACK** — Team 8214 的 2026 休赛季机器人。

这是 MACK 的 Java 机器人代码，采用 WPILib Command-based、AdvantageKit、Phoenix 6、PhotonVision、Choreo、MapleSim、Autopilot 和 Cyber Power。

本仓库是 Team 8214 2026 机器人代码的只读归档。

## 工程亮点

### 底盘与自动

- **旋转优先 Swerve：** 当模块速度受限时，优先保留旋转能力，仅按需缩放平移。
  ([Swerve.java#L401-L435](src/main/java/com/nextinnovation/team8214/subsystem/swerve/Swerve.java#L401-L435)
  · [Swerve.java#L696-L745](src/main/java/com/nextinnovation/team8214/subsystem/swerve/Swerve.java#L696-L745))
- **动态旋转中心：** 瞄准前检查车体扫掠区域与 Hub、Trench 几何的碰撞，必要时改用更安全的车角旋转中心。
  ([Swerve.java#L595-L670](src/main/java/com/nextinnovation/team8214/subsystem/swerve/Swerve.java#L595-L670)
  · [Field.java#L56-L76](src/main/java/com/nextinnovation/team8214/Field.java#L56-L76))
- **基于陀螺仪的Bump穿越判断：** 结合 IMU 倾角与行驶进度确认爬升和落稳，并独立设置越界与超时停止条件。
  ([BumpPounceController.java#L111-L185](src/main/java/com/nextinnovation/team8214/subsystem/swerve/controller/BumpPounceController.java#L111-L185)
  · [AutoCommands.java#L102-L136](src/main/java/com/nextinnovation/team8214/command/AutoCommands.java#L102-L136))
- **基于Drive2PointController的轨迹终点恢复：** 关键 Choreo 段结束后检查实测位姿，仅在确有偏差时追加短距离收敛动作。
  ([AutoCommands.java#L269-L320](src/main/java/com/nextinnovation/team8214/command/AutoCommands.java#L269-L320)
  · [AutoModes.java#L69-L79](src/main/java/com/nextinnovation/team8214/command/AutoModes.java#L69-L79))
- **底盘加速度限幅与能量管理（Cyber-Power）：** 分层限制各轴、防滑与高速前向加速度；Cyber Power 在同一时间线记录电机电流、转速、连接状态和电池电压。
  ([Swerve.java#L747-L809](src/main/java/com/nextinnovation/team8214/subsystem/swerve/Swerve.java#L747-L809)
  · [Module.java#L51-L71](src/main/java/com/nextinnovation/team8214/subsystem/swerve/Module.java#L51-L71)
  · [Robot.java#L83-L87](src/main/java/com/nextinnovation/team8214/Robot.java#L83-L87))

![Cyber Power 子系统能量分析](assets/cyber-power.png)

### 机构与场地安全

- **预测式 Trench 保护：** 通过 0.5 秒位姿预测提前识别边界穿越，并在到达前将射手压至安全角度。
  ([Odometry.java#L199-L215](src/main/java/com/nextinnovation/team8214/Odometry.java#L199-L215)
  · [Shooter.java#L270-L298](src/main/java/com/nextinnovation/team8214/subsystem/shooter/Shooter.java#L270-L298)
  · [Shooter.java#L380-L390](src/main/java/com/nextinnovation/team8214/subsystem/shooter/Shooter.java#L380-L390))
- **自动化提前射击判断：** 补偿弹道飞行与场地计数延迟，支持比赛中重启后按 FMS
  时间恢复，并在切换前告警和振动提醒。
  ([HubShiftUtil.java#L35-L45](src/main/java/com/nextinnovation/team8214/HubShiftUtil.java#L35-L45)
  · [HubShiftUtil.java#L161-L200](src/main/java/com/nextinnovation/team8214/HubShiftUtil.java#L161-L200)
  · [RobotContainer.java#L447-L499](src/main/java/com/nextinnovation/team8214/RobotContainer.java#L447-L499))
- **Transport禁区：** 根据联盟翻转场地区域，在禁止区域内自动阻止 Transport 喂球。
  ([Field.java#L84-L97](src/main/java/com/nextinnovation/team8214/Field.java#L84-L97)
  · [Odometry.java#L227-L240](src/main/java/com/nextinnovation/team8214/Odometry.java#L227-L240)
  · [RobotContainer.java#L337-L349](src/main/java/com/nextinnovation/team8214/RobotContainer.java#L337-L349))
- **失效安全的射表编辑：** 浏览器修改必须整体验证成功后才原子替换当前表，错误输入不会覆盖最后一份有效数据。
  ([ShootingCalculator.java#L32-L78](src/main/java/com/nextinnovation/team8214/subsystem/shooter/ShootingCalculator.java#L32-L78)
  · [ShootingCalculator.java#L203-L239](src/main/java/com/nextinnovation/team8214/subsystem/shooter/ShootingCalculator.java#L203-L239)
  · [ShootingCalculator.java#L399-L417](src/main/java/com/nextinnovation/team8214/subsystem/shooter/ShootingCalculator.java#L399-L417))
- **在线射击表调试：** 在实时调试版本中，Javalin 托管编辑页面，NT4 topic
  在线读写得分、Transport 和 Fence 射表，机器人端原子应用通过验证的修改。
  ([ShootingCalculator.java#L116-L200](src/main/java/com/nextinnovation/team8214/subsystem/shooter/ShootingCalculator.java#L116-L200)
  · [index.js#L1348-L1359](src/main/deploy/shootingcalculator/index.js#L1348-L1359)
  · [ShootingCalculator.java#L32-L78](src/main/java/com/nextinnovation/team8214/subsystem/shooter/ShootingCalculator.java#L32-L78))

![在线射击表调试](assets/shooting-calculator.png)

### 仿真与工具

- **储球量实时改变机器人物理参数：** 储存的 Fuel 会改变机器人质量和转动惯量，Bump 姿态同时进入陀螺仪与弹道，命中 Hub 的 Fuel 会返回场地。
  ([Sim.java#L192-L209](src/main/java/com/nextinnovation/team8214/Sim.java#L192-L209)
  · [Swerve.java#L211-L223](src/main/java/com/nextinnovation/team8214/subsystem/swerve/Swerve.java#L211-L223)
  · [Sim.java#L219-L327](src/main/java/com/nextinnovation/team8214/Sim.java#L219-L327))
- **双实例 Web Elastic：** 按端口命名的启动器在独立端口托管内置 Web 仪表盘，可同时运行两个本地 Elastic 实例。
  ([index.html#L1-L23](.elastic/index.html#L1-L23)
  · [elastic_launcher.go#L13-L48](.elastic/launcher/elastic_launcher.go#L13-L48)
  · [README.md#L17-L27](.elastic/README.md#L17-L27))

### 其他工程细节

- 基于SuperAutoPilot的无状态路径跟随。
  ([SuperAutopilot.java#L43-L109](src/main/java/com/nextinnovation/team8214/util/superautopilot/SuperAutopilot.java#L43-L109)
  · [DriveToPointController.java#L181-L208](src/main/java/com/nextinnovation/team8214/subsystem/swerve/controller/DriveToPointController.java#L181-L208))
- 一份 Choreo 路径可生成左/右和红/蓝四种变体。
  ([TrajectoryLoader.java#L66-L105](src/main/java/com/nextinnovation/team8214/TrajectoryLoader.java#L66-L105)
  · [TrajectoryLoader.java#L108-L158](src/main/java/com/nextinnovation/team8214/TrajectoryLoader.java#L108-L158))
- 相机仿真覆盖图像质量、延迟、处理后画面，并按六个场地区域限制可见标签。
  ([ApriltagVisionCamera.java#L50-L80](src/main/java/com/nextinnovation/team8214/subsystem/vision/ApriltagVisionCamera.java#L50-L80)
  · [ApriltagVisionIOPhotonSim.java#L22-L34](src/main/java/com/nextinnovation/team8214/subsystem/vision/ApriltagVisionIOPhotonSim.java#L22-L34)
  · [ApriltagVisionIOPhotonSim.java#L36-L60](src/main/java/com/nextinnovation/team8214/subsystem/vision/ApriltagVisionIOPhotonSim.java#L36-L60))
- 问答式自动选择器可组合路线变体，不需要在仪表盘堆满独立模式。
  ([AutoModeSelector.java#L21-L129](src/main/java/com/nextinnovation/team8214/AutoModeSelector.java#L21-L129)
  · [AutoModes.java#L198-L220](src/main/java/com/nextinnovation/team8214/command/AutoModes.java#L198-L220))
- 实时调参按子系统分组启用。
  ([Config.java#L14-L48](src/main/java/com/nextinnovation/team8214/Config.java#L14-L48)
  · [LoggedTunableNumber.java#L31-L49](src/main/java/com/nextinnovation/team8214/util/LoggedTunableNumber.java#L31-L49)
  · [Config.java#L105-L113](src/main/java/com/nextinnovation/team8214/Config.java#L105-L113))
- 基于TransformTree的子系统姿态解算。
  ([RobotContainer.java#L118-L136](src/main/java/com/nextinnovation/team8214/RobotContainer.java#L118-L136)
  · [Visualizer.java#L35-L78](src/main/java/com/nextinnovation/team8214/subsystem/Visualizer.java#L35-L78)
  · [config.json#L103-L136](.ascope/Robot_OFFSEASON2026/config.json#L103-L136))
- 待机预加速同时限制用于查表的场地位置与距离范围。
  ([IdleController.java#L22-L76](src/main/java/com/nextinnovation/team8214/subsystem/shooter/controller/IdleController.java#L22-L76)
  · [IdleController.java#L79-L86](src/main/java/com/nextinnovation/team8214/subsystem/shooter/controller/IdleController.java#L79-L86))
- 射手就绪判定按模式区分：得分要求双向速度误差，Transport 与 Fence 只要求达到最低速度。
  ([Shooter.java#L350-L373](src/main/java/com/nextinnovation/team8214/subsystem/shooter/Shooter.java#L350-L373))

## 环境要求

- WPILib 2026.2.1，包含其自带的 JDK 17
- Git
- Git LFS
- Windows、Linux 或 macOS 可进行普通构建；部署到 roboRIO 需要 WPILib 工具链

克隆并拉取 LFS 内容：

```bash
git lfs install
git clone <repository-url>
cd ni-8214-2026-public
git lfs pull
```

MACK 照片、PhotonVision ARM64 更新程序和 AdvantageScope GLB 模型使用 Git LFS
存储。LFS 指针文件不能直接作为资产使用；部署或制作发布包前请运行
`git lfs status` 检查。

## 构建

Windows PowerShell：

```powershell
./gradlew.bat spotlessCheck
./gradlew.bat build
```

Linux 或 macOS：

```bash
./gradlew spotlessCheck
./gradlew build
```

`compileJava` 当前会自动执行 Spotless 格式化，运行后请检查工作区。

## 运行模式

在 [`Config.java`](src/main/java/com/nextinnovation/team8214/Config.java) 中选择模式：

- `REAL`：roboRIO 与真实硬件
- `SIM`：本地 MapleSim/WPILib 仿真
- `REPLAY`：AdvantageKit 日志回放

归档时的 Release 配置为 `REAL`，并关闭实时调试。

### 仿真

```powershell
./gradlew.bat simulateJava
```

无界面的 agent-auto 验证：

```powershell
$env:AGENT_AUTO_NAME="Silence"
./gradlew.bat simulateJavaAgentAuto
```

### 回放

将 `Config.MODE` 设为 `REPLAY`，然后在 AdvantageScope 中选择日志，或显式指定路径：

```powershell
$env:AKIT_LOG_PATH="C:\path\to\input.wpilog"
./gradlew.bat replayOnce
```

`replayWatch` 会在源码变化后重新执行回放。

### 部署

部署前将 `Config.MODE` 设为 `REAL`：

```powershell
./gradlew.bat deploy
```

在 `event/*` 分支上，只有部署任务会在生成构建元数据前暂存并提交工作区全部修改。构建、仿真和回放任务不会触发该 event commit 任务。

## 资产使用

使用 AdvantageScope 机器人模型时，请导入 `.ascope/layout.json`，并将
`.ascope/Robot_OFFSEASON2026` 文件夹复制到 AdvantageScope 用户资产目录。

仓库中的 `.elastic` 是独立 Web 包。数字命名的 Windows 启动器会在对应的
localhost 端口托管该目录，具体见 [`.elastic/README.md`](.elastic/README.md)。

## 许可证

Team 8214 编写的代码使用 [MIT License](LICENSE)。

第三方依赖、Web 源码、资产与二进制文件保留其原许可证。详见
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 和
[THIRD_PARTY_LICENSES](THIRD_PARTY_LICENSES)。其中 WPILib 入口文件保留
BSD-3-Clause 条款；内置的 PhotonVision 定位与更新程序产物包含 GPLv3 代码，不随本项目改为 MIT。
