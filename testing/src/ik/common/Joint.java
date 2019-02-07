package ik.common;

/**
 * Created by sebchaparr on 21/07/18.
 */

import frames.core.Frame;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;

public class Joint extends Frame{
    public static boolean deph = false;
    protected String _name;
    protected int _color;
    protected float _radius;
    protected static PGraphics _pGraphics;
    //set to true only when the joint is the root (for rendering purposes)
    protected boolean _isRoot = false;

    public Joint(Scene scene, int color, float radius){
        super(scene);
        _color = color;
        _radius = radius;
        _pGraphics = scene.frontBuffer();
        setPickingThreshold(_radius*2);
    }

    public Joint(Scene scene, int color){
        this(scene, color, 5);
    }

    public Joint(Scene scene){
        this(scene, scene.pApplet().color(scene.pApplet().random(0,255),scene.pApplet().random(0,255), scene.pApplet().random(0,255)));
    }

    public Joint(Scene scene, float radius){
        this(scene, scene.pApplet().color(scene.pApplet().random(0,255),scene.pApplet().random(0,255), scene.pApplet().random(0,255)), radius);
    }

    public static void setPGraphics(PGraphics pg){
        _pGraphics = pg;
    }

    @Override
    public void visit(){
        Scene scene = (Scene) this._graph;
        PGraphics pg = _pGraphics;
        if(!deph)pg.hint(PConstants.DISABLE_DEPTH_TEST);
        pg.pushStyle();
        if (!_isRoot) {
            pg.strokeWeight(_radius/4);
            pg.stroke(_color);
            Vector v = location(new Vector(), reference());
            float m = v.magnitude();
            if (scene.is2D()) {
                pg.line(_radius * v.x() / m, _radius * v.y() / m, (m - _radius) * v.x() / m, (m - _radius) * v.y() / m);
            } else {
                pg.line(_radius * v.x() / m, _radius * v.y() / m, _radius * v.z() / m, (m - _radius) * v.x() / m, (m - _radius) * v.y() / m, (m - _radius) * v.z() / m);
            }
        }
        pg.fill(255);
        pg.noStroke();
        if (scene.is2D()) pg.ellipse(0, 0, _radius*2, _radius*2);
        else pg.sphere(_radius);
        pg.popStyle();

        if (constraint() != null) {
            scene.drawConstraint(pg,this);
        }
        pg.stroke(255);
        //scene.drawCross(this);
        //if (scene.is3D()) scene.drawAxes(_radius*2);
        if(!deph) pg.hint(PConstants.ENABLE_DEPTH_TEST);
    }

    public void setRadius(float radius){
        _radius = radius;
        setPickingThreshold(_radius*2);
    }
    public void setName(String name){
        _name = name;
    }
    public void setRoot(boolean isRoot){
        _isRoot = isRoot;
    }
    public float radius(){
        return _radius;
    }
}
