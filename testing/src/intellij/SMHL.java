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

public class SMHL extends PApplet {
  Scene scene;
  //Scene shadowMapScene;
  ShadowScene shadowMapScene;
  Node n1, n2, n3, f, light;
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
  Graph.Type shadowMapType = Graph.Type.ORTHOGRAPHIC;
  float zNear = 10;
  float zFar = 600;
  int w = 700;
  int h = 700;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    //scene = new Scene(this, P3D, w, h);
    scene = new Scene(this);
    scene.enableHint(Scene.BACKGROUND, color(0));
    scene.togglePerspective();
    scene.setRadius(max(w, h) / 3);
    scene.fit(1);
    n1 = new Node(this::landscape1);
    /*
    n1 = new Node() {
      @Override
      public void graphics(PGraphics pg) {
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
    };
    // */
    n1.tagging = false;
    n2 = new Node(this::landscape2);
    /*
    n2 = new Node() {
      @Override
      public void graphics(PGraphics pg) {
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
    };
    // */
    n2.tagging = false;
    n2.cull = true;
    n3 = new Node(this::landscape3);
    /*
    n3 = new Node() {
      @Override
      public void graphics(PGraphics pg) {
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
      }
    };
    // */
    n3.tagging = false;
    n3.cull = true;
    new Node(this::floor).tagging = false;
    /*
    f = new Node() {
      @Override
      public void graphics(PGraphics pg) {
        pg.noStroke();
        pg.fill(0xff222222);
        pg.box(360, 5, 360);
      }
    };
    f.disableTagging();
    // */

    // initShadowPass
    //String depthPath = Paths.get("testing/data/depth/depth_linear.glsl").toAbsolutePath().toString();
    String depthPath = Paths.get("testing/data/depth/depth_frag.glsl").toAbsolutePath().toString();
    depthShader = loadShader(depthPath);
    shadowMap = createGraphics(2048, 2048, P3D);
    if (shadowMap.parent == this)
      println("win!");
    else
      println("not just yet!");
    shadowMap.shader(depthShader);
    // Testing the appearance of artifacts first
    //shadowMap.noSmooth();

    light = new Node();
    light.togglePickingMode(Node.AXES | Node.CAMERA);
    light.enableHint(Node.BULLSEYE | Node.AXES | Node.CAMERA);
    light.configHint(Node.FRUSTUM, shadowMap, shadowMapType, zNear, zFar);
    //light.resetHint();
    //light.setMagnitude(0.195);

    animation = new TimingTask() {
      @Override
      public void execute() {
        if (!scene.isTagged(light)) {
          float lightAngle = frameCount * 0.002f;
          light.setPosition(sin(lightAngle) * 160, 160, cos(lightAngle) * 160);
        }
        light.setYAxis(Vector.projectVectorOnAxis(light.yAxis(), new Vector(0, 1, 0)));
        light.setZAxis(new Vector(light.position().x(), light.position().y(), light.position().z()));
      }
    };
    animation.run(60);

    // initDefaultPass
    String shadowVertPath = Paths.get("testing/data/shadow/shadow_vert.glsl").toAbsolutePath().toString();
    String shadowFragPath = Paths.get("testing/data/shadow/shadow_frag.glsl").toAbsolutePath().toString();
    shadowShader = loadShader(shadowFragPath, shadowVertPath);
    scene.context().shader(shadowShader);
    noStroke();

    // TODO key line to improving fps from 15 to 25!
    //scene.enablePicking(false);

    ///*
    //shadowMapScene = new Scene(this, shadowMap, light);
    shadowMapScene = new ShadowScene(shadowMap, light);
    shadowMapScene.resetHint();
    shadowMapScene.enableHint(Scene.BACKGROUND, 0xffffffff);
    shadowMapScene.picking = false;
    //shadowMapScene.setRadius(300);
    shadowMapScene.setType(shadowMapType);
    // */

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

  public void floor(PGraphics pg) {
    pg.noStroke();
    pg.fill(0xff222222);
    pg.box(360, 5, 360);
  }

  public void draw() {
    // 1. Render the shadowmap from light node 'point-of-view'
    /*
    shadowMap.beginDraw();
    shadowMap.noStroke();
    shadowMap.background(0xffffffff); // Will set the depth to 1.0 (maximum depth)
    Scene.render(shadowMap, light, shadowMapType, zNear, zFar);
    shadowMap.endDraw();
    // */
    shadowMapScene.render();

    // 2. Render the scene from the scene.eye() node
    //scene.openContext();
    Matrix projectionView = Scene.projectionView(light, shadowMapType, shadowMap.width, shadowMap.height, zNear, zFar);
    Matrix lightMatrix = Matrix.multiply(biasMatrix, projectionView);
    Scene.setUniform(shadowShader, "shadowTransform", Matrix.multiply(lightMatrix, scene.eye().viewInverse()));
    Vector lightDirection = scene.eye().displacement(light.zAxis(false));
    Scene.setUniform(shadowShader, "lightDirection", lightDirection);
    shadowShader.set("shadowMap", shadowMap);
    scene.render();
    //scene.closeContext();
    //scene.image();
    //scene.display();
    println("-> frameRate: " + Scene.TimingHandler.frameRate + " (nub) " + frameRate + " (p5)");
  }

  public void keyPressed() {
    if (key == '1' || key == '2' || key == '3') {
      n1.cull = key != '1';
      n2.cull = key != '2';
      n3.cull = key != '3';
    } else if (key == ' ') {
      shadowMapType = shadowMapType == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC;
      light.setMagnitude(shadowMapType == Graph.Type.ORTHOGRAPHIC ? 0.195f : tan(fov / 2));
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

  public class ShadowScene extends Scene {
    public ShadowScene(PGraphics pGraphics, Node eye) {
      super(pGraphics, eye);
    }

    @Override
    public float zNear() {
      return zNear;
    }

    @Override
    public float zFar() {
      return zFar;
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.SMHL"});
  }
}
