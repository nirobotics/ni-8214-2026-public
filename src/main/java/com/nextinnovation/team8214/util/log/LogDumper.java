// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util.log;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class LogDumper {
  private static final double MAX_CORRELATION_WINDOW_SEC = 0.05;

  private LogDumper() {}

  public static void main(String... args) {
    try {
      run(Options.parse(args));
    } catch (Exception e) {
      System.err.println(e.getMessage());
      printUsage();
      System.exit(1);
    }
  }

  private static void run(Options options) throws Exception {
    if (options.stats) {
      dumpStats(options);
    } else {
      dumpRows(options);
    }
  }

  private static void dumpRows(Options options) throws Exception {
    var entries = new HashMap<Integer, LogReaderUtil.EntrySummary>();
    var output = openOutput(options);

    try {
      output.writer.println(LogReaderUtil.csvRow("timestampSec", "key", "type", "value"));
      LogReaderUtil.forEachRecord(
          LogReaderUtil.openLog(options.logPath),
          record -> {
            if (record.isStart()) {
              var data = record.getStartData();
              entries.put(data.entry, new LogReaderUtil.EntrySummary(data));
              return;
            }

            if (record.isControl() || !options.includesTimestamp(record.getTimestamp())) {
              return;
            }

            var entry = entries.get(record.getEntry());
            if (entry == null || !options.includesKey(entry.name)) {
              return;
            }

            output.writer.println(
                LogReaderUtil.csvRow(
                    Double.toString(record.getTimestamp() / 1_000_000.0),
                    entry.name,
                    entry.type,
                    LogReaderUtil.decodeValue(record, entry.type)));
          });
    } finally {
      output.close();
    }
  }

  private static void dumpStats(Options options) throws Exception {
    var entries = new HashMap<Integer, LogReaderUtil.EntrySummary>();
    var statsByKey = new HashMap<String, RunningStats>();
    var typeByKey = new HashMap<String, String>();

    LogReaderUtil.forEachRecord(
        LogReaderUtil.openLog(options.logPath),
        record -> {
          if (record.isStart()) {
            var data = record.getStartData();
            entries.put(data.entry, new LogReaderUtil.EntrySummary(data));
            return;
          }

          if (record.isControl() || !options.includesTimestamp(record.getTimestamp())) {
            return;
          }

          var entry = entries.get(record.getEntry());
          if (entry == null || !options.includesKey(entry.name)) {
            return;
          }

          var value = LogReaderUtil.decodeNumericValue(record, entry.type);
          if (value.isEmpty()) {
            return;
          }

          typeByKey.put(entry.name, entry.type);
          statsByKey
              .computeIfAbsent(entry.name, unused -> new RunningStats())
              .add(record.getTimestamp() / 1_000_000.0, value.getAsDouble());
        });

    var output = openOutput(options);
    try {
      output.writer.println(
          LogReaderUtil.csvRow("key", "type", "count", "min", "max", "mean", "stddev"));
      statsByKey.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(
              entry ->
                  output.writer.println(
                      toStatsCsvRow(entry.getKey(), typeByKey, entry.getValue())));

      findCorrelationBaseline(statsByKey.keySet())
          .ifPresent(
              baselineKey -> {
                output.writer.println();
                output.writer.println(
                    LogReaderUtil.csvRow("baseline", "key", "pairedSamples", "correlation"));
                statsByKey.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(baselineKey))
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(
                        entry -> {
                          var correlation =
                              correlate(
                                  statsByKey.get(baselineKey).samples, entry.getValue().samples);
                          if (correlation.pairedSamples > 1) {
                            output.writer.println(
                                LogReaderUtil.csvRow(
                                    baselineKey,
                                    entry.getKey(),
                                    Integer.toString(correlation.pairedSamples),
                                    Double.toString(correlation.value)));
                          }
                        });
              });
    } finally {
      output.close();
    }
  }

  private static Output openOutput(Options options) throws Exception {
    if (options.outPath == null) {
      return new Output(new PrintWriter(System.out, true), false);
    }

    var parent = options.outPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    return new Output(new PrintWriter(Files.newBufferedWriter(options.outPath)), true);
  }

  private static Optional<String> findCorrelationBaseline(Set<String> keys) {
    return keys.stream()
        .filter(key -> key.endsWith("UserCodeMS") || key.contains("/UserCodeMS"))
        .findFirst();
  }

  private static CorrelationResult correlate(List<Sample> baseline, List<Sample> other) {
    var firstValues = new ArrayList<Double>();
    var secondValues = new ArrayList<Double>();
    var otherIndex = 0;

    for (var sample : baseline) {
      while (otherIndex + 1 < other.size()
          && Math.abs(other.get(otherIndex + 1).timestampSec - sample.timestampSec)
              <= Math.abs(other.get(otherIndex).timestampSec - sample.timestampSec)) {
        otherIndex++;
      }

      if (!other.isEmpty()
          && Math.abs(other.get(otherIndex).timestampSec - sample.timestampSec)
              <= MAX_CORRELATION_WINDOW_SEC) {
        firstValues.add(sample.value);
        secondValues.add(other.get(otherIndex).value);
      }
    }

    if (firstValues.size() < 2) {
      return new CorrelationResult(firstValues.size(), Double.NaN);
    }

    var firstMean = firstValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    var secondMean = secondValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    var numerator = 0.0;
    var firstSquares = 0.0;
    var secondSquares = 0.0;

    for (var i = 0; i < firstValues.size(); i++) {
      var firstDelta = firstValues.get(i) - firstMean;
      var secondDelta = secondValues.get(i) - secondMean;
      numerator += firstDelta * secondDelta;
      firstSquares += firstDelta * firstDelta;
      secondSquares += secondDelta * secondDelta;
    }

    var denominator = Math.sqrt(firstSquares * secondSquares);
    return new CorrelationResult(
        firstValues.size(), denominator == 0.0 ? Double.NaN : numerator / denominator);
  }

  private static String toStatsCsvRow(
      String key, Map<String, String> typeByKey, RunningStats runningStats) {
    return LogReaderUtil.csvRow(
        key,
        typeByKey.get(key),
        Integer.toString(runningStats.count),
        Double.toString(runningStats.min),
        Double.toString(runningStats.max),
        Double.toString(runningStats.mean()),
        Double.toString(runningStats.stddev()));
  }

  private static void printUsage() {
    System.err.println(
        "Usage: LogDumper --log <log.wpilog> [--keys k1,k2] [--start sec] [--end sec] [--out file.csv] [--stats]");
  }

  private record Sample(double timestampSec, double value) {}

  private record CorrelationResult(int pairedSamples, double value) {}

  private record Output(PrintWriter writer, boolean closeWhenDone) {
    void close() {
      if (closeWhenDone) {
        writer.close();
      } else {
        writer.flush();
      }
    }
  }

  private static final class RunningStats {
    final List<Sample> samples = new ArrayList<>();
    int count;
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    double sum;
    double sumSquares;

    void add(double timestampSec, double value) {
      samples.add(new Sample(timestampSec, value));
      count++;
      min = Math.min(min, value);
      max = Math.max(max, value);
      sum += value;
      sumSquares += value * value;
    }

    double mean() {
      return sum / count;
    }

    double stddev() {
      var variance = sumSquares / count - mean() * mean();
      return Math.sqrt(Math.max(variance, 0.0));
    }
  }

  private static final class Options {
    Path logPath;
    Set<String> keys;
    Double startSec;
    Double endSec;
    Path outPath;
    boolean stats;

    static Options parse(String... args) {
      var options = new Options();
      for (var i = 0; i < args.length; i++) {
        var arg = args[i];
        switch (arg) {
          case "--log" -> options.logPath = Path.of(requireValue(args, ++i, arg));
          case "--keys" -> options.keys = parseKeys(requireValue(args, ++i, arg));
          case "--start" -> options.startSec = Double.parseDouble(requireValue(args, ++i, arg));
          case "--end" -> options.endSec = Double.parseDouble(requireValue(args, ++i, arg));
          case "--out" -> options.outPath = Path.of(requireValue(args, ++i, arg));
          case "--stats" -> options.stats = true;
          default -> parseEqualsArg(options, arg);
        }
      }

      if (options.logPath == null) {
        throw new IllegalArgumentException("Missing required --log argument.");
      }
      return options;
    }

    boolean includesKey(String key) {
      return keys == null || keys.contains(key);
    }

    boolean includesTimestamp(long timestampMicro) {
      var timestampSec = timestampMicro / 1_000_000.0;
      return (startSec == null || timestampSec >= startSec)
          && (endSec == null || timestampSec <= endSec);
    }

    private static void parseEqualsArg(Options options, String arg) {
      if (arg.startsWith("--log=")) {
        options.logPath = Path.of(arg.substring("--log=".length()));
      } else if (arg.startsWith("--keys=")) {
        options.keys = parseKeys(arg.substring("--keys=".length()));
      } else if (arg.startsWith("--start=")) {
        options.startSec = Double.parseDouble(arg.substring("--start=".length()));
      } else if (arg.startsWith("--end=")) {
        options.endSec = Double.parseDouble(arg.substring("--end=".length()));
      } else if (arg.startsWith("--out=")) {
        options.outPath = Path.of(arg.substring("--out=".length()));
      } else {
        throw new IllegalArgumentException("Unknown argument: " + arg);
      }
    }

    private static Set<String> parseKeys(String rawKeys) {
      var keys = new LinkedHashSet<String>();
      for (var key : rawKeys.split(",")) {
        if (!key.isBlank()) {
          keys.add(key.trim());
        }
      }
      return keys;
    }

    private static String requireValue(String[] args, int index, String option) {
      if (index >= args.length) {
        throw new IllegalArgumentException("Missing value for " + option);
      }
      return args[index];
    }
  }
}
