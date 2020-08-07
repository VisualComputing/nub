package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Matrix;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import processing.opengl.PShader;

import java.nio.file.Paths;

public class ShadowMapping extends PApplet {
  Scene scene;
  Scene shadowMapScene;
  Node n1, n2, n3, light;
  TimingTask animation;
  PShader depthShader;
  PShader shadowShader;
  PGraphics shadowMap;
  float fov = THIRD_PI;
  Matrix biasMatrix = new Matrix(
          0.5f, 0, 0, 0,
          0, 0.5f, 0, 0,
          0, 0, 0.5f, 0,
          0.5f, 0.5f, 0.5f, 1
  );
  boolean debug;
  int w = 1200;
  int h = 1200;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    //scene = new Scene(createGraphics(w, h, P3D));
    scene = new Scene(this, max(w, h) / 3f);
    scene.enableHint(Scene.BACKGROUND, color(0));
    scene.togglePerspective();
    scene.fit(1);
    // initDefaultPass
    String shadowVertPath = Paths.get("testing/data/shadow/shadow_vert.glsl").toAbsolutePath().toString();
    String shadowFragPath = Paths.get("testing/data/shadow/shadow_frag.glsl").toAbsolutePath().toString();
    shadowShader = loadShader(shadowFragPath, shadowVertPath);
    scene.context().shader(shadowShader);
    n1 = new Node(this::landscape1);
    n1.tagging = false;
    n2 = new Node(this::landscape2);
    n2.tagging = false;
    n2.cull = true;
    n3 = new Node(this::landscape3);
    n3.tagging = false;
    n3.cull = true;
    new Node(this::floor).tagging = false;
    light = new Node();
    light.enableHint(Node.BULLSEYE | Node.AXES | Node.CAMERA);
    // initShadowPass
    //String depthPath = Paths.get("testing/data/depth/depth_linear.glsl").toAbsolutePath().toString();
    String depthPath = Paths.get("testing/data/depth/depth_frag.glsl").toAbsolutePath().toString();
    depthShader = loadShader(depthPath);
    shadowMap = createGraphics(2048, 2048, P3D);
    shadowMap.shader(depthShader);
    // Testing the appearance of artifacts first
    //shadowMap.noSmooth();
    shadowMapScene = new Scene(shadowMap, light, 10, 600);
    shadowMapScene.setType(Graph.Type.ORTHOGRAPHIC);
    shadowMapScene.enableHint(Scene.BACKGROUND, 0xffffffff);
    shadowMapScene.picking = false;
    animation = new TimingTask(() -> {
      if (!scene.isTagged(light)) {
        float lightAngle = frameCount * 0.002f;
        light.setPosition(sin(lightAngle) * 160, 160, cos(lightAngle) * 160);
      }
      light.setYAxis(Vector.projectVectorOnAxis(light.yAxis(), Vector.plusJ));
      light.setZAxis(light.position());
    });
    animation.run(60);
    frameRate(1000);
  }

  public void landscape1(PGraphics pg) {
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
  }

  public void landscape2(PGraphics pg) {
    pg.noStroke();
    float angle = -frameCount * 0.0015f, rotation = TWO_PI / 20;
    pg.fill(0xffff5500);
    for (int n = 0; n < 20; ++n, angle += rotation) {
      pg.pushMatrix();
      pg.noStroke();
      pg.translate(sin(angle) * 70, cos(angle * 4) * 10, cos(angle) * 70);
      pg.box(10, 100, 10);
      pg.popMatrix();
    }
    pg.fill(0xff0055ff);
    pg.sphere(50);
  }

  public void landscape3(PGraphics pg) {
    pg.noStroke();
    float angle = -frameCount * 0.0015f, rotation = TWO_PI / 20;
    pg.fill(0xffff5500);
    for(int n = 0; n < 20; ++n, angle += rotation) {
      pg.pushMatrix();
      pg.translate(sin(angle) * 70, cos(angle) * 70, 0);
      pg.box(10, 10, 100);
      pg.popMatrix();
    }
    pg.fill(0xff00ff55);
    pg.sphere(50);
  }

  public void floor(PGraphics pg) {
    pg.noStroke();
    pg.fill(0xff222222);
    pg.box(360, 5, 360);
  }

  public void draw() {
    // 1. Render the shadowmap
    shadowMapScene.render();
    // 2. Display the scene
    if (!debug) {
      Matrix lightMatrix = Matrix.multiply(biasMatrix, shadowMapScene.projectionView());
      Scene.setUniform(shadowShader, "shadowTransform", Matrix.multiply(lightMatrix, scene.eye().viewInverse()));
      Vector lightDirection = scene.eye().displacement(light.zAxis(false));
      Scene.setUniform(shadowShader, "lightDirection", lightDirection);
      shadowShader.set("shadowMap", shadowMap);
    }
    scene.display();
  }

  public void keyPressed() {
    if (key == '1' || key == '2' || key == '3') {
      n1.cull = key != '1';
      n2.cull = key != '2';
      n3.cull = key != '3';
    } else if (key == ' ') {
      if (shadowMapScene.type() == Graph.Type.PERSPECTIVE) {
        shadowMapScene.setType(Graph.Type.ORTHOGRAPHIC);
        light.setMagnitude(1);
      }
      else {
        shadowMapScene.setType(Graph.Type.PERSPECTIVE);
        light.setMagnitude(tan(fov / 2));
      }
    } else if (key == 'd') {
      light.toggleHint(Node.BULLSEYE | Node.AXES | Node.CAMERA | Node.BOUNDS);
      debug = !debug;
      if (debug)
        resetShader();
      else
        shader(shadowShader);
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
      float zNear = shadowMapScene.zNear();
      float zFar = shadowMapScene.zFar();
      shadowMapScene.setBounds(zNear, zFar + shift);
    } else
      scene.scale(event.getCount() * 20);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.ShadowMapping"});
  }
}
