/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.core;

import frames.primitives.Matrix;
import frames.primitives.Vector;

/**
 * The matrix handler specifies (and implements) various matrix operations needed by the
 * {@link Graph} to properly perform its geometry transformations.
 * <p>
 * To set shader matrix uniform variables call {@link #projection()}, {@link #modelView()}
 * and (possibly) {@code Matrix.multiply(projection(), modelView())}.
 * <p>
 * To bind a {@link Graph} object to a third party renderer (i.e., that renderer provides
 * its own matrix handling: matrix transformations, shader uniforms transfers, etc),
 * override: {@link #bindModelView(Matrix)}, {@link #modelView()}, {@link #applyModelView(Matrix)},
 * {@link #pushModelView()}, {@link #popModelView()}, {@link #translate(float, float, float)},
 * {@link #rotate(float)}, {@link #rotate(float, float, float, float)},
 * {@link #scale(float, float, float)}, {@link #projection()}, {@link #bindProjection(Matrix)},
 * {@link #applyProjection(Matrix)}, {@link #pushProjection()} and {@link #popProjection()} by
 * implementing them in terms of the renderer params (see {@link #_bind()}).
 *
 * @see Matrix
 * @see #projection()
 * @see #bindProjection(Matrix)
 * @see #modelView()
 * @see #bindModelView(Matrix)
 * @see #_bind()
 * @see Graph#preDraw()
 */
public class MatrixHandler {
  protected Graph _graph;
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
   * @param graph
   */
  public MatrixHandler(Graph graph) {
    _graph = graph;
    _projection = new Matrix();
    _view = new Matrix();
    _modelview = new Matrix();
    _projectionView = new Matrix();
    _isProjectionViewInverseCached = false;
  }

  /**
   * Returns the graph this matrix helper belongs to.
   */
  public Graph graph() {
    return _graph;
  }

  /**
   * Updates (computes and caches) the projection and view matrices from the {@link #graph()}
   * {@link Graph#eye()} parameters. This method is automatically called by
   * {@link Graph#preDraw()} right at the beginning of the main event loop.
   * <p>
   * If {@link #graph()} is bound to a third party renderer (i.e., that renderer provides
   * its own matrix matrix handling: matrix transformations, shader uniforms transfers, etc.)
   * this method also binds the projection and view matrices to that renderer.
   * In this case, note that {@link #bindProjection(Matrix)} and {@link #bindModelView(Matrix)}
   * should be overridden, by implementing them in terms of the renderer parameters.
   *
   * @see Graph#preDraw()
   * @see Graph#computeProjection()
   * @see Graph#computeCustomProjection()
   * @see Frame#view()
   * @see #bindProjection(Matrix)
   * @see #bindModelView(Matrix)
   */
  protected void _bind() {
    _projection.set(graph().computeProjection());
    _view.set(graph().eye().view());
    _cacheProjectionView(Matrix.multiply(cacheProjection(), cacheView()));
    bindProjection(cacheProjection());
    bindModelView(cacheView());
  }

  // 1. May be overridden

  /**
   * Binds the projection matrix to the renderer. Only meaningful for raster renderers.
   * Default implementation is empty (see {@link #_bind()}).
   */
  public void bindProjection(Matrix matrix) {
  }

  /**
   * Binds the modelview matrix to the renderer.
   */
  public void bindModelView(Matrix matrix) {
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
   */
  public void applyModelView(Matrix source) {
    _modelview.apply(source);
  }

  /**
   * Multiplies the current projection matrix by the one specified through the parameters.
   */
  public void applyProjection(Matrix source) {
    _projection.apply(source);
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
   */
  public void popModelView() {
    if (_modelviewStackDepth == 0) {
      throw new RuntimeException(ERROR_PUSHMATRIX_UNDERFLOW);
    }
    _modelviewStackDepth--;
    _modelview.set(_modelviewStack[_modelviewStackDepth]);
  }

  /**
   * Translate in X, Y, and Z.
   */
  public void translate(float x, float y, float z) {
    _modelview.translate(x, y, z);
  }

  /**
   * Rotate around the Z axis.
   */
  public void rotate(float angle) {
    _modelview.rotateZ(angle);
  }

  /**
   * Rotate about a vector in space.
   */
  public void rotate(float angle, float v0, float v1, float v2) {
    _modelview.rotate(angle, v0, v1, v2);
  }

  /**
   * Scale in X, Y, and Z.
   */
  public void scale(float x, float y, float z) {
    _modelview.scale(x, y, z);
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
   */
  public void popProjection() {
    if (_projectionStackDepth == 0) {
      throw new RuntimeException(ERROR_PUSHMATRIX_UNDERFLOW);
    }
    _projectionStackDepth--;
    _projection.set(_projectionStack[_projectionStackDepth]);
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
    float cameraZ = (_graph.height() / 2.0f) / (float) Math.tan((float) Math.PI / 8);
    float cameraNear = cameraZ / 2.0f;
    float cameraFar = cameraZ * 2.0f;

    float left = -_graph.width() / 2;
    float right = _graph.width() / 2;
    float bottom = -_graph.height() / 2;
    float top = _graph.height() / 2;
    float near = cameraNear;
    float far = cameraFar;

    float x = +2.0f / (right - left);
    float y = +2.0f / (top - bottom);
    float z = -2.0f / (far - near);

    float tx = -(right + left) / (right - left);
    float ty = -(top + bottom) / (top - bottom);
    float tz = -(far + near) / (far - near);

    // The minus sign is needed to invert the Y axis.
    bindProjection(new Matrix(x, 0, 0, 0, 0, -y, 0, 0, 0, 0, z, 0, tx, ty, tz, 1));
  }

  // as it's done in P5:
  protected void _resetViewPoint() {
    float eyeX = _graph.width() / 2f;
    float eyeY = _graph.height() / 2f;
    float eyeZ = (_graph.height() / 2f) / (float) Math.tan((float) Math.PI * 60 / 360);
    float centerX = _graph.width() / 2f;
    float centerY = _graph.height() / 2f;
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
    bindModelView(mv);
  }
}
