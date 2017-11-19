package third;

import processing.core.PApplet;
import remixlab.core.Graph;
import remixlab.core.Node;
import remixlab.input.event.KeyEvent;
import remixlab.input.event.MotionEvent;

public class InteractiveFrame extends Node {
    public InteractiveFrame(Graph graph) {
        super(graph);
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
                translateZ(event);
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