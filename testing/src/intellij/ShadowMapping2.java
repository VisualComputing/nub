package intellij;

import nub.core.Node;
import nub.primitives.Matrix;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import processing.opengl.PShader;

public class ShadowMapping2 extends PApplet {
  Scene scene, shadowMapScene;
  Node landscape1, landscape2, landscape3, light;
  TimingTask animation;
  PGraphics shadowMap;
  float fov = THIRD_PI;
  Matrix biasMatrix = new Matrix(
          0.5f, 0, 0, 0,
          0, 0.5f, 0, 0,
          0, 0, 0.5f, 0,
          0.5f, 0.5f, 0.5f, 1
  );
  Scene.Type shadowMapType = Scene.Type.ORTHOGRAPHIC;
  float zNear = 10;
  float zFar = 600;
  int w = 700;
  int h = 700;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.enableHint(Scene.BACKGROUND, color(0));
    scene.togglePerspective();
    scene.setRadius(max(w, h) / 3);
    scene.fit(1);
    Node floor = new Node() {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.noStroke();
        pg.fill(0xff222222);
        pg.box(360, 5, 360);
        pg.popStyle();
      }
    };
    floor.disableTagging();
    landscape1 = new Node() {
      @Override
      public void graphics(PGraphics pg) {
        if (!isCulled()) {
          pg.pushStyle();
          pg.noStroke();
          float offset = -frameCount * 0.01f;
          pg.fill(0xffff5500);
          for (int z = -5; z < 6; ++z)
            for (int x = -5; x < 6; ++x) {
              pg.pushMatrix();
              pg.translate(x * 12, sin(offset + x) * 20 + cos(offset + z) * 20, z * 12);
              pg.box(10, 100, 10);
              pg.popMatrix();
            }
          pg.popStyle();
        }
      }
    };
    landscape1.setPickingPolicy(Node.SHAPE);
    landscape2 = new Node() {
      @Override
      public void graphics(PGraphics pg) {
        if (!isCulled()) {
          pg.pushStyle();
          pg.noStroke();
          float angle = -frameCount * 0.0015f, rotation = TWO_PI / 20;
          pg.fill(0xffff5500);
          for (int n = 0; n < 20; ++n, angle += rotation) {
            pg.pushMatrix();
            pg.translate(sin(angle) * 70, cos(angle * 4) * 10, cos(angle) * 70);
            pg.box(10, 100, 10);
            pg.popMatrix();
          }
          pg.fill(0xff0055ff);
          pg.sphere(50);
          pg.popStyle();
        }
      }
    };
    landscape2.setPickingPolicy(Node.SHAPE);
    landscape2.cull();
    landscape3 = new Node() {
      @Override
      public void graphics(PGraphics pg) {
        if (!isCulled()) {
          pg.pushStyle();
          pg.noStroke();
          float angle = -frameCount * 0.0015f, rotation = TWO_PI / 20;
          pg.fill(0xffff5500);
          for (int n = 0; n < 20; ++n, angle += rotation) {
            pg.pushMatrix();
            pg.translate(sin(angle) * 70, cos(angle) * 70, 0);
            pg.box(10, 10, 100);
            pg.popMatrix();
          }
          pg.fill(0xff00ff55);
          pg.sphere(50);
          pg.popStyle();
        }
      }
    };
    landscape3.setPickingPolicy(Node.SHAPE);
    landscape3.cull();
    // initShadowPass
    shadowMap = createGraphics(2048, 2048, P3D);
    // Testing the appearance of artifacts first
    //shadowMap.noSmooth();
    // initDefaultPass

    light = new Node();
    light.enableHint(Node.BULLSEYE | Node.AXES | Node.CAMERA);
    light.configHint(Node.FRUSTUM, shadowMap, shadowMapType, zNear, zFar);

    animation = new TimingTask() {
      @Override
      public void execute() {
        if (!scene.isTagged(light)) {
          float lightAngle = frameCount * 0.002f;
          light.setPosition(sin(lightAngle) * 160, 160, cos(lightAngle) * 160);
        }
        light.setYAxis(Vector.projectVectorOnAxis(light.yAxis(), Vector.plusJ));
        light.setZAxis(light.position());
      }
    };
    animation.run(60);

    shadowMapScene = new Scene(this, shadowMap, light);
    //shadowMapScene.enableHint(Scene.BACKGROUND, color(0xffffffff));
  }

  public void draw() {
    // shadowMapScene.render();
    /*
    // 1. Render the shadowmap from light node 'point-of-view'
    shadowMap.beginDraw();
    shadowMap.noStroke();
    shadowMap.background(0xffffffff); // Will set the depth to 1.0 (maximum depth)
    Scene.render(shadowMap, light, shadowMapType, zNear, zFar);
    shadowMap.endDraw();
     */

    scene.render();
    shadowMapScene.render();
  }

  public void keyPressed() {
    if (key == '1' || key == '2' || key == '3') {
      landscape1.cull(key != '1');
      landscape2.cull(key != '2');
      landscape3.cull(key != '3');
    } else if (key == ' ') {
      shadowMapType = shadowMapType == Scene.Type.ORTHOGRAPHIC ? Scene.Type.PERSPECTIVE : Scene.Type.ORTHOGRAPHIC;
      light.setMagnitude(shadowMapType == Scene.Type.ORTHOGRAPHIC ? 0.195f : tan(fov / 2));
    }
  }

  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.moveForward(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (event.isShiftDown()) {
      int shift = event.getCount() * 20;
      if (zFar + shift > zNear)
        zFar += shift;
    } else
      scene.scale(event.getCount() * 20);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.ShadowMapping2"});
  }
}
