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

import frames.core.Frame;
import frames.core.Graph;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.core.PVector;
import processing.opengl.PGraphicsOpenGL;

/**
 * A shape is a {@link Frame} specialization that should always be attached to a {@code scene}
 * (see {@link Frame#isAttached(Graph)}) and that can be set from a retained-mode rendering Processing
 * {@code PShape} or from an immediate-mode rendering Processing procedure. Either case the shape is
 * split in two a front and a back shape. The front shape will be used for rendering and the back
 * shape for picking with exact precision by default, see {@link #setPrecision(Precision)}. When
 * picking a shape it will be highlighted according to a highlighting policy, see
 * {@link #setHighlighting(Highlighting)}.
 * <p>
 * Picking in an exact manner is costly. If performance is a concern, use a
 * {@link #precision()} different than {@link Precision#EXACT} or a simpler representation
 * for the back shape.
 * <h2>Retained mode</h2>
 * To set a retained-mode shape call {@link #setGraphics(PShape)} which will set both the front and
 * the back shape to be the same pshape. Call {@link #setFrontGraphics(PShape)} and
 * {@link #setFrontGraphics(PShape)} to set different graphics for rendering and picking, respectively.
 * <h2>Immediate mode</h2>
 * To set an immediate-mode shape override {@link #setGraphics(PGraphics)} which will set both the
 * front and the back shape to be the same graphics procedure. Override
 * {@link #setFrontGraphics(PGraphics)} and {@link #setFrontGraphics(PGraphics)} to set different
 * graphics procedures for rendering and picking, respectively.
 * <h2>Picking</h2>
 * Picking a shape is done according to a precision which can either be:
 * {@link Precision#FIXED}, {@link Precision#ADAPTIVE} or {@link Precision#EXACT}. Refer
 * to the {@link Frame} documentation for both, {@link Precision#FIXED} and
 * {@link Precision#ADAPTIVE}. The default {@link Precision#EXACT} precision use ray-casting
 * of the pointer device over the projected pixels of the back shape. To set a different
 * precision, use {@link #setPrecision(Precision)}. See also {@link #precision()}.
 * <h2>Highlighting</h2>
 * The shape may be highlighted when picking takes place according to a
 * {@link #highlighting()} policy as follows:
 *
 * <ol>
 * <li>{@link Highlighting#NONE}: no highlighting takes place.</li>
 * <li>{@link Highlighting#FRONT}: the front-shape (see {@link #setFrontGraphics(PShape)}
 * and {@link #setFrontGraphics(PGraphics)}) is scaled by a {@code 1.15} factor.</li>
 * <li>{@link Highlighting#BACK}: the back-shape (see {@link #setBackGraphics(PShape)} and
 * {@link #setBackGraphics(PGraphics)}) is displayed instead of the front-shape.</li>
 * <li>{@link Highlighting#FRONT_BACK}: both, the front and the back shapes are
 * displayed. The back shape is made translucent</li>
 * </ol>
 * <p>
 * Default is {@link Highlighting#FRONT}. Set the policy with
 * {@link #setHighlighting(Highlighting)}).
 */
public class Shape extends Frame {
  PShape _frontShape, _backShape;

  public enum Highlighting {
    NONE, FRONT, FRONT_BACK, BACK
  }

  Highlighting _highlight;

  /**
   * Constructs a scene 'attached' shape with {@link Precision#EXACT} and {@link Highlighting#FRONT} policy.
   */
  public Shape(Scene scene) {
    super(scene);
    if (graph().frontBuffer() instanceof PGraphicsOpenGL)
      setPrecision(Precision.EXACT);
    setHighlighting(Highlighting.FRONT);
  }

  /**
   * Constructs a shape with {@link Precision#EXACT} and {@link Highlighting#FRONT} policy.
   * Sets {@code reference} as its {@link #reference()} frame.
   */
  public Shape(Frame reference) {
    super(reference);
    if (reference.isDetached())
      throw new RuntimeException("Shape reference should be attached to an scene");
    if (!(reference.graph() instanceof Scene))
      throw new RuntimeException("Graph reference of the shape should be instance of Scene");
    if (graph().frontBuffer() instanceof PGraphicsOpenGL)
      setPrecision(Precision.EXACT);
    setHighlighting(Highlighting.FRONT);
  }

  /**
   * Constructs a scene 'attached' shape with {@link Precision#EXACT} and {@link Highlighting#FRONT} policy.
   * Sets {@code pShape} as its retained mode pshape.
   */
  public Shape(Scene scene, PShape pShape) {
    this(scene);
    setGraphics(pShape);
  }

  /**
   * Constructs a shape with {@link Precision#EXACT} and {@link Highlighting#FRONT} policy.
   * Sets {@code reference} as its {@link #reference() frame and {@code pShape} as its retained mode
   * pshape.
   */
  public Shape(Frame reference, PShape pShape) {
    this(reference);
    setGraphics(pShape);
  }

