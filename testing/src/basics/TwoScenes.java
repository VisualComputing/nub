package basics;

import processing.core.PApplet;
import processing.core.PGraphics;
import remixlab.proscene.Scene;

public class TwoScenes extends PApplet {
    Scene scene;

    //controls
    Scene auxScene;
    PGraphics auxCanvas;
    int w = 1200;
    int h = 800;
    int oW = w/3;
    int oH = h/3;
    int oX = w - oW;
    int oY = h - oH;
    boolean showMiniMap  = true;

    //Choose P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
    String renderer = P3D;

    public void settings() {
        size(w, h, renderer);
    }

    public void setup() {
        rectMode(CENTER);
        scene = new Scene(this);
        scene.setRadius(150);
        scene.fitBallInterpolation();

        // application control
        auxCanvas = createGraphics(oW, oH, P2D);
        auxScene = new Scene(this, auxCanvas, oX, oY);
        auxScene.setRadius(200);
        auxScene.fitBall();
    }

    public void draw() {
        background(0);
        pushMatrix();
        scene.drawAxes();
        popMatrix();

        //control graph
        scene.beginScreenDrawing();
        if (showMiniMap) {
            auxScene.beginDraw();
            auxCanvas.background(29, 153, 242);
            //auxScene.pg().fill(255, 0, 255, 125);

            auxScene.endDraw();
            //image(auxCanvas, auxScene.originCorner().x(), auxScene.originCorner().y());
            auxScene.display();
        }
        scene.endScreenDrawing();
    }

    public void keyPressed() {
        if(key == ' ')
            showMiniMap = !showMiniMap;
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"basics.TwoScenes"});
    }
}
