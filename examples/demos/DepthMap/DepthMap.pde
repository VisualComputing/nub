/**
 * Depth Map.
 * by Jean Pierre Charalambos.
 *
 * This example shows how to generate and display a depth map from a light point-of-view.
 *
 * The actual shadow mapping implementation is left as an exercise. Readers may refer to:
 * http://www.opengl-tutorial.org/intermediate-tutorials/tutorial-16-shadow-mapping/ and,
 * https://learnopengl.com/Advanced-Lighting/Shadows/Shadow-Mapping
 *
 * Press shift and pick (mouse move) a box to define it as the light source.
 * Press shift and scroll (mouse wheel) the light source to change the shadow map zFar plane.
 * Press ' ' to change the shadow map volume (ORTHOGRAPHIC / PERSPECTIVE).
 * Press 'p' to toggle the scene perspective.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene.Type shadowMapType = Scene.Type.ORTHOGRAPHIC;
Scene scene, shadowMapScene;
Node[] shapes;
PGraphics shadowMap;
PShader depthShader;
float zNear = 50;
float zFar = 700;
int w = 700;
int h = 700;

void settings() {
  size(w, h, P3D);
}

void setup() {
  shadowMap = createGraphics(w / 2, h / 2, P3D);
  depthShader = loadShader("depth.glsl");
  depthShader.set("near", zNear);
  depthShader.set("far", zFar);
  shadowMap.shader(depthShader);

  scene = new Scene(this);
  scene.enableHint(Scene.BACKGROUND, color(75, 25, 15));
  scene.setRadius(max(w, h));
  scene.fit(1);

  shapes = new Node[20];
  for (int i = 0; i < shapes.length; i++) {
    shapes[i] = new Node() {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        if (pg == shadowMap)
          pg.noStroke();
        else {
          pg.strokeWeight(3);
          pg.stroke(0, 255, 255);
        }
        pg.fill(255, 0, 0);
        pg.box(80);
        pg.popStyle();
      }
    };
    //shapes[i].setPickingPolicy(Node.PickingPolicy.BULLSEYE);
    //shapes[i].enableHint(Node.BULLSEYE);
    shapes[i].configHint(Node.FRUSTUM, shadowMap, shadowMapType, zNear, zFar);
    scene.randomize(shapes[i]);
    //shapes[i].disableHint(Node.HIGHLIGHT);
  }

  scene.tag("light", shapes[(int) random(0, shapes.length - 1)]);
  scene.node("light").toggleHint(Node.SHAPE | Node.FRUSTUM | Node.AXES);
  scene.node("light").setOrientation(Quaternion.from(Vector.plusK, scene.node("light").position()));

  shadowMapScene = new Scene(shadowMap, scene.node("light"));
  shadowMapScene.resetHint();
  shadowMapScene.enableHint(Scene.BACKGROUND, color(140, 160, 125));
  shadowMapScene.enablePicking(false);
  //shadowMapScene.setRadius(300);
  shadowMapScene.setType(shadowMapType);
}

void draw() {
  // 1. Fill in and display front-buffer
  scene.render();
  // 2. Fill in shadow map using the light point of view
  if (scene.isTagValid("light")) {
    shadowMapScene.display(w / 2, h / 2);
  }
}

void mouseMoved(MouseEvent event) {
  if (event.isShiftDown()) {
    if (scene.isTagValid("light"))
      scene.node("light").toggleHint(Node.SHAPE | Node.FRUSTUM | Node.AXES);
    // no calling mouseTag since we need to immediately update the tagged node
    scene.updateMouseTag("light");
    if (scene.isTagValid("light")) {
      scene.node("light").toggleHint(Node.SHAPE | Node.FRUSTUM | Node.AXES);
      shadowMapScene.setEye(scene.node("light"));
    }
  } else
    scene.mouseTag();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.mouseSpin();
  else if (mouseButton == RIGHT)
    scene.mouseTranslate();
  else
    scene.moveForward(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  if (event.isShiftDown() && scene.isTagValid("light")) {
    depthShader.set("far", zFar += event.getCount() * 20);
    scene.node("light").configHint(Node.FRUSTUM, shadowMap, shadowMapType, zNear, zFar);
  } else
    scene.scale(event.getCount() * 20);
}

void keyPressed() {
  if (key == ' ' && scene.isTagValid("light")) {
    shadowMapType = shadowMapType == Scene.Type.ORTHOGRAPHIC ? Scene.Type.PERSPECTIVE : Scene.Type.ORTHOGRAPHIC;
    scene.node("light").configHint(Node.FRUSTUM, shadowMap, shadowMapType, zNear, zFar);
  }
  if (key == 'p')
    scene.togglePerspective();
}
