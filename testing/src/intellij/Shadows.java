package intellij;

import nub.core.Graph;
import nub.core.MatrixHandler;
import nub.core.Node;
import nub.primitives.Matrix;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PMatrix3D;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;

public class Shadows extends PApplet {
  // ported to nub from: https://forum.processing.org/two/discussion/12775/simple-shadow-mapping
  Scene scene;
  Node nodeLandscape, light;
  PShader depthShader;
  PShader shadowShader;
  PGraphics shadowMap;
  float fov = THIRD_PI;
  Matrix biasMatrix = new Matrix(
      0.5f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.5f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.5f, 0.0f,
      0.5f, 0.5f, 0.5f, 1.0f
  );
  boolean debug;
  Graph.Type shadowMapType = Graph.Type.ORTHOGRAPHIC;
  int landscape = 1;
  float zNear = 10;
  float zFar = 1000;
  int w = 1000;
  int h = 1000;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.togglePerspective();
    scene.setRadius(max(w, h)/3);
    scene.fit(1);
    nodeLandscape = new Node(scene) {
      @Override
      public boolean graphics(PGraphics canvas) {
        canvas.fill(0xffff5500);
        canvas.box(40, 40, 100);
        //canvas.fill(0xff00ff55);
        //canvas.sphere(30);
        canvas.fill(0xff222222);
        canvas.box(360, 5, 360);
        return true;
      }
    };
    light = new Node(scene) {
      @Override
      public boolean graphics(PGraphics pg) {
        pg.pushStyle();
        if(debug) {
          pg.fill(0, scene.isTrackedNode(this) ? 255 : 0, 255, 120);
          Scene.drawFrustum(pg, shadowMap, shadowMapType, this, zNear, zFar);
        }
        Scene.drawAxes(pg, 300);
        pg.pushStyle();
        return true;
      }
    };
    light.setMagnitude(400f / 2048f);
    // initShadowPass
    depthShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/depth_gen/depth_frag.glsl", "/home/pierre/IdeaProjects/nubjs/testing/data/depth_gen/depth_vert.glsl");
    shadowMap = createGraphics(2048, 2048, P3D);
    shadowMap.shader(depthShader);
    // TODO testing the appearance of artifacts first
    //shadowMap.noSmooth();
    shadowMap.resetMatrix();

    // initDefaultPass
    shadowShader = loadShader("/home/pierre/IdeaProjects/nubjs/testing/data/shadow_gen/shadow_frag.glsl", "/home/pierre/IdeaProjects/nubjs/testing/data/shadow_gen/shadow_vert.glsl");
    shader(shadowShader);
    GLSLMatrixHandler glslMatrixHandler = new GLSLMatrixHandler(scene, shadowShader);
    scene.setMatrixHandler(glslMatrixHandler);
    noStroke();
    resetMatrix();
  }

  public void draw() {
    // 1. Calculate the light position and orientation
    float lightAngle = frameCount * 0.002f;
    light.setPosition(sin(lightAngle) * 160, 160, cos(lightAngle) * 160);
    light.setYAxis(Vector.projectVectorOnAxis(light.yAxis(), new Vector(0,1,0)));
    light.setZAxis(new Vector(light.position().x(), light.position().y(), light.position().z()));

    // 2. Render the shadowmap from light node 'point-of-view'
    shadowMap.beginDraw();
    shadowMap.noStroke();
    shadowMap.background(0xffffffff); // Will set the depth to 1.0 (maximum depth)
    Matrix projectionView = light.projectionView(shadowMapType, shadowMap.width, shadowMap.height, zNear, zFar);
    Scene.setUniform(depthShader, "light_transform", projectionView);
    scene.render(shadowMap, shadowMapType, light, zNear, zFar);
    shadowMap.endDraw();

    // 3. Render the scene from the scene.eye() node
    background(0xff222222);
    if(!debug) {
      //Matrix projectionView = light.projectionView(shadowMapType, shadowMap.width, shadowMap.height, zNear, zFar);
      Matrix lightMatrix = Matrix.multiply(biasMatrix, projectionView);
      // TODO: how to avoid calling g.modelviewInv?
      //lightMatrix.apply(Scene.toMatrix(((PGraphicsOpenGL) g).modelviewInv));
      Scene.setUniform(shadowShader, "shadowTransform", lightMatrix);
      Vector lightDirection = scene.eye().displacement(light.zAxis(false));
      Scene.setUniform(shadowShader, "lightDirection", lightDirection);
      shadowShader.set("shadowMap", shadowMap);
    }
    scene.render();
  }

  public void keyPressed() {
    if(key != CODED) {
      if(key >= '1' && key <= '3')
        landscape = key - '0';
      else if(key == ' ') {
        shadowMapType = shadowMapType == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC;
        light.setMagnitude(shadowMapType == Graph.Type.ORTHOGRAPHIC ? 400f / 2048f : tan(fov / 2));
      }
      else if(key == 'd') {
        debug = !debug;
        if(debug)
          resetShader();
        else
          shader(shadowShader);
      }
    }
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
    if (event.isShiftDown()) {
      int shift = event.getCount() * 20;
      if (zFar + shift > zNear)
        zFar += shift;
    }
    else
      scene.scale(event.getCount() * 20);
  }

  public class GLSLMatrixHandler extends MatrixHandler {
    PShader _shader;
    PMatrix3D _pmatrix = new PMatrix3D();

    public GLSLMatrixHandler(Graph graph, PShader shader) {
      super(graph.width(), graph.height());
      _shader = shader;
    }

    @Override
    protected void _setUniforms() {
      shader(_shader);
      Scene.setUniform(_shader, "nub_transform", projectionModelView());
      Scene.setUniform(_shader, "nub_modelview", modelView());
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.Shadows"});
  }
}