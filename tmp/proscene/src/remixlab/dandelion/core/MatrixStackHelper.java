/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.dandelion.core;

import remixlab.dandelion.geom.Mat;

/**
 * Complete implementation of {@link remixlab.dandelion.core.MatrixHelper} which attaches
 * to it a projection matrix stack and a modelview matrix stack.
 */
public class MatrixStackHelper extends MatrixHelper {
  private static final int MATRIX_STACK_DEPTH = 32;

  private static final String ERROR_PUSHMATRIX_OVERFLOW = "Too many calls to pushModelView().";
  private static final String ERROR_PUSHMATRIX_UNDERFLOW = "Too many calls to popModelView(), and not enough to pushModelView().";

  float[][] matrixStack = new float[MATRIX_STACK_DEPTH][16];
  int matrixStackDepth;

  float[][] pmatrixStack = new float[MATRIX_STACK_DEPTH][16];
  int pmatrixStackDepth;

  Mat projection, modelview;

  public MatrixStackHelper(AbstractScene scn) {
    super(scn);
    modelview = new Mat();
    projection = new Mat();
  }

  @Override
  public void pushModelView() {
    if (matrixStackDepth == MATRIX_STACK_DEPTH) {
      throw new RuntimeException(ERROR_PUSHMATRIX_OVERFLOW);
    }
    modelview.get(matrixStack[matrixStackDepth]);
    matrixStackDepth++;
  }

  @Override
  public void popModelView() {
    if (matrixStackDepth == 0) {
      throw new RuntimeException(ERROR_PUSHMATRIX_UNDERFLOW);
    }
    matrixStackDepth--;
    modelview.set(matrixStack[matrixStackDepth]);
  }

  @Override
  public void resetModelView() {
    modelview.reset();
  }

  @Override
  public Mat modelView() {
    return modelview.get();
  }

  @Override
  public Mat getModelView(Mat target) {
    if (target == null)
      target = new Mat();
    target.set(modelview);
    return target;
  }

  @Override
  public void printModelView() {
    modelview.print();
  }

  @Override
  public void setModelView(Mat source) {
    resetModelView();
    applyModelView(source);
  }

  @Override
  public void applyModelView(Mat source) {
    modelview.apply(source);
  }

  @Override
  public void translate(float tx, float ty) {
    translate(tx, ty, 0);
  }

  @Override
  public void translate(float tx, float ty, float tz) {
    modelview.translate(tx, ty, tz);
  }

  @Override
  public void rotate(float angle) {
    rotateZ(angle);
  }

  @Override
  public void rotateX(float angle) {
    modelview.rotateX(angle);
  }

  @Override
  public void rotateY(float angle) {
    modelview.rotateY(angle);
  }

  @Override
  public void rotateZ(float angle) {
    modelview.rotateZ(angle);
  }

  @Override
  public void rotate(float angle, float v0, float v1, float v2) {
    modelview.rotate(angle, v0, v1, v2);
  }

  @Override
  public void scale(float s) {
    scale(s, s, s);
  }

  @Override
  public void scale(float sx, float sy) {
    scale(sx, sy, 1);
  }

  @Override
  public void scale(float x, float y, float z) {
    modelview.scale(x, y, z);
  }

  @Override
  public void pushProjection() {
    if (pmatrixStackDepth == MATRIX_STACK_DEPTH) {
      throw new RuntimeException(ERROR_PUSHMATRIX_OVERFLOW);
    }
    projection.get(pmatrixStack[pmatrixStackDepth]);
    pmatrixStackDepth++;
  }

  @Override
  public void popProjection() {
    if (pmatrixStackDepth == 0) {
      throw new RuntimeException(ERROR_PUSHMATRIX_UNDERFLOW);
    }
    pmatrixStackDepth--;
    projection.set(pmatrixStack[pmatrixStackDepth]);
  }

  @Override
  public void setProjection(Mat source) {
    resetProjection();
    applyProjection(source);
  }

  @Override
  public void resetProjection() {
    projection.reset();
  }

  @Override
  public void applyProjection(Mat source) {
    projection.apply(source);
  }

  @Override
  public Mat projection() {
    return projection.get();
  }

  @Override
  public Mat getProjection(Mat target) {
    if (target == null)
      target = new Mat();
    target.set(projection);
    return target;
  }

  @Override
  public void printProjection() {
    projection.print();
  }
}
