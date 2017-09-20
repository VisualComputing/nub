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

import remixlab.dandelion.geom.*;
import remixlab.util.Copyable;

/**
 * 2D implementation of the {@link remixlab.dandelion.core.Eye} abstract class.
 * <p>
 * <b>Attention: </b> the {@link #frame()}
 * {@link remixlab.dandelion.geom.Frame#magnitude()} is used to scale the view. The Window
 * magnitude is thus generally different from that of the scene. Use
 * {@link #eyeCoordinatesOf(Vec)} and {@link #worldCoordinatesOf(Vec)} (or any of the
 * powerful Frame transformations (
 * {@link remixlab.dandelion.geom.Frame#coordinatesOf(Vec)},
 * {@link remixlab.dandelion.geom.Frame#transformOf(Vec)}, ...)) to convert to and from
 * the Eye {@link #frame()} coordinate system.
 */
public class Window extends Eye implements Copyable {
  static final float FAKED_ZNEAR = -10;
  static final float FAKED_ZFAR = 10;

  public Window(AbstractScene scn) {
    super(scn);
    if (gScene.is3D())
      throw new RuntimeException("Use Window only for a 2D Scene");
    computeProjection();
  }

  protected Window(Window oVW) {
    super(oVW);
  }

  @Override
  public Window get() {
    return new Window(this);
  }

  @Override
  public void computeView() {
    Rot q = (Rot) frame().orientation();

    float cosB = (float) Math.cos((double) q.angle());
    float sinB = (float) Math.sin((double) q.angle());

    viewMat.mat[0] = cosB;
    viewMat.mat[1] = -sinB;
    viewMat.mat[2] = 0.0f;
    viewMat.mat[3] = 0.0f;

    viewMat.mat[4] = sinB;
    viewMat.mat[5] = cosB;
    viewMat.mat[6] = 0.0f;
    viewMat.mat[7] = 0.0f;

    viewMat.mat[8] = 0.0f;
    viewMat.mat[9] = 0.0f;
    viewMat.mat[10] = 1.0f;
    viewMat.mat[11] = 0.0f;

    Vec t = q.inverseRotate(frame().position());

    viewMat.mat[12] = -t.vec[0];
    viewMat.mat[13] = -t.vec[1];
    viewMat.mat[14] = -t.vec[2];
    viewMat.mat[15] = 1.0f;
  }

  @Override
  public void computeProjection() {
    float[] wh = getBoundaryWidthHeight();
    projectionMat.mat[0] = 1.0f / wh[0];
    projectionMat.mat[5] = (gScene.isLeftHanded() ? -1.0f : 1.0f) / wh[1];
    projectionMat.mat[10] = -2.0f / (FAKED_ZFAR - FAKED_ZNEAR);
    projectionMat.mat[11] = 0.0f;
    projectionMat.mat[14] = -(FAKED_ZFAR + FAKED_ZNEAR) / (FAKED_ZFAR - FAKED_ZNEAR);
    projectionMat.mat[15] = 1.0f;
  }

  // TODO needs test
  @Override
  public void fromView(Mat mv, boolean recompute) {
    Rot q = new Rot();
    q.fromMatrix(mv);
    setOrientation(q);
    setPosition(Vec.multiply(q.rotate(new Vec(mv.mat[12], mv.mat[13], mv.mat[14])), -1));
    if (recompute)
      this.computeView();
  }

  @Override
  public void setUpVector(Vec up, boolean noMove) {
    Rot r = new Rot(new Vec(0.0f, 1.0f), frame().transformOf(up));

    if (!noMove)
      frame().setPosition(
          Vec.subtract(anchor(), (Rot.compose((Rot) frame().orientation(), r)).rotate(frame().coordinatesOf(anchor()))));

    frame().rotate(r);

    // Useful in fly mode to keep the horizontal direction.
    frame().updateSceneUpVector();
  }

  /**
   * Same as {@code setUpVector(new Vec(x,y))}.
   *
   * @see #setUpVector(Vec)
   */
  public void setUpVector(float x, float y) {
    setUpVector(new Vec(x, y));
  }

  /**
   * Same as {@code setUpVector(new Vec(x,y), boolean noMove)}.
   *
   * @see #setUpVector(Vec, boolean)
   */
  public void setUpVector(float x, float y, boolean noMove) {
    setUpVector(new Vec(x, y), noMove);
  }

