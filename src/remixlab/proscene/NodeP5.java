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
import remixlab.primitives.Frame;
import remixlab.primitives.Vector;

public class NodeP5  extends Node {
    Scene _scene;
    PShape _frontShape, _backShape;
    public enum Highlighting {
        NONE, FRONT_SHAPE, FRONT_PICKING_SHAPES, PICKING_SHAPE
    }
    Highlighting _highlight;
    Vector _shift;

    public NodeP5(Scene scene) {
        super(scene);
        _scene = scene;
        if(_scene.frontBuffer() instanceof PGraphicsOpenGL)
            setPrecision(Precision.EXACT);
        setShift(new Vector());
        setHighlighting(Highlighting.FRONT_SHAPE);
    }

    public NodeP5(NodeP5 reference) {
        super(reference);
        _scene = reference.scene();
        if(_scene.frontBuffer() instanceof PGraphicsOpenGL)
            setPrecision(Precision.EXACT);
        setShift(new Vector());
        setHighlighting(Highlighting.FRONT_SHAPE);
    }

    protected NodeP5(Scene otherGraph, NodeP5 otherNodeP5) {
        super(otherGraph, otherNodeP5);
    }

    @Override
    public NodeP5 get() {
        return new NodeP5(scene(), this);
    }

    public Scene scene() {
        return _scene;
    }

    /**
     * Enables highlighting of the frame visual representation when picking takes place
     * ({@link #grabsInput()} returns {@code true}) according to {@code mode} as follows:
     * <p>
     * <ol>
     * <li>{@link Highlighting#NONE}: no highlighting
     * takes place.</li>
     * <li>{@link Highlighting#FRONT_SHAPE}: the
     * front-shape (see {@link #setFrontShape(PShape)}) is scaled by a {@code 1.15}
     * factor.</li>
     * <li>{@link Highlighting#PICKING_SHAPE}: the
     * picking-shape (see {@link #setBackShape(PShape)} is displayed instead of the
     * front-shape.</li>
     * <li>{@link Highlighting#FRONT_PICKING_SHAPES}:
     * both, the front and the picking shapes are displayed.</li>
     * </ol>
     * <p>
     * Default is {@link Highlighting#FRONT_SHAPE}.
     *
     * @see #highlighting()
     */
    public void setHighlighting(Highlighting mode) {
        _highlight = mode;
    }

    /**
     * Returns the highlighting mode.
     *
     * @see #setHighlighting(Highlighting)
     */
    public Highlighting highlighting() {
        return _highlight;
    }

    @Override
    public void setPrecision(Precision precision) {
        if (precision == Precision.EXACT)
            if (scene()._bb == null) {
                System.out.println("Warning: EXACT picking precision is not enabled by your PGraphics.");
                return;
            }
        this._Precision = precision;
        // enables or disables the grabbing buffer
        if (precision() == Precision.EXACT) {
            scene()._bbEnabled = true;
            return;
        }
        for (Node node : scene().nodes())
            if(node instanceof NodeP5)
                if (node.precision() == Precision.EXACT) {
                    scene()._bbEnabled = true;
                    return;
                }
        scene()._bbEnabled = false;
    }
    
    /**
     * Same as {@code draw(scene.pg())}.
     *
     * @see remixlab.proscene.Scene#traverse(PGraphics)
     */
    public void draw() {
        draw(scene().frontBuffer());
    }

    /**
     * Draw the visual representation of the node into the given PGraphics using the
     * current point of view (see
     * {@link remixlab.proscene.Scene#applyTransformation(PGraphics, Frame)}).
     * <p>
     * This method is internally called by {@link Scene#traverse(PGraphics)} to draw
     * the node into the {@link Scene#backBuffer()} and by {@link #draw()} to draw
     * the node into the scene main {@link Scene#frontBuffer()}.
     */
    public boolean draw(PGraphics pg) {
        pg.pushMatrix();
        Scene.applyWorldTransformation(pg, this);
        visit(pg);
        pg.popMatrix();
        return true;
    }

    @Override
    public void visit() {
        visit(scene()._targetPGraphics);
    }

