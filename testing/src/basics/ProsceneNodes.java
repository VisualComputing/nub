package basics;

import common.InteractiveNode;
import processing.core.PApplet;
import processing.core.PGraphics;
import remixlab.core.Graph;
import remixlab.core.Node;
import remixlab.input.event.KeyEvent;
import remixlab.input.event.MotionEvent;
import remixlab.input.event.TapEvent;
import remixlab.proscene.Shape;
import remixlab.proscene.Scene;

public class ProsceneNodes extends PApplet {
    Scene scene;
    INode node;

    public void settings() {
        size(800, 800, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        InteractiveNode eye = new InteractiveNode(scene);
        scene.setEye(eye);
        //interactivity defaults to the eye
        scene.setDefaultNode(eye);
        scene.setRadius(200);
        scene.fitBallInterpolation();

        node = new INode();
        //node.setPrecision(Node.Precision.FIXED);
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

    public class INode extends Shape {
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
        protected void set(PGraphics pg) {
            pg.fill(255,0,0);
            pg.sphere(50);
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"basics.ProsceneNodes"});
    }
}
