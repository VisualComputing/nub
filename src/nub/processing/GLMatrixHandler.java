/****************************************************************************************
 * nub
 * Copyright (c) 2019 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

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
  public Matrix projection() {
    return Scene.toMatrix(_pggl.projection.get());
  }

  @Override
  public void _bindProjection(Matrix matrix) {
    _pggl.setProjection(Scene.toPMatrix(matrix));
  }

  @Override
  public void applyProjection(Matrix matrix) {
    _pggl.applyProjection(Scene.toPMatrix(matrix));
  }

  @Override
  public void pushModelView() {
    _pggl.pushMatrix();
  }

  @Override
  public void popModelView() {
    _pggl.popMatrix();
  }

  @Override
  public Matrix modelView() {
    return Scene.toMatrix((PMatrix3D) _pggl.getMatrix());
  }

  @Override
  public void _bindModelView(Matrix matrix) {
    if (_pggl.is3D())
      _pggl.setMatrix(Scene.toPMatrix(matrix));// in P5 this caches projmodelview
    else {
      _pggl.modelview.set(Scene.toPMatrix(matrix));
      _pggl.projmodelview.set(Matrix.multiply(projection(), matrix).get(new float[16], false));
    }
  }

  @Override
  public void applyModelView(Matrix matrix) {
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
