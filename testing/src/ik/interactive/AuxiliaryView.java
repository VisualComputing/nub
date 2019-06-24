package ik.interactive;

import nub.core.Node;
import nub.core.Graph;
import nub.primitives.Point;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PGraphics;

public class AuxiliaryView{
    boolean _enabled = true;
    Scene _scene;
    Node _eye;
    Graph.Type _type;
    PGraphics _pGraphics;
    float _x, _y;
    int _width, _height;
    int _colorBackground = 0;
    Vector _center;
    float _radius;

    public AuxiliaryView(Scene scene, PGraphics pGraphics, Node eye, float x, float y, int width, int height){
        _scene = scene;
        _eye = eye;
        _x = x;
        _y = y;
        _width = width;
        _height = height;
        _pGraphics = pGraphics;
        _type = scene.type();
        _center = scene.center().get();
        _radius = scene.radius();
    }

    public AuxiliaryView(Scene scene, Node eye, float x, float y, int width, int height){
        this(scene, scene.pApplet().createGraphics(scene.width(), scene.height(), scene.pApplet().sketchRenderer()), eye, x, y, width, height);
    }

    public boolean enabled(){
        return _enabled;
    }

    public void setEnabled(boolean enabled){
        _enabled = enabled;
    }

    public Graph.Type type(){
        return _type;
    }

    public void setType(Graph.Type type){
        _type = type;
    }

    public Scene scene(){
        return _scene;
    }

    public Node eye(){
        return _eye;
    }

    public Vector center(){
        return _center;
    }

    public void setCenter(Vector center){
        _center = center;
    }

    public float radius(){
        return _radius;
    }

    public void setRadius(float radius){
        _radius = radius;
    }

    public void setBackground(int color){
        _colorBackground = color;
    }

    public Point cursorLocation(float x, float y){
        return new Point((x - _x)*(1.f*_scene.width()/_width), (y - _y)*(1.f*_scene.height()/_height));
    }

    public boolean focus(float x, float y){
        return _enabled && (x >= _x && x <= _x + _width) && (y >= _y && y <= _y + _height);
    }

    //TODO : AVOID CODE DUPLICATION
    //SAME METHOD FOUND IN GRAPH
    public float zNear() {
        float z = Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis()) - scene().zClippingCoefficient() * radius();

        // Prevents negative or null zNear values.
        float zMin = scene().zNearCoefficient() * scene().zClippingCoefficient() * radius();
        if (z < zMin)
            switch (type()) {
                case PERSPECTIVE:
                    z = zMin;
                    break;
                case TWO_D:
                case ORTHOGRAPHIC:
                    z = 0.0f;
                    break;
            }
        return z;
    }

    public float zFar() {
        return Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis()) + scene().zClippingCoefficient() * radius();
    }

    //Things to be drawn in addition to Frames attached to scene
    public void draw(){
        if(_enabled){
            _pGraphics.beginDraw();
            _pGraphics.background(_colorBackground);
            _scene.render(_pGraphics, _type, _eye, zNear(), zFar());
            _pGraphics.endDraw();
        }
    }

    public void display(){
        _scene.beginHUD();
        _scene.pApplet().image(_pGraphics, _x, _y, _width, _height);
        _scene.endHUD();
    }
}