    protected void visit(PGraphics pg) {
        if (scene().eye() == this)
            return;
        if(pg != scene().backBuffer()) {
            pg.pushStyle();
            pg.pushMatrix();
            /*
            if(_frontShape != null)
                pg.shape(_frontShape);
            setShape(pg);
            setFrontShape(pg);
            //*/
            ///*
            if (pg.is3D())
                pg.translate(_shift.x(), _shift.y(), _shift.z());
            else
                pg.translate(_shift.x(), _shift.y());
            switch (highlighting()) {
                case FRONT_SHAPE:
                    if (grabsInput())
                        pg.scale(1.15f);
                case NONE:
                    if(_frontShape != null)
                        pg.shape(_frontShape);
                    setShape(pg);
                    break;
                case FRONT_PICKING_SHAPES:
                    if(_frontShape != null)
                        pg.shape(_frontShape);
                    setFrontShape(pg);
                    if (grabsInput()) {
                        if (_backShape != null)
                            pg.shape(_backShape);
                        setBackShape(pg);
                    }
                    break;
                case PICKING_SHAPE:
                    if (grabsInput()) {
                        if (_backShape != null)
                            pg.shape(_backShape);
                        setBackShape(pg);
                    }
                    else {
                        if(_frontShape != null)
                            pg.shape(_frontShape);
                        setFrontShape(pg);
                    }
                    break;
            }
            //*/
            pg.popStyle();
            pg.popMatrix();
        }
        else {
            if (precision() == Precision.EXACT) {
                float r = (float) (_id & 255) / 255.f;
                float g = (float) ((_id >> 8) & 255) / 255.f;
                float b = (float) ((_id >> 16) & 255) / 255.f;
                // funny, only safe way. Otherwise break things horribly when setting shapes
                // and there are more than one iFrame
                pg.shader(scene()._triangleShader);
                pg.shader(scene()._lineShader, PApplet.LINES);
                pg.shader(scene()._pointShader, PApplet.POINTS);

                scene()._triangleShader.set("id", new PVector(r, g, b));
                scene()._lineShader.set("id", new PVector(r, g, b));
                scene()._pointShader.set("id", new PVector(r, g, b));
                pg.pushStyle();
                pg.pushMatrix();
                /*
                if (_backShape != null)
                    pg.shape(_backShape);
                setShape(pg);
                setBackShape(pg);
                //*/
                ///*
                if (pg.is3D())
                    pg.translate(_shift.x(), _shift.y(), _shift.z());
                else
                    pg.translate(_shift.x(), _shift.y());
                if (_frontShape != null)
                    pg.shapeMode(scene().frontBuffer().shapeMode);
                if (_backShape != null)
                    pg.shape(_backShape);
                setShape(pg);
                setBackShape(pg);
                //*/
                pg.popStyle();
                pg.popMatrix();
            }
        }
    }

    /**
     * Same as {@code shiftFrontShape(_shift); shiftPickingShape(_shift)}.
     * <p>
     * This method is only meaningful when frame is not eyeFrame.
     *
     * @see #shift()
     */
    public void setShift(Vector s) {
        _shift = s;
    }

    public Vector shift() {
        return _shift;
    }

    protected void setShape(PGraphics pg) {
    }

    protected void setFrontShape(PGraphics pg) {
    }

    protected void setBackShape(PGraphics pg) {
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
     * color buffer (see {@link remixlab.proscene.Scene#backBuffer()}). This method
     * compares the color of the {@link remixlab.proscene.Scene#backBuffer()} at
     * {@code (x,y)} with {@link #id()}. Returns true if both colors are the same, and false
     * otherwise.
     * <p>
     * This method is only meaningful when this node is not an eye.
     *
     * @see #setPrecision(Precision)
     */
    @Override
    public final boolean track(float x, float y) {
        if (this == scene().eye()) {
            Scene.showOnlyEyeWarning("checkIfGrabsInput", false);
            return false;
        }
        if (precision() != Precision.EXACT)
            return super.track(x, y);
        int index = (int) y * scene().width() + (int) x;
        if ((0 <= index) && (index < scene().backBuffer().pixels.length))
            return scene().backBuffer().pixels[index] == id();
        return false;
    }
}
