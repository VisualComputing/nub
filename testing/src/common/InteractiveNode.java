package common;


import processing.core.PApplet;
import processing.core.PGraphics;
import remixlab.input.event.KeyEvent;
import remixlab.input.event.MotionEvent;
import remixlab.proscene.Scene;
import remixlab.proscene.Shape;

public class InteractiveNode extends Shape {
    public InteractiveNode(Scene s) {
        super(s);
        //this.setPrecision(Precision.EXACT);
    }

    public InteractiveNode(InteractiveNode n) {
        super(n);
        //this.setPrecision(Precision.EXACT);
    }

    // this one gotta be overridden because we want a copied frame (e.g., line 141 above, i.e.,
    // scene.eye().get()) to have the same behavior as its original.
    protected InteractiveNode(Scene otherScene, Shape otherFrame) {
        super(otherScene, otherFrame);
    }

    @Override
    public InteractiveNode get() {
        return new InteractiveNode(this.scene(), this);
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
