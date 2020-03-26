package ik.biped;

import nub.core.Node;
import nub.core.constraint.PlanarPolygon;
import nub.ik.solver.geometric.ChainSolver;
import nub.ik.solver.geometric.ClosedLoopChainSolver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PConstants;
import processing.core.PGraphics;

import java.util.ArrayList;

import static processing.core.PConstants.PI;
import static processing.core.PApplet.cos;
import static processing.core.PApplet.sin;
import static processing.core.PApplet.radians;

/**
 * Created by sebchaparr on 7/08/18.
 */

//TODO : Update

public class Biped {
    ClosedLoopChainSolver upper_body;
    ClosedLoopChainSolver lower_body;
    ChainSolver left_arm, right_arm;
    ChainSolver left_leg, right_leg;
    Scene scene;
    Node left_arm_target;
    Node right_arm_target;
    Node left_leg_target;
    Node right_leg_target;
    int color;

    public Biped(Scene scene, Node reference, Vector translation){
        this.scene = scene;
        createBiped(reference, translation);
        color = scene.pApplet().color(0,255,0);
    }

    public void createBiped(Node reference, Vector translation){
        //Chain Left Arm
        ArrayList<Node> upper, lower, l_arm, r_arm, l_leg, r_leg;
        upper = new ArrayList<>();
        lower = new ArrayList<>();
        l_arm = new ArrayList<>();
        r_arm = new ArrayList<>();
        l_leg = new ArrayList<>();
        r_leg = new ArrayList<>();

        Node la_0 = new Node();
        la_0.setReference(reference);
        la_0.translate(translation);
        setConstraint(la_0, 25, new Vector(0,-1), 30);
        Node la_1 = new Node();
        la_1.setReference(la_0);
        la_1.translate(-50,-100, 0);
        setConstraint(la_1, 25, new Vector(-25,50), 60);
        Node la_2 = new Node();
        la_2.setReference(la_1);
        la_2.translate(-25,50, 0);
        setConstraint(la_2, 25, new Vector(-25,50), 60);
        Node la_3 = new Node();
        la_3.setReference(la_2);
        la_3.translate(-25,50, 0);
        l_arm.add(la_0);
        l_arm.add(la_1);
        l_arm.add(la_2);
        l_arm.add(la_3);
        //Chain right Arm
        Node ra_0 = new Node();
        ra_0.setReference(reference);
        ra_0.translate(translation);
        setConstraint(ra_0, 25, new Vector(0,-1), 30);
        Node ra_1 = new Node();
        ra_1.setReference(ra_0);
        ra_1.translate(50,-100, 0);
        setConstraint(ra_1, 25, new Vector(25,50), 60);
        Node ra_2 = new Node();
        ra_2.setReference(ra_1);
        ra_2.translate(25,50, 0);
        setConstraint(ra_2, 25, new Vector(25,50), 60);
        Node ra_3 = new Node();
        ra_3.setReference(ra_2);
        ra_3.translate(25,50, 0);
        r_arm.add(ra_0);
        r_arm.add(ra_1);
        r_arm.add(ra_2);
        r_arm.add(ra_3);

        //Chain Left Leg
        Node ll_0 = new Node();
        ll_0.translate(translation);
        Node ll_1 = new Node();
        ll_1.setReference(ll_0);
        ll_1.translate(-25,50, 0);
        Node ll_2 = new Node();
        ll_2.setReference(ll_1);
        ll_2.translate(0,50, 0);
        Node ll_3 = new Node();
        ll_3.setReference(ll_2);
        ll_3.translate(0,50, 0);
        l_leg.add(ll_0);
        l_leg.add(ll_1);
        l_leg.add(ll_2);
        l_leg.add(ll_3);

        //Chain Right Leg
        Node rl_0 = new Node();
        rl_0.setReference(reference);
        rl_0.translate(translation);
        Node rl_1 = new Node();
        rl_1.setReference(rl_0);
        rl_1.translate(25,50, 0);
        Node rl_2 = new Node();
        rl_2.setReference(rl_1);
        rl_2.translate(0,50, 0);
        Node rl_3 = new Node();
        rl_3.setReference(rl_2);
        rl_3.translate(0,50, 0);
        r_leg.add(rl_0);
        r_leg.add(rl_1);
        r_leg.add(rl_2);
        r_leg.add(rl_3);

        //Upper Loop
        Node ul_0 = new Node();
        ul_0.setReference(reference);
        ul_0.translate(translation);
        Node ul_1 = new Node();
        ul_1.setReference(ul_0);
        ul_1.translate(-50,-100, 0);
        Node ul_2 = new Node();
        ul_2.setReference(ul_0);
        ul_2.translate(50,-100, 0);
        upper.add(ul_0);
        upper.add(ul_1);
        upper.add(ul_2);
        //Lower Loop
        Node lol_0 = new Node();
        lol_0.setReference(reference);
        lol_0.translate(translation);
        Node lol_1 = new Node();
        lol_1.setReference(lol_0);
        lol_1.translate(-25,50, 0);
        Node lol_2 = new Node();
        lol_2.setReference(lol_0);
        lol_2.translate(25,50, 0);
        lower.add(lol_0);
        lower.add(lol_1);
        lower.add(lol_2);

        //prepare solvers
        upper_body = new ClosedLoopChainSolver(upper);
        lower_body = new ClosedLoopChainSolver(lower);
        left_arm = new ChainSolver(l_arm);
        right_arm = new ChainSolver(r_arm);
        left_leg = new ChainSolver(l_leg);
        right_leg = new ChainSolver(r_leg);
        //set number of _iterations
        upper_body.setMaxIterations(5);
        lower_body.setMaxIterations(5);
        left_arm.setMaxIterations(5);
        right_arm.setMaxIterations(5);
        left_leg.setMaxIterations(5);
        right_leg.setMaxIterations(5);
    }

