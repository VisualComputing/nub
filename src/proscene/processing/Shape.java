/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package proscene.processing;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.core.PVector;
import processing.opengl.PGraphicsOpenGL;
import proscene.core.Node;
import proscene.primitives.Frame;

/**
 * A shape is a {@link Node} specialization that can be set from a retained-mode rendering
 * Processing {@code PShape} or from an immediate-mode rendering Processing procedure. Either
 * case the shape is split in two a front and a back shape. The front shape will be used for
 * rendering and the back shape for picking with exact precision by default, see
 * {@link #setPrecision(Precision)}. When picking a shape it will be highlighted according
 * to a highlighting policy, see {@link #setHighlighting(Highlighting)}.
 * <p>
 * Picking in an exact manner is costly. If performance is a concern, use a
 * {@link #precision()} different than {@link Precision#EXACT} or a simpler representation
 * for the back shape.
 * <h2>Retained mode</h2>
 * To set a retained mode shape call {@link #set(PShape)} which will set both the front and
 * the back shape to be the same pshape. Call {@link #setFront(PShape)} and
 * {@link #setFront(PShape)} to set different pshapes for rendering and picking, respectively.
 * <h2>Immediate mode</h2>
 * To set an immediate mode shape override {@link #set(PGraphics)} which will set both the
 * front and the back shape to be the same graphics procedure. Override
 * {@link #setFront(PGraphics)} and {@link #setFront(PGraphics)} to set different
 * graphics procedures for rendering and picking, respectively.
 * <h2>Picking</h2>
 * Picking a shape is done according to a precision which can either be:
 * {@link Precision#FIXED}, {@link Precision#ADAPTIVE} or {@link Precision#EXACT}. Refer
 * to the {@link Node} documentation for both, {@link Precision#FIXED} and
 * {@link Precision#ADAPTIVE}. The default {@link Precision#EXACT} precision use ray-casting
 * of the pointer device over the projected pixels of the back shape. To set a different
 * precision, use {@link #setPrecision(Precision)}. See also {@link #precision()}.
 * <h2>Highlighting</h2>
 * The shape may be highlighted when picking takes place according to a
 * {@link #highlighting()} policy as follows:
 * <p>
 * <ol>
 * <li>{@link Highlighting#NONE}: no highlighting takes place.</li>
 * <li>{@link Highlighting#FRONT}: the front-shape (see {@link #setFront(PShape)}
 * and {@link #setFront(PGraphics)}) is scaled by a {@code 1.15} factor.</li>
 * <li>{@link Highlighting#BACK}: the back-shape (see {@link #setBack(PShape)} and
 * {@link #setBack(PGraphics)}) is displayed instead of the front-shape.</li>
 * <li>{@link Highlighting#FRONT_BACK}: both, the front and the back shapes are
 * displayed. The back shape is made translucent</li>
 * </ol>
 * <p>
 * Default is {@link Highlighting#FRONT}. Set the policy with
 * {@link #setHighlighting(Highlighting)}).
 */
public class Shape extends Node {
  Scene _scene;
  PShape _frontShape, _backShape;

  public enum Highlighting {
    NONE, FRONT, FRONT_BACK, BACK
  }

  Highlighting _highlight;

  /**
   * Constructs a shape with {@link Precision#EXACT} and {@link Highlighting#FRONT} policy.
   */
  public Shape(Scene scene) {
    super(scene);
    _scene = scene;
    if (_scene.frontBuffer() instanceof PGraphicsOpenGL)
      setPrecision(Precision.EXACT);
    setHighlighting(Highlighting.FRONT);
  }

  /**
   * Constructs a shape with {@link Precision#EXACT} and {@link Highlighting#FRONT} policy.
   * Sets {@code reference} as its {@link #reference() node.
   */
  public Shape(Node reference) {
    super(reference);
    if (reference.graph() instanceof Scene)
      _scene = (Scene) reference.graph();
    else
      throw new RuntimeException("reference graph of the node should ber instance of Scene");
    if (_scene.frontBuffer() instanceof PGraphicsOpenGL)
      setPrecision(Precision.EXACT);
    setHighlighting(Highlighting.FRONT);
  }

  /**
   * Constructs a scene shape with {@link Precision#EXACT} and {@link Highlighting#FRONT} policy.
   * Sets {@code pShape} as its retained mode pshape.
   */
  public Shape(Scene scene, PShape pShape) {
    this(scene);
    set(pShape);
  }

