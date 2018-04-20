/**
 * Mini Map
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates how to use off-screen rendering to build
 * a mini-map of the main Scene where all objetcs are interactive. It
 * also shows Frame syncing among views.
 */

import frames.input.*;
import frames.core.*;
import frames.processing.*;

Scene scene, minimap;
PGraphics sceneCanvas, minimapCanvas;
TorusShape node1, node2, node3;
EyeShape eye;
TorusShape node4;

int w = 1800;
int h = 1200;
int oW = w / 3;
int oH = h / 3;
int oX = w - oW;
int oY = h - oH;
boolean showMiniMap = true;

//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P3D;

void setup() {
  size(1800, 1200, renderer);
  sceneCanvas = createGraphics(w, h, renderer);
  scene = new Scene(this, sceneCanvas);
  node1 = new TorusShape(scene);
  node1.translate(30, 30);
  node2 = new TorusShape(node1);
  node2.translate(40, 0);
  node3 = new TorusShape(node2);
  node3.translate(40, 0);
  OrbitShape sceneEye = new OrbitShape(scene);
  scene.setEye(sceneEye);
  scene.setFieldOfView((float) Math.PI / 3);
  //interactivity defaults to the eye
  scene.setDefaultGrabber(sceneEye);
  scene.setRadius(150);
  scene.fitBall();

  minimapCanvas = createGraphics(oW, oH, renderer);
  minimap = new Scene(this, minimapCanvas, oX, oY);
  node4 = new TorusShape(minimap);
  ////node1.setPrecision(Node.Precision.EXACT);
  node4.translate(30, 30);
  if (minimap.is3D()) minimap.setType(Graph.Type.ORTHOGRAPHIC);
  OrbitShape minimapEye = new OrbitShape(minimap);
  minimap.setEye(minimapEye);
  //interactivity defaults to the eye
  minimap.setDefaultGrabber(minimapEye);
  minimap.setRadius(500);
  minimap.fitBall();

  eye = new EyeShape(minimap);
  //to not scale the eye on mouse hover uncomment:
  eye.setHighlighting(Shape.Highlighting.NONE);
  eye.set(scene.eye());
}

void draw() {
  Node.sync((Node) scene.eye(), eye);
  scene.beginDraw();
  sceneCanvas.background(0);
  scene.traverse();
  scene.drawAxes();
  scene.endDraw();
  scene.display();
  if (showMiniMap) {
    minimap.beginDraw();
    minimapCanvas.background(29, 153, 243);
    minimap.frontBuffer().fill(255, 0, 255, 125);
    minimap.traverse();
    for (Node node : scene.nodes())
      if (node instanceof Shape)
        ((Shape) node).draw(minimap.frontBuffer());
    minimap.drawAxes();
    minimap.endDraw();
    minimap.display();
  }
}

void keyPressed() {
  if (key == ' ')
    showMiniMap = !showMiniMap;
  if (key == 's')
    scene.fitBallInterpolation();
  if (key == 'S')
    minimap.fitBallInterpolation();
}

public class EyeShape extends OrbitShape {
  public EyeShape(Scene scene) {
    super(scene);
  }

  public EyeShape(EyeShape eyeShape) {
    super(eyeShape);
  }

  @Override
  protected void set(PGraphics pGraphics) {
    pGraphics.fill(0, 255, 0);
    pGraphics.stroke(0, 0, 255);
    pGraphics.strokeWeight(2);
    minimap.drawEye(pGraphics, scene, true);
  }
}

public class TorusShape extends OrbitShape {
  public TorusShape(Scene scene) {
    super(scene);
  }

  public TorusShape(TorusShape torusShape) {
    super(torusShape);
  }

  @Override
  protected void set(PGraphics pGraphics) {
    pGraphics.fill(graph().pApplet().random(255), graph().pApplet().random(255), graph().pApplet().random(255), graph().pApplet().random(255));
    Scene.drawTorusSolenoid(pGraphics, 6, 8);
  }
}
