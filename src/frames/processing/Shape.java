/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.processing;

import frames.core.Node;
import frames.primitives.Frame;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.core.PVector;
import processing.opengl.PGraphicsOpenGL;

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
 *
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
    if (graph().frontBuffer() instanceof PGraphicsOpenGL)
      setPrecision(Precision.EXACT);
    setHighlighting(Highlighting.FRONT);
  }

  /**
   * Constructs a shape with {@link Precision#EXACT} and {@link Highlighting#FRONT} policy.
   * Sets {@code reference} as its {@link #reference()} node.
   */
  public Shape(Node reference) {
    super(reference);
    if (!(reference.graph() instanceof Scene))
      throw new RuntimeException("Graph reference of the shape should be instance of Scene");
    if (graph().frontBuffer() instanceof PGraphicsOpenGL)
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
    return new Shape(graph(), this);
  }

  @Override
  public Scene graph() {
    return (Scene) _graph;
  }

  /**
   * Highlights the shape when picking takes place as follows:
   *
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
      if (graph()._bb == null) {
        System.out.println("Warning: EXACT picking precision is not enabled by your PGraphics.");
        return;
      }
    this._Precision = precision;
    // enables or disables the grabbing buffer
    if (precision() == Precision.EXACT) {
      graph()._bbEnabled = true;
      return;
    }
    for (Node node : graph().nodes())
      if (node instanceof Shape)
        if (node.precision() == Precision.EXACT) {
          graph()._bbEnabled = true;
          return;
        }
    graph()._bbEnabled = false;
  }

  /**
   * Same as {@code draw(scene.frontBuffer())}.
   * <p>
   * Call it only instead of {@link Scene#traverse()}.
   *
   * @see frames.processing.Scene#traverse(PGraphics)
   */
  public void draw() {
    draw(graph().frontBuffer());
  }

  /**
   * Draws the shape into {@code pGraphics} using the current point of view (see
   * {@link frames.processing.Scene#applyTransformation(PGraphics, Frame)}).
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
    _visit(graph()._targetPGraphics);
  }

  /**
   * Internal use.
   */
  protected void _visit(PGraphics pGraphics) {
    if (pGraphics != graph().backBuffer()) {
      pGraphics.pushStyle();
      pGraphics.pushMatrix();
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
            pGraphics.scale(1.15f);
        case NONE:
          if (_frontShape != null)
            pGraphics.shape(_frontShape);
          else
            set(pGraphics);
          break;
        case FRONT_BACK:
          if (_frontShape != null)
            pGraphics.shape(_frontShape);
          else
            setFront(pGraphics);
          if (grabsInput()) {
            if (_backShape != null)
              pGraphics.shape(_backShape);
            else
              setBack(pGraphics);
          }
          break;
        case BACK:
          if (grabsInput()) {
            if (_backShape != null)
              pGraphics.shape(_backShape);
            else
              setBack(pGraphics);
          } else {
            if (_frontShape != null)
              pGraphics.shape(_frontShape);
            else
              setFront(pGraphics);
          }
          break;
      }
      //*/
      pGraphics.popStyle();
      pGraphics.popMatrix();
    } else {
      if (precision() == Precision.EXACT) {
        float r = (float) (_id & 255) / 255.f;
        float g = (float) ((_id >> 8) & 255) / 255.f;
        float b = (float) ((_id >> 16) & 255) / 255.f;
        // funny, only safe way. Otherwise break things horribly when setting shapes
        // and there are more than one iFrame
        pGraphics.shader(graph()._triangleShader);
        pGraphics.shader(graph()._lineShader, PApplet.LINES);
        pGraphics.shader(graph()._pointShader, PApplet.POINTS);

        graph()._triangleShader.set("id", new PVector(r, g, b));
        graph()._lineShader.set("id", new PVector(r, g, b));
        graph()._pointShader.set("id", new PVector(r, g, b));
        pGraphics.pushStyle();
        pGraphics.pushMatrix();
                /*
                if (_backShape != null)
                    pg.shape(_backShape);
                set(pg);
                setBack(pg);
                //*/
        ///*
        if (_frontShape != null)
          pGraphics.shapeMode(graph().frontBuffer().shapeMode);
        if (_backShape != null)
          pGraphics.shape(_backShape);
        else {
          set(pGraphics);
          setBack(pGraphics);
        }
        //*/
        pGraphics.popStyle();
        pGraphics.popMatrix();
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
   * color buffer (see {@link frames.processing.Scene#backBuffer()}). This method
   * compares the color of the {@link frames.processing.Scene#backBuffer()} at
   * {@code (x,y)} with the shape id. Returns true if both colors are the same, and false
   * otherwise.
   * <p>
   * This method is only meaningful when this shape is not an eye.
   *
   * @see #setPrecision(Precision)
   */
  @Override
  public final boolean track(float x, float y) {
    if (this == graph().eye()) {
      return false;
    }
    if (precision() != Precision.EXACT)
      return super.track(x, y);
    int index = (int) y * graph().width() + (int) x;
    if (graph().backBuffer().pixels != null)
      if ((0 <= index) && (index < graph().backBuffer().pixels.length))
        return graph().backBuffer().pixels[index] == _id();
    return false;
  }
}
