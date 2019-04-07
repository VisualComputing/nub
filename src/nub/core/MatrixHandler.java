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
import nub.primitives.Vector;

/**
 * The matrix handler specifies (and implements) various matrix operations needed by the
 * {@link Graph} to properly perform its geometry transformations.
 * <p>
 * To emit the {@link #projectionModelView()} matrix to a shader override the
 * {@link #_setUniforms()} signal, which is fired automatically by the handler every time
 * one of its matrices change state. See also {@link #projection()}, {@link #modelView()}.
 * <p>
 * To bind a {@link Graph} object to a third party renderer (i.e., that renderer provides
 * its own matrix handling: matrix transformations, shader uniforms transfers, etc),
 * override: {@link #_bindModelView(Matrix)}, {@link #modelView()}, {@link #applyModelView(Matrix)},
 * {@link #pushModelView()}, {@link #popModelView()}, {@link #translate(float, float, float)},
 * {@link #rotate(float)}, {@link #rotate(float, float, float, float)},
 * {@link #scale(float, float, float)}, {@link #projection()}, {@link #_bindProjection(Matrix)},
 * {@link #applyProjection(Matrix)}, {@link #pushProjection()} and {@link #popProjection()} by
 * implementing them in terms of the renderer params (see {@link #_bind(Matrix, Matrix)}).
 *
 * @see Matrix
 * @see #projection()
 * @see #_bindProjection(Matrix)
 * @see #modelView()
 * @see #_bindModelView(Matrix)
 * @see #_bind(Matrix, Matrix)
 * @see Graph#preDraw()
 */
public class MatrixHandler {
  protected int _width, _height;
  protected Matrix _projection, _view, _modelview;
  protected Matrix _projectionView, _projectionViewInverse;
  protected boolean _isProjectionViewInverseCached, _projectionViewHasInverse;

  public static int STACK_DEPTH = 32;
  public static String ERROR_PUSHMATRIX_OVERFLOW = "Too many calls to pushModelView().";
  public static String ERROR_PUSHMATRIX_UNDERFLOW = "Too many calls to popModelView(), and not enough to pushModelView().";
  protected float[][] _modelviewStack = new float[STACK_DEPTH][16];
  protected int _modelviewStackDepth;
  protected float[][] _projectionStack = new float[STACK_DEPTH][16];
  protected int _projectionStackDepth;

  /**
   * Instantiates matrices and sets {@link #isProjectionViewInverseCached()} to {@code false}.
   *
   * @param width of the renderer context
   * @param height of the renderer context
   */
  public MatrixHandler(int width, int height) {
    _width = width;
    _height = height;
    _projection = new Matrix();
    _view = new Matrix();
    _modelview = new Matrix();
    _projectionView = new Matrix();
    _isProjectionViewInverseCached = false;
  }

  public void applyTransformation(Node node) {
    applyModelView(node.matrix());
  }

  public void applyWorldTransformation(Node node) {
    applyModelView(node.worldMatrix());
  }

  /**
   * @return width of the screen window.
   */
  public int width() {
    return _width;
  }

  /**
   * @return height of the screen window.
   */
  public int height() {
    return _height;
  }

  /**
   * Sets eye {@link #width()} and {@link #height()} (expressed in pixels).
   * <p>
   * Non-positive dimension are silently replaced by a 1 pixel value to ensure boundary
   * coherence.
   */
  public void setWidth(int width) {
    // Prevent negative and zero dimensions that would cause divisions by zero.
    _width = width > 0 ? width : 1;
  }

  public void setHeight(int height) {
    // Prevent negative and zero dimensions that would cause divisions by zero.
    _height = height > 0 ? height : 1;
  }

  /**
   * Updates (computes and caches) the projection and view matrices from the renderer context
   * {@link Graph#eye()} parameters and call {@link #_setUniforms()}. This method is automatically
   * called by {@link Graph#render()} right at the beginning of the main event loop.
   * <p>
   * If this matrix handler is bound to a third party renderer (i.e., that renderer provides
   * its own matrix matrix handling: matrix transformations, shader uniforms transfers, etc.)
   * this method also binds the projection and view matrices to that renderer.
   * In this case, note that {@link #_bindProjection(Matrix)} and {@link #_bindModelView(Matrix)}
   * should be overridden, by implementing them in terms of the renderer parameters.
   *
   * @see Graph#render()
   * @see Node#projection(Graph.Type, float, float, float, float, boolean)
   * @see Node#view()
   * @see #_bindProjection(Matrix)
   * @see #_bindModelView(Matrix)
   */
  protected void _bind(Matrix projection, Matrix view) {
    //_projection.set(projection);
    //_view.set(view);
    //TODO experimental
    _projection = projection;
    _view = view;
    _cacheProjectionView(Matrix.multiply(cacheProjection(), cacheView()));
    // TODO _bindProjection is redundant when there's no binding of the matrices
    // We could go like this (but I don't know if it works in JS):
    //if(graph().getClass() != Graph.class)
    _bindProjection(cacheProjection());
    _bindModelView(cacheView());
    _setUniforms();
  }

