package intellij;

import frames.core.Frame;
import frames.core.Graph;
import frames.primitives.Point;
import frames.processing.GLMatrixHandler;
import processing.core.PApplet;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;

/**
 * Created by pierre on 11/15/16.
 */
public class Graph2 extends PApplet {
  Graph graph;
  Frame[] frames;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    graph = new Graph(width, height);
    graph.fit(1);
    graph.setMatrixHandler(new GLMatrixHandler((PGraphicsOpenGL) g));
    frames = new Frame[50];
    for (int i = 0; i < frames.length; i++) {
      frames[i] = new Frame(graph) {
        @Override
        public void visit() {
          pushStyle();
          fill(255, 0, 255);
          box(5);
          popStyle();
        }
      };
      frames[i].randomize();
    }
  }

  public void draw() {
    graph.preDraw();
    background(0);
    graph.traverse();
  }

  @Override
  public void mouseMoved() {
    graph.track(mouseX, mouseY);
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
    PApplet.main(new String[]{"intellij.Graph1"});
  }
}
