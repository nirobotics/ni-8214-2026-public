// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util.log;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.util.datalog.DataLogReader;
import edu.wpi.first.util.datalog.DataLogRecord;
import edu.wpi.first.util.struct.Struct;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.stream.Collectors;

final class LogReaderUtil {
  private LogReaderUtil() {}

  static DataLogReader openLog(Path logPath) throws IOException {
    if (!Files.isRegularFile(logPath)) {
      throw new IOException("Log file does not exist: " + logPath);
    }

    var reader = new DataLogReader(logPath.toString());
    if (!reader.isValid()) {
      throw new IOException("Invalid WPILOG file: " + logPath);
    }

    return reader;
  }

  static void forEachRecord(DataLogReader reader, Consumer<DataLogRecord> action) {
    var iterator = reader.iterator();
    while (true) {
      DataLogRecord record;
      try {
        if (!iterator.hasNext()) {
          return;
        }
        record = iterator.next();
      } catch (RuntimeException e) {
        System.err.println("Warning: stopped reading log at an incomplete or invalid record.");
        return;
      }

      action.accept(record);
    }
  }

  static String decodeValue(DataLogRecord record, String type) {
    return switch (type) {
      case "boolean" -> Boolean.toString(record.getBoolean());
      case "int64" -> Long.toString(record.getInteger());
      case "float" -> Float.toString(record.getFloat());
      case "double" -> Double.toString(record.getDouble());
      case "string", "json" -> record.getString();
      case "boolean[]" -> Arrays.toString(record.getBooleanArray());
      case "int64[]" -> Arrays.toString(record.getIntegerArray());
      case "float[]" -> Arrays.toString(record.getFloatArray());
      case "double[]" -> Arrays.toString(record.getDoubleArray());
      case "string[]" -> Arrays.toString(record.getStringArray());
      case "struct:Pose2d" -> formatPose2d(unpackPose2d(record.getRawBuffer()));
      case "struct:Pose2d[]" -> formatPose2dArray(record.getRawBuffer());
      default -> "raw[" + record.getSize() + " bytes]";
    };
  }

  private static Pose2d unpackPose2d(ByteBuffer rawBuffer) {
    var buffer = rawBuffer.slice().order(ByteOrder.LITTLE_ENDIAN);
    return Pose2d.struct.unpack(buffer);
  }

  private static String formatPose2d(Pose2d pose) {
    return String.format(
        "x=%.6f y=%.6f deg=%.3f", pose.getX(), pose.getY(), pose.getRotation().getDegrees());
  }

  private static String formatPose2dArray(ByteBuffer rawBuffer) {
    var buffer = rawBuffer.slice().order(ByteOrder.LITTLE_ENDIAN);
    var count = buffer.remaining() / Pose2d.struct.getSize();
    var poses = Struct.unpackArray(buffer, count, Pose2d.struct);
    return Arrays.stream(poses).map(LogReaderUtil::formatPose2d).collect(Collectors.joining("; "));
  }

  static OptionalDouble decodeNumericValue(DataLogRecord record, String type) {
    return switch (type) {
      case "int64" -> OptionalDouble.of(record.getInteger());
      case "float" -> OptionalDouble.of(record.getFloat());
      case "double" -> OptionalDouble.of(record.getDouble());
      default -> OptionalDouble.empty();
    };
  }

  static String csvRow(String... fields) {
    return Arrays.stream(fields).map(LogReaderUtil::csvField).collect(Collectors.joining(","));
  }

  private static String csvField(String field) {
    if (field == null) {
      return "";
    }

    if (field.indexOf(',') < 0
        && field.indexOf('"') < 0
        && field.indexOf('\n') < 0
        && field.indexOf('\r') < 0) {
      return field;
    }

    return "\"" + field.replace("\"", "\"\"") + "\"";
  }

  static final class EntrySummary {
    final int entry;
    final String name;
    final String type;
    String metadata;
    int records;
    double firstTimestampSec = Double.NaN;
    double lastTimestampSec = Double.NaN;

    EntrySummary(DataLogRecord.StartRecordData data) {
      entry = data.entry;
      name = data.name;
      type = data.type;
      metadata = data.metadata;
    }

    void updateTimestamp(long timestampMicro) {
      var timestampSec = timestampMicro / 1_000_000.0;
      if (Double.isNaN(firstTimestampSec)) {
        firstTimestampSec = timestampSec;
      }
      lastTimestampSec = timestampSec;
      records++;
    }
  }
}
