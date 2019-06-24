package ik.constraintTest;

import nub.core.Node;
import nub.core.Graph;
import nub.core.constraint.Hinge;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.ik.visual.Joint;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;


public class SimpleHinge extends PApplet {
    float constraint_factor_x = 180;
    float constraint_factor_y = 180;
    float boneLength = 50;
    float radius = 10;

    Scene scene;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(200);
        scene.fit(1);
        scene.setRightHanded();

        PShape redBall = createShape(SPHERE, radius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));

        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2.f));
        scene.eye().rotate(new Quaternion(new Vector(0,1,0), PI));

        //Generate a basic structure
        Joint j1 = new Joint(scene, radius);
        Joint j2 = new Joint(scene, radius);
        j2.setReference(j1);
        j2.translate(0,50,0);
        Joint j3 = new Joint(scene, radius);
        j3.setReference(j2);
        j3.translate(30,30,0);

        Hinge c1 = new Hinge(radians(30),radians(30), j1.rotation(), new Vector(0,1,0), new Vector(0,0,1));
        j1.setConstraint(c1);

        Hinge c2 = new Hinge(radians(180),radians(50), j2.rotation().get(), j3.translation().get(), Vector.cross(new Vector(0,-1,0), j3.translation(), null));
        j2.setConstraint(c2);

    }

    public void draw() {
        background(0);
        lights();
        scene.drawAxes();
        scene.render();
    }

    Node n = null;
    float d = 5;
    public void keyPressed(){
        if(scene.trackedNode() != null) {
            n =  scene.trackedNode();
            if(n != null) {
                if (key == 'A' || key == 'a') {
                    d += 1;
                    System.out.println("Speed --- : " + d);
                }
                if (key == 's' || key == 'S') {
                    d *= -1;
                    System.out.println("Speed --- : " + d);
                }
                if (key == 'X' || key == 'x') {
                    n.rotate(new Quaternion(new Vector(1, 0, 0), radians(d)));
                }
                if (key == 'Y' || key == 'y') {
                    n.rotate(new Quaternion(new Vector(0, 1, 0), radians(d)));
                }
                if (key == 'Z' || key == 'z') {
                    n.rotate(new Quaternion(new Vector(0, 0, 1), radians(d)));
                }
            }
        }
    }

    @Override
    public void mouseMoved() {
        scene.cast();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            scene.spin();
        } else if (mouseButton == RIGHT) {
            scene.translate();
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
        PApplet.main(new String[]{"ik.constraintTest.SimpleHinge"});
    }

}

