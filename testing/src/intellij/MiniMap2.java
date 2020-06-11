package intellij;

import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

public class MiniMap2 extends PApplet {
  Scene scene, minimap, focus;
  Node[] models;
  boolean displayMinimap = true;
  // whilst scene is either on-screen or not, the minimap is always off-screen
  // test both cases here:
  boolean onScreen = true;
  boolean interactiveEye;

  int w = 1200;
  int h = 1200;

  //Choose FX2D, JAVA2D, P2D or P3D
  String renderer = P2D;

  public void settings() {
    size(w, h, renderer);
  }

  public void setup() {
    /*
    Node eye = new Node() {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        Scene.drawFrustum(pg, scene);
        pg.popStyle();
      }
    };
    scene = onScreen ? new Scene(this, eye) : new Scene(this, renderer, eye);
    // */
    scene = onScreen ? new Scene(this) : new Scene(this, renderer);
    scene.eye().enableHint(Node.BULLSEYE);
    scene.setRadius(1000);
    rectMode(CENTER);
    scene.fit(1);
    models = new Node[30];
    for (int i = 0; i < models.length; i++) {
      if ((i & 1) == 0) {
        models[i] = new Node(shape());
      } else {
        models[i] = new Node() {
          int _faces = (int) MiniMap2.this.random(3, 15);
          // We need to call the PApplet random function instead of the node random version
          int _color = color(MiniMap2.this.random(255), MiniMap2.this.random(255), MiniMap2.this.random(255));

          @Override
          public void graphics(PGraphics pg) {
            pg.pushStyle();
            pg.fill(_color);
            Scene.drawTorusSolenoid(pg, _faces, scene.radius() / 30);
            pg.popStyle();
          }
        };
      }
      scene.randomize(models[i]);
    }
    // Note that we pass the upper left corner coordinates where the minimap
    // is to be drawn (see drawing code below) to its constructor.
    minimap = new Scene(this, renderer, w / 2, h / 2);
    minimap.setRadius(2000);
    if (renderer == P3D)
      minimap.togglePerspective();
    minimap.fit(1);
    scene.eye().setBullsEyeSize(30);
  }

  PShape shape() {
    PShape shape = renderer == P3D ? createShape(BOX, 60) : createShape(RECT, 0, 0, 80, 100);
    shape.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return shape;
  }

  public void keyPressed() {
    if (key == ' ')
      displayMinimap = !displayMinimap;
    if (key == 'i') {
      interactiveEye = !interactiveEye;
      if (interactiveEye)
        minimap.tag(scene.eye());
      else
        minimap.untag(scene.eye());
    }
    if (key == 'f')
      focus.fit(1);
    if (key == 't')
      focus.togglePerspective();
  }

  public void mouseMoved() {
    if (!interactiveEye || focus == scene)
      focus.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      focus.mouseSpin();
    else if (mouseButton == RIGHT)
      focus.mouseTranslate();
    else
      focus.scale(focus.mouseDX());
  }

  public void mouseWheel(MouseEvent event) {
    if (renderer == P3D)
      focus.moveForward(event.getCount() * 40);
    else
      focus.scale(event.getCount() * 40);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focus();
      else
        focus.align();
  }

  public void draw() {
    focus = displayMinimap ? (mouseX > w / 2 && mouseY > h / 2) ? minimap : scene : scene;
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
      if (!scene.isOffscreen())
        scene.beginHUD();
      minimap.beginDraw();
      minimap.context().background(125, 80, 90);
      minimap.drawAxes();
      minimap.render();
      // /*
      // draw scene eye
      minimap.context().fill(minimap.isTagged(scene.eye()) ? 255 : 25, minimap.isTagged(scene.eye()) ? 0 : 255, 255, 125);
      minimap.context().strokeWeight(2);
      minimap.context().stroke(0, 0, 255);
      minimap.drawFrustum(scene);
      // */
      minimap.endDraw();
      minimap.display(w / 2, h / 2);
      if (!scene.isOffscreen())
        scene.endHUD();
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.MiniMap2"});
  }
}
