package ik.interactive;

import frames.core.Frame;
import frames.core.Graph;
import frames.primitives.Point;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public class AuxiliaryViews extends PApplet {
    public class AuxiliaryView{
        boolean _enabled = true;
        Scene _scene;
        Frame _eye;
        Graph.Type _type;
        PGraphics _pGraphics;
        float _x, _y;
        int _width, _height;
        float _zNear, _zFar;

        public AuxiliaryView(Scene scene, Frame eye, float x, float y, int width, int height){
            _scene = scene;
            _eye = eye;
            _x = x;
            _y = y;
            _width = width;
            _height = height;
            _pGraphics = scene.pApplet().createGraphics(scene.width(), scene.height(), scene.pApplet().sketchRenderer());
            _type = scene.type();
            _zFar = scene.zFar();
            _zNear = scene.zNear();
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

        public Frame eye(){
            return _eye;
        }

        public float zNear(){
            return _zNear;
        }

        public float zFar(){
            return _zFar;
        }

        public Point cursorLocation(float x, float y){
            return new Point((x - _x)*(1.f*_scene.width()/_width), (y - _y)*(1.f*_scene.height()/_height));
        }

        public boolean focus(float x, float y){
            return _enabled && (x >= _x && x <= _x + _width) && (y >= _y && y <= _y + _width);
        }

        public void draw(){
            if(_enabled){
                _pGraphics.beginDraw();
                _pGraphics.background(90, 80, 125);
                _scene.traverse(_pGraphics, _type, _eye, _zNear, _zFar);
                _pGraphics.endDraw();
                _scene.beginHUD();
                _scene.pApplet().image(_pGraphics, _x, _y, _width, _height);
                //Draw cursor position

                _scene.pApplet().pushStyle();
                _scene.pApplet().fill(255,0,0);
                _scene.pApplet().ellipse(cursorLocation(mouseX, mouseY).x(), cursorLocation(mouseX, mouseY).y(), 10, 10);
                _scene.pApplet().popStyle();
                _scene.endHUD();
            }
        }

    }

    Scene scene;
    Shape[] shapes;
    Shape light;
    int w = 1000;
    int h = 1000;
    List<AuxiliaryView> views;

    public void settings() {
        size(w, h, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setRadius(max(w, h));
        shapes = new Shape[20];
        for (int i = 0; i < shapes.length; i++) {
            shapes[i] = new Shape(scene);
            shapes[i].setGraphics(caja());
            shapes[i].randomize();
        }
        light = new Shape(scene) {
            @Override
            public void setGraphics(PGraphics pg) {
                pg.pushStyle();
                Scene.drawAxes(pg, 150);
                pg.fill(isTracked() ? 255 : 25, isTracked() ? 0 : 255, 255);
                //Scene.drawEye(pg, views.get(0)._pGraphics, views.get(0)._type, this, views.get(0)._zNear, views.get(0)._zFar);
                pg.popStyle();
            }
        };
        scene.fitBallInterpolation();
        //create an auxiliary view
        views = new ArrayList<AuxiliaryView>();
        views.add(new AuxiliaryView(scene, light, w/2, h/2, w/2, h/2));
    }

    public void draw() {
        background(90, 80, 125);
        scene.traverse();
        setBackBuffer(mouseX, mouseY);
        //Drawing back buffer
        /*
        scene.beginHUD();
        image(scene.backBuffer(), 0, 0);
        scene.endHUD();
        */

        for(AuxiliaryView view : views)
            view.draw();
    }

    public void setBackBuffer(float x, float y){
        for(AuxiliaryView view : views){
            //check bounds
            if(view.focus(x,y)){
                scene.backBuffer().beginDraw();
                scene.backBuffer().background(0);
                scene.traverse(view.scene().backBuffer(), view.type(), view.eye(), view.zNear(), view.zFar());
                scene.backBuffer().endDraw();
                scene.backBuffer().loadPixels();
                return;
            }
        }
    }

    public AuxiliaryView currentView(float x, float y){
        for(AuxiliaryView view : views) {
            if (view.focus(x, y)) return view;
        }
        return null;
    }


    public Point cursorLocation(float x , float y){
        for(AuxiliaryView view : views) {
            //check bounds
            if (view.focus(x, y)) return view.cursorLocation(x, y);
        }
        return new Point(x, y);
    }


    public void mouseMoved() {
        Point point = cursorLocation(mouseX, mouseY);
        scene.cast(point);
    }

    public void mouseDragged() {
        AuxiliaryView current = currentView(mouseX, mouseY);
        Point previous = current == null ? new Point(pmouseX, pmouseY) : current.cursorLocation(pmouseX, pmouseY);
        Point point = current == null ? new Point(mouseX, mouseY) : current.cursorLocation(mouseX, mouseY);
        Frame eye = scene.eye();
        Graph.Type type = scene.type();
        if(current != null){
            scene.setEye(current.eye());
            scene.setType(current.type());
        }
        if (mouseButton == LEFT)
            scene.spin(previous, point);
        else if (mouseButton == RIGHT)
            scene.translate(point.x() - previous.x(), point.y() - previous.y());
        else
            scene.moveForward(mouseX - pmouseX);
        if(current != null){
            scene.setEye(eye);
            scene.setType(type);
        }
    }

    public void mouseWheel(MouseEvent event) {
        scene.scale(event.getCount() * 20);
    }

    public void keyPressed() {
        if (key == '3')
            scene.setAperture(PI / 3);
        if (key == '4')
            scene.setAperture(PI / 4);
        if (key == ' ')
            for(AuxiliaryView view : views)
                view.setEnabled(!view.enabled());
        if (key == 'o')
            for(AuxiliaryView view : views)
                view.setType(view.type() == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC);
        if (key == 't') {
            scene.setType(scene.type() == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC);
        }
        if (key == 'p')
            scene.eye().position().print();
    }

    PShape caja() {
        PShape caja = scene.is3D() ? createShape(BOX, random(60, 100)) : createShape(RECT, 0, 0, random(60, 100), random(60, 100));
        caja.setStrokeWeight(3);
        caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
        caja.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
        return caja;
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.interactive.AuxiliaryViews"});
    }
}
