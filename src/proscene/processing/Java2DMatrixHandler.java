/**************************************************************************************
 * ProScene (version 3.0.0)
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive scenes
 * in Processing, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package proscene.processing;

import processing.core.PGraphics;
import processing.core.PMatrix2D;
import proscene.core.Graph;
import proscene.core.MatrixHandler;
import proscene.primitives.Matrix;
import proscene.primitives.Quaternion;
import proscene.primitives.Vector;

/**
 * Internal {@link MatrixHandler} based on PGraphicsJava2D graphics
 * transformations.
 */
public class Java2DMatrixHandler extends MatrixHandler {
  protected PGraphics _pgraphics;

  public Java2DMatrixHandler(Graph graph, PGraphics renderer) {
    super(graph);
    _pgraphics = renderer;
  }

  public PGraphics pg() {
    return _pgraphics;
  }

  // Comment the above line and uncomment this one to develop the driver:
  // public PGraphicsJava2D frontBuffer() { return (PGraphicsJava2D) frontBuffer; }

  @Override
  public void bind() {
    _cacheProjection(graph().computeProjection());
    _cacheView(graph().computeView());
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
  public void beginScreenCoordinates() {
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
  public void endScreenCoordinates() {
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
    return Scene.toMat(new PMatrix2D(pg().getMatrix()));
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
  public void rotateX(float angle) {
    pg().rotateX(angle);
  }

  @Override
  public void rotateY(float angle) {
    pg().rotateY(angle);
  }

  @Override
  public void rotateZ(float angle) {
    pg().rotateZ(angle);
  }

  @Override
  public void rotate(float angle, float vx, float vy, float vz) {
    pg().rotate(angle, vx, vy, vz);
  }

  @Override
  public void scale(float s) {
    pg().scale(s);
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
