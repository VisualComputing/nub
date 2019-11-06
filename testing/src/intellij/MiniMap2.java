package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

public class MiniMap2 extends PApplet {
  Scene scene, minimap, focus;
  Node[] models;
  // the sceneEye holds a graphics representation
  Node sceneEye;
  boolean displayMinimap = true;
  // whilst scene is either on-screen or not, the minimap is always off-screen
  // test both cases here:
  // when onScreen = true, the minimap countour is not rendered and it gives:
  /*
  The pixels array is null.
  */
  boolean onScreen = true;

  int w = 1200;
  int h = 1200;

  //Choose FX2D, JAVA2D, P2D or P3D
  String renderer = P2D;

  public void settings() {
    size(w, h, renderer);
  }

  public void setup() {
    scene = onScreen ? new Scene(this) : new Scene(this, renderer);
    scene.setRadius(1000);
    // set a detached eye node

    scene.setEye(new Node());
    if (scene.is2D())
      rectMode(CENTER);
    scene.fit(1);
    models = new Node[6];
    for (int i = 0; i < models.length; i++) {
      if ((i & 1) == 0) {
        //models[i] = new Node(scene, shape());
        models[i] = new Node(scene);
        models[i].setShape(shape());
      } else {
        models[i] = new Node(scene) {
          int _faces = (int) MiniMap2.this.random(3, 15), _color = color(MiniMap2.this.random(255), MiniMap2.this.random(255), MiniMap2.this.random(255));
          @Override
          public void graphics(PGraphics pg) {
            pg.pushStyle();
            pg.fill(_color);
            Scene.drawTorusSolenoid(pg, _faces, scene.radius() / 30);
            pg.popStyle();
          }
        };
      }
      models[i].setPickingThreshold(0);
      scene.randomize(models[i]);
    }

    // Note that we pass the upper left corner coordinates where the scene1
    // is to be drawn (see drawing code below) to its constructor.
    minimap = new Scene(this, renderer, w / 2, h / 2, w / 2, h / 2);
    minimap.setRadius(2000);
    // set a detached eye node
    minimap.setEye(new Node());
    //if (renderer == P3D)
    //minimap.setType(Graph.Type.ORTHOGRAPHIC);
    minimap.fit(1);
    sceneEye = new Node(minimap) {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.fill(255, isTagged() ? 25 : 50, 0, 125);
        pg.stroke(0, 0, 255);
        pg.strokeWeight(2);
        // comment and the above errors disappear
        // it has to do with the near plane drawing ant the texture
        // see Scene._drawPlane
        minimap.drawFrustum(pg, scene);
        pg.popStyle();
      }
    };
  }

  PShape shape() {
    PShape shape = renderer == P3D ? createShape(BOX, 60) : createShape(RECT, 0, 0, 80, 100);
    shape.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return shape;
  }

  public void keyPressed() {
    if (key == ' ')
      displayMinimap = !displayMinimap;
    if (key == 'f')
      focus.fit(1);
    if (key == 't')
      if (renderer == P3D)
        if (focus.type() == Graph.Type.PERSPECTIVE)
          focus.setType(Graph.Type.ORTHOGRAPHIC);
        else
          focus.setType(Graph.Type.PERSPECTIVE);
  }

  @Override
  public void mouseMoved() {
    focus.tag();
  }

  @Override
  public void mouseDragged() {
    if (mouseButton == LEFT)
      focus.spin();
    else if (mouseButton == RIGHT)
      focus.translate();
    else
      focus.scale(focus.mouseDX());
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    if (renderer == P3D)
      focus.moveForward(event.getCount() * 50);
    else
      focus.scale(event.getCount() * 50);
  }

  @Override
  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focus();
      else
        focus.align();
  }

  public void draw() {
    focus = displayMinimap ? (mouseX > w / 2 && mouseY > h / 2) ? minimap : scene : scene;
    Node.sync(scene.eye(), sceneEye);
    background(75, 25, 15);
    if (scene.isOffscreen()) {
      scene.beginDraw();
      scene.context().background(75, 25, 15);
      scene.drawAxes();
      scene.render();
      scene.endDraw();
      scene.display();
    } else {
      scene.drawAxes();
      scene.render();
    }
    if (displayMinimap) {
      scene.shift(minimap);
      if (!scene.isOffscreen())
        scene.beginHUD();
      minimap.beginDraw();
      minimap.context().background(125, 80, 90);
      minimap.drawAxes();
      minimap.render();
      minimap.endDraw();
      minimap.display();
      if (!scene.isOffscreen())
        scene.endHUD();
      minimap.shift(scene);
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.MiniMap2"});
  }
}
