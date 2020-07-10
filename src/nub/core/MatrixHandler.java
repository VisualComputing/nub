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

package nub.core;

import nub.primitives.Matrix;

/**
 * The matrix handler specifies (and implements) various matrix operations needed by the
 * {@link Graph} to properly perform its geometry transformations.
 * <p>
 * To emit the {@link #transform()} matrix to a shader override the {@link #_setUniforms()} signal,
 * which is fired automatically by the handler every time one of its matrices change state.
 * See also {@link #projection()}, {@link #view()} and {@link #model()}.
 * <p>
 * To bind a {@link Graph} object to a third party renderer (i.e., that renderer provides
 * its own matrix handling: matrix transformations, shader uniforms transfers, etc),
 * refer to the {@link #bind(Matrix, Matrix)} documentation.
 */
public class MatrixHandler {
  protected Matrix _projection, _view, _model;
  public static int STACK_DEPTH = 32;
  public static String ERROR_PUSHMATRIX_OVERFLOW = "Too many calls to pushMatrix().";
  public static String ERROR_PUSHMATRIX_UNDERFLOW = "Too many calls to popMatrix(), and not enough to pushMatrix().";
  protected float[][] _matrixStack = new float[STACK_DEPTH][16];
  protected int _matrixStackDepth;
  protected float[][] _projectionStack = new float[STACK_DEPTH][16];
  protected int _projectionStackDepth;
  protected static int _hudCalls;

  // 1. May be overridden

  // bind

  /**
   * Updates the {@link #projection()}, {@link #view()} and {@link #model()} matrices and call
   * {@link #_setUniforms()}. This method is automatically called by {@link Graph#render()}
   * right at the beginning of the main event loop.
   * <p>
   * If this matrix handler is bound to a third party renderer (i.e., that renderer provides
   * its own matrix matrix handling: matrix transformations, shader uniforms transfers, etc.)
   * override this method together with: {@link #_bindProjection(Matrix)}, {@link #_bindMatrix(Matrix)},
   * {@link #model()}, {@link #modelview()}, {@link #applyMatrix(Matrix)}, {@link #pushMatrix()},
   * {@link #popMatrix()}, {@link #translate(float, float, float)}, {@link #rotate(float)},
   * {@link #rotate(float, float, float, float)}, {@link #scale(float, float, float)},
   * {@link #projection()}, {@link #_bindProjection(Matrix)},
   * {@link #applyProjection(Matrix)}, {@link #pushProjection()} and {@link #popProjection()} by
   * implementing them in terms of that renderer.
   *
   * @see Graph#render()
   * @see Graph#projection(Node, Graph.Type, float, float, float, float)
   * @see Node#view()
   */
  public void bind(Matrix projection, Matrix view) {
    _projection = projection;
    _view = view;
    _model = new Matrix();
    _setUniforms();
  }

  /**
   * Emits the {@link #transform()} to the vertex shader whenever the {@link #projection()}
   * or {@link #model()} matrices change. Default implementation is empty.
   */
  protected void _setUniforms() {
  }

  /**
   * Binds the projection matrix to the renderer. Only meaningful for raster renderers.
   */
  protected void _bindProjection(Matrix matrix) {
    _projection = matrix;
    _setUniforms();
  }

  /**
   * Binds the matrix to the renderer.
   */
  protected void _bindMatrix(Matrix matrix) {
    _model = matrix;
    _setUniforms();
  }

  // matrices

  /**
   * Returns the projection matrix.
   *
   * @see #view()
   * @see #model()
   * @see #modelview()
   * @see #transform()
   */
  public Matrix projection() {
    return _projection == null ? Matrix.perspective(1, 1, 1, 100) : _projection;
  }

  /**
   * Returns the view matrix.
   *
   * @see #projection()
   * @see #model()
   * @see #modelview()
   * @see #transform()
   */
  public Matrix view() {
    return _view == null ? new Matrix() : _view;
  }

  /**
   * Returns the model matrix.
   *
   * @see #projection()
   * @see #view()
   * @see #modelview()
   * @see #transform()
   */
  public Matrix model() {
    return _model == null ? new Matrix() : _model;
  }

  /**
   * Same as {@code return Matrix.multiply(view(), model())}.
   *
   * @see #projection()
   * @see #view()
   * @see #model()
   * @see #transform()
   */
  public Matrix modelview() {
    return Matrix.multiply(view(), model());
  }

  /**
   * Returns {@link #projection()} times {@link #modelview()}.
   *
   * @see #projection()
   * @see #view()
   * @see #model()
   * @see #modelview()
   */
  public Matrix transform() {
    return Matrix.multiply(projection(), modelview());
  }

  // matrix operations

  /**
   * Multiplies the current {@link #model()} matrix by the one specified through the parameters.
   * Calls {@link #_setUniforms()}.
   */
  public void applyMatrix(Matrix source) {
    _model.apply(source);
    _setUniforms();
  }

