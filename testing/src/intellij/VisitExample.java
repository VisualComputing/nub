package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class VisitExample extends PApplet {
  /**
   * This example modifies the visual representation on a Node
   * depending on the scene it is drawn.
   */

  boolean relativeToGraph = true;
  int rows = 2, cols = 2;
  int n = rows * cols;
  Scene[] scenes = new Scene[n];
  Scene focus;
  int w = 1400, h = 1400;
  int[] strokeColors = new int[n];
  int[] fillColors = new int[n];

  public void settings() {
    size(w, h, P3D);
  }

  @Override
  public void setup() {
    //create the scenes
    for(int i = 0; i < n; i++){
      final int idx = i;
      scenes[i] = new Scene(createGraphics(w / cols, h / rows, P3D));
      scenes[i].setBounds(50);
      strokeColors[i] = color(random(255), random(255), random(255));
      fillColors[i] = color(random(255), random(255), random(255));
      scenes[i].setHUD((pg) -> sceneHUD(idx, pg));
      scenes[i].fit();
      if(Math.random() < 0.5) scenes[i].togglePerspective();
      scenes[i].enableHint(Scene.BACKGROUND | Scene.AXES);
      scenes[i].configHint(Scene.BACKGROUND, random(0, 100));

    }
    generateRandomNodes(10);
  }

  @Override
  public void draw(){
    for(int i = 0; i < n; i++){
      focus = scenes[i].hasMouseFocus() ? scenes[i] : focus;
      scenes[i].display(i / rows * h / rows, (i % cols) * w / cols);
    }
  }

  public void sceneHUD(int i, PGraphics pg) {
    pg.pushStyle();
    pg.stroke(255);
    pg.fill(255);
    pg.text("On scene " + i + " radius : " + (int) (scenes[i].radius()), 10, pg.height / 10);
    pg.popStyle();
  }

  public void generateRandomNodes(int n){
    for(int i = 0; i < n; i++){
      Node node = new CustomNode();
      node.randomize(new Vector(), 150,  true);
      node.setScaling(1);
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
      for (Scene scene : scenes) setVisit(scene, this::setupAttributesByGraph);
      setShape(pg -> {
        pg.pushStyle();
        pg.stroke(strokeCol);
        pg.fill(fillCol);
        pg.box(radius);
        pg.popStyle();
      });
    }

    public void setupAttributesByGraph(Graph g) {
      if (relativeToGraph) {
        //keep the size of the node relative to the graph such that it always occupies 10 pixels
        this.radius = this.radiusInPixels * g.sceneToPixelRatio(position());
        //change color based on Graph
        for (int i = 0; i < n; i++) {
          if (g == scenes[i]) {
            this.strokeCol = strokeColors[i];
            this.fillCol = fillColors[i];
          }
        }
      } else{
        this.radius = 10;
        this.strokeCol = color(255);
        this.fillCol = color(255,255, 0);
      }
    }
  }

  @Override
  public void mouseMoved(){
    focus.mouseTag();
  }

  @Override
  public void mouseDragged(){
    if(mouseButton == LEFT)
      focus.mouseSpin();
    else if(mouseButton == RIGHT)
      focus.mouseTranslate();
    else
      focus.scale(focus.mouseDX());
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    focus.moveForward(event.getCount() * 40);
  }

  @Override
  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focus();
      else
        focus.align();
  }

  @Override
  public void keyPressed(){
    if (key == ' ') {
      relativeToGraph = !relativeToGraph;
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.VisitExample"});
  }
}
