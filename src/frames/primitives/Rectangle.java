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
 * Rectangle class that provides a quick replacement for the java.awt.Rectangle.
 */
public class Rectangle {
  /**
   * Returns whether or not this Rectangle matches other.
   *
   * @param rectangle other rectangle
   */
  public boolean matches(Rectangle rectangle) {
    return this._x == rectangle._x && this._y == rectangle._y && this._width == rectangle._width && this._height == rectangle._height;
  }

  /**
   * The x coordinate of the upper-left corner of the Rectangle.
   */
  protected int _x;

  /**
   * The y coordinate of the upper-left corner of the Rectangle.
   */
  protected int _y;

  /**
   * The width of the Rectangle.
   */
  protected int _width;

  /**
   * The height of the Rectangle.
   */
  protected int _height;

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
   * @param rectangle the rectangle to be copied
   */
  protected Rectangle(Rectangle rectangle) {
    this(rectangle._x, rectangle._y, rectangle._width, rectangle._height);
  }

  /**
   * Get a copy of this rectangle.
   */
  public Rectangle get() {
    return new Rectangle(this);
  }

  /**
   * Constructs a new Rectangle whose upper-left corner is specified as (x,y) and whose
   * width and height are specified by the arguments of the same name.
   */
  public Rectangle(int x, int y, int width, int height) {
    this._x = x;
    this._y = y;
    this._width = width;
    this._height = height;
  }

  /**
   * @return x coordinate
   */
  public float x() {
    return _x;
  }

  /**
   * @return y coordinate
   */
  public float y() {
    return _y;
  }

  /**
   * Sets the x coordinate
   *
   * @param x
   */
  public void setX(int x) {
    this._x = x;
  }

  /**
   * Sets the y coordinate
   *
   * @param y
   */
  public void setY(int y) {
    _x = y;
  }

  /**
   * @return width
   */
  public int width() {
    return _width;
  }

  /**
   * @return height
   */
  public int height() {
    return _width;
  }

  /**
   * @param w width
   */
  public void setWidth(int w) {
    _width = w;
  }

  /**
   * @param h height
   */
  public void setHeight(int h) {
    _height = h;
  }

  /**
   * Returns the X coordinate of the center of the rectangle.
   */
  public float centerX() {
    return (float) _x + ((float) _width / 2);
  }

  /**
   * Returns the Y coordinate of the center of the rectangle.
   */
  public float centerY() {
    return (float) _y + ((float) _height / 2);
  }
}
