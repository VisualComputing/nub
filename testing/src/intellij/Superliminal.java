package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

public class Superliminal extends PApplet {
  Scene scene, lateralView, focus;
  Node[] cubes;
  boolean displayLateralView;
  // whilst scene1 is either on-screen or not; scene2 and scene3 are off-screen
  // test both cases here
  boolean onScreen = false;

  int w = 1920;
  int h = 1080;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene = onScreen ? new Scene(g) : new Scene(createGraphics(w, h, P3D));
    scene.eye().enableHint(Node.BOUNDS);
    scene.eye().tagging = false;
    scene.setRadius(200);
    scene.fit(1000);
    cubes = new Node[15];
    for (int i = 0; i < cubes.length; i++) {
      cubes[i] = new CustomNode();
      //shapes[i].setHUD(this::hud);
      //shapes[i].disablePicking(Node.SHAPE);
      //shapes[i].enableHint(Node.BULLSEYE);
      //shapes[i].disablePickingMode(Node.SHAPE);
      scene.randomize(cubes[i]);
    }

    // Note that we pass the upper left corner coordinates where the scene1
    // is to be drawn (see drawing code below) to its constructor.
    lateralView = new Scene(createGraphics(w / 2, h, P3D));
    lateralView.togglePerspective();
    lateralView.eye().tagging = false;
    lateralView.setRadius(400);
    lateralView.fit();

    /*
    int si = int(vertTexCoord.s * binsize);
    vec2(float(si) / binsize
    // */

    /*
    float w = 600;
    float binsize = 5;
    float vertTexCoord;
    float coef = w / binsize;
    for(float i = 0; i < w; i++) {
      vertTexCoord = i / w;
      int si = (int) (vertTexCoord * coef);
      println(vertTexCoord + " " + ((float)(si)) / coef);
    }
    // */

    /*
    float binsize = 5;
    //float vertTexCoord;
    for(float i = 0; i < 1; i+=1/600f) {
      //vertTexCoord = i;
      int si = (int) (i * binsize);
      println(i + " " + ((float)(si)) / binsize);
    }
    // */

    /*
    float w = 600;
    float binsize = 5;
    //float vertTexCoord;
    float coef = w / binsize;
    for(float i = 0; i < w; i++) {
      //vertTexCoord = i / w;
      int si = (int) (i * coef);
      println(i + " " + ((float)(si)) / coef);
    }
    // */

    /*
    float w = 600;
    float binsize = 5;
    float vertTexCoord;
    float coef = w / binsize;
    for(float i = 0; i < w; i++) {
      vertTexCoord = i / w;
      int si = (int) (vertTexCoord * binsize);
      println(i + " " + vertTexCoord + " " + ((float)(si)) / binsize);
    }
    // */
  }

  class CustomNode extends Node {
    //Add some attributes
    float radiusInPixels = 50, radius;
    int strokeCol = color(255, 255, 0), fillCol = color(0, 255, 255, 125);

    public CustomNode() {
      super();
      setShape(pg -> {
        //this.radius = this.radiusInPixels;
        this.radius = this.radiusInPixels * scene.sceneToPixelRatio(worldPosition());
        pg.pushStyle();
        pg.stroke(strokeCol);
        pg.fill(fillCol);
        pg.box(radius);
        pg.popStyle();
      });
    }
  }

  public void keyPressed() {
    if (key == ' ')
      displayLateralView = !displayLateralView;
    if (key == 'f')
      focus.fit(1000);
    if (key == 't') {
      if (focus == null)
        return;
      focus.togglePerspective();
    }
  }

  @Override
  public void mouseMoved() {
    if (focus == null)
      return;
    focus.tag();
  }

  @Override
  public void mouseDragged() {
    if (focus == null)
      return;
    if (mouseButton == LEFT)
      focus.spin();
    else if (mouseButton == RIGHT)
      focus.shift();
    else {
      focus.zoom(mouseX - pmouseX);
    }
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    Node node = scene.node();
    Node eye = scene.eye();
    if(node != null) {
      float amount = event.getCount();
      //scene.translateNode(node, 0, 0, amount / 50, Scene.inertia);
      ///*
      if (scene.type() == Graph.Type.PERSPECTIVE) {
        Vector v = Vector.subtract(node.worldPosition(), eye.worldPosition());
        v.normalize();
        v.multiply(amount * 10);
        node.translate(v, Scene.inertia);
      }
      else {
        scene.shift(node, 0, 0, amount / 50, Scene.inertia);
      }
      // */
    }
    else {
      focus.moveForward(event.getCount() * 20);
    }
  }

  @Override
  public void mouseClicked(MouseEvent event) {
    if (focus == null)
      return;
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focus();
      else
        focus.align();
  }

  public void draw() {
    focus = lateralView.hasFocus() ? lateralView : scene;
    scene.openContext();
    scene.context().background(75, 25, 15);
    scene.render();
    scene.closeContext();
    scene.image();
    //stroke(0, 225, 15);
    //scene.drawGrid();
    if (displayLateralView) {
      lateralView.openContext();
      lateralView.context().background(75, 25, 175);
      lateralView.drawAxes();
      lateralView.render();
      lateralView.closeContext();
      lateralView.image(w / 2, 0);
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.Superliminal"});
  }
}