  /**
   * Same as {@code setPosition(new Vec(x,y))}.
   *
   * @see #setPosition(Vec)
   */
  public void setPosition(float x, float y) {
    setPosition(new Vec(x, y));
  }

  @Override
  public boolean setSceneCenterFromPixel(Point pixel) {
    setSceneCenter(new Vec(pixel.x(), pixel.y()));
    return true;
  }

  @Override
  public void fitBoundingBox(Vec min, Vec max) {
    float diameter = Math.max(Math.abs(max.vec[1] - min.vec[1]), Math.abs(max.vec[0] - min.vec[0]));
    diameter = Math.max(Math.abs(max.vec[2] - min.vec[2]), diameter);
    fitBall(Vec.multiply(Vec.add(min, max), 0.5f), 0.5f * diameter);
  }

  @Override
  public void fitBall(Vec center, float radius) {
    float size = Math.min(gScene.width(), gScene.height());
    frame().setMagnitude(2 * radius / size);

    lookAt(center);
  }

  @Override
  public void setSceneBoundingBox(Vec min, Vec max) {
    Vec mn = new Vec(min.x(), min.y(), 0);
    Vec mx = new Vec(max.x(), max.y(), 0);
    setSceneCenter(Vec.multiply(Vec.add(mn, mx), 1 / 2.0f));
    setSceneRadius(0.5f * (Vec.subtract(mx, mn)).magnitude());
  }

  @Override
  public void fitScreenRegion(Rect rectangle) {
    float rectRatio = (float) rectangle.width() / (float) rectangle.height();

    if (aspectRatio() < 1.0f) {
      if (aspectRatio() < rectRatio)
        frame().setMagnitude(frame().magnitude() * (float) rectangle.width() / screenWidth());
      else
        frame().setMagnitude(frame().magnitude() * (float) rectangle.height() / screenHeight());
    } else {
      if (aspectRatio() < rectRatio)
        frame().setMagnitude(frame().magnitude() * (float) rectangle.width() / screenWidth());
      else
        frame().setMagnitude(frame().magnitude() * (float) rectangle.height() / screenHeight());
    }
    lookAt(unprojectedCoordinatesOf(new Vec(rectangle.centerX(), rectangle.centerY(), 0)));
  }

  @Override
  public void setOrientation(Rotation q) {
    setOrientation(q.angle());
  }

  public void setOrientation(float angle) {
    Rotation r = new Rot(angle);
    frame().setOrientation(r);
    frame().updateSceneUpVector();
  }

  @Override
  public float[][] computeBoundaryEquations() {
    return computeBoundaryEquations(new float[4][3]);
  }

  @Override
  public float[][] computeBoundaryEquations(float[][] coef) {
    if (coef == null || (coef.length == 0))
      coef = new float[4][3];
    else if ((coef.length != 4) || (coef[0].length != 3))
      coef = new float[4][3];

    // Computed once and for all
    Vec pos = position();
    // Vec viewDir = viewDirection();
    Vec up = upVector();
    Vec right = rightVector();

    normal[0] = Vec.multiply(right, -1);
    normal[1] = right;
    normal[2] = up;
    normal[3] = Vec.multiply(up, -1);

    float[] wh = getBoundaryWidthHeight();

    dist[0] = Vec.dot(Vec.subtract(pos, Vec.multiply(right, wh[0])), normal[0]);
    dist[1] = Vec.dot(Vec.add(pos, Vec.multiply(right, wh[0])), normal[1]);
    dist[2] = Vec.dot(Vec.add(pos, Vec.multiply(up, wh[1])), normal[2]);
    dist[3] = Vec.dot(Vec.subtract(pos, Vec.multiply(up, wh[1])), normal[3]);

    for (int i = 0; i < 4; ++i) {
      coef[i][0] = normal[i].vec[0];
      coef[i][1] = normal[i].vec[1];
      // Change respect to Camera occurs here:
      coef[i][2] = -dist[i];
    }

    return coef;
  }

