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

import processing.core.PMatrix3D;
import processing.opengl.PGraphicsOpenGL;
import proscene.core.Graph;
import proscene.core.MatrixHandler;
import proscene.primitives.Matrix;

/**
 * Internal {@link MatrixHandler} based on PGraphicsOpenGL graphics
 * transformation.
 */
public class GLMatrixHandler extends MatrixHandler {
  PGraphicsOpenGL _pgraphics;

  public GLMatrixHandler(Graph graph, PGraphicsOpenGL renderer) {
    super(graph);
    _pgraphics = renderer;
  }

  public PGraphicsOpenGL pggl() {
    return _pgraphics;
  }

  @Override
  public void pushProjection() {
    pggl().pushProjection();
  }

  @Override
  public void popProjection() {
    pggl().popProjection();
  }

  @Override
  public Matrix projection() {
    return Scene.toMat(pggl().projection.get());
  }

  @Override
  public void bindProjection(Matrix matrix) {
    pggl().setProjection(Scene.toPMatrix(matrix));
  }

  @Override
  public void applyProjection(Matrix matrix) {
    pggl().applyProjection(Scene.toPMatrix(matrix));
  }

  @Override
  public void pushModelView() {
    pggl().pushMatrix();
  }

  @Override
  public void popModelView() {
    pggl().popMatrix();
  }

  @Override
  public Matrix modelView() {
    return Scene.toMat((PMatrix3D) pggl().getMatrix());
  }

  @Override
  public void bindModelView(Matrix matrix) {
    if (_graph.is3D())
      pggl().setMatrix(Scene.toPMatrix(matrix));// in P5 this caches projmodelview
    else {
      pggl().modelview.set(Scene.toPMatrix(matrix));
      pggl().projmodelview.set(Matrix.multiply(projection(), view()).getTransposed(new float[16]));
    }
  }

  @Override
  public void applyModelView(Matrix matrix) {
    pggl().applyMatrix(Scene.toPMatrix(matrix));
  }

  @Override
  public void translate(float x, float y) {
    pggl().translate(x, y);
  }

  @Override
  public void translate(float x, float y, float z) {
    pggl().translate(x, y, z);
  }

  @Override
  public void rotate(float angle) {
    pggl().rotate(angle);
  }

  @Override
  public void rotateX(float angle) {
    pggl().rotateX(angle);
  }

  @Override
  public void rotateY(float angle) {
    pggl().rotateY(angle);
  }

  @Override
  public void rotateZ(float angle) {
    pggl().rotateZ(angle);
  }

  @Override
  public void rotate(float angle, float vx, float vy, float vz) {
    pggl().rotate(angle, vx, vy, vz);
  }

  @Override
  public void scale(float s) {
    pggl().scale(s);
  }

  @Override
  public void scale(float sx, float sy) {
    pggl().scale(sx, sy);
  }

  @Override
  public void scale(float x, float y, float z) {
    pggl().scale(x, y, z);
  }
}
