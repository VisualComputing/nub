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
import remixlab.dandelion.geom.Vec;
import remixlab.util.Util;

/**
 * Various matrix operations dandelion should support either through a third-party
 * implementation or locally through the {@link remixlab.dandelion.core.MatrixStackHelper}
 * .
 */
public abstract class MatrixHelper {
  protected AbstractScene gScene;

  protected Mat projectionViewMat, projectionViewInverseMat;
  protected boolean isProjViwInvCached, projectionViewMatHasInv;

  /**
   * Instantiates the {@link #projectionView()} matrix and sets
   * {@link #isProjectionViewInverseCached()} to {@code false}.
   *
   * @param scn
   */
  public MatrixHelper(AbstractScene scn) {
    gScene = scn;
    projectionViewMat = new Mat();
    isProjViwInvCached = false;
  }

  /**
   * Returns the scene this object belongs to
   */
  public AbstractScene scene() {
    return gScene;
  }

  public void bind(boolean recompute) {
    loadProjection(recompute);
    loadModelView(recompute);
    if (recompute)
      cacheProjectionView();
  }

  /**
   * Load {@link #projection()} and {@link #modelView()} in
   * {@link remixlab.dandelion.core.AbstractScene#preDraw()}.
   */
  public void bind() {
    bind(true);
  }

  /**
   * Internal use. Called in {@link #bind()}. Note that P x V is always cached.
   */
  protected void cacheProjectionView() {
    Mat.multiply(projection(), modelView(), projectionViewMat);
    if (isProjectionViewInverseCached()) {
      if (projectionViewInverseMat == null)
        projectionViewInverseMat = new Mat();
      projectionViewMatHasInv = projectionViewMat.invert(projectionViewInverseMat);
    }
  }

  /**
   * Returns {@code true} if {@code P x M} and {@code inv (P x M)} are being cached, and
   * {@code false} otherwise.
   *
   * @see #cacheProjectionView()
   * @see #cacheProjectionViewInverse(boolean)
   */
  public boolean isProjectionViewInverseCached() {
    return isProjViwInvCached;
  }

  /**
   * Cache {@code inv (P x M)} (and also {@code (P x M)} ) so that
   * {@link remixlab.dandelion.core.AbstractScene#unprojectedCoordinatesOf(Vec)} is
   * optimized.
   *
   * @see #isProjectionViewInverseCached()
   * @see #cacheProjectionView()
   */
  public void cacheProjectionViewInverse(boolean optimise) {
    isProjViwInvCached = optimise;
  }

  /**
   * @return {@link #projection()} * {@link #modelView()}.
   */
  public Mat projectionView() {
    return projectionViewMat;
  }

  /**
   * {@link #cacheProjectionViewInverse(boolean)} should be called first for this method
   * to take effect.
   *
   * @return inv({@link #projection()} * {@link #modelView()})
   */
  public Mat projectionViewInverse() {
    if (!isProjectionViewInverseCached())
      throw new RuntimeException("optimizeUnprojectCache(true) should be called first");
    return projectionViewInverseMat;
  }

  /**
   * Same as {@code setProjection(gScene.eye().getProjection(recompute))}.
   *
   * @see #setProjection(Mat)
   * @see remixlab.dandelion.core.Eye#getProjection(boolean)
   */
  public void loadProjection(boolean recompute) {
    setProjection(gScene.eye().getProjection(recompute));
  }

  /**
   * Computes the projection matrix from
   * {@link remixlab.dandelion.core.AbstractScene#eye()} parameters and loads it into the
   * matrix helper. Used in {@link #bind()}.
   *
   * @see remixlab.dandelion.core.Eye#getProjection(boolean)
   */
  public void loadProjection() {
    loadProjection(true);
  }

  /**
   * Same as {@code setModelView(gScene.eye().getView(recompute))}.
   *
   * @see #setModelView(Mat)
   * @see remixlab.dandelion.core.Eye#getView(boolean)
   */
  public void loadModelView(boolean recompute) {
    setModelView(gScene.eye().getView(recompute));
  }

  /**
   * Computes the view matrix from {@link remixlab.dandelion.core.AbstractScene#eye()}
   * parameters and loads it into the matrix helper. Used in {@link #bind()}. If
   * {@code includeView} is {@code false}
   *
   * @see remixlab.dandelion.core.Eye#getView(boolean)
   */
  public void loadModelView() {
    loadModelView(true);
    // other way is to compute view but load identity. Maybe usfull for a webgl
    // context?
    // scene.eye().computeView();
    // resetModelView();// loads identity -> only model, (excludes view)
  }

