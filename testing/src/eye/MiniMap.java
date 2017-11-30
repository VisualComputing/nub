package eye;

import common.InteractiveNode;
import processing.core.PApplet;
import processing.core.PGraphics;
import remixlab.core.Node;
import remixlab.proscene.Scene;
import remixlab.proscene.Shape;

public class MiniMap extends PApplet {
    Scene scene, minimap;
    PGraphics sceneCanvas, minimapCanvas;
    InteractiveShape node1, node2, node3, eye;

    int w = 1800;
    int h = 1200;
    int oW = w/3;
    int oH = h/3;
    int oX = w - oW;
    int oY = h - oH;
    boolean showMiniMap  = true;

    //Choose FX2D, JAVA2D, P2D or P3D
    String renderer = P3D;

    public void settings() {
        size(w, h, renderer);
    }

    public void setup() {
        sceneCanvas = createGraphics(w, h, renderer);
        scene = new Scene(this, sceneCanvas);
        node1 = new InteractiveShape(scene);
        node1.setPrecision(Node.Precision.EXACT);
        node1.translate(30, 30);
        node2 = new InteractiveShape(node1);
        node2.translate(40, 0);
        node3 = new InteractiveShape(node2);
        node3.translate(40, 0);
        InteractiveNode sceneEye = new InteractiveNode(scene);
        scene.setEye(sceneEye);
        //interactivity defaults to the eye
        scene.setDefaultNode(sceneEye);
        scene.setRadius(150);
        //scene.fitBallInterpolation();
        scene.fitBall();
        scene.disableAutoFocus();
        scene.disableMouseAgent();

        minimapCanvas = createGraphics(oW, oH, renderer);
        minimap = new Scene(this, minimapCanvas, oX, oY);
        InteractiveNode minimapEye = new InteractiveNode(minimap);
        minimap.setEye(minimapEye);
        //interactivity defaults to the eye
        minimap.setDefaultNode(minimapEye);
        minimap.setRadius(200);
        minimap.fitBall();
        //minimap.fitBallInterpolation();
        minimap.disableAutoFocus();

        eye = new InteractiveShape(minimap);
        //to not scale the eye on mouse hover uncomment:
        eye.setHighlighting(Shape.Highlighting.NONE);
        eye.setWorldMatrix(scene.eye());
        //eye.setShape(scene.eye());
    }

    public void draw() {
        scene.beginDraw();
        sceneCanvas.background(0);
        scene.traverse();
        scene.endDraw();
        scene.display();
        if (showMiniMap) {
            minimap.beginDraw();
            minimapCanvas.background(29, 153, 243);
            minimap.frontBuffer().fill(255, 0, 255, 125);
            //minimap.traverse();
            eye.draw();
            for(Node node : scene.nodes())
                if(node instanceof Shape)
                    ((Shape)node).draw(minimap.frontBuffer());
            minimap.endDraw();
            minimap.display();
        }
    }

    public void keyPressed() {
        if (key == ' ')
            showMiniMap = !showMiniMap;
        /*
        if (key == 'x')
            eye.setShape("eyeDrawing");
        if (key == 'y')
            eye.setShape(scene.eyeFrame());
            */
    }

    public void frameDrawing(PGraphics pg) {
        pg.fill(random(0, 255), random(0, 255), random(0, 255));
        if (scene.is3D())
            pg.box(40, 10, 5);
        else
            pg.rect(0, 0, 40, 10, 5);
    }

    public void eyeDrawing(PGraphics pg) {
        if (minimap.is3D())
            pg.box(200);
        else {
            pg.pushStyle();
            pg.rectMode(CENTER);
            pg.rect(0, 0, 200, 200);
            pg.popStyle();
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"eye.MiniMap"});
    }

    public class InteractiveShape extends InteractiveNode {
        public InteractiveShape(Scene s) {
            super(s);
            //this.setPrecision(Precision.EXACT);
        }

        public InteractiveShape(InteractiveShape n) {
            super(n);
            //this.setPrecision(Precision.EXACT);
        }

        @Override
        protected void set(PGraphics pg) {
            if(scene() == scene) {
                //pg.fill(scene().pApplet().random(255), scene().pApplet().random(255), scene().pApplet().random(255), scene().pApplet().random(255));
                //Scene.drawTorusSolenoid(pg, (int)scene().pApplet().random(2,10), scene().pApplet().random(2,10));
                pg.fill(scene().pApplet().random(255), scene().pApplet().random(255), scene().pApplet().random(255), scene().pApplet().random(255));
                Scene.drawTorusSolenoid(pg, 6, 8);
            }
            if(scene() == minimap) {
                minimap.drawEye(scene);
            }
        }
    }
}
