/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.processing;

import frames.core.Graph;
import frames.core.MatrixHandler;
import frames.primitives.Matrix;
import processing.core.PMatrix3D;
import processing.opengl.PGraphicsOpenGL;

/**
 * Internal {@link MatrixHandler} based on PGraphicsOpenGL graphics transformation.
 */
public class GLMatrixHandler extends MatrixHandler {
  PGraphicsOpenGL _pgraphics;

  public GLMatrixHandler(Graph graph, PGraphicsOpenGL renderer) {
    super(graph);
    _pgraphics = renderer;
  }

  /**
   * Returns the PGraphics object to be bound by this handler.
   */
  public PGraphicsOpenGL pggl() {
    return _pgraphics;
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
  public Matrix projection() {
    return Scene.toMatrix(pggl().projection.get());
  }

  @Override
  public void bindProjection(Matrix matrix) {
    pggl().setProjection(Scene.toPMatrix(matrix));
  }

  @Override
  public void applyProjection(Matrix matrix) {
    pggl().applyProjection(Scene.toPMatrix(matrix));
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
  public Matrix modelView() {
    return Scene.toMatrix((PMatrix3D) pggl().getMatrix());
  }

  @Override
  public void bindModelView(Matrix matrix) {
    if (_graph.is3D())
      pggl().setMatrix(Scene.toPMatrix(matrix));// in P5 this caches projmodelview
    else {
      pggl().modelview.set(Scene.toPMatrix(matrix));
      pggl().projmodelview.set(Matrix.multiply(projection(), cacheView()).get(new float[16], false));
    }
  }

  @Override
  public void applyModelView(Matrix matrix) {
    pggl().applyMatrix(Scene.toPMatrix(matrix));
  }

  @Override
  public void translate(float x, float y) {
    pggl().translate(x, y);
  }

  @Override
  public void translate(float x, float y, float z) {
    pggl().translate(x, y, z);
  }

  @Override
  public void rotate(float angle) {
    pggl().rotate(angle);
  }

  @Override
  public void rotate(float angle, float vx, float vy, float vz) {
    pggl().rotate(angle, vx, vy, vz);
  }

  @Override
  public void scale(float sx, float sy) {
    pggl().scale(sx, sy);
  }

  @Override
  public void scale(float x, float y, float z) {
    pggl().scale(x, y, z);
  }
}
