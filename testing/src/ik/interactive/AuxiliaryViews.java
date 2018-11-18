package ik.interactive;

import frames.core.Frame;
import frames.core.Graph;
import frames.primitives.Point;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public class AuxiliaryViews extends PApplet {

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
        //create an auxiliary view per Orthogonal Plane
        views = new ArrayList<AuxiliaryView>();
        //create an auxiliary view to look at the XY Plane
        Frame eyeXY = new Frame();
        eyeXY.setPosition(0, 0, scene.radius());
        views.add(new AuxiliaryView(scene, eyeXY, 0, 2*h/3, w/3, h/3));
        //create an auxiliary view to look at the XY Plane
        Frame eyeXZ = new Frame();
        eyeXZ.setPosition(0, scene.radius(), 0);
        eyeXZ.setOrientation(new Quaternion(new Vector(1,0,0), -HALF_PI));
        views.add(new AuxiliaryView(scene, eyeXZ, w/3, 2*h/3, w/3, h/3));
        //create an auxiliary view to look at the XY Plane
        Frame eyeYZ = new Frame();
        eyeYZ.setPosition(scene.radius(), 0, 0);
        eyeYZ.setOrientation(new Quaternion(new Vector(0,1,0), HALF_PI));
        views.add(new AuxiliaryView(scene, eyeYZ, 2*w/3, 2*h/3, w/3, h/3));

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

        for(AuxiliaryView view : views) {
            view.draw();
            view.display();
        }
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
        if (key == 'p') {
            scene.eye().position().print();
            scene.eye().orientation().print();
        }
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