  /**
   * Push a copy of the modelview matrix onto the stack.
   */
  public void pushModelView() {
    AbstractScene.showMissingImplementationWarning("pushModelView", getClass().getName());
  }

  /**
   * Replace the current modelview matrix with the top of the stack.
   */
  public void popModelView() {
    AbstractScene.showMissingImplementationWarning("popModelView", getClass().getName());
  }

  /**
   * Push a copy of the projection matrix onto the stack.
   */
  public void pushProjection() {
    AbstractScene.showMissingImplementationWarning("pushProjection", getClass().getName());
  }

  /**
   * Replace the current projection matrix with the top of the stack.
   */
  public void popProjection() {
    AbstractScene.showMissingImplementationWarning("popProjection", getClass().getName());
  }

  /**
   * Set the current projection matrix to identity.
   */
  public void resetProjection() {
    AbstractScene.showMissingImplementationWarning("resetProjection", getClass().getName());
  }

  /**
   * Multiplies the current projection matrix by the one specified through the parameters.
   */
  public void applyProjection(Mat source) {
    AbstractScene.showMissingImplementationWarning("applyProjection", getClass().getName());
  }

  /**
   * Set the current projection matrix to the contents of another.
   */
  public void setProjection(Mat source) {
    AbstractScene.showMissingImplementationWarning("setProjection", getClass().getName());
  }

  /**
   * Translate in X and Y.
   */
  public void translate(float tx, float ty) {
    AbstractScene.showMissingImplementationWarning("translate", getClass().getName());
  }

  /**
   * Translate in X, Y, and Z.
   */
  public void translate(float tx, float ty, float tz) {
    AbstractScene.showMissingImplementationWarning("translate", getClass().getName());
  }

  /**
   * Two dimensional rotation.
   * <p>
   * Same as rotateZ (this is identical to a 3D rotation along the z-axis) but included
   * for clarity. It'd be weird for people drawing 2D graphics to be using rotateZ. And
   * they might kick our a-- for the confusion.
   * <p>
   * <A HREF="http://www.xkcd.com/c184.html">Additional background</A>.
   */
  public void rotate(float angle) {
    AbstractScene.showMissingImplementationWarning("rotate", getClass().getName());
  }

  /**
   * Rotate around the X axis.
   */
  public void rotateX(float angle) {
    AbstractScene.showMissingImplementationWarning("rotateX", getClass().getName());
  }

  /**
   * Rotate around the Y axis.
   */
  public void rotateY(float angle) {
    AbstractScene.showMissingImplementationWarning("rotateY", getClass().getName());
  }

  /**
   * Rotate around the Z axis.
   */
  public void rotateZ(float angle) {
    AbstractScene.showMissingImplementationWarning("rotateZ", getClass().getName());
  }

  /**
   * Rotate about a vector in space. Same as the glRotatef() function.
   */
  public void rotate(float angle, float vx, float vy, float vz) {
    AbstractScene.showMissingImplementationWarning("rotate", getClass().getName());
  }

  /**
   * Scale equally in all dimensions.
   */
  public void scale(float s) {
    AbstractScene.showMissingImplementationWarning("scale", getClass().getName());
  }

  /**
   * Scale in X and Y. Equivalent to scale(sx, sy, 1).
   * <p>
   * Not recommended for use in 3D, because the z-dimension is just scaled by 1, since
   * there's no way to know what else to scale it by.
   */
  public void scale(float sx, float sy) {
    AbstractScene.showMissingImplementationWarning("scale", getClass().getName());
  }

  /**
   * Scale in X, Y, and Z.
   */
  public void scale(float x, float y, float z) {
    AbstractScene.showMissingImplementationWarning("scale", getClass().getName());
  }

  /**
   * Set the current modelview matrix to identity.
   */
  public abstract void resetModelView();

  /**
   * Multiplies the current modelview matrix by the one specified through the parameters.
   */
  public abstract void applyModelView(Mat source);

  /**
   * @return modelview matrix
   */
  public abstract Mat modelView();