    void setConstraint(Node frame, float boneLength, Vector twist, float degrees){
        ArrayList<Vector> vertices = new ArrayList<Vector>();
        int sides = 10;
        float radius = 40;
        float step = 2*PI/sides;
        for(int i = 0; i < sides; i++){
            float angle = i*step;
            vertices.add(new Vector(radius*cos(angle), radius*sin(angle)));
        }
        PlanarPolygon constraint = new PlanarPolygon(vertices);
        constraint.setAngle(radians(degrees));
        constraint.setRestRotation(frame.rotation().get(), twist.orthogonalVector(), twist);
        frame.setConstraint(constraint);
    }


    void updateClosedLoopTargets(ClosedLoopChainSolver solver, ChainSolver left, ChainSolver right){
        solver.setUnknown(1, left.chain().get(1).position());
        solver.setUnknown(2, right.chain().get(1).position());
        solver.setChildPosition(1, left.chain().get(2).position());
        solver.setChildPosition(2, right.chain().get(2).position());
        solver.setChildDistance(1, Vector.distance(left.chain().get(1).position(), left.chain().get(2).position()));
        solver.setChildDistance(2, Vector.distance(right.chain().get(1).position(), right.chain().get(2).position()));
    }

    void fixRotation(Node frame, Vector new_position){
        Node reference = frame.reference();
        if(reference == null){
            //Perhaps translate to the given position
            return;
        }
        Vector translation = frame.translation();
        Vector new_translation = reference.location(new_position);
        Quaternion quaternion = new Quaternion(translation, new_translation);
        reference.rotate(quaternion);
    }

    void updateAfterFixing(){
        fixRotation(left_arm.chain().get(1), upper_body.positions().get(1));
        fixRotation(right_arm.chain().get(1), upper_body.positions().get(2));
        fixRotation(left_leg.chain().get(1), lower_body.positions().get(1));
        fixRotation(right_leg.chain().get(1), lower_body.positions().get(2));
    }

    public void solve() {
        //solve arms
        left_arm.hasChanged(true);
        right_arm.hasChanged(true);
        left_leg.hasChanged(true);
        right_leg.hasChanged(true);

        left_arm.solve();
        right_arm.solve();
        //solve legs
        left_leg.solve();
        right_leg.solve();
        //fix distance constraints
        updateClosedLoopTargets(upper_body, left_arm, right_arm);
        updateClosedLoopTargets(lower_body, left_leg, right_leg);
        upper_body.solve();
        lower_body.solve();
        //set chain positions to obtained ones
        updateAfterFixing();
    }

    public void draw() {
        PGraphics pg = scene.context();
        pg.hint(PConstants.DISABLE_DEPTH_TEST);
        pg.pushStyle();
        pg.fill(color);
        //draw arms
        for (Node frame : left_arm.chain()) {
            drawJoint(pg, frame);
        }
        for (Node frame : right_arm.chain()) {
            drawJoint(pg, frame);
        }
        //draw legs
        for (Node frame : left_leg.chain()) {
            drawJoint(pg, frame);
        }
        for (Node frame : right_leg.chain()) {
            drawJoint(pg, frame);
        }
        for (Node frame : upper_body.chain()) {
            drawJoint(pg, frame);
        }

        Vector v = upper_body.positions().get(1);
        pg.pushMatrix();
        pg.translate(v.x(), v.y(), v.z());
        pg.sphere(8);
        pg.popMatrix();
        v = upper_body.positions().get(2);
        pg.pushMatrix();
        pg.translate(v.x(), v.y(), v.z());
        pg.sphere(8);
        pg.popMatrix();

        for (Node frame : lower_body.chain()) {
            drawJoint(pg, frame);
        }

        pg.hint(PConstants.ENABLE_DEPTH_TEST);
        pg.popStyle();
    }

    void drawJoint(PGraphics pg, Node frame){
        pg.pushMatrix();
        scene.applyWorldTransformation(frame);
        pg.noStroke();
        pg.sphere(5);
        pg.stroke(color);
        Vector v = frame.location(new Vector(), frame.reference());
        pg.line(0, 0, 0, v.x(), v.y(), v.z());
        if (frame.constraint() != null) {
            scene.drawConstraint(frame);
        }
        pg.popMatrix();
    }
}
