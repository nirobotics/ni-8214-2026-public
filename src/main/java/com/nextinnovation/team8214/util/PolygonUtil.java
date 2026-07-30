// Copyright (c) 2026 FRC Team 8214 contributors
// SPDX-License-Identifier: MIT

package com.nextinnovation.team8214.util;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;

public class PolygonUtil {
  private static final double GEOMETRY_EPSILON = 1e-9;

  /** Returns whether a line segment intersects or touches an axis-aligned rectangle. */
  public static boolean segmentIntersectsRectangle(
      Translation2d start, Translation2d end, double minX, double maxX, double minY, double maxY) {
    if (isInsideRectangle(start, minX, maxX, minY, maxY)
        || isInsideRectangle(end, minX, maxX, minY, maxY)) {
      return true;
    }

    var dx = end.getX() - start.getX();
    var dy = end.getY() - start.getY();
    if (dx != 0.0) {
      var minXT = (minX - start.getX()) / dx;
      var maxXT = (maxX - start.getX()) / dx;
      if ((minXT >= 0.0
              && minXT <= 1.0
              && start.getY() + minXT * dy >= minY
              && start.getY() + minXT * dy <= maxY)
          || (maxXT >= 0.0
              && maxXT <= 1.0
              && start.getY() + maxXT * dy >= minY
              && start.getY() + maxXT * dy <= maxY)) {
        return true;
      }
    }

    if (dy != 0.0) {
      var minYT = (minY - start.getY()) / dy;
      var maxYT = (maxY - start.getY()) / dy;
      return (minYT >= 0.0
              && minYT <= 1.0
              && start.getX() + minYT * dx >= minX
              && start.getX() + minYT * dx <= maxX)
          || (maxYT >= 0.0
              && maxYT <= 1.0
              && start.getX() + maxYT * dx >= minX
              && start.getX() + maxYT * dx <= maxX);
    }

    return false;
  }

  /**
   * Returns whether a line segment intersects the exact area swept by an axis-aligned rectangle
   * rotating about the origin.
   */
  public static boolean segmentIntersectsRotatedRectangleSweep(
      Translation2d segmentStart,
      Translation2d segmentEnd,
      Rotation2d deltaRotation,
      double minX,
      double maxX,
      double minY,
      double maxY) {
    if (segmentIntersectsRectangle(segmentStart, segmentEnd, minX, maxX, minY, maxY)) {
      return true;
    }

    var inverseRotation = deltaRotation.unaryMinus();
    if (segmentIntersectsRectangle(
        segmentStart.rotateBy(inverseRotation),
        segmentEnd.rotateBy(inverseRotation),
        minX,
        maxX,
        minY,
        maxY)) {
      return true;
    }

    var deltaRotationRad = deltaRotation.getRadians();
    for (var vertex :
        new Translation2d[] {
          new Translation2d(minX, minY),
          new Translation2d(minX, maxY),
          new Translation2d(maxX, minY),
          new Translation2d(maxX, maxY)
        }) {
      if (segmentIntersectsRotationArc(segmentStart, segmentEnd, vertex, deltaRotationRad)) {
        return true;
      }
    }

    if (minY <= 0.0
        && maxY >= 0.0
        && (segmentIntersectsRotationArc(
                segmentStart, segmentEnd, new Translation2d(minX, 0.0), deltaRotationRad)
            || segmentIntersectsRotationArc(
                segmentStart, segmentEnd, new Translation2d(maxX, 0.0), deltaRotationRad))) {
      return true;
    }
    if (minX <= 0.0
        && maxX >= 0.0
        && (segmentIntersectsRotationArc(
                segmentStart, segmentEnd, new Translation2d(0.0, minY), deltaRotationRad)
            || segmentIntersectsRotationArc(
                segmentStart, segmentEnd, new Translation2d(0.0, maxY), deltaRotationRad))) {
      return true;
    }

    return arcIntersectsRectangle(segmentStart, inverseRotation, minX, maxX, minY, maxY)
        || arcIntersectsRectangle(segmentEnd, inverseRotation, minX, maxX, minY, maxY);
  }

