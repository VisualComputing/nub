/**************************************************************************************
 * ProScene (version 3.0.0)
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive scenes
 * in Processing, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.proscene;

import processing.core.PMatrix3D;
import processing.opengl.PGraphicsOpenGL;
import remixlab.geom.MatrixHandler;
import remixlab.primitives.Matrix;

/**
 * Internal {@link MatrixHandler} based on PGraphicsOpenGL graphics
 * transformation.
 */
class GLMatrixHandler extends MatrixHandler {
  PGraphicsOpenGL pg;

  public GLMatrixHandler(Scene scn, PGraphicsOpenGL renderer) {
    super(scn);
    pg = renderer;
  }

  public PGraphicsOpenGL pggl() {
    return pg;
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
  public void bindProjection(Matrix source) {
    pggl().setProjection(Scene.toPMatrix(source));
  }

  @Override
  public void applyProjection(Matrix source) {
    pggl().applyProjection(Scene.toPMatrix(source));
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
  public void bindModelView(Matrix source) {
    if (graph.is3D())
      pggl().setMatrix(Scene.toPMatrix(source));// in P5 this caches projmodelview
    else {
      pggl().modelview.set(Scene.toPMatrix(source));
      pggl().projmodelview.set(Matrix.multiply(projection(), view()).getTransposed(new float[16]));
    }
  }

  @Override
  public void applyModelView(Matrix source) {
    pggl().applyMatrix(Scene.toPMatrix(source));
  }

  @Override
  public void translate(float tx, float ty) {
    pggl().translate(tx, ty);
  }

  @Override
  public void translate(float tx, float ty, float tz) {
    pggl().translate(tx, ty, tz);
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