  protected Shape(Scene scene, Shape shape) {
    super(scene, shape);
    this._frontShape = shape._frontShape;
    this._backShape = shape._backShape;
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
   * <li>{@link Highlighting#FRONT}: the front-shape (see {@link #setFrontGraphics(PShape)}
   * and {@link #setFrontGraphics(PGraphics)}) is scaled by a {@code 1.15} factor.</li>
   * <li>{@link Highlighting#BACK}: the back-shape (see {@link #setBackGraphics(PShape)} and
   * {@link #setBackGraphics(PGraphics)}) is displayed instead of the front-shape.</li>
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
    this._precision = precision;
    // optimizes the back-buffer
    if (precision() == Precision.EXACT) {
      graph()._bbEnabled = true;
      return;
    }
    for (Frame frame : graph().frames())
      if (frame instanceof Shape)
        if (frame.precision() == Precision.EXACT) {
          graph()._bbEnabled = true;
          return;
        }
    graph()._bbEnabled = false;
  }

  @Override
  public void visit() {
    draw(graph()._targetPGraphics);
  }

  /**
   * Same as {@code draw(graph().frontBuffer())}. Use it instead of {@link Scene#traverse()}.
   *
   * @see Scene#traverse()
   * @see #draw(PGraphics)
   */
  public void draw() {
    draw(graph().frontBuffer());
  }

  /**
   * Draws the shape into the {@code pGraphics}. Use it instead of {@link Scene#traverse(PGraphics)} (which
   * in turn calls it) such as:
   *
   * <pre>
   * {@code
   * void draw() {
   *   pGraphics.pushMatrix();
   *   // apply the local shape transformation before drawing
   *   Scene.applyTransformation(pGraphics, shape);
   *   // or apply the global shape transformation
   *   //Scene.applyWorldTransformation(pGraphics, shape);
   *   shape.draw(pGraphics);
   *   pGraphics.popMatrix();
   * }
   * }
   * </pre>
   *
   * @see Scene#traverse(PGraphics)
   * @see #draw()
   */
  public void draw(PGraphics pGraphics) {
    if (pGraphics != graph().backBuffer()) {
      pGraphics.pushStyle();
      pGraphics.pushMatrix();
            /*
            if(_frontShape != null)
                pg.shape(_frontShape);
            set(pg);
            setFrontGraphics(pg);
            //*/
      ///*
      //TODO needs more thinking
      switch (highlighting()) {
        case FRONT:
          if (isTracked())
            pGraphics.scale(1.15f);
        case NONE:
          if (_frontShape != null)
            pGraphics.shape(_frontShape);
          else
            setGraphics(pGraphics);
          break;
        case FRONT_BACK:
          if (_frontShape != null)
            pGraphics.shape(_frontShape);
          else
            setFrontGraphics(pGraphics);
          if (isTracked()) {
            if (_backShape != null)
              pGraphics.shape(_backShape);
            else
              setBackGraphics(pGraphics);
          }
          break;
        case BACK:
          if (isTracked()) {
            if (_backShape != null)
              pGraphics.shape(_backShape);
            else
              setBackGraphics(pGraphics);
          } else {
            if (_frontShape != null)
              pGraphics.shape(_frontShape);
            else
              setFrontGraphics(pGraphics);
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
                setBackGraphics(pg);
                //*/
        ///*
        if (_frontShape != null)
          pGraphics.shapeMode(graph().frontBuffer().shapeMode);
        if (_backShape != null)
          pGraphics.shape(_backShape);
        else {
          setGraphics(pGraphics);
          setBackGraphics(pGraphics);
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
   * @see #setFrontGraphics(PGraphics)
   * @see #setBackGraphics(PGraphics)
   * @see #setGraphics(PShape)
   */
  protected void setGraphics(PGraphics pGraphics) {
  }

  /**
   * Override this method to set an immediate mode graphics procedure to draw the
   * front shape. Use it in conjunction with @see #setBackGraphics(PGraphics).
   *
   * @see #setGraphics(PGraphics)
   * @see #setGraphics(PShape)
   */
  protected void setFrontGraphics(PGraphics pGraphics) {
  }

  /**
   * Override this method to set an immediate mode graphics procedure to draw the
   * back shape. Use it in conjunction with @see #setFrontGraphics(PGraphics).
   *
   * @see #setGraphics(PGraphics)
   * @see #setGraphics(PShape)
   */
  protected void setBackGraphics(PGraphics pGraphics) {
  }

  /**
   * Sets the retained mode pshape for both, the front and the back shapes.
   *
   * @see #setFrontGraphics(PShape)
   * @see #setBackGraphics(PShape)
   * @see #setGraphics(PGraphics)
   */
  public void setGraphics(PShape shape) {
    setFrontGraphics(shape);
    setBackGraphics(shape);
  }

  /**
   * Sets the retained mode pshape for the front shape. Use it in conjunction
   * with @see #setBackGraphics(PShape)}.
   *
   * @see #setGraphics(PShape)
   * @see #setGraphics(PGraphics)
   */
  public void setFrontGraphics(PShape shape) {
    _frontShape = shape;
  }

  /**
   * Sets the retained mode pshape for the back shape. Use it in conjunction
   * with @see #setFrontGraphics(PShape)}.
   *
   * @see #setGraphics(PShape)
   * @see #setGraphics(PGraphics)
   */
  public void setBackGraphics(PShape shape) {
    _backShape = shape;
  }
}
