// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util;

public class LoggedTunableGains {
  private final LoggedTunableNumber kp;
  private final LoggedTunableNumber ki;
  private final LoggedTunableNumber kd;
  private final LoggedTunableNumber kv;
  private final LoggedTunableNumber ks;
  private final LoggedTunableNumber kg;

  public LoggedTunableGains(
      String logRoot, double kp, double ki, double kd, double kv, double ks, double kg) {
    this("Default", logRoot, kp, ki, kd, kv, ks, kg);
  }

  public LoggedTunableGains(
      String logGroup,
      String logRoot,
      double kp,
      double ki,
      double kd,
      double kv,
      double ks,
      double kg) {
    var cleanLogGroup = logGroup.replaceFirst("/$", "");
    var cleanLogRoot = logRoot.replaceFirst("/$", "") + "/gains";

    this.kp = new LoggedTunableNumber(cleanLogGroup, cleanLogRoot + "/kp", kp);
    this.ki = new LoggedTunableNumber(cleanLogGroup, cleanLogRoot + "/ki", ki);
    this.kd = new LoggedTunableNumber(cleanLogGroup, cleanLogRoot + "/kd", kd);
    this.kv = new LoggedTunableNumber(cleanLogGroup, cleanLogRoot + "/kv", kv);
    this.ks = new LoggedTunableNumber(cleanLogGroup, cleanLogRoot + "/ks", ks);
    this.kg = new LoggedTunableNumber(cleanLogGroup, cleanLogRoot + "/kg", kg);
  }

  public double getKp() {
    return kp.getAsDouble();
  }

  public double getKi() {
    return ki.getAsDouble();
  }

  public double getKd() {
    return kd.getAsDouble();
  }

  public double getKv() {
    return kv.getAsDouble();
  }

  public double getKs() {
    return ks.getAsDouble();
  }

  public double getKg() {
    return kg.getAsDouble();
  }

  public LoggedTunableNumber[] getEntries() {
    return new LoggedTunableNumber[] {kp, ki, kd, kv, ks, kg};
  }
}
