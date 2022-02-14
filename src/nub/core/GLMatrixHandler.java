/***************************************************************************************
 * nub
 * Copyright (c) 2019-2021 Universidad Nacional de Colombia
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A simple, expressive, language-agnostic, and extensible visual
 * computing library, featuring interaction, visualization and animation frameworks and
 * supporting advanced (onscreen/offscreen) (real/non-real time) rendering techniques.
 * Released under the terms of the GPLv3, refer to: http://www.gnu.org/licenses/gpl.html
 ***************************************************************************************/

package nub.core;

import nub.primitives.Matrix;
import processing.core.PMatrix3D;
import processing.opengl.PGraphics3D;
import processing.opengl.PGraphicsOpenGL;

/**
 * Internal {@link MatrixHandler} based on PGraphicsOpenGL graphics transformation.
 */
class GLMatrixHandler extends MatrixHandler {
  PGraphicsOpenGL _pggl;
  protected float[][] _matrixStackInv = new float[STACK_DEPTH][16];

  public GLMatrixHandler(PGraphicsOpenGL pggl) {
    _pggl = pggl;
  }

  @Override
  public void bind(Matrix projection, Matrix view) {
    _bindProjection(projection);
    _view = view;
    _bindMatrix(view);
  }

  @Override
  public Node eye() {
    Node node = new Node(false);
    node.fromWorldMatrix(Matrix.inverse(Scene.toMatrix(_pggl.modelview)));
    return node;
  }

  @Override
  public float left() {
    if (_pggl.projection.m33 != 1) {
      throw new RuntimeException("Error: left only works for an orthographic projection");
    }
    return -(1 + _pggl.projection.m03 ) / _pggl.projection.m00;
  }

  @Override
  public float right() {
    if (_pggl.projection.m33 != 1) {
      throw new RuntimeException("Error: right only works for an orthographic projection");
    }
    return (1 - _pggl.projection.m03) / _pggl.projection.m00;
  }

  @Override
  public float top() {
    if (_pggl.projection.m33 != 1) {
      throw new RuntimeException("Error: top only works for an orthographic projection");
    }
    return -(1 + _pggl.projection.m13) / _pggl.projection.m11;
  }

  @Override
  public float bottom() {
    if (_pggl.projection.m33 != 1) {
      throw new RuntimeException("Error: bottom only works for an orthographic projection");
    }
    return (1 - _pggl.projection.m13) / _pggl.projection.m11;
  }

  @Override
  public float fov() {
    if (_pggl.projection.m33 != 0) {
      throw new RuntimeException("Error: fov only works for a perspective projection");
    }
    return Math.abs(2 * (float) Math.atan(1 / _pggl.projection.m11));
  }

  @Override
  public float hfov() {
    if (_pggl.projection.m33 != 0) {
      throw new RuntimeException("Error: hfov only works for a perspective projection");
    }
    return Math.abs(2 * (float) Math.atan(1 / _pggl.projection.m00));
  }

  @Override
  public float near() {
    return _pggl.projection.m33 == 0 ? _pggl.projection.m23 / (_pggl.projection.m22 - 1) : (1 + _pggl.projection.m23) / _pggl.projection.m22;
  }

  @Override
  public float far() {
    return _pggl.projection.m33 == 0 ? _pggl.projection.m23 / (_pggl.projection.m22 + 1) : - (1 - _pggl.projection.m23) / _pggl.projection.m22;
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
      translate(node.position()._vector[0], node.position()._vector[1], node.position()._vector[2]);
      rotate(node.orientation().angle(), (node.orientation()).axis()._vector[0], (node.orientation()).axis()._vector[1], (node.orientation()).axis()._vector[2]);
      scale(node.magnitude(), node.magnitude(), node.magnitude());
    } else {
      translate(node.position().x(), node.position().y());
      rotate(node.orientation().angle2D());
      scale(node.magnitude(), node.magnitude());
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
    if (_matrixStackDepth == STACK_DEPTH) {
      throw new RuntimeException(ERROR_PUSHMATRIX_OVERFLOW);
    }
    _pggl.modelview.get(_matrixStack[_matrixStackDepth]);
    _pggl.modelviewInv.get(_matrixStackInv[_matrixStackDepth]);
    _matrixStackDepth++;
  }


  @Override
  public void popMatrix() {
    if (_matrixStackDepth == 0) {
      throw new RuntimeException(ERROR_PUSHMATRIX_UNDERFLOW);
    }
    _matrixStackDepth--;
    _pggl.modelview.set(_matrixStack[_matrixStackDepth]);
    _pggl.modelviewInv.set(_matrixStackInv[_matrixStackDepth]);
    _pggl.updateProjmodelview();
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
