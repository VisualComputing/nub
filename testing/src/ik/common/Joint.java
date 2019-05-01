package ik.common;

/**
 * Created by sebchaparr on 21/07/18.
 */

import nub.primitives.Vector;
import nub.processing.Scene;
import nub.core.Node;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;

public class Joint extends Node {
    public static boolean deph = false;
    protected String _name;
    protected int _color;
    protected float _radius;
    protected static PGraphics _pGraphics;
    public static boolean axes = true;
    //set to true only when the joint is the root (for rendering purposes)
    protected boolean _isRoot = false, _drawConstraint = true;


    public Joint(Scene scene, int color, float radius){
        super(scene);
        _color = color;
        _radius = radius;
        _pGraphics = scene.context();
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

    public void setDrawConstraint(boolean drawConstraint){
        _drawConstraint = drawConstraint;
    }

    @Override
    public void visit(){
        Scene scene = (Scene) this._graph;
        PGraphics pg = _pGraphics;
        if(!deph)pg.hint(PConstants.DISABLE_DEPTH_TEST);
        pg.pushStyle();
        if (!_isRoot) {
            pg.strokeWeight(_radius/4f);
            pg.stroke(_color);
            Vector v = location(new Vector(), reference());
            float m = v.magnitude();
            if (scene.is2D()) {
                pg.line(_radius * v.x() / m, _radius * v.y() / m, (m - _radius) * v.x() / m, (m - _radius) * v.y() / m);
            } else {
                pg.line(_radius * v.x() / m, _radius * v.y() / m, _radius * v.z() / m, (m - _radius) * v.x() / m, (m - _radius) * v.y() / m, (m - _radius) * v.z() / m);
            }
        }
        pg.fill(_color);
        pg.noStroke();
        if (scene.is2D()) pg.ellipse(0, 0, _radius*2, _radius*2);
        else pg.sphere(_radius);
        pg.strokeWeight(_radius/4f);
        if (constraint() != null && _drawConstraint) {
            scene.drawConstraint(pg,this);
        }
        //scene.drawCross(this);
        //if (scene.is3D()) scene.drawAxes(_radius*2);
        if(!deph) pg.hint(PConstants.ENABLE_DEPTH_TEST);
        if(axes) scene.drawAxes(_radius*2);
        pg.popStyle();

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
