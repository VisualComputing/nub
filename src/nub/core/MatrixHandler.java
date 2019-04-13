/****************************************************************************************
 * nub
 * Copyright (c) 2019 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package nub.core;

import nub.primitives.Matrix;

/**
 * The matrix handler specifies (and implements) various matrix operations needed by the
 * {@link Graph} to properly perform its geometry transformations.
 * <p>
 * To emit the {@link #transform()} matrix to a shader override the
 * {@link #_setUniforms()} signal, which is fired automatically by the handler every time
 * one of its matrices change state. See also {@link #projection()}, {@link #matrix()}.
 * <p>
 * To bind a {@link Graph} object to a third party renderer (i.e., that renderer provides
 * its own matrix handling: matrix transformations, shader uniforms transfers, etc),
 * override: {@link #_bindMatrix(Matrix)}, {@link #matrix()}, {@link #applyMatrix(Matrix)},
 * {@link #pushMatrix()}, {@link #popMatrix()}, {@link #translate(float, float, float)},
 * {@link #rotate(float)}, {@link #rotate(float, float, float, float)},
 * {@link #scale(float, float, float)}, {@link #projection()}, {@link #_bindProjection(Matrix)},
 * {@link #applyProjection(Matrix)}, {@link #pushProjection()} and {@link #popProjection()} by
 * implementing them in terms of the renderer params (see {@link #_bind(Matrix, Matrix)}).
 *
 * @see Matrix
 * @see #projection()
 * @see #_bindProjection(Matrix)
 * @see #matrix()
 * @see #_bindMatrix(Matrix)
 * @see #_bind(Matrix, Matrix)
 * @see Graph#preDraw()
 */
public class MatrixHandler {
  protected Matrix _projection, _modelview;

  public static int STACK_DEPTH = 32;
  public static String ERROR_PUSHMATRIX_OVERFLOW = "Too many calls to pushMatrix().";
  public static String ERROR_PUSHMATRIX_UNDERFLOW = "Too many calls to popMatrix(), and not enough to pushMatrix().";
  protected float[][] _matrixStack = new float[STACK_DEPTH][16];
  protected int _matrixStackDepth;
  protected float[][] _projectionStack = new float[STACK_DEPTH][16];
  protected int _projectionStackDepth;

  /**
   * Updates (computes and caches) the projection and view matrices from the renderer context
   * {@link Graph#eye()} parameters and call {@link #_setUniforms()}. This method is automatically
   * called by {@link Graph#render()} right at the beginning of the main event loop.
   * <p>
   * If this matrix handler is bound to a third party renderer (i.e., that renderer provides
   * its own matrix matrix handling: matrix transformations, shader uniforms transfers, etc.)
   * this method also binds the projection and view matrices to that renderer.
   * In this case, note that {@link #_bindProjection(Matrix)} and {@link #_bindMatrix(Matrix)}
   * should be overridden, by implementing them in terms of the renderer parameters.
   *
   * @see Graph#render()
   * @see Node#projection(Graph.Type, float, float, float, float, boolean)
   * @see Node#view()
   * @see #_bindProjection(Matrix)
   * @see #_bindMatrix(Matrix)
   */
  protected void _bind(Matrix projection, Matrix view) {
    _bindProjection(projection);
    _bindMatrix(view);
    //_setUniforms();
  }

  // 1. May be overridden

  // bind

  /**
   * Returns {@link #projection()} times {@link #matrix()}.
   *
   * @see #_setUniforms()
   * @see #projection()
   * @see #matrix()
   */
  public Matrix transform() {
    return Matrix.multiply(projection(), matrix());
  }

  /**
   * Emits the {@link #transform()} to the vertex shader whenever the {@link #projection()}
   * or {@link #matrix()} matrices change. Default implementation is empty.
   */
  protected void _setUniforms() {
  }

  /**
   * Binds the projection matrix to the renderer. Only meaningful for raster renderers.
   */
  public void _bindProjection(Matrix matrix) {
    _projection = matrix;
    _setUniforms();
  }

