/****************************************************************************************
 * nub
 * Copyright (c) 2019 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package nub.processing;

import nub.core.Graph;
import nub.core.MatrixHandler;
import nub.core.Node;
import nub.primitives.Matrix;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import processing.core.PGraphics;
import processing.core.PMatrix2D;

/**
 * Internal {@link MatrixHandler} based on PGraphicsJava2D graphics transformations.
 */
class Java2DMatrixHandler extends MatrixHandler {
  protected Graph _graph;

  public Java2DMatrixHandler(Graph graph) {
    super(graph.width(), graph.height());
    _graph = graph;
  }

  @Override
  public void applyTransformation(Node node) {
    translate(node.translation().x(), node.translation().y());
    rotate(node.rotation().angle2D());
    scale(node.scaling(), node.scaling());
  }

  @Override
  public void applyWorldTransformation(Node node) {
    Node reference = node.reference();
    if (reference != null) {
      applyWorldTransformation(reference);
      applyTransformation(node);
    } else {
      applyTransformation(node);
    }
  }

  /**
   * Returns the PGraphics object to be bound by this handler.
   */
  public PGraphics pg() {
    return (PGraphics) _graph.context();
  }

  // Comment the above line and uncomment this one to develop the driver:
  // public PGraphicsJava2D context() { return (PGraphicsJava2D) context; }

  @Override
  protected void _bind(Matrix projection, Matrix view) {
    _projection = projection;
    _view = view;
    _cacheProjectionView(Matrix.multiply(cacheProjection(), cacheView()));
    Vector pos = _graph.eye().position();
    Quaternion o = _graph.eye().orientation();
    translate(_graph.width() / 2, _graph.height() / 2);
    scale(1 / _graph.eye().magnitude(), (_graph.isRightHanded() ? -1 : 1) / _graph.eye().magnitude());
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
    scale(_graph.eye().magnitude(), _graph.isRightHanded() ? -_graph.eye().magnitude() : _graph.eye().magnitude());
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
  public void _bindModelView(Matrix matrix) {
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
  public void scale(float sx, float sy, float sz) {
    pg().scale(sx, sy, sz);
  }
}