  /** Returns whether a point is inside or on the boundary of an axis-aligned rectangle. */
  private static boolean isInsideRectangle(
      Translation2d point, double minX, double maxX, double minY, double maxY) {
    return point.getX() >= minX
        && point.getX() <= maxX
        && point.getY() >= minY
        && point.getY() <= maxY;
  }

  private static boolean segmentIntersectsRotationArc(
      Translation2d segmentStart,
      Translation2d segmentEnd,
      Translation2d sweptPoint,
      double deltaRotationRad) {
    return segmentIntersectsArc(
        segmentStart,
        segmentEnd,
        sweptPoint.getNorm(),
        sweptPoint.getAngle().getRadians(),
        deltaRotationRad);
  }

  private static boolean arcIntersectsRectangle(
      Translation2d arcStart,
      Rotation2d deltaRotation,
      double minX,
      double maxX,
      double minY,
      double maxY) {
    if (isInsideRectangle(arcStart, minX, maxX, minY, maxY)
        || isInsideRectangle(arcStart.rotateBy(deltaRotation), minX, maxX, minY, maxY)) {
      return true;
    }

    var radius = arcStart.getNorm();
    var startAngleRad = arcStart.getAngle().getRadians();
    var deltaRotationRad = deltaRotation.getRadians();
    return segmentIntersectsArc(
            new Translation2d(minX, minY),
            new Translation2d(maxX, minY),
            radius,
            startAngleRad,
            deltaRotationRad)
        || segmentIntersectsArc(
            new Translation2d(maxX, minY),
            new Translation2d(maxX, maxY),
            radius,
            startAngleRad,
            deltaRotationRad)
        || segmentIntersectsArc(
            new Translation2d(maxX, maxY),
            new Translation2d(minX, maxY),
            radius,
            startAngleRad,
            deltaRotationRad)
        || segmentIntersectsArc(
            new Translation2d(minX, maxY),
            new Translation2d(minX, minY),
            radius,
            startAngleRad,
            deltaRotationRad);
  }

  private static boolean segmentIntersectsArc(
      Translation2d segmentStart,
      Translation2d segmentEnd,
      double radius,
      double startAngleRad,
      double deltaRotationRad) {
    if (radius <= GEOMETRY_EPSILON) {
      return isPointOnSegment(Translation2d.kZero, segmentStart, segmentEnd);
    }

    var dx = segmentEnd.getX() - segmentStart.getX();
    var dy = segmentEnd.getY() - segmentStart.getY();
    var a = dx * dx + dy * dy;
    var radiusSquared = radius * radius;
    if (a <= GEOMETRY_EPSILON * GEOMETRY_EPSILON) {
      return Math.abs(segmentStart.getNorm() * segmentStart.getNorm() - radiusSquared)
              <= GEOMETRY_EPSILON
          && isAngleInSweep(segmentStart.getAngle().getRadians(), startAngleRad, deltaRotationRad);
    }

    var b = 2.0 * (segmentStart.getX() * dx + segmentStart.getY() * dy);
    var c =
        segmentStart.getX() * segmentStart.getX()
            + segmentStart.getY() * segmentStart.getY()
            - radiusSquared;
    var discriminant = b * b - 4.0 * a * c;
    if (discriminant < -GEOMETRY_EPSILON) {
      return false;
    }

    var sqrtDiscriminant = Math.sqrt(Math.max(0.0, discriminant));
    return arcRootIsOnSegment(
            (-b - sqrtDiscriminant) / (2.0 * a),
            segmentStart,
            dx,
            dy,
            startAngleRad,
            deltaRotationRad)
        || arcRootIsOnSegment(
            (-b + sqrtDiscriminant) / (2.0 * a),
            segmentStart,
            dx,
            dy,
            startAngleRad,
            deltaRotationRad);
  }

  private static boolean arcRootIsOnSegment(
      double root,
      Translation2d segmentStart,
      double dx,
      double dy,
      double startAngleRad,
      double deltaRotationRad) {
    if (root < -GEOMETRY_EPSILON || root > 1.0 + GEOMETRY_EPSILON) {
      return false;
    }

    var clampedRoot = Math.max(0.0, Math.min(1.0, root));
    return isAngleInSweep(
        Math.atan2(segmentStart.getY() + clampedRoot * dy, segmentStart.getX() + clampedRoot * dx),
        startAngleRad,
        deltaRotationRad);
  }

