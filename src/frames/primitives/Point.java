/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.primitives;

/**
 * Point class that provides a quick replacement for the java.awt.Point.
 */
public class Point {
  /**
   * Returns whether or not this Rectangle matches other.
   *
   * @param point other point
   */
  public boolean matches(Point point) {
    return this._x == point._x && this._y == point._y;
  }

  /**
   * The x coordinate of this Point.
   */
  protected int _x;

  /**
   * The y coordinate of this Point.
   */
  protected int _y;

  /**
   * Constructs and initializes a point at the (0,0) location in the coordinate space.
   */
  public Point() {
    this(0, 0);
  }

  /**
   * Copy constructor
   *
   * @param point the point to be copied
   */
  protected Point(Point point) {
    this(point.x(), point.y());
  }

  /**
   * Get a copy of this point.
   */
  public Point get() {
    return new Point(this);
  }

  /**
   * Constructs and initializes a point at the specified (x,y) location in the
   * coordinate space.
   */
  public Point(int x, int y) {
    set(x, y);
  }

  /**
   * Constructs and initializes a point at the specified (xCoord,yCoord) location in the
   * coordinate space. The location (x,y) is given in single float precision.
   */
  public Point(float x, float y) {
    set(x, y);
  }

  /**
   * Sets the (x,y) coordinates of this point from the given (xCoord,yCoord) coordinates.
   */
  public void set(int x, int y) {
    this._x = x;
    this._y = y;
  }

  /**
   * Sets the (x,y) coordinates of this point from the given single float precision
   * (x,y) coordinates.
   */
  public void set(float x, float y) {
    this._x = (int) x;
    this._y = (int) y;
  }

  /**
   * Returns the x coordinate of the point.
   */
  public int x() {
    return _x;
  }

  /**
   * Returns the y coordinate of the point.
   */
  public int y() {
    return _y;
  }

  public void setX(int x) {
    this._x = x;
  }

  public void setY(int y) {
    this._y = y;
  }

  /**
   * Convenience wrapper function that simply returns {@code Point.distance(new
   * Point(x1, y1), new Point(x2, y2))}.
   *
   * @see #distance(Point, Point)
   */
  public static float distance(int x1, int y1, int x2, int y2) {
    return Point.distance((float) x1, (float) y1, (float) x2, (float) y2);
  }

  /**
   * Convenience wrapper function that simply returns {@code Point.distance(new
   * Point(x1, y1), new Point(x2, y2))}.
   *
   * @see #distance(Point, Point)
   */
  public static float distance(float x1, float y1, float x2, float y2) {
    return (float) Math.sqrt((float) Math.pow((x2 - x1), 2.0) + (float) Math.pow((y2 - y1), 2.0));
  }

  /**
   * Returns the Euclidean distance between points point1 and point2.
   */
  public static float distance(Point point1, Point point2) {
    return Point.distance(point1.x(), point1.y(), point2.x(), point2.y());
  }

  public static float distance(float deltaX, float deltaY) {
    return (float) Math.sqrt((float) Math.pow((deltaX), 2.0) + (float) Math.pow((deltaY), 2.0));
  }
}
