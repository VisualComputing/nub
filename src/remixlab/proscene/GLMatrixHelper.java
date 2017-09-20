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
import remixlab.dandelion.core.MatrixHelper;
import remixlab.dandelion.geom.Mat;

/**
 * Internal {@link remixlab.dandelion.core.MatrixHelper} based on PGraphicsOpenGL graphics
 * transformation.
 */
class GLMatrixHelper extends MatrixHelper {
  PGraphicsOpenGL pg;

  public GLMatrixHelper(Scene scn, PGraphicsOpenGL renderer) {
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
  public void resetProjection() {
    pggl().resetProjection();
  }

  @Override
  public void printProjection() {
    pggl().printProjection();
  }

  @Override
  public Mat projection() {
    return Scene.toMat(pggl().projection.get());
  }

  @Override
  public Mat getProjection(Mat target) {
    if (target == null)
      target = Scene.toMat(pggl().projection.get()).get();
    else
      target.set(Scene.toMat(pggl().projection.get()));
    return target;
  }

  @Override
  public void applyProjection(Mat source) {
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
  public void resetModelView() {
    pggl().resetMatrix();
  }

  @Override
  public Mat modelView() {
    return Scene.toMat((PMatrix3D) pggl().getMatrix());
  }

  @Override
  public Mat getModelView(Mat target) {
    if (target == null)
      target = Scene.toMat((PMatrix3D) pggl().getMatrix()).get();
    else
      target.set(Scene.toMat((PMatrix3D) pggl().getMatrix()));
    return target;
  }

  @Override
  public void printModelView() {
    pggl().printMatrix();
  }

  @Override
  public void applyModelView(Mat source) {
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

  @Override
  public void setProjection(Mat source) {
    pggl().setProjection(Scene.toPMatrix(source));
  }

  @Override
  public void setModelView(Mat source) {
    if (gScene.is3D())
      pggl().setMatrix(Scene.toPMatrix(source));// in P5 this caches projmodelview
    else {
      pggl().modelview.set(Scene.toPMatrix(source));
      pggl().projmodelview
          .set(Mat.multiply(gScene.eye().getProjection(false), gScene.eye().getView(false)).getTransposed(new float[16]));
    }
  }
}
