import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

boolean relativeToGraph = true;
int rows = 2, cols = 2;
int n = rows * cols;
Scene[] scenes = new Scene[n];
Scene focus;
int w = 1400, h = 1400;
int[] strokeColors = new int[n];
int[] fillColors = new int[n];

void settings() {
  size(w, h, P3D);
}

void setup() {
  //create the scenes
  for (int i = 0; i < n; i++) {
    scenes[i] = new Scene(createGraphics(w / cols, h / rows, P3D));
    scenes[i].setRadius(50);
    strokeColors[i] = color(random(255), random(255), random(255));
    fillColors[i] = color(random(255), random(255), random(255));
    scenes[i].fit();
    if (Math.random() < 0.5) scenes[i].togglePerspective();
  }
  generateRandomNodes(10);
}

void draw() {
  for (int i = 0; i < n; i++) {
    focus = scenes[i].hasFocus() ? scenes[i] : focus;
    scenes[i].openContext();
    scenes[i].context().background(125);
    scenes[i].render();
    scenes[i].drawAxes();
    scenes[i].beginHUD();
    sceneHUD(i, scenes[i].context());
    scenes[i].endHUD();
    scenes[i].closeContext();
    scenes[i].image(i / rows * h / rows, (i % cols) * w / cols);
  }
}

void sceneHUD(int i, PGraphics pg) {
  pg.pushStyle();
  pg.stroke(255);
  pg.fill(255);
  pg.textSize(28);
  pg.text("On scene " + i + " radius : " + (int) (scenes[i].radius()), 10, pg.height / 10);
  pg.popStyle();
}

void generateRandomNodes(int n) {
  for (int i = 0; i < n; i++) {
    Node node = new CustomNode();
    node.randomize(new Vector(), 150, true);
    node.setMagnitude(1);
  }
}

class CustomNode extends Node {
  //Add some attributes
  float radiusInPixels = 50, radius;
  int strokeCol, fillCol;

  public CustomNode() {
    super();
    //without modifying Node class
    //for(Scene scene : scenes) setVisit(scene, (g , n) -> setupAttributesByGraph(g));
    //modifying the node class
    for (Scene scene : scenes) setBehavior(scene, this::setupAttributesByGraph);
    setShape(pg -> {
      pg.pushStyle();
      pg.stroke(strokeCol);
      pg.fill(fillCol);
      pg.box(radius);
      pg.popStyle();
    }
    );
  }

  public void setupAttributesByGraph(Graph g) {
    if (relativeToGraph) {
      //keep the size of the node relative to the graph such that it always occupies 10 pixels
      this.radius = this.radiusInPixels * g.sceneToPixelRatio(worldPosition());
      //change color based on Graph
      for (int i = 0; i < n; i++) {
        if (g == scenes[i]) {
          this.strokeCol = strokeColors[i];
          this.fillCol = fillColors[i];
        }
      }
    } else {
      this.radius = 10;
      this.strokeCol = color(255);
      this.fillCol = color(255, 255, 0);
    }
  }
}

void mouseMoved() {
  focus.tag();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    focus.spin();
  else if (mouseButton == RIGHT)
    focus.shift();
  else
    focus.zoom(focus.mouseDX());
}

void mouseWheel(MouseEvent event) {
  focus.moveForward(event.getCount() * 40);
}

void mouseClicked(MouseEvent event) {
  if (event.getCount() == 2)
    if (event.getButton() == LEFT)
      focus.focus();
    else
      focus.align();
}

void keyPressed() {
  if (key == ' ') {
    relativeToGraph = !relativeToGraph;
  }
}