  /**
   * Binds the matrix to the renderer.
   */
  public void _bindMatrix(Matrix matrix) {
    _modelview = matrix;
    _setUniforms();
  }

  /**
   * @return projection matrix
   */
  public Matrix projection() {
    return _projection == null ? Matrix.perspective(1, 1, 1, 100) : _projection;
  }

  /**
   * @return modelview matrix
   */
  public Matrix matrix() {
    return _modelview == null ? new Matrix() : _modelview;
  }

  // matrix operations

  /**
   * Multiplies the current modelview matrix by the one specified through the parameters.
   * Calls {@link #_setUniforms()}.
   */
  public void applyMatrix(Matrix source) {
    _modelview.apply(source);
    _setUniforms();
  }

  /**
   * Multiplies the current projection matrix by the one specified through the parameters.
   * Calls {@link #_setUniforms()}.
   */
  public void applyProjection(Matrix source) {
    _projection.apply(source);
    _setUniforms();
  }

  /**
   * Push a copy of the modelview matrix onto the stack.
   */
  public void pushMatrix() {
    if (_matrixStackDepth == STACK_DEPTH) {
      throw new RuntimeException(ERROR_PUSHMATRIX_OVERFLOW);
    }
    _modelview.get(_matrixStack[_matrixStackDepth]);
    _matrixStackDepth++;
  }

  /**
   * Replace the current modelview matrix with the top of the stack.
   * Calls {@link #_setUniforms()}.
   */
  public void popMatrix() {
    if (_matrixStackDepth == 0) {
      throw new RuntimeException(ERROR_PUSHMATRIX_UNDERFLOW);
    }
    _matrixStackDepth--;
    _modelview.set(_matrixStack[_matrixStackDepth]);
    _setUniforms();
  }

  /**
   * Translate in X, Y, and Z. Calls {@link #_setUniforms()}.
   */
  public void translate(float x, float y, float z) {
    _modelview.translate(x, y, z);
    _setUniforms();
  }

  /**
   * Rotate around the Z axis. Calls {@link #_setUniforms()}.
   */
  public void rotate(float angle) {
    _modelview.rotateZ(angle);
    _setUniforms();
  }

  /**
   * Rotate about a vector in space. Calls {@link #_setUniforms()}.
   */
  public void rotate(float angle, float v0, float v1, float v2) {
    _modelview.rotate(angle, v0, v1, v2);
    _setUniforms();
  }

  /**
   * Scale in X, Y, and Z. Calls {@link #_setUniforms()}.
   */
  public void scale(float sx, float sy, float sz) {
    _modelview.scale(sx, sy, sz);
    _setUniforms();
  }

  /**
   * Push a copy of the projection matrix onto the stack.
   */
  public void pushProjection() {
    if (_projectionStackDepth == STACK_DEPTH) {
      throw new RuntimeException(ERROR_PUSHMATRIX_OVERFLOW);
    }
    _projection.get(_projectionStack[_projectionStackDepth]);
    _projectionStackDepth++;
  }

  /**
   * Replace the current projection matrix with the top of the stack.
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

  // heads up display

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
    pushProjection();
    _bindProjection(Matrix.hudProjection(width, height));
    pushMatrix();
    _bindMatrix(Matrix.hudView(width, height));
  }

  /**
   * Ends Heads Up Display (HUD).
   * <p>
   * Restores the projection and modelview matrices. See {@link #beginHUD(int, int)} for details.
   *
   * @see #beginHUD(int, int)
   */
  public void endHUD() {
    popProjection();
    popMatrix();
  }

  // nub specific transformations

  // TODO docs are missing

  public void applyTransformation(Node node) {
    applyMatrix(node.matrix());
  }

  public void applyWorldTransformation(Node node) {
    applyMatrix(node.worldMatrix());
  }

  // 2. WARNING don't override from here ever!

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
