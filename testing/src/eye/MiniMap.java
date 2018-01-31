package eye;

import common.InteractiveNode;
import common.InteractiveShape;
import processing.core.PApplet;
import processing.core.PGraphics;
import remixlab.core.Graph;
import remixlab.core.Node;
import remixlab.proscene.Scene;
import remixlab.proscene.Shape;

public class MiniMap extends PApplet {
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

  public void settings() {
    size(w, h, renderer);
  }

  public void setup() {
    sceneCanvas = createGraphics(w, h, renderer);
    scene = new Scene(this, sceneCanvas);
    //if(scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
    node1 = new TorusShape(scene);
    //node1.setPrecision(Node.Precision.EXACT);
    node1.translate(30, 30);
    node2 = new TorusShape(node1);
    node2.translate(40, 0);
    node3 = new TorusShape(node2);
    node3.translate(40, 0);
    InteractiveNode sceneEye = new InteractiveNode(scene);
    scene.setEye(sceneEye);
    scene.setFieldOfView((float) Math.PI / 3);
    //interactivity defaults to the eye
    scene.setDefaultNode(sceneEye);
    scene.setRadius(150);
    //scene.fitBallInterpolation();
    scene.fitBall();
    //scene.disableAutoFocus();
    //scene.disableMouseAgent();

    minimapCanvas = createGraphics(oW, oH, renderer);
    minimap = new Scene(this, minimapCanvas, oX, oY);
    node4 = new TorusShape(minimap);
    ////node1.setPrecision(Node.Precision.EXACT);
    node4.translate(30, 30);
    if (minimap.is3D()) minimap.setType(Graph.Type.ORTHOGRAPHIC);
    InteractiveNode minimapEye = new InteractiveNode(minimap);
    minimap.setEye(minimapEye);
    //interactivity defaults to the eye
    minimap.setDefaultNode(minimapEye);
    minimap.setRadius(500);
    minimap.fitBall();
    //minimap.fitBallInterpolation();
    //minimap.disableAutoFocus();

    ///*
    eye = new EyeShape(minimap);
    //to not scale the eye on mouse hover uncomment:
    eye.setHighlighting(Shape.Highlighting.NONE);
    eye.setWorldMatrix(scene.eye());
    //eye.setShape(scene.eye());
    //*/
  }

  public void draw() {
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
      //eye.draw();
      ///*
      for (Node node : scene.nodes())
        if (node instanceof Shape)
          ((Shape) node).draw(minimap.frontBuffer());
      //*/
      minimap.drawAxes();
      minimap.endDraw();
      minimap.display();
    }
  }

  public void keyPressed() {
    if (key == ' ')
      showMiniMap = !showMiniMap;
    if (key == 's')
      scene.fitBallInterpolation();
    if (key == 'S')
      minimap.fitBallInterpolation();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"eye.MiniMap"});
  }

  public class EyeShape extends InteractiveShape {
    public EyeShape(Scene scene) {
      super(scene);
      //this.setPrecision(Precision.EXACT);
    }

    public EyeShape(EyeShape eyeShape) {
      super(eyeShape);
      //this.setPrecision(Precision.EXACT);
    }

    @Override
    protected void set(PGraphics pg) {
      pg.fill(0, 255, 0);
      pg.stroke(0, 0, 255);
      pg.strokeWeight(2);
      minimap.drawEye(pg, scene, true);
    }
  }

  public class TorusShape extends InteractiveShape {
    public TorusShape(Scene scene) {
      super(scene);
    }

    public TorusShape(TorusShape torusShape) {
      super(torusShape);
    }

    @Override
    protected void set(PGraphics pg) {
      pg.fill(scene().pApplet().random(255), scene().pApplet().random(255), scene().pApplet().random(255), scene().pApplet().random(255));
      Scene.drawTorusSolenoid(pg, 6, 8);
    }
  }
}
