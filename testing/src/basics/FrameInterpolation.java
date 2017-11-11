package basics;

import processing.core.PApplet;
import remixlab.bias.event.KeyEvent;
import remixlab.bias.event.MotionEvent;
import remixlab.proscene.*;
import remixlab.core.*;

public class FrameInterpolation extends PApplet {
    Scene scene;
    Node keyFrame[];
    Interpolator kfi;
    int nbKeyFrames;
    Interpolator interpolator;
    boolean showEyePath;

    public void settings() {
        size(640, 360, P3D);
    }

    public void setup() {
        nbKeyFrames = 4;
        scene = new Scene(this);
        //unsets grid and axis altogether
        InteractiveFrame eye = new InteractiveFrame();
        scene.setEye(eye);
        scene.setDefaultNode(eye);
        scene.setRadius(70);
        scene.fitBallInterpolation();

        interpolator = new Interpolator(scene, scene.eye());

        kfi = new Interpolator(scene);
        kfi.setLoop();

        // An array of interactive (key) frames.
        keyFrame = new Node[nbKeyFrames];
        // Create an initial path
        for (int i=0; i<nbKeyFrames; i++) {
            keyFrame[i] = new InteractiveFrame();
            keyFrame[i].setPosition(-100 + 200*i/(nbKeyFrames-1), 0, 0);
            keyFrame[i].setScaling(random(0.25f, 4.0f));
            kfi.addKeyFrame(keyFrame[i]);
        }

        kfi.start();
    }

    public void draw() {
        background(0);
        pushMatrix();
        scene.applyTransformation(kfi.frame());
        scene.drawAxes(30);
        popMatrix();

        pushStyle();
        stroke(255);
        //scene.drawPath(kfi);
        scene.drawPath(kfi, 5);
        popStyle();

        for (int i=0; i<nbKeyFrames; ++i) {
            pushMatrix();
            scene.applyTransformation(kfi.keyFrame(i));
            //kfi.keyFrame(i).applyTransformation(scene);

            if ( keyFrame[i].grabsInput() )
                scene.drawAxes(40);
            else
                scene.drawAxes(20);

            popMatrix();
        }

        if(showEyePath) {
            pushStyle();
            fill(255,0,0);
            stroke(0,255,0);
            scene.drawPath(interpolator, 3);
            popStyle();
        }
    }

    public void keyPressed() {
        if(key == ' ')
            showEyePath = !showEyePath;
        if(key == 'l')
            interpolator.addKeyFrame(scene.eye().get());
        if(key == 'm')
            interpolator.toggle();
        if(key == 'n')
            interpolator.clear();
        if ( key == 'u')
            kfi.setSpeed(kfi.speed()-0.25f);
        if ( key == 'v')
            kfi.setSpeed(kfi.speed()+0.25f);
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
                    translate(event);
                    break;
                case PApplet.RIGHT:
                    rotate(event);
                    break;
                case processing.event.MouseEvent.WHEEL:
                    if(isEye())
                        translateZ(event);
                    else
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
        PApplet.main(new String[]{"basics.FrameInterpolation"});
    }
}
