package basics;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import remixlab.input.Event;
import remixlab.input.event.*;
import remixlab.primitives.Frame;
import remixlab.proscene.*;
import remixlab.core.*;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class FrameInterpolation2 extends PApplet {
    Scene scene;
    Interpolator nodeInterpolator, eyeInterpolator;
    boolean showEyePath;

    //controls
    Scene auxScene;
    Button button;
    PGraphics auxCanvas;
    int w = 1200;
    int h = 900;
    int oW = 2*w/3;
    int oH = h/3;
    int oX = w - oW;
    int oY = h - oH;
    boolean showControls = true;

    public void settings() {
        size(w, h, P3D);
    }

    public void setup() {
        scene = new Scene(this);
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
        auxCanvas = createGraphics(oW, oH, P3D);
        auxCanvas.rectMode(CENTER);
        auxScene = new Scene(this, auxCanvas, oX, oY);
        //auxScene.disablePickingBuffer();
        InteractiveFrame eye1 = new InteractiveFrame(auxScene);
        auxScene.setEye(eye1);
        //interactivity defaults to the eye
        auxScene.setDefaultNode(eye1);
        button = new Button(100,60);
        //button.setPosition(200,50);
        auxScene.setRadius(200);
        auxScene.fitBall();
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
        if(scene.is2D())
            rect(0,0,100,100);
        else
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
                scene.drawAxes(35);
            else
                scene.drawAxes(20);
            popMatrix();
        }
        if(showEyePath) {
            pushStyle();
            fill(255,0,0);
            stroke(0,255,255);
            scene.drawPath(eyeInterpolator, 3);
            popStyle();
        }

        // Note that autoFocus is currently broken when one scene is onscreen
        // and the other is offscreen. It will be fixed ... next year...
        // in the mean time please write conditions on mouseX mouseY to (dis)enable
        // (e.g., scene.disableKeyAgent(); scene.disableMouseAgent();)
        // the two scenes agents according to their dimensions and placement.
        if (showControls) {
            scene.beginScreenDrawing();
            auxScene.beginDraw();
            auxCanvas.background(29, 153, 243);
            auxScene.drawAxes();
            // calls visit() for each node in the graph
            //auxScene.traverse();
            button.draw();
            auxScene.endDraw();
            auxScene.display();
            scene.endScreenDrawing();
        }
    }

    public void keyPressed() {
        if(key == 'c')
            showControls = !showControls;
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

        // this one gotta be overridden because we want a copied frame (e.g., line 141 above, i.e.,
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

    // Controls are implemented from using (the new) Shape class which uses
    // the picking buffer (exact picking according to the picking shape)
    public class Button extends Shape {
        //button dimensions
        int _w , _h;
        public Button(int w, int h) {
            super(auxScene);
            _w = w;
            _h = h;
            ///*
            PShape rectangle = createShape(RECT,0,0,_w,_h);
            rectangle.setStroke(color(255));
            rectangle.setStrokeWeight(4);
            rectangle.setFill(color(127));
            //set(rectangle);
            //*/
        }

        @Override
        public void interact(MotionEvent2 event) {
            switch (event.shortcut().id()) {
                case LEFT:
                    nodeInterpolator.setSpeed(nodeInterpolator.speed() + event.dx()/10);
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
        public void interact(MotionEvent1 event) {
            nodeInterpolator.setSpeed(nodeInterpolator.speed() + event.dx()/10);
        }

        @Override
        public void interact(TapEvent event) {
            TapShortcut left = new TapShortcut(Event.NO_MODIFIER_MASK, LEFT, 2);
            TapShortcut right = new TapShortcut(Event.SHIFT, RIGHT, 1);
            if(event.shortcut().matches(left))
                scene.fitBallInterpolation();
            if(event.shortcut().matches(right))
                println("got me!");
        }

        ///*
        @Override
        protected void set(PGraphics pg) {
            pg.rectMode(CENTER);
            pg.fill(255,0,0);
            pg.rect(0,0,_w,_h);
        }
        //*/
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"basics.FrameInterpolation2"});
    }
}
