/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.proscene;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.core.PVector;
import processing.opengl.PGraphicsOpenGL;
import remixlab.core.Node;

public class NodeP5  extends Node {
    Scene _scene;
    PShape _frontShape, _backShape;

    public NodeP5(Scene scene) {
        super(scene);
        _scene = scene;
        if(_scene.pg() instanceof PGraphicsOpenGL)
            setPickingPrecision(PickingPrecision.EXACT);
    }

    public NodeP5(NodeP5 reference) {
        super(reference);
        _scene = reference.scene();
        if(_scene.pg() instanceof PGraphicsOpenGL)
            setPickingPrecision(PickingPrecision.EXACT);
    }

    protected NodeP5(Scene otherGraph, NodeP5 otherFrame) {
        super(otherGraph, otherFrame);
    }

    @Override
    public NodeP5 get() {
        return new NodeP5(scene(), this);
    }

    public Scene scene() {
        return _scene;
    }

    @Override
    public void setPickingPrecision(PickingPrecision precision) {
        if (precision == PickingPrecision.EXACT)
            if (!scene().isPickingBufferEnabled())
                System.out.println("Warning: EXACT picking precision will behave like FIXED until the scene.pickingBuffer() is enabled.");
        this._pickingPrecision = precision;
    }

    @Override
    protected void visit() {
        if (scene().eye() == this)
            return;
        PGraphics pg = scene()._targetPGraphics;
        if(pg != scene().pickingBuffer()) {
            pg.pushStyle();
            if(_frontShape != null)
                pg.shape(_frontShape);
            display(pg);
            frontDisplay(pg);
            pg.popStyle();
        }
        else {
            if (scene().isPickingBufferEnabled() && pickingPrecision() == PickingPrecision.EXACT) {
                float r = (float) (_id & 255) / 255.f;
                float g = (float) ((_id >> 8) & 255) / 255.f;
                float b = (float) ((_id >> 16) & 255) / 255.f;
                // funny, only safe way. Otherwise break things horribly when setting shapes
                // and there are more than one iFrame
                pg.shader(scene()._pbTriangleShader);
                pg.shader(scene()._pbLineShader, PApplet.LINES);
                pg.shader(scene()._pbPointShader, PApplet.POINTS);

                scene()._pbTriangleShader.set("id", new PVector(r, g, b));
                scene()._pbLineShader.set("id", new PVector(r, g, b));
                scene()._pbPointShader.set("id", new PVector(r, g, b));
                pg.pushStyle();
                if (_backShape != null)
                    pg.shape(_backShape);
                display(pg);
                backDisplay(pg);
                pg.popStyle();
            }
        }
    }

    protected void display(PGraphics pg) {

    }

    protected void frontDisplay(PGraphics pg) {

    }

    protected void backDisplay(PGraphics pg) {

    }

    public void setShape(PShape shape) {
        setFrontShape(shape);
        setBackShape(shape);
    }

    public void setFrontShape(PShape shape) {
        _frontShape = shape;
    }

    public void setBackShape(PShape shape) {
        _backShape = shape;
    }

    /**
     * An interactive-frame may be picked using
     * <a href="http://schabby.de/picking-opengl-ray-tracing/">'ray-picking'</a> with a
     * color buffer (see {@link remixlab.proscene.Scene#pickingBuffer()}). This method
     * compares the color of the {@link remixlab.proscene.Scene#pickingBuffer()} at
     * {@code (x,y)} with {@link #id()}. Returns true if both colors are the same, and false
     * otherwise.
     * <p>
     * This method is only meaningful when this node is not an eye.
     *
     * @see #setPickingPrecision(PickingPrecision)
     */
    @Override
    public final boolean track(float x, float y) {
        if (this == scene().eye()) {
            Scene.showOnlyEyeWarning("checkIfGrabsInput", false);
            return false;
        }
        if (pickingPrecision() != PickingPrecision.EXACT || !scene().isPickingBufferEnabled())
            return super.track(x, y);
        int index = (int) y * scene().width() + (int) x;
        if ((0 <= index) && (index < scene().pickingBuffer().pixels.length))
            return scene().pickingBuffer().pixels[index] == id();
        return false;
    }
}
