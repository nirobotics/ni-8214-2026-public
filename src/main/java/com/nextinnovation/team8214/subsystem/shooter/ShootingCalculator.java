// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.subsystem.shooter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextinnovation.team8214.Config;
import com.nextinnovation.team8214.util.VirtualSubsystem;
import edu.wpi.first.networktables.DoublePublisher;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StringPublisher;
import edu.wpi.first.networktables.StringSubscriber;
import edu.wpi.first.wpilibj.Filesystem;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

public class ShootingCalculator extends VirtualSubsystem {
  record TableEntry(double distance, double xVel, double yVel) {}

  private record TableInfo(String id, String method) {}

  @Getter
  static class ShootingInterpolatedTable {
    private record TableState(String json, List<TableEntry> entries) {}

    private final String id;
    private final String method; // "interpolated" or "constant"
    private final String defaultJson;
    private volatile TableState currentState;
    private String rejectedJson;
    private boolean hasRejectedJson;

    ShootingInterpolatedTable(
        String id, String method, String defaultJson, List<TableEntry> defaultEntries) {
      this.id = id;
      this.method = method;
      this.defaultJson = defaultJson;
      currentState = new TableState(defaultJson, defaultEntries);
    }

    public String getCurrentJson() {
      return currentState.json();
    }

    public List<TableEntry> getCurrentEntries() {
      return currentState.entries();
    }

    boolean tryUpdate(String json, ObjectMapper objectMapper) {
      if (hasRejectedJson && Objects.equals(json, rejectedJson)) {
        return false;
      }

      final List<TableEntry> entries;
      try {
        entries = parseEntries(objectMapper, method, json);
      } catch (IllegalArgumentException e) {
        rejectedJson = json;
        hasRejectedJson = true;
        System.err.println("Rejected shooting table '" + id + "': " + e.getMessage());
        return false;
      }

      currentState = new TableState(json, entries);
      rejectedJson = null;
      hasRejectedJson = false;
      return true;
    }

    public String getTableTopic() {
      return "/shootingcalculator/tables/" + id;
    }

    public String getModifiedTableTopic() {
      return "/shootingcalculator/tables/" + id + "/modified";
    }
  }

  private final NetworkTableInstance ntInstance;
  private final ObjectMapper objectMapper;

  private final Map<String, ShootingInterpolatedTable> tables;
  private final Map<String, StringPublisher> tablePublishers;
  private final Map<String, StringSubscriber> modifiedTableSubscribers;

  private final StringPublisher tableListPublisher;
  private final StringPublisher currentTableIdPublisher;
  private final DoublePublisher currentDistancePublisher;

  // Map to store shooter-specific velocity publishers
  private final Map<String, DoublePublisher> shooterXVelPublishers;
  private final Map<String, DoublePublisher> shooterYVelPublishers;

  private String currentTableId;

  private static ShootingCalculator instance = null;

  public static ShootingCalculator getInstance() {
    if (instance == null) {
      instance = new ShootingCalculator();
    }

    return instance;
  }

  private ShootingCalculator() {
    ntInstance = NetworkTableInstance.getDefault();
    objectMapper =
        new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

    tables = new LinkedHashMap<>();
    tablePublishers = new HashMap<>();
    modifiedTableSubscribers = new HashMap<>();

    shooterXVelPublishers = new HashMap<>();
    shooterYVelPublishers = new HashMap<>();

    tableListPublisher = ntInstance.getStringTopic("/shootingcalculator/tableList").publish();

    currentTableIdPublisher =
        ntInstance.getStringTopic("/shootingcalculator/currentTableId").publish();

    currentDistancePublisher =
        ntInstance.getDoubleTopic("/shootingcalculator/currentDistance").publish();

    registerDefaultTables();

    if (!tables.isEmpty()) {
      currentTableId = tables.keySet().iterator().next();
      currentTableIdPublisher.set(currentTableId);
    }

    publishTableList();

    if (Config.MODE != Config.Mode.REPLAY) {
      var app =
          Javalin.create(
              config ->
                  config.staticFiles.add(
                      Paths.get(
                              Filesystem.getDeployDirectory().getAbsolutePath(),
                              "shootingcalculator")
                          .toString(),
                      Location.EXTERNAL));
      app.start(5800);
    }
  }

