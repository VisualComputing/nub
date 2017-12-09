package frame;

import processing.core.PApplet;
import remixlab.core.Graph;
import remixlab.core.Node;
import remixlab.input.Event;
import remixlab.input.event.KeyEvent;
import remixlab.input.event.MotionEvent;
import remixlab.proscene.*;

public class FirstPerson extends PApplet {
    Scene scene;
    Node iFrame;

     @Override
    public void settings() {
        size(800, 800, P3D);
    }

    @Override
    public void setup() {
        scene = new Scene(this);
        iFrame = new Node(scene) {
            @Override
            public void motionInteraction(MotionEvent event) {
                switch (event.shortcut().id()) {
                    case PApplet.LEFT:
                        rotate(event);
                        break;
                    case PApplet.CENTER:
                        scale(event);
                        break;
                    case PApplet.RIGHT:
                        translate(event);
                        break;

                }
            }
        };
        iFrame.translate(30, 30);

        scene.mouseAgent().setMode(MouseAgent.Mode.CLICK);

        InteractiveFrame eye = new InteractiveFrame();
        scene.setEye(eye);
        scene.setDefaultNode(eye);
        scene.fitBallInterpolation();
    }

    @Override
    public void draw() {
        background(0);
        fill(204, 102, 0, 150);
        scene.drawTorusSolenoid();

        // Save the current model view matrix
        pushMatrix();
        // Multiply matrix to get in the frame coordinate system.
        // applyMatrix(Scene.toPMatrix(iFrame.matrix())); //is possible but inefficient
        iFrame.applyTransformation();//very efficient
        // Draw an axis using the Scene static function
        scene.drawAxes(20);

        // Draw a second torus
        if (scene.mouseAgent().defaultGrabber() == iFrame) {
            fill(0, 255, 255);
            scene.drawTorusSolenoid();
        }
        else if (iFrame.grabsInput()) {
            fill(255, 0, 0);
            scene.drawTorusSolenoid();
        }
        else {
            fill(0, 0, 255, 150);
            scene.drawTorusSolenoid();
        }

        popMatrix();
    }

    public void keyPressed() {
        if ( key == 'i')
            scene.inputHandler().shiftDefaultGrabber((Node)scene.eye(), iFrame);
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
        public void motionInteraction(MotionEvent event) {
            switch (event.shortcut().id()) {
                case PApplet.LEFT:
                    moveForward(event);
                    break;
                case PApplet.RIGHT:
                    moveBackward(event);
                    break;
                case processing.event.MouseEvent.WHEEL:
                    if(isEye())
                        translateZ(event);
                    else
                        scale(event);
                    break;
                case Event.NO_ID:
                    if(isEye())
                        lookAround(event);
                    break;
            }
        }

        @Override
        public void keyInteraction(KeyEvent event) {
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
        PApplet.main(new String[]{"frame.FirstPerson"});
    }
}
