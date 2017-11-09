package basics;

import processing.core.PApplet;
import remixlab.bias.event.KeyEvent;
import remixlab.bias.event.MotionEvent;
import remixlab.geom.Graph;
import remixlab.geom.Node;
import remixlab.proscene.Scene;

public class TwoD extends PApplet {
    Scene scene;
    Node node, frame;
    boolean target;

    @Override
    public void settings() {
        size(800, 800, JAVA2D);
    }

    @Override
    public void setup() {
        rectMode(CENTER);
        scene = new Scene(this);

        //scene.setType(Graph.Type.ORTHOGRAPHIC);

        frame = new InteractiveFrame();
        node = new InteractiveFrame();

        scene.setEye(node);
        scene.setDefaultNode(node);
        scene.setRadius(200);
        scene.fitBallInterpolation();
    }

    @Override
    public void draw() {
        background(0);
        scene.drawAxes(scene.radius());
        if(target)
            scene.drawPickingTarget(frame);
        pushMatrix();
        scene.applyTransformation(frame);
        if (frame.grabsInput(scene.motionAgent()))
            fill(255, 0, 0);
        else
            fill(0,255,0);
        rect(0,0,50,100);
        popMatrix();
        scene.beginScreenDrawing();
        fill(0, 0, 255);
        rect(80,80,50,100);
        scene.endScreenDrawing();
    }

    @Override
    public void keyPressed() {
        if(key == 'f')
            target = !target;
        if(key == ' ')
            scene.flip();
    }

    public class InteractiveFrame extends Node {
        public InteractiveFrame() {
            super(scene);
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
                    scale(event);
                    break;
            }
        }

        @Override
        public void interact(KeyEvent event) {
            if (event.id() == PApplet.UP)
                translateY(true);
            if (event.id() == PApplet.DOWN)
                translateY(false);
            if (event.id() == PApplet.LEFT)
                translateX(false);
            if (event.id() == PApplet.RIGHT)
                translateX(true);
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"basics.TwoD"});
    }
}
