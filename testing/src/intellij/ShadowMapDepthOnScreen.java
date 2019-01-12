package intellij;

import frames.core.Graph;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import processing.opengl.PShader;

public class ShadowMapDepthOnScreen extends PApplet {
  Graph.Type shadowMapType = Graph.Type.ORTHOGRAPHIC;
  Scene scene;
  Shape[] shapes;
  PGraphics shadowMap;
  PShader depthShader;
  boolean one;
  float zNear = 50;
  float zFar = 1000;
  int w = 1000;
  int h = 1000;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setRadius(max(w, h));
    shapes = new Shape[20];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Shape(scene) {
        @Override
        public void setGraphics(PGraphics pg) {
          pg.pushStyle();
          if (scene.trackedFrame("light") == this) {
            Scene.drawAxes(pg, 150);
            pg.fill(0, scene.isTrackedFrame(this) ? 255 : 0, 255, 120);
            Scene.drawFrustum(pg, shadowMap, shadowMapType, this, zNear, zFar);

          } else {
            if (pg == shadowMap)
              pg.noStroke();
            else {
              pg.strokeWeight(3);
              pg.stroke(0, 255, 255);
            }
            pg.fill(255, 0, 0);
            pg.box(80);
          }
          pg.popStyle();
        }

        @Override
        public void interact(Object... gesture) {
          if (gesture.length == 1)
            if (gesture[0] instanceof Integer)
              if (zFar + (Integer) gesture[0] > zNear) {
                zFar += (Integer) gesture[0];
                if (one)
                  depthShader.set("maxDepth", zFar - zNear);
                else
                  depthShader.set("far", zFar);
              }
        }
      };
      shapes[i].randomize();
      shapes[i].setHighlighting(Shape.Highlighting.NONE);
    }
    scene.setRadius(scene.radius() * 1.2f);
    scene.fit(1);

    shadowMap = createGraphics(w / 2, h / 2, P3D);
    setShader(false);

    scene.setTrackedFrame("light", shapes[(int) random(0, shapes.length - 1)]);
    scene.trackedFrame("light").setOrientation(new Quaternion(new Vector(0, 0, 1), scene.trackedFrame("light").position()));
  }

  public void setShader() {
    setShader(true);
  }

  public void setShader(boolean one_) {
    one = one_;
    if (one) {
      depthShader = loadShader("/home/pierre/IdeaProjects/frames/testing/data/dof/depth.glsl");
      depthShader.set("maxDepth", 10);
      println("version 1 of the shader");
    } else {
      depthShader = loadShader("/home/pierre/IdeaProjects/frames/testing/data/depth/depth_linear.glsl");
      depthShader.set("near", zNear);
      depthShader.set("far", zFar);
      println("version 2 of the shader");
    }
    shadowMap.shader(depthShader);
  }

  public void draw() {
    background(75, 25, 15);
    // 1. Fill in and display front-buffer
    scene.drawAxes();
    scene.render();
    // 2. Fill in shadow map using the light point of view
    if (scene.trackedFrame("light") != null) {
      shadowMap.beginDraw();
      shadowMap.background(140, 160, 125);
      scene.render(shadowMap, shadowMapType, scene.trackedFrame("light"), zNear, zFar);
      shadowMap.endDraw();
      // 3. Display shadow map
      scene.beginHUD();
      image(shadowMap, w / 2, h / 2);
      scene.endHUD();
    }
  }

  public void mouseMoved(MouseEvent event) {
    if (event.isShiftDown())
      scene.cast("light");
    else
      scene.cast();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.translate();
    else
      scene.moveForward(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (event.isShiftDown())
      // application control of the light: set the zfar plan of the light
      // it is implemented as a custom behavior by frame.interact()
      scene.control("light", event.getCount() * 20);
    else
      scene.scale(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == 'f')
      scene.fitFOV(1);
    if (key == 'o') {
      one = !one;
      setShader(one);
    }
    if (key == 'p')
      scene.togglePerspective();
    if (key == 't')
      shadowMapType = shadowMapType == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ShadowMapDepthOnScreen"});
  }
}