  /**
   * Constructs a shape with {@link Precision#EXACT} and {@link Highlighting#FRONT} policy.
   * Sets {@code reference} as its {@link #reference() node and {@code pShape} as its retained mode
   * pshape.
   */
  public Shape(Node reference, PShape pShape) {
    this(reference);
    set(pShape);
  }

  protected Shape(Scene otherGraph, Shape otherShape) {
    super(otherGraph, otherShape);
    this._frontShape = otherShape._frontShape;
    this._backShape = otherShape._backShape;
  }

  @Override
  public Shape get() {
    return new Shape(scene(), this);
  }

  /**
   * Returns the scene this shape belongs to.
   */
  public Scene scene() {
    return _scene;
  }

  /**
   * Highlights the shape when picking takes place as follows:
   * <p>
   * <ol>
   * <li>{@link Highlighting#NONE}: no highlighting takes place.</li>
   * <li>{@link Highlighting#FRONT}: the front-shape (see {@link #setFront(PShape)}
   * and {@link #setFront(PGraphics)}) is scaled by a {@code 1.15} factor.</li>
   * <li>{@link Highlighting#BACK}: the back-shape (see {@link #setBack(PShape)} and
   * {@link #setBack(PGraphics)}) is displayed instead of the front-shape.</li>
   * <li>{@link Highlighting#FRONT_BACK}: both, the front and the back shapes are
   * displayed. The back shape is made translucent</li>
   * </ol>
   * <p>
   * Default is {@link Highlighting#FRONT}.
   *
   * @see #highlighting()
   */
  public void setHighlighting(Highlighting highlighting) {
    _highlight = highlighting;
  }

  /**
   * Returns the highlighting mode.
   *
   * @see #setHighlighting(Highlighting)
   */
  public Highlighting highlighting() {
    return _highlight;
  }

  @Override
  public void setPrecision(Precision precision) {
    if (precision == Precision.EXACT)
      if (scene()._bb == null) {
        System.out.println("Warning: EXACT picking precision is not enabled by your PGraphics.");
        return;
      }
    this._Precision = precision;
    // enables or disables the grabbing buffer
    if (precision() == Precision.EXACT) {
      scene()._bbEnabled = true;
      return;
    }
    for (Node node : scene().nodes())
      if (node instanceof Shape)
        if (node.precision() == Precision.EXACT) {
          scene()._bbEnabled = true;
          return;
        }
    scene()._bbEnabled = false;
  }

  /**
   * Same as {@code draw(scene.frontBuffer())}.
   * <p>
   * Call it only instead of {@link Scene#traverse()}.
   *
   * @see proscene.processing.Scene#traverse(PGraphics)
   */
  public void draw() {
    draw(scene().frontBuffer());
  }

  /**
   * Draws the shape into {@code pGraphics} using the current point of view (see
   * {@link proscene.processing.Scene#applyTransformation(PGraphics, Frame)}).
   * <p>
   * This method is internally called by {@link Scene#traverse(PGraphics)} to draw
   * the node into the {@link Scene#backBuffer()} and by {@link #draw()} to draw
   * the node into the scene main {@link Scene#frontBuffer()}.
   * <p>
   * Call it only instead of {@link Scene#traverse(PGraphics)}.
   */
  public void draw(PGraphics pGraphics) {
    pGraphics.pushMatrix();
    Scene.applyWorldTransformation(pGraphics, this);
    _visit(pGraphics);
    pGraphics.popMatrix();
  }

  @Override
  public void visit() {
    _visit(scene()._targetPGraphics);
  }