  /**
   * Multiplies the current {@link #projection()} matrix by the one specified through the parameters.
   * Calls {@link #_setUniforms()}.
   */
  public void applyProjection(Matrix source) {
    _projection.apply(source);
    _setUniforms();
  }

  /**
   * Push a copy of the {@link #model()} matrix onto the stack.
   */
  public void pushMatrix() {
    if (_matrixStackDepth == STACK_DEPTH) {
      throw new RuntimeException(ERROR_PUSHMATRIX_OVERFLOW);
    }
    _model.get(_matrixStack[_matrixStackDepth]);
    _matrixStackDepth++;
  }

  /**
   * Replace the current {@link #model()} matrix with the top of the stack.
   * Calls {@link #_setUniforms()}.
   */
  public void popMatrix() {
    if (_matrixStackDepth == 0) {
      throw new RuntimeException(ERROR_PUSHMATRIX_UNDERFLOW);
    }
    _matrixStackDepth--;
    _model.set(_matrixStack[_matrixStackDepth]);
    _setUniforms();
  }

  /**
   * Translate in X, Y, and Z. Calls {@link #_setUniforms()}.
   */
  public void translate(float x, float y, float z) {
    _model.translate(x, y, z);
    _setUniforms();
  }

  /**
   * Rotate around the Z axis. Calls {@link #_setUniforms()}.
   */
  public void rotate(float angle) {
    _model.rotateZ(angle);
    _setUniforms();
  }

  /**
   * Rotate about a vector in space. Calls {@link #_setUniforms()}.
   */
  public void rotate(float angle, float v0, float v1, float v2) {
    _model.rotate(angle, v0, v1, v2);
    _setUniforms();
  }

  /**
   * Scale in X, Y, and Z. Calls {@link #_setUniforms()}.
   */
  public void scale(float sx, float sy, float sz) {
    _model.scale(sx, sy, sz);
    _setUniforms();
  }

  /**
   * Push a copy of the {@link #projection()} matrix onto the stack.
   */
  public void pushProjection() {
    if (_projectionStackDepth == STACK_DEPTH) {
      throw new RuntimeException(ERROR_PUSHMATRIX_OVERFLOW);
    }
    _projection.get(_projectionStack[_projectionStackDepth]);
    _projectionStackDepth++;
  }

  /**
   * Replace the current {@link #projection()} matrix with the top of the stack.
   * Calls {@link #_setUniforms()}.
   */
  public void popProjection() {
    if (_projectionStackDepth == 0) {
      throw new RuntimeException(ERROR_PUSHMATRIX_UNDERFLOW);
    }
    _projectionStackDepth--;
    _projection.set(_projectionStack[_projectionStackDepth]);
    _setUniforms();
  }

  // 2. WARNING don't override from here ever!

  // HUD

  /**
   * Begins Heads Up Display (HUD).
   * <p>
   * Binds {@code Matrix.hudProjection(width, height)} and {@code Matrix.hudView(width, height)}
   * so that so that drawing can be done using 2D screen coordinates.
   * Calls {@link #_setUniforms()}.
   * <p>
   *
   * @see #endHUD()
   * @see Matrix#hudProjection(int, int)
   * @see Matrix#hudView(int, int)
   */
  public void beginHUD(int width, int height) {
    if (_hudCalls < 0)
      throw new RuntimeException("There should be exactly one beginHUD() call followed by a "
          + "endHUD() and they cannot be nested. Check your implementation!");
    _hudCalls++;
    pushProjection();
    _bindProjection(Matrix.hudProjection(width, height));
    pushMatrix();
    _bindMatrix(Matrix.hudView(width, height));
  }

  /**
   * Ends Heads Up Display (HUD).
   * <p>
   * Restores the {@link #projection()} and {@link #model()} matrices.
   * See {@link #beginHUD(int, int)} for details.
   *
   * @see #beginHUD(int, int)
   */
  public void endHUD() {
    _hudCalls--;
    if (_hudCalls < 0)
      throw new RuntimeException("There should be exactly one beginHUD() call followed by a "
          + "endHUD() and they cannot be nested. Check your implementation!");
    popProjection();
    popMatrix();
  }

  // nub specific transformations

  /**
   * Apply the local transformation defined by {@code node}, i.e., respect to its
   * {@link Node#reference()}. The {@code node} is first translated, then rotated around
   * the new translated origin and then scaled.
   */
  public void applyTransformation(Node node) {
    applyMatrix(node.matrix());
  }

  /**
   * Similar to {@link #applyTransformation(Node)}, but applies the global transformation
   * defined by the {@code node}.
   */
  public void applyWorldTransformation(Node node) {
    applyMatrix(node.worldMatrix());
  }

  /**
   * Translate in X and Y.
   */
  public void translate(float x, float y) {
    translate(x, y, 0);
  }

  /**
   * Scale in X and Y. Equivalent to scale(sx, sy, 1).
   * <p>
   * Not recommended for use in 3D, because the z-dimension is just scaled by 1, since
   * there's no way to know what else to scale it by.
   */
  public void scale(float sx, float sy) {
    scale(sx, sy, 1);
  }
}
