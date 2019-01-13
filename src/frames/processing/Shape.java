/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

// Thanks goes to Sebastian Chaparro, https://github.com/sechaparroc
// for searching and providing an initial working picking example using a back buffer

package frames.processing;

import frames.core.Frame;
import frames.core.Graph;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.core.PVector;

/**
 * A shape is a {@link Frame} specialization that should always be attached to a {@code scene}
 * (see {@link Frame#isAttached(Graph)}) and that can be set from a retained-mode rendering Processing
 * {@code Object} or from an immediate-mode rendering Processing procedure. Either case the shape is
 * split in two a front and a back shape. The front shape will be used for rendering and the back
 * shape for picking with exact precision by default, see {@link #setPrecision(Precision)}. When
 * picking a shape it will be highlighted according to a highlighting policy, see
 * {@link #setHighlighting(Highlighting)}.
 * <p>
 * Picking in an exact manner is costly. If performance is a concern, use a
 * {@link #precision()} different than {@link Precision#EXACT} or a simpler representation
 * for the back shape.
 * <h2>Retained mode</h2>
 * To set a retained-mode shape call {@link #shape(Object)} which will set both the front and
 * the back shape to be the same pshape. Call {@link #frontShape(Object)} and
 * {@link #frontShape(Object)} to set different graphics for rendering and picking, respectively.
 * <h2>Immediate mode</h2>
 * To set an immediate-mode shape override {@link #graphics(PGraphics)} which will set both the
 * front and the back shape to be the same graphics procedure. Override
 * {@link #frontGraphics(PGraphics)} and {@link #frontGraphics(PGraphics)} to set different
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
 * <li>{@link Highlighting#FRONT}: the front-shape (see {@link #frontShape(Object)}
 * and {@link #frontGraphics(PGraphics)}) is scaled by a {@code 1.15} factor.</li>
 * <li>{@link Highlighting#BACK}: the back-shape (see {@link #backShape(Object)} and
 * {@link #backGraphics(PGraphics)}) is displayed instead of the front-shape.</li>
 * <li>{@link Highlighting#FRONT_BACK}: both, the front and the back shapes are
 * displayed. The back shape is made translucent</li>
 * </ol>
 * <p>
 * Default is {@link Highlighting#FRONT}. Set the policy with
 * {@link #setHighlighting(Highlighting)}).
 */
public class Shape extends Frame {
  /**
   * Constructs a scene 'attached' shape with {@link Precision#EXACT} and {@link Highlighting#FRONT} policy.
   */
  public Shape(Scene scene) {
    super(scene);
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
  }

  /**
   * Constructs a scene 'attached' shape with {@link Precision#EXACT} and {@link Highlighting#FRONT} policy.
   * Sets {@code pShape} as its retained mode pshape.
   */
  public Shape(Scene scene, Object pShape) {
    this(scene);
    shape(pShape);
  }

  /**
   * Constructs a shape with {@link Precision#EXACT} and {@link Highlighting#FRONT} policy.
   * Sets {@code reference} as its {@link #reference() frame and {@code pShape} as its retained mode
   * pshape.
   */
  public Shape(Frame reference, Object pShape) {
    this(reference);
    shape(pShape);
  }

  @Override
  public Scene graph() {
    return (Scene) _graph;
  }

  /**
   * Same as {@code draw(graph().frontBuffer())}. Use it instead of {@link Scene#render()}.
   *
   * @see Scene#render()
   * @see #draw(Object)
   */
  public void draw() {
    draw(graph().frontBuffer());
  }

  @Override
  public void draw(Object context) {
    //if(context instanceof PGraphics)
    if (context == graph().backBuffer())
      _drawBackBuffer((PGraphics) context);
    else
      _draw((PGraphics) context);
  }

  protected void _draw(PGraphics pGraphics) {
    pGraphics.pushStyle();
    pGraphics.pushMatrix();
            /*
            if(_frontShape != null)
                pg.shape(_frontShape);
            set(pg);
            frontShape(pg);
            //*/
    ///*
    //TODO needs more thinking
    switch (highlighting()) {
      case FRONT:
        if (isTracked())
          pGraphics.scale(1.15f);
      case NONE:
        if (_frontShape != null)
          pGraphics.shape((PShape) _frontShape);
        else
          graphics(pGraphics);
        break;
      case FRONT_BACK:
        if (_frontShape != null)
          pGraphics.shape((PShape) _frontShape);
        else
          frontGraphics(pGraphics);
        if (isTracked()) {
          if (_backShape != null)
            pGraphics.shape((PShape) _backShape);
          else
            backGraphics(pGraphics);
        }
        break;
      case BACK:
        if (isTracked()) {
          if (_backShape != null)
            pGraphics.shape((PShape) _backShape);
          else
            backGraphics(pGraphics);
        } else {
          if (_frontShape != null)
            pGraphics.shape((PShape) _frontShape);
          else
            frontGraphics(pGraphics);
        }
        break;
    }
    //*/
    pGraphics.popStyle();
    pGraphics.popMatrix();
  }

  protected void _drawBackBuffer(PGraphics pGraphics) {
    if (precision() == Precision.EXACT) {
      pGraphics.pushStyle();
      pGraphics.pushMatrix();

      float r = (float) (_id & 255) / 255.f;
      float g = (float) ((_id >> 8) & 255) / 255.f;
      float b = (float) ((_id >> 16) & 255) / 255.f;

      // funny, only safe way. Otherwise break things horribly when setting shapes
      // and there are more than one shape
      pGraphics.shader(graph()._triangleShader);
      pGraphics.shader(graph()._lineShader, PApplet.LINES);
      pGraphics.shader(graph()._pointShader, PApplet.POINTS);

      graph()._triangleShader.set("id", new PVector(r, g, b));
      graph()._lineShader.set("id", new PVector(r, g, b));
      graph()._pointShader.set("id", new PVector(r, g, b));
      //pGraphics.pushStyle();
      //pGraphics.pushMatrix();
                /*
                if (_backShape != null)
                    pg.shape(_backShape);
                set(pg);
                backShape(pg);
                //*/
      ///*
      if (_frontShape != null)
        pGraphics.shapeMode(graph().frontBuffer().shapeMode);
      if (_backShape != null)
        pGraphics.shape((PShape) _backShape);
      else {
        graphics(pGraphics);
        backGraphics(pGraphics);
      }
      //*/
      pGraphics.popStyle();
      pGraphics.popMatrix();
    }
  }
}
