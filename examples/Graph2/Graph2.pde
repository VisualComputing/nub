import frames.core.*;
import frames.primitives.*;
import frames.processing.*;

Graph graph;
Frame[] frames;
//Choose P2D or P3D
String renderer = P3D;

void setup() {
  size(800, 800, renderer);
  graph = new Graph(width, height);
  if(g.is3D()) {
    graph.setType(Graph.Type.PERSPECTIVE);
    graph.setFieldOfView(PI / 3);
  }
  else {
    ellipseMode(CENTER);
    graph.setType(Graph.Type.TWO_D);
  }
  graph.fitBallInterpolation();
  graph.setMatrixHandler(new GLMatrixHandler(graph, (PGraphicsOpenGL) g));
  frames = new Frame[50];
  for (int i = 0; i < frames.length; i++) {
    frames[i] = new Frame(graph) {
      @Override
        public void visit() {
        pushStyle();
        fill(255, 0, 255);
        if(g.is3D())
          box(5);
        else
          ellipse(0, 0, 10, 5);
        popStyle();
      }
    };
    frames[i].randomize();
  }
}

void draw() {
  graph.preDraw();
  background(0);
  graph.traverse();
}

void mouseMoved() {
  graph.track(mouseX, mouseY);
}

void mouseDragged() {
  if (mouseButton == LEFT)
    graph.spin(new Point(pmouseX, pmouseY), new Point(mouseX, mouseY));
  else if (mouseButton == RIGHT)
    graph.translate(mouseX - pmouseX, mouseY - pmouseY);
  else
    graph.scale(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  graph.scale(event.getCount() * 20);
}