  // 1. May be overridden

  /**
   * Returns {@link #projection()} times {@link #modelView()}.
   *
   * @see #_setUniforms()
   * @see #projection()
   * @see #modelView()
   */
  public Matrix projectionModelView() {
    return Matrix.multiply(projection(), modelView());
  }

  /**
   * Emits the {@link #projectionModelView()} to the vertex shader whenever the {@link #projection()}
   * or {@link #modelView()} matrices change. Default implementation is empty.
   */
  protected void _setUniforms() {
  }

  /**
   * Binds the projection matrix to the renderer. Only meaningful for raster renderers.
   */
  public void _bindProjection(Matrix matrix) {
    _projection.set(matrix);
  }

  /**
   * Binds the modelview matrix to the renderer.
   */
  public void _bindModelView(Matrix matrix) {
    _modelview.set(matrix);
  }

  /**
   * @return projection matrix
   */
  public Matrix projection() {
    return cacheProjection();
  }

  /**
   * @return modelview matrix
   */
  public Matrix modelView() {
    return _modelview;
  }

  // matrix operations

  /**
   * Multiplies the current modelview matrix by the one specified through the parameters.
   * Calls {@link #_setUniforms()}.
   */
  public void applyModelView(Matrix source) {
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
  public void pushModelView() {
    if (_modelviewStackDepth == STACK_DEPTH) {
      throw new RuntimeException(ERROR_PUSHMATRIX_OVERFLOW);
    }
    _modelview.get(_modelviewStack[_modelviewStackDepth]);
    _modelviewStackDepth++;
  }

  /**
   * Replace the current modelview matrix with the top of the stack.
   * Calls {@link #_setUniforms()}.
   */
  public void popModelView() {
    if (_modelviewStackDepth == 0) {
      throw new RuntimeException(ERROR_PUSHMATRIX_UNDERFLOW);
    }
    _modelviewStackDepth--;
    _modelview.set(_modelviewStack[_modelviewStackDepth]);
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

  // 2. WARNING don't override from here ever!

  // 2a macros

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

  // 2b caches

  /**
   * Returns the cached projection matrix.
   */
  public Matrix cacheProjection() {
    return _projection;
  }

  /**
   * Returns the cached view matrix.
   */
  public Matrix cacheView() {
    return _view;
  }

  /**
   * Returns the cached projection * view matrix.
   */
  public Matrix cacheProjectionView() {
    return _projectionView;
  }

  /**
   * Returns the cached projection times view inverse matrix.
   */
  public Matrix cacheProjectionViewInverse() {
    if (!isProjectionViewInverseCached())
      throw new RuntimeException("optimizeUnprojectCache(true) should be called first");
    return _projectionViewInverse;
  }

  // cache setters for projection times view and its inverse

  /**
   * Returns {@code true} if the projection * view matrix and its inverse are being cached, and
   * {@code false} otherwise.
   *
   * @see #cacheProjectionView()
   * @see #cacheProjectionViewInverse(boolean)
   */
  public boolean isProjectionViewInverseCached() {
    return _isProjectionViewInverseCached;
  }

  /**
   * Caches the projection * view matrix.
   *
   * @see #isProjectionViewInverseCached()
   */
  protected void _cacheProjectionView(Matrix matrix) {
    _projectionView.set(matrix);
    if (isProjectionViewInverseCached()) {
      if (_projectionViewInverse == null)
        _projectionViewInverse = new Matrix();
      _projectionViewHasInverse = _projectionView.invert(_projectionViewInverse);
    }
  }

  /**
   * Cache projection * view inverse matrix (and also projection * view) so that
   * {@link Graph#location(Vector)} is optimized.
   *
   * @see #isProjectionViewInverseCached()
   * @see #cacheProjectionView()
   */
  public void cacheProjectionViewInverse(boolean optimise) {
    _isProjectionViewInverseCached = optimise;
  }

  // 2c screen drawing

  /**
   * Begin Heads Up Display (HUD) so that drawing can be done using 2D screen coordinates.
   * Calls {@link #_setUniforms()}.
   * <p>
   * All screen drawing should be enclosed between {@link #beginHUD()} and
   * {@link #endHUD()}. Then you can just begin drawing your screen shapes.
   * <b>Attention:</b> If you want your screen drawing to appear on top of your 3d graph
   * then draw first all your 3d before doing any call to a {@link #beginHUD()}
   * and {@link #endHUD()} pair.
   *
   * @see #endHUD()
   */
  public void beginHUD() {
    pushProjection();
    _ortho2D();
    pushModelView();
    _resetViewPoint();
    // TODO needs testing
    _setUniforms();
  }

  /**
   * Ends Heads Up Display (HUD). See {@link #beginHUD()} for details.
   *
   * @see #beginHUD()
   */
  public void endHUD() {
    popProjection();
    popModelView();
  }

  // see:
  // http://www.opengl.org/archives/resources/faq/technical/transformations.htm
  // "9.030 How do I draw 2D controls over my 3D rendering?"
  protected void _ortho2D() {
    float cameraZ = (_height / 2.0f) / (float) Math.tan((float) Math.PI / 8);
    float cameraNear = cameraZ / 2.0f;
    float cameraFar = cameraZ * 2.0f;

    float left = -_width / 2;
    float right = _width / 2;
    float bottom = -_height / 2;
    float top = _height / 2;
    float near = cameraNear;
    float far = cameraFar;

    float x = +2.0f / (right - left);
    float y = +2.0f / (top - bottom);
    float z = -2.0f / (far - near);

    float tx = -(right + left) / (right - left);
    float ty = -(top + bottom) / (top - bottom);
    float tz = -(far + near) / (far - near);

    // The minus sign is needed to invert the Y axis.
    _bindProjection(new Matrix(x, 0, 0, 0, 0, -y, 0, 0, 0, 0, z, 0, tx, ty, tz, 1));
  }

  // as it's done in P5:
  protected void _resetViewPoint() {
    float eyeX = _width / 2f;
    float eyeY = _height / 2f;
    float eyeZ = (_height / 2f) / (float) Math.tan((float) Math.PI * 60 / 360);
    float centerX = _width / 2f;
    float centerY = _height / 2f;
    float centerZ = 0;
    float upX = 0;
    float upY = 1;
    float upZ = 0;

    // Calculating Z vector
    float z0 = eyeX - centerX;
    float z1 = eyeY - centerY;
    float z2 = eyeZ - centerZ;
    float mag = (float) Math.sqrt(z0 * z0 + z1 * z1 + z2 * z2);
    if (mag != 0) {
      z0 /= mag;
      z1 /= mag;
      z2 /= mag;
    }

    // Calculating Y vector
    float y0 = upX;
    float y1 = upY;
    float y2 = upZ;

    // Computing X vector as Y cross Z
    float x0 = y1 * z2 - y2 * z1;
    float x1 = -y0 * z2 + y2 * z0;
    float x2 = y0 * z1 - y1 * z0;

    // Recompute Y = Z cross X
    y0 = z1 * x2 - z2 * x1;
    y1 = -z0 * x2 + z2 * x0;
    y2 = z0 * x1 - z1 * x0;

    // Cross product gives area of parallelogram, which is < 1.0 for
    // non-perpendicular unit-length vectors; so normalize x, y here:
    mag = (float) Math.sqrt(x0 * x0 + x1 * x1 + x2 * x2);
    if (mag != 0) {
      x0 /= mag;
      x1 /= mag;
      x2 /= mag;
    }

    mag = (float) Math.sqrt(y0 * y0 + y1 * y1 + y2 * y2);
    if (mag != 0) {
      y0 /= mag;
      y1 /= mag;
      y2 /= mag;
    }

    Matrix mv = new Matrix(x0, y0, z0, 0, x1, y1, z1, 0, x2, y2, z2, 0, 0, 0, 0, 1);

    float tx = -eyeX;
    float ty = -eyeY;
    float tz = -eyeZ;

    mv.translate(tx, ty, tz);
    _bindModelView(mv);
  }
}