  @Override
  public float distanceToBoundary(int index, Vec pos) {
    if (!gScene.areBoundaryEquationsEnabled())
      System.out.println("The camera frustum plane equations (needed by distanceToBoundary) may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    // check this: http://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
    return (fpCoefficients[index][0] * pos.x() + fpCoefficients[index][1] * pos.y() + fpCoefficients[index][2])
        / (float) Math
        .sqrt(fpCoefficients[index][0] * fpCoefficients[index][0] + fpCoefficients[index][1] * fpCoefficients[index][1]);
  }

  @Override
  public Visibility boxVisibility(Vec p1, Vec p2) {
    if (!gScene.areBoundaryEquationsEnabled())
      System.out.println("The camera frustum plane equations (needed by aaBoxIsVisible) may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    boolean allInForAllPlanes = true;

    for (int i = 0; i < 4; ++i) {
      boolean allOut = true;
      for (int c = 0; c < 4; ++c) {
        Vec pos = new Vec(((c & 2) != 0) ? p1.vec[0] : p2.vec[0], ((c & 1) != 0) ? p1.vec[1] : p2.vec[1]);
        if (distanceToBoundary(i, pos) > 0.0)
          allInForAllPlanes = false;
        else
          allOut = false;
      }
      // The eight points are on the outside side of this plane
      if (allOut)
        return Eye.Visibility.INVISIBLE;
    }

    if (allInForAllPlanes)
      return Eye.Visibility.VISIBLE;

    // Too conservative, but tangent cases are too expensive to detect
    return Eye.Visibility.SEMIVISIBLE;
  }

  @Override
  public Visibility ballVisibility(Vec center, float radius) {
    if (!gScene.areBoundaryEquationsEnabled())
      System.out.println("The camera frustum plane equations (needed by sphereIsVisible) may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    boolean allInForAllPlanes = true;
    for (int i = 0; i < 4; ++i) {
      float d = distanceToBoundary(i, center);
      if (d > radius)
        return Eye.Visibility.INVISIBLE;
      if ((d > 0) || (-d < radius))
        allInForAllPlanes = false;
    }
    if (allInForAllPlanes)
      return Eye.Visibility.VISIBLE;
    return Eye.Visibility.SEMIVISIBLE;
  }

  @Override
  public boolean isPointVisible(Vec point) {
    if (!gScene.areBoundaryEquationsEnabled())
      System.out.println("The camera frustum plane equations (needed by pointIsVisible) may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    for (int i = 0; i < 4; ++i)
      if (distanceToBoundary(i, point) > 0)
        return false;
    return true;
  }

  /**
   * Same as {@code return isPointVisible(new Vec(x, y))}.
   *
   * @see #isPointVisible(Vec)
   */
  public boolean isPointVisible(float x, float y) {
    return isPointVisible(new Vec(x, y));
  }

  @Override
  public float sceneToPixelRatio(Vec position) {
    float[] wh = getBoundaryWidthHeight();
    return 2.0f * wh[1] / screenHeight();
  }

  @Override
  public void lookAt(Vec target) {
    frame().setPosition(target.x(), target.y());
  }

  /**
   * Same as {@code lookAt(new Vec(x,y))}.
   *
   * @see #lookAt(Vec)
   */
  public void lookAt(float x, float y) {
    lookAt(new Vec(x, y));
  }

  /**
   * Same as {@code setAnchor(new Vec(x,y))}.
   *
   * @see AbstractScene#setAnchor(Vec)
   */
  public void setAnchor(float x, float y) {
    setAnchor(new Vec(x, y));
  }

  @Override
  public boolean setAnchorFromPixel(Point pixel) {
    setAnchor(unprojectedCoordinatesOf(new Vec((float) pixel.x(), (float) pixel.y(), 0.5f)));
    // new
    anchorFlag = true;
    timerFx.runOnce(1000);
    return true;
  }

  @Override
  public void interpolateToZoomOnPixel(Point pixel) {
    float winW = this.screenWidth() / 3;
    float winH = this.screenHeight() / 3;
    float cX = (float) pixel.x() - winW / 2;
    float cY = (float) pixel.y() - winH / 2;
    Rect rect = new Rect((int) cX, (int) cY, (int) winW, (int) winH);
    this.interpolateToZoomOnRegion(rect);
    // draw hint
    pupVec = unprojectedCoordinatesOf(new Vec(pixel.x(), pixel.y(), 0.5f));
    pupFlag = true;
    timerFx.runOnce(1000);
  }

  @Override
  public float distanceToSceneCenter() {
    return Vec.distance(position(), sceneCenter());
  }

  @Override
  public float distanceToAnchor() {
    return Vec.distance(position(), anchor());
  }

  @Override
  public Vec at() {
    return position();
  }
}
