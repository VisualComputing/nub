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

import frames.core.Graph;
import frames.core.MatrixHandler;
import frames.primitives.Matrix;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import processing.core.PGraphics;
import processing.core.PMatrix2D;

/**
 * Internal {@link MatrixHandler} based on PGraphicsJava2D graphics transformations.
 */
public class Java2DMatrixHandler extends MatrixHandler {
  protected PGraphics _pgraphics;

  public Java2DMatrixHandler(Graph graph, PGraphics renderer) {
    super(graph);
    _pgraphics = renderer;
  }

  /**
   * Returns the PGraphics object to be bound by this handler.
   */
  public PGraphics pg() {
    return _pgraphics;
  }

  // Comment the above line and uncomment this one to develop the driver:
  // public PGraphicsJava2D frontBuffer() { return (PGraphicsJava2D) frontBuffer; }

  @Override
  protected void _bind() {
    _projection.set(graph().computeProjection());
    _view.set(graph().eye().view());
    _cacheProjectionView(Matrix.multiply(cacheProjection(), cacheView()));
    Vector pos = _graph.eye().position();
    Quaternion o = _graph.eye().orientation();
    translate(_graph.width() / 2, _graph.height() / 2);
    if (_graph.isRightHanded())
      scale(1, -1);
    scale(1 / _graph.eye().magnitude(), 1 / _graph.eye().magnitude());
    rotate(-o.angle2D());
    translate(-pos.x(), -pos.y());
  }

  @Override
  public void applyModelView(Matrix matrix) {
    pg().applyMatrix(Scene.toPMatrix2D(matrix));
  }

  @Override
  public void beginHUD() {
    Vector pos = _graph.eye().position();
    Quaternion o = _graph.eye().orientation();

    pushModelView();
    translate(pos.x(), pos.y());
    rotate(o.angle2D());
    scale(_graph.eye().magnitude(), _graph.eye().magnitude());
    if (_graph.isRightHanded())
      scale(1, -1);
    translate(-_graph.width() / 2, -_graph.height() / 2);
  }

  @Override
  public void endHUD() {
    popModelView();
  }

  @Override
  public void pushModelView() {
    pg().pushMatrix();
  }

  @Override
  public void popModelView() {
    pg().popMatrix();
  }

  @Override
  public Matrix modelView() {
    return Scene.toMatrix(new PMatrix2D(pg().getMatrix()));
  }

  @Override
  public void bindModelView(Matrix matrix) {
    pg().setMatrix(Scene.toPMatrix2D(matrix));
  }

  @Override
  public void translate(float x, float y) {
    pg().translate(x, y);
  }

  @Override
  public void translate(float x, float y, float z) {
    pg().translate(x, y, z);
  }

  @Override
  public void rotate(float angle) {
    pg().rotate(angle);
  }

  @Override
  public void rotate(float angle, float vx, float vy, float vz) {
    pg().rotate(angle, vx, vy, vz);
  }

  @Override
  public void scale(float sx, float sy) {
    pg().scale(sx, sy);
  }

  @Override
  public void scale(float x, float y, float z) {
    pg().scale(x, y, z);
  }
}
