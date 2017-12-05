package common;

import processing.core.PApplet;
import processing.core.PShape;
import remixlab.core.Graph;
import remixlab.core.Node;
import remixlab.input.event.KeyEvent;
import remixlab.input.event.MotionEvent;

public class InteractiveNode extends Node {
    public InteractiveNode(Graph graph) {
        super(graph);
    }

    // this one gotta be overridden because we want a copied frame (e.g., line 100 above, i.e.,
    // scene.eye().get()) to have the same behavior as its original.
    protected InteractiveNode(Graph otherGraph, InteractiveNode otherNode) {
        super(otherGraph, otherNode);
    }

    @Override
    public InteractiveNode get() {
        return new InteractiveNode(this.graph(), this);
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
