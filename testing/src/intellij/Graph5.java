package intellij;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.MatrixHandler;
import frames.primitives.Matrix;
import frames.primitives.Point;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PMatrix3D;
import processing.event.MouseEvent;
import processing.opengl.PShader;

/**
 * Created by pierre on 11/15/16.
 */
public class Graph5 extends PApplet {
  Graph graph;
  Frame[] frames;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    graph = new Graph(width, height);
    GLSLMatrixHandler mh = new GLSLMatrixHandler(graph);
    graph.setMatrixHandler(mh);
    graph.setFieldOfView(PI / 3);
    graph.fitBallInterpolation();
    frames = new Frame[50];
    for (int i = 0; i < frames.length; i++) {
      frames[i] = new Frame(graph) {
        @Override
        public void visit() {
          pushStyle();
          fill(isTracked(graph) ? 0 : 255, 0, 255);
          box(5);
          popStyle();
        }
      };
      frames[i].randomize();
    }
    //discard Processing matrices
    resetMatrix();
  }

  public void draw() {
    background(0);
    graph.preDraw();
    pushStyle();
    fill(0, 255, 0);
    Scene.drawTorusSolenoid(g);
    popStyle();
    graph.traverse();
  }

  public void mouseMoved() {
    graph.track(mouseX, mouseY, frames);
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      graph.spin(new Point(pmouseX, pmouseY), new Point(mouseX, mouseY));
    else if (mouseButton == RIGHT)
      graph.translate(mouseX - pmouseX, mouseY - pmouseY);
    else
      graph.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    graph.scale(event.getCount() * 20);
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.Graph5"});
  }

  public class GLSLMatrixHandler extends MatrixHandler {
    PShader framesShader;
    Matrix pmv;
    PMatrix3D pmatrix = new PMatrix3D();

    public GLSLMatrixHandler(Graph graph) {
      super(graph);
      framesShader = loadShader("/home/pierre/IdeaProjects/framesjs/testing/data/matrix_handler/FrameFrag.glsl", "/home/pierre/IdeaProjects/framesjs/testing/data/matrix_handler/FrameVert_pmv.glsl");
    }

    void updateMatrices() {
      shader(framesShader);
      pmv = Matrix.multiply(projection(), modelView());
      pmatrix.set(pmv.get(new float[16]));
      framesShader.set("frames_transform", pmatrix);
    }

    @Override
    protected void _bind() {
      super._bind();
      //shader(framesShader);
      //pmatrix.set(cacheView().get(new float[16]));
      //framesShader.set("frames_transform", pmatrix);
      updateMatrices();
    }

    /*
    @Override
    public void bindProjection(Matrix matrix) {
      super.bindProjection(matrix);
      updateMatrices();
    }

    @Override
    public void bindModelView(Matrix matrix) {
      super.bindModelView(matrix);
      updateMatrices();
    }
    // */

    @Override
    public void applyProjection(Matrix matrix) {
      super.applyProjection(matrix);
      updateMatrices();
    }

    @Override
    public void applyModelView(Matrix matrix) {
      super.applyModelView(matrix);
      updateMatrices();
    }

    @Override
    public void translate(float x, float y) {
      super.translate(x, y);
      updateMatrices();
    }

    @Override
    public void translate(float x, float y, float z) {
      super.translate(x, y, z);
      updateMatrices();
    }

    @Override
    public void rotate(float angle) {
      super.rotate(angle);
      updateMatrices();
    }

    @Override
    public void rotate(float angle, float vx, float vy, float vz) {
      super.rotate(angle, vx, vy, vz);
      updateMatrices();
    }

    @Override
    public void scale(float sx, float sy) {
      super.scale(sx, sy);
      updateMatrices();
    }

    @Override
    public void scale(float x, float y, float z) {
      super.scale(x, y, z);
      updateMatrices();
    }
  }
}
