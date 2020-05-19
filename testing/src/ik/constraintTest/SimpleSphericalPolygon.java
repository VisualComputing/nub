package ik.constraintTest;

import ik.basic.Util;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.SphericalPolygon;
import nub.ik.animation.Joint;
import nub.ik.solver.Solver;
import nub.ik.solver.trik.implementations.SimpleTRIK;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public class SimpleSphericalPolygon extends PApplet{
    Scene scene;
    Solver solver;
    Joint j1, j2;
    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(200);
        scene.fit(1);
        scene.setRightHanded();
        //Create a simple structure
        j1 = new Joint();
        j1.rotate(Quaternion.random());
        j2 = new Joint();
        j2.setReference(j1);
        j2.translate(100,0,0, 0);
        //Add a constraint
        //Defines vertices at theta degrees

        float theta = radians(120);
        float z = cos(theta);
        int times = 10;
        float step = 2 * PI / (times - 1);
        ArrayList<Vector> vertices = new ArrayList<Vector>();
        for(int i = 0; i < times; i++){
            float a = i * step;
            float x = cos(a);
            float y = sin(a);
            vertices.add(new Vector(x,y,z));
        }
        SphericalPolygon s1 = new SphericalPolygon(theta, 2 * theta);
        s1.setRestRotation(j1.rotation(), new Vector(0,0,1), new Vector(1,0,0));
        j1.setConstraint(s1);
        List<Node> skeleton = new ArrayList<Node>();
        skeleton.add(j1);
        skeleton.add(j2);
        solver = new SimpleTRIK(skeleton, SimpleTRIK.HeuristicMode.FINAL);
        Node target = Util.createTarget(scene, scene.radius() * 0.07f);
        target.set(j2);
        solver.setTarget(j2, target);
    }

    public void draw() {
        background(0);
        lights();
        scene.drawAxes();
        scene.render();
        solver.solve();
    }

    @Override
    public void mouseMoved() {
        scene.mouseTag();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT) {
            scene.mouseSpin();
        } else if (mouseButton == RIGHT) {
            scene.mouseTranslate();
        } else {
            scene.scale(scene.mouseDX());
        }
    }

    public void mouseWheel(MouseEvent event) {
        scene.scale(event.getCount() * 20);
    }

    public void mouseClicked(MouseEvent event) {
        if (event.getCount() == 2)
            if (event.getButton() == LEFT)
                scene.focus();
            else
                scene.align();
    }


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.constraintTest.SimpleSphericalPolygon"});
    }


}
