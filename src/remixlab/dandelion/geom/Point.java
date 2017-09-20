/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.dandelion.geom;

import remixlab.util.EqualsBuilder;
import remixlab.util.HashCodeBuilder;

/**
 * Point class that provides a quick replacement for the java.awt.Point.
 */
public class Point {
  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(x).append(y).toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (obj == this)
      return true;
    if (obj.getClass() != getClass())
      return false;

    Point other = (Point) obj;
    return new EqualsBuilder().append(x, other.x).append(y, other.y).isEquals();
  }

  /**
   * The X coordinate of this Point.
   */
  protected int x;

  /**
   * The Y coordinate of this Point.
   */
  protected int y;

  /**
   * Constructs and initializes a point at the (0,0) location in the coordinate space.
   */
  public Point() {
    this(0, 0);
  }

  /**
   * Copy constructor
   *
   * @param p the point to be copied
   */
  public Point(Point p) {
    this(p.x(), p.y());
  }

  /**
   * Constructs and initializes a point at the specified (xCoord,yCoord) location in the
   * coordinate space.
   */
  public Point(int xCoord, int yCoord) {
    set(xCoord, yCoord);
  }

  /**
   * Constructs and initializes a point at the specified (xCoord,yCoord) location in the
   * coordinate space. The location (xCoord,yCoord) is given in single float precision.
   */
  public Point(float xCoord, float yCoord) {
    set(xCoord, yCoord);
  }

  /**
   * Sets the (x,y) coordinates of this point from the given (xCoord,yCoord) coordinates.
   */
  public void set(int xCoord, int yCoord) {
    this.x = xCoord;
    this.y = yCoord;
  }

  /**
   * Sets the (x,y) coordinates of this point from the given single float precision
   * (xCoord,yCoord) coordinates.
   */
  public void set(float xCoord, float yCoord) {
    this.x = (int) xCoord;
    this.y = (int) yCoord;
  }

  /**
   * Returns the x coordinate of the point.
   */
  public int x() {
    return x;
  }

  /**
   * Returns the y coordinate of the point.
   */
  public int y() {
    return y;
  }

  public void setX(int xVal) {
    x = xVal;
  }

  public void setY(int yVal) {
    y = yVal;
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
   * Returns the Euclidean distance between points p1 and p2.
   */
  public static float distance(Point p1, Point p2) {
    return Point.distance(p1.x(), p1.y(), p2.x(), p2.y());
  }

  public static float distance(float deltaX, float deltaY) {
    return (float) Math.sqrt((float) Math.pow((deltaX), 2.0) + (float) Math.pow((deltaY), 2.0));
  }
}
