// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util.log;

import java.nio.file.Path;
import java.util.HashMap;

public final class ListKeys {
  private ListKeys() {}

  public static void main(String... args) {
    try {
      run(args);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      printUsage();
      System.exit(1);
    }
  }

  private static void run(String... args) throws Exception {
    var logPath = parseLogPath(args);
    var entries = new HashMap<Integer, LogReaderUtil.EntrySummary>();

    LogReaderUtil.forEachRecord(
        LogReaderUtil.openLog(logPath),
        record -> {
          if (record.isStart()) {
            var data = record.getStartData();
            entries.put(data.entry, new LogReaderUtil.EntrySummary(data));
            return;
          }

          if (record.isSetMetadata()) {
            var data = record.getSetMetadataData();
            var entry = entries.get(data.entry);
            if (entry != null) {
              entry.metadata = data.metadata;
            }
            return;
          }

          if (!record.isControl()) {
            var entry = entries.get(record.getEntry());
            if (entry != null) {
              entry.updateTimestamp(record.getTimestamp());
            }
          }
        });

    System.out.println(
        LogReaderUtil.csvRow(
            "entry", "name", "type", "records", "firstTimestampSec", "lastTimestampSec"));
    entries.values().stream()
        .sorted(
            (first, second) -> {
              var nameCompare = first.name.compareTo(second.name);
              return nameCompare != 0 ? nameCompare : Integer.compare(first.entry, second.entry);
            })
        .map(ListKeys::toCsvRow)
        .forEach(System.out::println);
  }

  private static Path parseLogPath(String... args) {
    if (args.length == 2 && args[0].equals("--log")) {
      return Path.of(args[1]);
    }

    if (args.length == 1 && args[0].startsWith("--log=")) {
      return Path.of(args[0].substring("--log=".length()));
    }

    if (args.length == 1) {
      return Path.of(args[0]);
    }

    throw new IllegalArgumentException("Missing log path.");
  }

  private static String toCsvRow(LogReaderUtil.EntrySummary entry) {
    return LogReaderUtil.csvRow(
        Integer.toString(entry.entry),
        entry.name,
        entry.type,
        Integer.toString(entry.records),
        Double.isNaN(entry.firstTimestampSec) ? "" : Double.toString(entry.firstTimestampSec),
        Double.isNaN(entry.lastTimestampSec) ? "" : Double.toString(entry.lastTimestampSec));
  }

  private static void printUsage() {
    System.err.println("Usage: ListKeys <log.wpilog>");
    System.err.println("   or: ListKeys --log <log.wpilog>");
    System.err.println("   or: ListKeys --log=<log.wpilog>");
  }
}
