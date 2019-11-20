package ik.animation.eventVisualizer;

import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import processing.core.PGraphics;

public class Slot extends Node {
    protected EventCell _cell;
    protected int _color , _colorLine, _colorHighLight, _duration;
    protected Vector _endPoint;

    public Slot(EventCell cell, Vector t, int color, int duration){
        super(cell);
        _cell = cell;
        _color = color;
        _colorLine = _cell._board._white;
        _colorHighLight = _cell._board._green2;
        _duration = duration;
        _endPoint = new Vector(_duration * _cell._board._cellWidth, 0);
        setPickingThreshold(0);
        setHighlighting(0);
        translate(t);
        setConstraint(new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Node node) {
                return new Vector();
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Node node) {
                return new Quaternion();
            }
        });

    }

    @Override
    public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.stroke(graph().node() == this ? _colorHighLight : _colorLine);
        pg.strokeWeight(graph().node() == this ? 3 : 1);
        pg.line(0,0, _endPoint.x(), _endPoint.y());
        pg.fill(_color);
        pg.ellipse(0,0,_cell._board._pointDiameter, _cell._board._pointDiameter);
        pg.ellipse(_endPoint.x(),_endPoint.y(),_cell._board._pointDiameter, _cell._board._pointDiameter);
        pg.popStyle();
    }

    @Override
    public void interact(Object... gesture) {
        String command = (String) gesture[0];
        if(command.equals("OnMovement")){
            setEndPoint(translateDesired((Vector) gesture[1]));
        } else if(command.equals("OnFinishedMovement")){
            //set end point to the closest slot
            updateEndPoint();
        }
    }

    protected void setEndPoint(Vector v){
        if(v.x() < 0) v.setX(0);
        v.setY(0);
        _endPoint = v;
        _duration = Math.round(_endPoint.x() /_cell._board._cellWidth);
        _cell._board._fixRowOverlapping(_cell._row);
    }

    protected void updateEndPoint(){
        //update duration
        _duration = Math.round(_endPoint.x() /_cell._board._cellWidth);
        float x =  _duration * _cell._board._cellWidth;
        _endPoint.setX(x);
        //update row
        _cell._board._fixRowOverlapping(_cell._row);
    }

    protected void _updateTranslation(float x, float y){
        Constraint constraint = constraint();
        setConstraint(null);
        setTranslation(x, y);
        setConstraint(constraint);
        //update end point
        _endPoint = new Vector(_duration * _cell._board._cellWidth, 0);
    }


    //------------------------------------
    //Interactive actions - same method found in Graph Class (duplicated cause of visibility)
    protected Vector _translateDesired(Graph graph, float dx, float dy, float dz, int zMax, Node node) {
        if (graph.is2D() && dz != 0) {
            System.out.println("Warning: graph is 2D. Z-translation reset");
            dz = 0;
        }
        dx = graph.isEye(node) ? -dx : dx;
        dy = graph.isRightHanded() ^ graph.isEye(node) ? -dy : dy;
        dz = graph.isEye(node) ? dz : -dz;
        // Scale to fit the screen relative vector displacement
        if (graph.type() == Graph.Type.PERSPECTIVE) {
            float k = (float) Math.tan(graph.fov() / 2.0f) * Math.abs(
                    graph.eye().location(graph.isEye(node) ? graph.anchor() : node.position())._vector[2] * graph.eye().magnitude());
            //TODO check me weird to find height instead of width working (may it has to do with fov?)
            dx *= 2.0 * k / (graph.height() * graph.eye().magnitude());
            dy *= 2.0 * k / (graph.height() *graph. eye().magnitude());
        }
        // this expresses the dz coordinate in world units:
        //Vector eyeVector = new Vector(dx, dy, dz / eye().magnitude());
        Vector eyeVector = new Vector(dx, dy, dz * 2 * graph.radius() / zMax);
        return node.reference() == null ? graph.eye().worldDisplacement(eyeVector) : node.reference().displacement(eyeVector, graph.eye());
    }


    public Vector translateDesired(Vector point){
        Vector delta = Vector.subtract(point, graph().screenLocation(position()));
        return _translateDesired(graph(), delta.x(), delta.y(), 0, Math.min(graph().width(), graph().height()), this);
    }

}