  /**
   * Copy the current modelview matrix into the specified target. Pass in null to create a
   * new matrix.
   */
  public abstract Mat getModelView(Mat target);

  /**
   * @return projection matrix
   */
  public Mat projection() {
    AbstractScene.showMissingImplementationWarning("projection", getClass().getName());
    return null;
  }

  /**
   * Copy the current projection matrix into the specified target. Pass in null to create
   * a new matrix.
   */
  public Mat getProjection(Mat target) {
    AbstractScene.showMissingImplementationWarning("getProjection", getClass().getName());
    return null;
  }

  /**
   * Set the current modelview matrix to the contents of another.
   */
  public abstract void setModelView(Mat source);

  /**
   * Print the current modelview matrix.
   */
  public abstract void printModelView();

  /**
   * Print the current projection matrix.
   */
  public void printProjection() {
    AbstractScene.showMissingImplementationWarning("printProjection", getClass().getName());
  }

  /**
   * Computes the world coordinates of an screen object so that drawing can be done
   * directly with 2D screen coordinates.
   * <p>
   * All screen drawing should be enclosed between {@link #beginScreenDrawing()} and
   * {@link #endScreenDrawing()}. Then you can just begin drawing your screen shapes.
   * <b>Attention:</b> If you want your screen drawing to appear on top of your 3d scene
   * then draw first all your 3d before doing any call to a {@link #beginScreenDrawing()}
   * and {@link #endScreenDrawing()} pair.
   *
   * @see #endScreenDrawing()
   */
  public void beginScreenDrawing() {
    pushProjection();
    ortho2D();
    pushModelView();
    resetViewPoint();
  }

  /**
   * Ends screen drawing. See {@link #beginScreenDrawing()} for details.
   *
   * @see #beginScreenDrawing()
   */
  public void endScreenDrawing() {
    popProjection();
    popModelView();
  }

  // see:
  // http://www.opengl.org/archives/resources/faq/technical/transformations.htm
  // "9.030 How do I draw 2D controls over my 3D rendering?"
  protected void ortho2D() {
    float cameraZ = (gScene.height() / 2.0f) / (float) Math.tan((float) Math.PI / 8);
    float cameraNear = cameraZ / 2.0f;
    float cameraFar = cameraZ * 2.0f;

    float left = -gScene.width() / 2;
    float right = gScene.width() / 2;
    float bottom = -gScene.height() / 2;
    float top = gScene.height() / 2;
    float near = cameraNear;
    float far = cameraFar;

    float x = +2.0f / (right - left);
    float y = +2.0f / (top - bottom);
    float z = -2.0f / (far - near);

    float tx = -(right + left) / (right - left);
    float ty = -(top + bottom) / (top - bottom);
    float tz = -(far + near) / (far - near);

    // The minus sign is needed to invert the Y axis.
    setProjection(new Mat(x, 0, 0, 0, 0, -y, 0, 0, 0, 0, z, 0, tx, ty, tz, 1));
  }

  // as it's done in P5:
  protected void resetViewPoint() {
    float eyeX = gScene.width() / 2f;
    float eyeY = gScene.height() / 2f;
    float eyeZ = (gScene.height() / 2f) / (float) Math.tan((float) Math.PI * 60 / 360);
    float centerX = gScene.width() / 2f;
    float centerY = gScene.height() / 2f;
    float centerZ = 0;
    float upX = 0;
    float upY = 1;
    float upZ = 0;

    // Calculating Z vector
    float z0 = eyeX - centerX;
    float z1 = eyeY - centerY;
    float z2 = eyeZ - centerZ;
    float mag = (float) Math.sqrt(z0 * z0 + z1 * z1 + z2 * z2);
    if (Util.nonZero(mag)) {
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
    if (Util.nonZero(mag)) {
      x0 /= mag;
      x1 /= mag;
      x2 /= mag;
    }

    mag = (float) Math.sqrt(y0 * y0 + y1 * y1 + y2 * y2);
    if (Util.nonZero(mag)) {
      y0 /= mag;
      y1 /= mag;
      y2 /= mag;
    }

    Mat mv = new Mat(x0, y0, z0, 0, x1, y1, z1, 0, x2, y2, z2, 0, 0, 0, 0, 1);

    float tx = -eyeX;
    float ty = -eyeY;
    float tz = -eyeZ;

    mv.translate(tx, ty, tz);

    setModelView(mv);
  }
}
