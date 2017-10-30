/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.primitives;

/**
 * Rectangle class that provides a quick replacement for the java.awt.Rectangle.
 */
public class Rectangle {
  /**
   * Returns whether or not this Rectangle matches other.
   *
   * @param other rect
   */
  public boolean matches(Rectangle other) {
    return this.x == other.x && this.y == other.y && this.width == other.width && this.height == other.height;
  }

  /**
   * The X coordinate of the upper-left corner of the Rectangle.
   */
  protected int x;

  /**
   * The Y coordinate of the upper-left corner of the Rectangle.
   */
  protected int y;

  /**
   * The width of the Rectangle.
   */
  protected int width;

  /**
   * The height of the Rectangle.
   */
  protected int height;

  /**
   * Constructs a new Rectangle whose upper-left corner is at (0, 0) in the coordinate
   * space, and whose width and height are both zero.
   */
  public Rectangle() {
    this(0, 0, 0, 0);
  }

  /**
   * Copy constructor
   *
   * @param r the rectangle to be copied
   */
  public Rectangle(Rectangle r) {
    this(r.x, r.y, r.width, r.height);
  }

  /**
   * Constructs a new Rectangle whose upper-left corner is specified as (x,y) and whose
   * width and height are specified by the arguments of the same name.
   */
  public Rectangle(int x, int y, int width, int height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  /**
   * @return x coordinate
   */
  public float x() {
    return x;
  }

  /**
   * @return y coordinate
   */
  public float y() {
    return y;
  }

  /**
   * Sets the x coordinate
   *
   * @param xVal
   */
  public void setX(int xVal) {
    x = xVal;
  }

  /**
   * Sets the y coordinate
   *
   * @param yVal
   */
  public void setY(int yVal) {
    x = yVal;
  }

  /**
   * @return width
   */
  public int width() {
    return width;
  }

  /**
   * @return height
   */
  public int height() {
    return width;
  }

  /**
   * @param w width
   */
  public void setWidth(int w) {
    width = w;
  }

  /**
   * @param h height
   */
  public void setHeight(int h) {
    height = h;
  }

  /**
   * Returns the X coordinate of the center of the rectangle.
   */
  public float centerX() {
    return (float) x + ((float) width / 2);
  }

  /**
   * Returns the Y coordinate of the center of the rectangle.
   */
  public float centerY() {
    return (float) y + ((float) height / 2);
  }
}
