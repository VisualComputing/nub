package vfc;

import processing.core.*;
import remixlab.core.Graph;
import remixlab.core.Node;
import remixlab.input.event.KeyEvent;
import remixlab.input.event.MotionEvent;
import remixlab.primitives.*;
import remixlab.proscene.*;

public class ViewFrustumCulling extends PApplet {
    OctreeNode root;
    Scene scene, auxScene;
    PGraphics canvas, auxCanvas;

    //Choose one of P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
    String renderer = P3D;
    int w = 1110;
    int h = 1110;

    public void settings() {
        size(w, h, renderer);
    }

    @Override
    public void setup() {
        // declare and build the octree hierarchy
        Vector p = new Vector(100, 70, 130);
        root = new OctreeNode(p, Vector.multiply(p, -1.0f));
        root.buildBoxHierarchy(4);

        canvas = createGraphics(w, h/2, P3D);
        scene = new Scene(this, canvas);
        scene.enableBoundaryEquations();
        InteractiveFrame eye = new InteractiveFrame(scene);
        scene.setEye(eye);
        scene.setDefaultNode(eye);
        scene.fitBall();

        auxCanvas = createGraphics(w, h/2, P3D);
        // Note that we pass the upper left corner coordinates where the scene
        // is to be drawn (see drawing code below) to its constructor.
        auxScene = new Scene(this, auxCanvas, 0, h/2);
        //auxScene.camera().setType(Camera.Type.ORTHOGRAPHIC);
        InteractiveFrame auxEye = new InteractiveFrame(auxScene);
        auxScene.setEye(auxEye);
        auxScene.setDefaultNode(auxEye);
        auxScene.setRadius(200);
        auxScene.fitBall();
    }

    @Override
    public void draw() {
        background(0);
        scene.beginDraw();
        canvas.background(0);
        root.drawIfAllChildrenAreVisible(scene.pg(), scene);
        scene.endDraw();
        scene.display();

        auxScene.beginDraw();
        auxCanvas.background(0);
        root.drawIfAllChildrenAreVisible(auxScene.pg(), scene);
        auxScene.pg().pushStyle();
        auxScene.pg().stroke(255, 255, 0);
        auxScene.pg().fill(255, 255, 0, 160);
        auxScene.drawEye(scene);
        auxScene.pg().popStyle();
        auxScene.endDraw();
        auxScene.display();
    }

    public class InteractiveFrame extends Node {
        public InteractiveFrame(Graph graph) {
            super(graph);
        }

        protected InteractiveFrame(Graph otherGraph, InteractiveFrame otherFrame) {
            super(otherGraph, otherFrame);
        }

        @Override
        public InteractiveFrame get() {
            return new InteractiveFrame(this.graph(), this);
        }

        @Override
        public void interact(MotionEvent event) {
            switch (event.shortcut().id()) {
                case PApplet.LEFT:
                    rotate(event);
                    break;
                case PApplet.RIGHT:
                    translate(event);
                    break;
                case processing.event.MouseEvent.WHEEL:
                    if(isEye() && graph().is3D())
                        translateZ(event);
                    else
                        scale(event);
                    break;
            }
        }

        @Override
        public void interact(KeyEvent event) {
            if (event.id() == PApplet.UP)
                translateYPos();
            if (event.id() == PApplet.DOWN)
                translateYNeg();
            if (event.id() == PApplet.LEFT)
                translateXNeg();
            if (event.id() == PApplet.RIGHT)
                translateXPos();
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"vfc.ViewFrustumCulling"});
    }
}
