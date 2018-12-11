package ik.biped;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.constraint.FixedConstraint;
import frames.core.constraint.Hinge;
import frames.core.constraint.PlanarPolygon;
import frames.ik.Solver;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sebchaparr on 7/08/18.
 */

//TODO : Update
public class testBiped extends PApplet {
    Scene scene;
    ArrayList<Frame> targets;
    Biped biped;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setFOV(PI / 3);
        scene.setRadius(50 * 5);
        scene.fit(1);
        scene.disableBackBuffer();

        targets = new ArrayList<>();

        PShape redBall = createShape(SPHERE, 5);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));

        targets.add(new Shape(scene, redBall));
        targets.add(new Shape(scene, redBall));
        targets.add(new Shape(scene, redBall));
        targets.add(new Shape(scene, redBall));

        biped = new Biped(scene, null, new Vector());

        targets.get(0).setPosition(biped.left_arm.endEffector().position());
        biped.left_arm.setTarget(targets.get(0));
        targets.get(1).setPosition(biped.right_arm.endEffector().position());
        biped.right_arm.setTarget(targets.get(1));
        targets.get(2).setPosition(biped.left_leg.endEffector().position());
        biped.left_leg.setTarget(targets.get(2));
        targets.get(3).setPosition(biped.right_leg.endEffector().position());
        biped.right_leg.setTarget(targets.get(3));

    }
    public void draw() {
        background(0);
        lights();
        scene.drawAxes();
        biped.draw();
        biped.solve();
        scene.traverse();
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
        PApplet.main(new String[]{"ik.biped.testBiped"});
    }
}