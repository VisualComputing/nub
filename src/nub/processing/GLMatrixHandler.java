/***************************************************************************************
 * nub
 * Copyright (c) 2019-2020 Universidad Nacional de Colombia
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A simple, expressive, language-agnostic, and extensible visual
 * computing library, featuring interaction, visualization and animation frameworks and
 * supporting advanced (onscreen/offscreen) (real/non-real time) rendering techniques.
 * Released under the terms of the GPLv3, refer to: http://www.gnu.org/licenses/gpl.html
 ***************************************************************************************/

package nub.processing;

import nub.core.MatrixHandler;
import nub.core.Node;
import nub.primitives.Matrix;
import processing.core.PMatrix3D;
import processing.opengl.PGraphics3D;
import processing.opengl.PGraphicsOpenGL;

/**
 * Internal {@link MatrixHandler} based on PGraphicsOpenGL graphics transformation.
 */
class GLMatrixHandler extends MatrixHandler {
  PGraphicsOpenGL _pggl;

  public GLMatrixHandler(PGraphicsOpenGL pggl) {
    _pggl = pggl;
  }

  @Override
  public void bind(Matrix projection, Matrix view) {
    _bindProjection(projection);
    _view = view;
    _bindMatrix(view);
  }

  /**
   * Same as {@code beginHUD(_pggl.width, _pggl.height)}.
   */
  public void beginHUD() {
    beginHUD(_pggl.width, _pggl.height);
  }

  @Override
  public void beginHUD(int width, int height) {
    // Otherwise Processing says: "Optimized strokes can only be disabled in 3D"
    if (_pggl.is3D())
      _pggl.hint(_pggl.DISABLE_OPTIMIZED_STROKE);
    Scene.disableDepthTest(_pggl);
    super.beginHUD(width, height);
  }

  @Override
  public void endHUD() {
    super.endHUD();
    Scene.enableDepthTest(_pggl);
    // Otherwise Processing says: "Optimized strokes can only be disabled in 3D"
    if (_pggl.is3D())
      _pggl.hint(_pggl.ENABLE_OPTIMIZED_STROKE);
  }

  @Override
  protected void _bindProjection(Matrix matrix) {
    _pggl.setProjection(Scene.toPMatrix(matrix));
  }

  @Override
  protected void _bindMatrix(Matrix matrix) {
    if (_pggl.is3D())
      _pggl.setMatrix(Scene.toPMatrix(matrix));// in P5 this caches projmodelview
    else {
      _pggl.modelview.set(Scene.toPMatrix(matrix));
      _pggl.projmodelview.set(Matrix.multiply(projection(), matrix).get(new float[16], false));
    }
  }

  @Override
  public Matrix projection() {
    return Scene.toMatrix(_pggl.projection.get());
  }

  @Override
  public Matrix model() {
    return Matrix.multiply(Matrix.inverse(view()), modelview());
  }

  @Override
  public Matrix modelview() {
    return Scene.toMatrix((PMatrix3D) _pggl.getMatrix());
  }

  @Override
  public void applyTransformation(Node node) {
    if (_pggl instanceof PGraphics3D) {
      translate(node.translation()._vector[0], node.translation()._vector[1], node.translation()._vector[2]);
      rotate(node.rotation().angle(), (node.rotation()).axis()._vector[0], (node.rotation()).axis()._vector[1], (node.rotation()).axis()._vector[2]);
      scale(node.scaling(), node.scaling(), node.scaling());
    } else {
      translate(node.translation().x(), node.translation().y());
      rotate(node.rotation().angle2D());
      scale(node.scaling(), node.scaling());
    }
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
  public void pushProjection() {
    _pggl.pushProjection();
  }

  @Override
  public void popProjection() {
    _pggl.popProjection();
  }

  @Override
  public void applyProjection(Matrix matrix) {
    _pggl.applyProjection(Scene.toPMatrix(matrix));
  }

  @Override
  public void pushMatrix() {
    _pggl.pushMatrix();
  }

  @Override
  public void popMatrix() {
    _pggl.popMatrix();
  }

  @Override
  public void applyMatrix(Matrix matrix) {
    _pggl.applyMatrix(Scene.toPMatrix(matrix));
  }

  @Override
  public void translate(float x, float y) {
    _pggl.translate(x, y);
  }

  @Override
  public void translate(float x, float y, float z) {
    _pggl.translate(x, y, z);
  }

  @Override
  public void rotate(float angle) {
    _pggl.rotate(angle);
  }

  @Override
  public void rotate(float angle, float vx, float vy, float vz) {
    _pggl.rotate(angle, vx, vy, vz);
  }

  @Override
  public void scale(float sx, float sy) {
    _pggl.scale(sx, sy);
  }

  @Override
  public void scale(float sx, float sy, float sz) {
    _pggl.scale(sx, sy, sz);
  }
}
