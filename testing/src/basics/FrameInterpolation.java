package basics;

import processing.core.PApplet;
import remixlab.input.event.KeyEvent;
import remixlab.input.event.MotionEvent;
import remixlab.primitives.Frame;
import remixlab.proscene.*;
import remixlab.core.*;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class FrameInterpolation extends PApplet {
    Scene scene;
    Interpolator nodeInterpolator, eyeInterpolator;
    boolean showEyePath;

    public void settings() {
        size(1000, 800, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        InteractiveFrame eye = new InteractiveFrame();
        scene.setEye(eye);
        //interactivity defaults to the eye
        scene.setDefaultNode(eye);
        scene.setRadius(150);

        // interpolation 1. Default eye interpolations
        scene.fitBallInterpolation();

        // interpolation 2. Custom eye interpolations
        // (for the composed constructor please refer to my note @105 line)
        eyeInterpolator = new Interpolator(scene, scene.eye());

        // interpolation 3. Custom (arbitrary)frame interpolations, like the one
        // you guys David & Juan are currently exploring to deform a shape
        nodeInterpolator = new Interpolator(scene);
        nodeInterpolator.setLoop();
        // Create an initial path
        int nbKeyFrames = 4;
        for (int i=0; i<nbKeyFrames; i++) {
            InteractiveFrame iFrame = new InteractiveFrame();
            iFrame.setPosition(-100 + 200*i/(nbKeyFrames-1), 0, 0);
            iFrame.setScaling(random(0.25f, 4.0f));
            nodeInterpolator.addKeyFrame(iFrame);
        }
        nodeInterpolator.start();
    }

    public void draw() {
        background(0);
        pushMatrix();
        scene.applyTransformation(nodeInterpolator.frame());
        scene.drawAxes(30);
        pushStyle();
        fill(0,255,255,125);
        stroke(0,0,255);
        strokeWeight(2);
        box(30);
        popStyle();
        popMatrix();

        pushStyle();
        stroke(255);
        scene.drawPath(nodeInterpolator, 5);
        popStyle();

        for(Frame frame : nodeInterpolator.keyFrames()) {
            pushMatrix();
            scene.applyTransformation(frame);
            // Horrible cast, but Java is just horrible
            if ( ((InteractiveFrame)frame).grabsInput() )
                scene.drawAxes(40);
            else
                scene.drawAxes(20);
            popMatrix();
        }
        if(showEyePath) {
            pushStyle();
            fill(255,0,0);
            stroke(0,255,0);
            scene.drawPath(eyeInterpolator, 3);
            popStyle();
        }
    }

    public void keyPressed() {
        if(key == ' ')
            showEyePath = !showEyePath;
        if(key == 'l')
            eyeInterpolator.addKeyFrame(scene.eye().get());
        if(key == 'm')
            eyeInterpolator.toggle();
        if(key == 'n')
            eyeInterpolator.clear();
        if ( key == 'u')
            nodeInterpolator.setSpeed(nodeInterpolator.speed()-0.25f);
        if ( key == 'v')
            nodeInterpolator.setSpeed(nodeInterpolator.speed()+0.25f);
        if(key == 's')
            scene.fitBallInterpolation();
        if(key == 'f')
            scene.fitBall();
    }

    public class InteractiveFrame extends Node {
        public InteractiveFrame() {
            super(scene);
        }

        // this one gotta be overridden because we want a copied frame (e.g., line 85 above, i.e.,
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
                    translate(event);
                    break;
                case PApplet.RIGHT:
                    rotate(event);
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
        PApplet.main(new String[]{"basics.FrameInterpolation"});
    }
}