  private void registerDefaultTables() {
    registerTable(
        "score",
        "interpolated",
        "["
            + "{\"distance\":1.300,\"xVel\":1.288,\"yVel\":6.800},"
            + "{\"distance\":2.000,\"xVel\":2.821,\"yVel\":6.800},"
            + "{\"distance\":3.000,\"xVel\":4.474,\"yVel\":6.566},"
            + "{\"distance\":4.000,\"xVel\":5.350,\"yVel\":6.400},"
            + "{\"distance\":5.000,\"xVel\":5.633,\"yVel\":6.400}"
            + "]");

    registerTable(
        "transport",
        "interpolated",
        "["
            + "{\"distance\":0.000,\"xVel\":3.410,\"yVel\":3.657},"
            + "{\"distance\":5.300,\"xVel\":3.751,\"yVel\":4.022},"
            + "{\"distance\":9.000,\"xVel\":6.138,\"yVel\":6.582},"
            + "{\"distance\":10.600,\"xVel\":6.820,\"yVel\":7.314},"
            + "{\"distance\":14.970,\"xVel\":8.184,\"yVel\":8.777}"
            + "]");

    registerTable("fence", "constant", "[{\"distance\":0.000,\"xVel\":3.751,\"yVel\":4.022}]");
  }

  public void registerTable(String id, String method, String defaultJson) {
    var table =
        new ShootingInterpolatedTable(
            id, method, defaultJson, parseEntries(objectMapper, method, defaultJson));
    tables.put(table.id, table);

    var publisher = ntInstance.getStringTopic(table.getTableTopic()).publish();
    tablePublishers.put(table.id, publisher);
    publisher.set(defaultJson);

    if (Config.IS_LIVE_DEBUG) {
      var subscriber = ntInstance.getStringTopic(table.getModifiedTableTopic()).subscribe("[]");
      modifiedTableSubscribers.put(id, subscriber);
    }
  }

  static List<TableEntry> parseEntries(ObjectMapper objectMapper, String method, String tableJson) {
    final List<TableEntry> entries;
    try {
      entries = objectMapper.readValue(tableJson, new TypeReference<List<TableEntry>>() {});
    } catch (Exception e) {
      throw new IllegalArgumentException("invalid JSON", e);
    }

    if (!"interpolated".equals(method) && !"constant".equals(method)) {
      throw new IllegalArgumentException("unknown method '" + method + "'");
    }
    if (entries == null || entries.isEmpty()) {
      throw new IllegalArgumentException("table must contain at least one entry");
    }
    if (method.equals("constant") && entries.size() != 1) {
      throw new IllegalArgumentException("constant table must contain exactly one entry");
    }

    var previousDistance = Double.NEGATIVE_INFINITY;
    for (var entry : entries) {
      if (entry == null) {
        throw new IllegalArgumentException("table entries cannot be null");
      }
      if (!Double.isFinite(entry.distance())
          || !Double.isFinite(entry.xVel())
          || !Double.isFinite(entry.yVel())) {
        throw new IllegalArgumentException("distance and velocities must be finite");
      }
      if ("interpolated".equals(method) && entry.distance() <= previousDistance) {
        throw new IllegalArgumentException(
            "interpolated table distances must be strictly increasing");
      }
      previousDistance = entry.distance();
    }

    return List.copyOf(entries);
  }

