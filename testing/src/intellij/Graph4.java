package intellij;

import frames.core.Frame;
import frames.core.Graph;
import frames.primitives.Matrix;
import frames.primitives.Point;
import processing.core.PApplet;
import processing.core.PMatrix3D;
import processing.event.MouseEvent;
import processing.opengl.PShader;

/**
 * Created by pierre on 11/15/16.
 */
public class Graph4 extends PApplet {
  Graph graph;
  PShader framesShader;
  Matrix pmv;
  PMatrix3D pmatrix = new PMatrix3D();
  Frame[] frames;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    graph = new Graph(width, height) {
      // Note that within visit() geometry is defined
      // at the frame local coordinate system.
      @Override
      public void applyTransformation(Frame frame) {
        super.applyTransformation(frame);
        shader(framesShader);
        pmv = Matrix.multiply(projection(), modelView());
        pmatrix.set(pmv.get(new float[16]));
        framesShader.set("frames_transform", pmatrix);
      }
    };
    graph.setFieldOfView(PI / 3);
    graph.fitBallInterpolation();
    framesShader = loadShader("/home/pierre/IdeaProjects/framesjs/testing/data/matrix_handler/FrameFrag.glsl", "/home/pierre/IdeaProjects/framesjs/testing/data/matrix_handler/FrameVert_pmv.glsl");
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
    graph.preDraw();
    background(0);
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
    PApplet.main(new String[]{"intellij.Graph4"});
  }
}
