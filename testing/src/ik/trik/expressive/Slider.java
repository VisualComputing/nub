package ik.trik.expressive;

import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import processing.core.PConstants;
import processing.core.PGraphics;

public class Slider extends Node {
    protected DelegationPanel _panel;
    protected float _value; //relative value between 0 and 1
    protected int _idx;
    protected Vector _endPoint = new Vector();

    public Slider(DelegationPanel panel, int i, Vector t, float value){
        super(panel);
        _idx = i;
        _panel = panel;
        _value = value;
        setPickingThreshold(0);
        setHighlighting(0);
        translate(t);
        updateEndPoint();
    }

    @Override
    public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.stroke(_panel._green1);
        pg.fill(_panel._scene.node() == this ? _panel._colorSliderHighlight : _panel._colorSlider);
        pg.strokeWeight(_panel._scene.node() == this ? 3 : 1);
        pg.rect(0, -_value * _panel._sliderHeight, _panel._sliderWidth, _value * _panel._sliderHeight);
        pg.fill(_panel._green1);
        pg.ellipse(_panel._sliderWidth * 0.5f, _endPoint.y(), _panel._sliderWidth * 0.1f,_panel._sliderWidth * 0.1f);

        pg.fill(_panel._colorText);
        pg.textFont(_panel._font36);
        pg.textSize(16);
        pg.textAlign(PConstants.CENTER, PConstants.CENTER);
        pg.text("" + _idx, 0, 0, _panel._sliderWidth, _panel._sliderHeight * 0.1f);
        pg.textSize(10);
        pg.textAlign(PConstants.CENTER, PConstants.CENTER);
        pg.text("" + String.format("%.3f", _value), 0, -_value * _panel._sliderHeight, _panel._sliderWidth, _value * _panel._sliderHeight);

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
            onValueChanged();
        }
    }

    //Override this method to apply an action when slider value has changed
    public void onValueChanged(){

    }

    public void setValue(float value){
        _value =  value;
        updateEndPoint();
    }

    protected void setEndPoint(Vector v){
        //clamp vector
        if(v.y() < -_panel._sliderHeight) v.setY(-_panel._sliderHeight);
        else if(v.y() > 0) v.setY(0);
        v.setX(0);
        _endPoint = v;
        _value = -v.y() / _panel._sliderHeight;
    }

    protected void updateEndPoint(){ //given current value update end point
        //update duration
        float y = -_value * _panel._sliderHeight;
        _endPoint.setY(y);
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
        Vector delta = Vector.subtract(point, _panel._scene.screenLocation(position()));
        return _translateDesired(_panel._scene, delta.x(), delta.y(), 0, Math.min(_panel._scene.width(), _panel._scene.height()), this);
    }
}