  private void publishTableList() {
    try {
      List<TableInfo> tableInfoList = new ArrayList<>();
      for (var table : tables.values()) {
        tableInfoList.add(new TableInfo(table.getId(), table.getMethod()));
      }
      var tableListJson = objectMapper.writeValueAsString(tableInfoList);
      tableListPublisher.set(tableListJson);
      System.out.println("Published table list: " + tableListJson);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void switchTable(String tableId) {
    if (tables.containsKey(tableId)) {
      if (!tableId.equals(this.currentTableId)) {
        this.currentTableId = tableId;
        currentTableIdPublisher.set(tableId);
        System.out.println("Switched to table: " + tableId);
      }
    } else {
      System.err.println("Table not found: " + tableId);
    }
  }

  public void updateTable(String tableId, String tableJson) {
    var table = tables.get(tableId);
    if (table != null && table.tryUpdate(tableJson, objectMapper)) {
      var publisher = tablePublishers.get(tableId);
      if (publisher != null) {
        publisher.set(tableJson);
      }
    }
  }

  public double getInterpolatedX(double distance) {
    return interpolateValue(currentTableId, distance, true);
  }

  public double getInterpolatedY(double distance) {
    return interpolateValue(currentTableId, distance, false);
  }

  public double getInterpolatedX(String tableId, double distance) {
    return interpolateValue(tableId, distance, true);
  }

  public double getInterpolatedY(String tableId, double distance) {
    return interpolateValue(tableId, distance, false);
  }

  private double interpolateValue(String tableId, double distance, boolean isX) {
    try {
      var table = tables.get(tableId);
      if (table == null) {
        return 0.0;
      }

      var entries = table.getCurrentEntries();

      if (entries.isEmpty()) {
        return 0.0;
      }

      // For constant method, always return the single value regardless of distance
      if ("constant".equals(table.getMethod())) {
        return isX ? entries.get(0).xVel : entries.get(0).yVel;
      }

      // For interpolated method
      if (entries.size() == 1) {
        return isX ? entries.get(0).xVel : entries.get(0).yVel;
      }

      if (distance <= entries.get(0).distance) {
        return isX ? entries.get(0).xVel : entries.get(0).yVel;
      }
      if (distance >= entries.get(entries.size() - 1).distance) {
        var last = entries.get(entries.size() - 1);
        return isX ? last.xVel : last.yVel;
      }

      for (var i = 0; i < entries.size() - 1; i++) {
        var lower = entries.get(i);
        var upper = entries.get(i + 1);

        if (lower.distance <= distance && upper.distance >= distance) {
          var t = (distance - lower.distance) / (upper.distance - lower.distance);
          if (isX) {
            return lower.xVel + t * (upper.xVel - lower.xVel);
          } else {
            return lower.yVel + t * (upper.yVel - lower.yVel);
          }
        }
      }

      return 0.0;
    } catch (Exception e) {
      e.printStackTrace();
      return 0.0;
    }
  }

  public void publishCurrentDistance(double distance) {
    currentDistancePublisher.set(distance);
  }

  public void publishMainCurrentXYVel(double xVel, double yVel) {
    publishCurrentXVel("main", xVel);
    publishCurrentYVel("main", yVel);
  }

  public void publishSecondaryCurrentXYVel(double xVel, double yVel) {
    publishCurrentXVel("secondary", xVel);
    publishCurrentYVel("secondary", yVel);
  }

  /**
   * Publish current X velocity for a specific shooter.
   *
   * @param shooterName The name of the shooter (e.g., "main", "secondary")
   * @param xVel The X velocity value
   */
  private void publishCurrentXVel(String shooterName, double xVel) {
    DoublePublisher publisher = shooterXVelPublishers.get(shooterName);
    if (publisher == null) {
      String topic = "/shootingcalculator/shooters/" + shooterName + "/xVel";
      publisher = ntInstance.getDoubleTopic(topic).publish();
      shooterXVelPublishers.put(shooterName, publisher);
    }
    publisher.set(xVel);
  }

  /**
   * Publish current Y velocity for a specific shooter.
   *
   * @param shooterName The name of the shooter (e.g., "main", "secondary")
   * @param yVel The Y velocity value
   */
  private void publishCurrentYVel(String shooterName, double yVel) {
    DoublePublisher publisher = shooterYVelPublishers.get(shooterName);
    if (publisher == null) {
      String topic = "/shootingcalculator/shooters/" + shooterName + "/yVel";
      publisher = ntInstance.getDoubleTopic(topic).publish();
      shooterYVelPublishers.put(shooterName, publisher);
    }
    publisher.set(yVel);
  }

  @Override
  public void periodic() {
    if (!Config.IS_LIVE_DEBUG) {
      return;
    }
    processModifiedTables();
  }

  public void processModifiedTables() {
    for (var entry : modifiedTableSubscribers.entrySet()) {
      var tableId = entry.getKey();
      var subscriber = entry.getValue();

      var modifiedJson = subscriber.get();
      if (modifiedJson != null && !modifiedJson.isEmpty() && !modifiedJson.equals("[]")) {
        var table = tables.get(tableId);
        if (table != null) {
          var currentJson = table.getCurrentJson();
          if (!modifiedJson.equals(currentJson)) {
            updateTable(tableId, modifiedJson);
            if (!currentJson.equals(table.getCurrentJson())) {
              System.out.println("Updated table '" + tableId + "' from web");
            }
          }
        }
      }
    }
  }
}