  /**
   * Internal use.
   */
  protected void _visit(PGraphics pg) {
    if (scene().eye() == this)
      return;
    if (pg != scene().backBuffer()) {
      pg.pushStyle();
      pg.pushMatrix();
            /*
            if(_frontShape != null)
                pg.shape(_frontShape);
            set(pg);
            setFront(pg);
            //*/
      ///*
      //TODO needs more thinking
      switch (highlighting()) {
        case FRONT:
          if (grabsInput())
            pg.scale(1.15f);
        case NONE:
          if (_frontShape != null)
            pg.shape(_frontShape);
          else
            set(pg);
          break;
        case FRONT_BACK:
          if (_frontShape != null)
            pg.shape(_frontShape);
          else
            setFront(pg);
          if (grabsInput()) {
            if (_backShape != null)
              pg.shape(_backShape);
            else
              setBack(pg);
          }
          break;
        case BACK:
          if (grabsInput()) {
            if (_backShape != null)
              pg.shape(_backShape);
            else
              setBack(pg);
          } else {
            if (_frontShape != null)
              pg.shape(_frontShape);
            else
              setFront(pg);
          }
          break;
      }
      //*/
      pg.popStyle();
      pg.popMatrix();
    } else {
      if (precision() == Precision.EXACT) {
        float r = (float) (_id & 255) / 255.f;
        float g = (float) ((_id >> 8) & 255) / 255.f;
        float b = (float) ((_id >> 16) & 255) / 255.f;
        // funny, only safe way. Otherwise break things horribly when setting shapes
        // and there are more than one iFrame
        pg.shader(scene()._triangleShader);
        pg.shader(scene()._lineShader, PApplet.LINES);
        pg.shader(scene()._pointShader, PApplet.POINTS);

        scene()._triangleShader.set("id", new PVector(r, g, b));
        scene()._lineShader.set("id", new PVector(r, g, b));
        scene()._pointShader.set("id", new PVector(r, g, b));
        pg.pushStyle();
        pg.pushMatrix();
                /*
                if (_backShape != null)
                    pg.shape(_backShape);
                set(pg);
                setBack(pg);
                //*/
        ///*
        if (_frontShape != null)
          pg.shapeMode(scene().frontBuffer().shapeMode);
        if (_backShape != null)
          pg.shape(_backShape);
        else {
          set(pg);
          setBack(pg);
        }
        //*/
        pg.popStyle();
        pg.popMatrix();
      }
    }
  }

  /**
   * Override this method to set an immediate mode graphics procedure to draw the shape.
   * <p>
   * Sets both the front and the back shape to the same graphics procedure.
   *
   * @see #setFront(PGraphics)
   * @see #setBack(PGraphics)
   * @see #set(PShape)
   */
  protected void set(PGraphics pGraphics) {
  }

  /**
   * Override this method to set an immediate mode graphics procedure to draw the
   * front shape. Use it in conjunction with @see #setBack(PGraphics).
   *
   * @see #set(PGraphics)
   * @see #set(PShape)
   */
  protected void setFront(PGraphics pGraphics) {
  }

  /**
   * Override this method to set an immediate mode graphics procedure to draw the
   * back shape. Use it in conjunction with @see #setFront(PGraphics).
   *
   * @see #set(PGraphics)
   * @see #set(PShape)
   */
  protected void setBack(PGraphics pGraphics) {
  }

  /**
   * Sets the retained mode pshape for both, the front and the back shapes.
   *
   * @see #setFront(PShape)
   * @see #setBack(PShape)
   * @see #set(PGraphics)
   */
  public void set(PShape shape) {
    setFront(shape);
    setBack(shape);
  }

  /**
   * Sets the retained mode pshape for the front shape. Use it in conjunction
   * with @see #setBack(PShape)}.
   *
   * @see #set(PShape)
   * @see #set(PGraphics)
   */
  public void setFront(PShape shape) {
    _frontShape = shape;
  }

  /**
   * Sets the retained mode pshape for the back shape. Use it in conjunction
   * with @see #setFront(PShape)}.
   *
   * @see #set(PShape)
   * @see #set(PGraphics)
   */
  public void setBack(PShape shape) {
    _backShape = shape;
  }

  /**
   * A shape may be picked using
   * <a href="http://schabby.de/picking-opengl-ray-tracing/">'ray-picking'</a> with a
   * color buffer (see {@link proscene.processing.Scene#backBuffer()}). This method
   * compares the color of the {@link proscene.processing.Scene#backBuffer()} at
   * {@code (x,y)} with the shape id. Returns true if both colors are the same, and false
   * otherwise.
   * <p>
   * This method is only meaningful when this shape is not an eye.
   *
   * @see #setPrecision(Precision)
   */
  @Override
  public final boolean track(float x, float y) {
    if (this == scene().eye()) {
      return false;
    }
    if (precision() != Precision.EXACT)
      return super.track(x, y);
    int index = (int) y * scene().width() + (int) x;
    if (scene().backBuffer().pixels != null)
      if ((0 <= index) && (index < scene().backBuffer().pixels.length))
        return scene().backBuffer().pixels[index] == _id();
    return false;
  }
}