  private static boolean isAngleInSweep(
      double angleRad, double startAngleRad, double deltaRotationRad) {
    if (Math.abs(deltaRotationRad) <= GEOMETRY_EPSILON) {
      return Math.abs(Math.IEEEremainder(angleRad - startAngleRad, 2.0 * Math.PI))
          <= GEOMETRY_EPSILON;
    }

    var progressRad =
        deltaRotationRad > 0.0
            ? positiveMod(angleRad - startAngleRad, 2.0 * Math.PI)
            : positiveMod(startAngleRad - angleRad, 2.0 * Math.PI);
    return progressRad <= Math.abs(deltaRotationRad) + GEOMETRY_EPSILON;
  }

  private static double positiveMod(double value, double modulus) {
    var result = value % modulus;
    return result < 0.0 ? result + modulus : result;
  }

  private static boolean isPointOnSegment(
      Translation2d point, Translation2d segmentStart, Translation2d segmentEnd) {
    var dx = segmentEnd.getX() - segmentStart.getX();
    var dy = segmentEnd.getY() - segmentStart.getY();
    var lengthSquared = dx * dx + dy * dy;
    if (lengthSquared <= GEOMETRY_EPSILON * GEOMETRY_EPSILON) {
      return point.getDistance(segmentStart) <= GEOMETRY_EPSILON;
    }

    var projection =
        ((point.getX() - segmentStart.getX()) * dx + (point.getY() - segmentStart.getY()) * dy)
            / lengthSquared;
    var clampedProjection = Math.max(0.0, Math.min(1.0, projection));
    var closestPoint =
        new Translation2d(
            segmentStart.getX() + clampedProjection * dx,
            segmentStart.getY() + clampedProjection * dy);
    return point.getDistance(closestPoint) <= GEOMETRY_EPSILON;
  }

  /**
   * Check if a point lies inside a polygon using ray casting algorithm with boundary box
   * optimization
   *
   * @param point The point to check
   * @param polygon Array of polygon vertices
   * @return if the point is inside the polygon
   */
  public static boolean isInPolygon(Translation2d point, Translation2d[] polygon) {
    // Quick rejection using bounding box check
    if (!isInBoundingBox(point, polygon)) {
      return false;
    }

    var inside = false;
    var j = polygon.length - 1;
    var x = point.getX();
    var y = point.getY();

    for (var i = 0; i < polygon.length; i++) {
      double xi = polygon[i].getX();
      double yi = polygon[i].getY();
      double xj = polygon[j].getX();
      double yj = polygon[j].getY();

      // Check if point is exactly on the edge
      if (isOnLine(point, polygon[i], polygon[j])) {
        return true;
      }

      if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
        inside = !inside;
      }
      j = i;
    }

    return inside;
  }

  /** Check if point is within the bounding box of the polygon */
  private static boolean isInBoundingBox(Translation2d point, Translation2d[] polygon) {
    var minX = polygon[0].getX();
    var maxX = minX;
    var minY = polygon[0].getY();
    var maxY = minY;

    // Find bounding box
    for (Translation2d vertex : polygon) {
      minX = Math.min(minX, vertex.getX());
      maxX = Math.max(maxX, vertex.getX());
      minY = Math.min(minY, vertex.getY());
      maxY = Math.max(maxY, vertex.getY());
    }

    // Check if point is within bounding box
    return point.getX() >= minX
        && point.getX() <= maxX
        && point.getY() >= minY
        && point.getY() <= maxY;
  }

  /** Check if point lies on a line segment */
  private static boolean isOnLine(Translation2d point, Translation2d start, Translation2d end) {
    final var EPSILON = 1e-10;

    var lineLength = start.getDistance(end);
    var d1 = point.getDistance(start);
    var d2 = point.getDistance(end);

    return Math.abs(d1 + d2 - lineLength) < EPSILON;
  }
}
