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
  Graph _graph;
  PGraphics _pg;

  public Java2DMatrixHandler(Graph graph) {
    _graph = graph;
    _pg = (PGraphics) graph.context();
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

  @Override
  public void bind(Matrix projection, Matrix view) {
    _projection = projection;
    _view = view;
    Vector pos = _graph.eye().position();
    Quaternion o = _graph.eye().orientation();
    translate(_graph.width() / 2, _graph.height() / 2);
    scale(1 / _graph.eye().magnitude(), (_graph.isRightHanded() ? -1 : 1) / _graph.eye().magnitude());
    rotate(-o.angle2D());
    translate(-pos.x(), -pos.y());
  }

  @Override
  public void applyMatrix(Matrix matrix) {
    _pg.applyMatrix(Scene.toPMatrix2D(matrix));
  }

  @Override
  public void beginHUD(int width, int height) {
    Vector pos = _graph.eye().position();
    Quaternion o = _graph.eye().orientation();
    pushMatrix();
    translate(pos.x(), pos.y());
    rotate(o.angle2D());
    scale(_graph.eye().magnitude(), _graph.isRightHanded() ? -_graph.eye().magnitude() : _graph.eye().magnitude());
    translate(-width / 2, -height / 2);
  }

  @Override
  public void endHUD() {
    popMatrix();
  }

  @Override
  public void pushMatrix() {
    _pg.pushMatrix();
  }

  @Override
  public void popMatrix() {
    _pg.popMatrix();
  }

  @Override
  public Matrix model() {
    return Scene.toMatrix(new PMatrix2D(_pg.getMatrix()));
  }

  @Override
  public void _bindMatrix(Matrix matrix) {
    _pg.setMatrix(Scene.toPMatrix2D(matrix));
  }

  @Override
  public void translate(float x, float y) {
    _pg.translate(x, y);
  }

  @Override
  public void translate(float x, float y, float z) {
    _pg.translate(x, y, z);
  }

  @Override
  public void rotate(float angle) {
    _pg.rotate(angle);
  }

  @Override
  public void rotate(float angle, float vx, float vy, float vz) {
    _pg.rotate(angle, vx, vy, vz);
  }

  @Override
  public void scale(float sx, float sy) {
    _pg.scale(sx, sy);
  }

  @Override
  public void scale(float sx, float sy, float sz) {
    _pg.scale(sx, sy, sz);
  }
}
