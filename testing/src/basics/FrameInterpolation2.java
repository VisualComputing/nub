package basics;

import processing.core.PApplet;
import processing.core.PGraphics;
import remixlab.input.event.KeyEvent;
import remixlab.input.event.MotionEvent;
import remixlab.input.event.TapEvent;
import remixlab.primitives.Frame;
import remixlab.primitives.Vector;
import remixlab.proscene.*;
import remixlab.core.*;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class FrameInterpolation2 extends PApplet {
    Scene scene;
    PGraphics canvas;
    Interpolator nodeInterpolator, eyeInterpolator;
    boolean showEyePath;

    //controls
    Scene auxScene;
    PGraphics auxCanvas;
    int w = 1200;
    int h = 900;
    int oW = 2*w/3;
    int oH = h/3;
    int oX = w - oW;
    int oY = h - oH;
    boolean showMiniMap  = true;

    public void settings() {
        size(w, h, P3D);
    }

    public void setup() {
        canvas = createGraphics(w, h, P3D);
        canvas.rectMode(CENTER);
        scene = new Scene(this, canvas);
        InteractiveFrame eye = new InteractiveFrame(scene);
        scene.setEye(eye);
        //interactivity defaults to the eye
        scene.setDefaultNode(eye);
        scene.setRadius(150);

        // interpolation 1. Default eye interpolations
        scene.fitBallInterpolation();

        // interpolation 2. Custom eye interpolations
        eyeInterpolator = new Interpolator(eye);

        // interpolation 3. Custom (arbitrary)frame interpolations, like the one
        // you guys David & Juan are currently exploring to deform a shape
        nodeInterpolator = new Interpolator(scene);
        nodeInterpolator.setLoop();
        // Create an initial path
        int nbKeyFrames = 4;
        for (int i=0; i<nbKeyFrames; i++) {
            InteractiveFrame iFrame = new InteractiveFrame(scene);
            iFrame.setPosition(-100 + 200*i/(nbKeyFrames-1), 0, 0);
            iFrame.setScaling(random(0.25f, 4.0f));
            nodeInterpolator.addKeyFrame(iFrame);
        }
        nodeInterpolator.start();

        // application control
        auxCanvas = createGraphics(oW, oH, P2D);
        auxCanvas.rectMode(CENTER);
        auxScene = new Scene(this, auxCanvas, oX, oY);
        InteractiveFrame eye1 = new InteractiveFrame(auxScene);
        auxScene.setEye(eye1);
        //interactivity defaults to the eye
        auxScene.setDefaultNode(eye1);
        Button button = new Button(100,60);
        //note that we can transform (but no rotate) the button and it still will be correctly picked
        button.setPosition(100,20);
        button.scale(0.8f);
        auxScene.setRadius(200);
        auxScene.fitBall();
    }

    public void draw() {
        background(0);
        scene.beginDraw();
        canvas.background(0);

        canvas.pushMatrix();
        scene.applyTransformation(nodeInterpolator.frame());
        scene.drawAxes(30);
        canvas.pushStyle();
        canvas.fill(0,255,255,125);
        canvas.stroke(0,0,255);
        canvas.strokeWeight(2);
        if(scene.is2D())
            canvas.rect(0,0,100,100);
        else
            canvas.box(30);
        canvas.popStyle();
        canvas.popMatrix();

        canvas.pushStyle();
        canvas.stroke(255);
        scene.drawPath(nodeInterpolator, 5);
        canvas.popStyle();

        for(Frame frame : nodeInterpolator.keyFrames()) {
            canvas.pushMatrix();
            scene.applyTransformation(frame);
            // Horrible cast, but Java is just horrible
            if ( ((InteractiveFrame)frame).grabsInput() )
                scene.drawAxes(35);
            else
                scene.drawAxes(20);
            canvas.popMatrix();
        }
        if(showEyePath) {
            canvas.pushStyle();
            canvas.fill(255,0,0);
            canvas.stroke(0,255,255);
            scene.drawPath(eyeInterpolator, 3);
            canvas.popStyle();
        }

        scene.endDraw();
        scene.display();

        //control graph
        if (showMiniMap) {
            auxScene.beginDraw();
            auxCanvas.background(29, 153, 243);
            auxScene.drawAxes();
            // calls visit() for each node in the graph
            auxScene.traverse();
            auxScene.endDraw();
            auxScene.display();
        }
    }

    public void keyPressed() {
        if(key == 'c')
            showMiniMap = !showMiniMap;
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

    // eye node, good for both scenes
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

    // button
    public class Button extends Node {
        //button dimensions
        int _w , _h;
        public Button(int w, int h) {
            super(auxScene);
            _w = w;
            _h = h;
        }

        @Override
        public void interact(MotionEvent event) {
            switch (event.shortcut().id()) {
                case LEFT:

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
        protected void visit() {
            auxCanvas.pushStyle();
            auxCanvas.rectMode(CENTER);
            auxCanvas.fill(255,0,0);
            auxCanvas.rect(0,0,_w,_h);
            auxCanvas.pushStyle();
        }

        // emulate exact picking precision. Better to use the graph picking buffer
        // next time for sure :P
        @Override
        public boolean track(float x, float y) {
            //convert world to screen
            Vector origin = graph().projectedCoordinatesOf(position());
            float halfThresholdX = _w / 2 * scaling() * graph().pixelToSceneRatio(position());
            float halfThresholdY = _h / 2 * scaling() * graph().pixelToSceneRatio(position());
            return ((Math.abs(x - origin._vector[0]) < halfThresholdX) && (Math.abs(y - origin._vector[1]) < halfThresholdY));
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"basics.FrameInterpolation2"});
    }
}
