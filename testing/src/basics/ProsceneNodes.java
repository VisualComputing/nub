package basics;

import processing.core.PApplet;
import processing.core.PGraphics;
import remixlab.core.Graph;
import remixlab.core.Node;
import remixlab.input.event.KeyEvent;
import remixlab.input.event.MotionEvent;
import remixlab.input.event.TapEvent;
import remixlab.proscene.NodeP5;
import remixlab.proscene.Scene;

public class ProsceneNodes extends PApplet {
    Scene scene;
    INode node;

    public void settings() {
        size(800, 800, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        InteractiveFrame eye = new InteractiveFrame(scene);
        scene.setEye(eye);
        //interactivity defaults to the eye
        scene.setDefaultNode(eye);
        scene.setRadius(200);
        scene.fitBallInterpolation();

        node = new INode();
        //node.setPickingPrecision(Node.PickingPrecision.FIXED);
    }

    public void draw() {
        background(0);
        scene.drawAxes();
        scene.traverse();
    }

    public void keyPressed() {
        if(key == 'e')
            if(scene.type() == Graph.Type.PERSPECTIVE)
                scene.setType(Graph.Type.ORTHOGRAPHIC);
            else
                scene.setType(Graph.Type.PERSPECTIVE);

    }

    public class InteractiveFrame extends Node {
        public InteractiveFrame(Scene s) {
            super(s);
        }

        // this one gotta be overridden because we want a copied frame (e.g., line 100 above, i.e.,
        // scene.eye().get()) to have the same behavior as its original.
        protected InteractiveFrame(Graph otherGraph, InteractiveFrame otherFrame) {
            super(otherGraph, otherFrame);
        }

        @Override
        public InteractiveFrame get() {
            return new InteractiveFrame(this.graph(), this);
        }

        // behavior is here :P
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

    public class INode extends NodeP5 {
        //button dimensions
        public INode() {
            super(scene);
        }

        @Override
        public void interact(MotionEvent event) {
            switch (event.shortcut().id()) {
                case LEFT:
                    rotate(event);
                    break;
                case RIGHT:
                    translate(event);
                    break;
                case processing.event.MouseEvent.WHEEL:
                    scale(event);
                    break;
            }
        }

        @Override
        public void interact(TapEvent event) {

        }

        @Override
        protected void display(PGraphics pg) {
            pg.fill(255,0,0);
            pg.sphere(50);
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"basics.ProsceneNodes"});
    }
}
