package ik.biped;

import frames.core.Frame;
import frames.ik.ChainSolver;
import frames.ik.ClosedLoopChainSolver;
import frames.ik.Solver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PConstants;
import processing.core.PGraphics;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 7/08/18.
 */
public class Biped {
    ClosedLoopChainSolver upper_body;
    ClosedLoopChainSolver lower_body;
    ChainSolver left_arm, right_arm;
    ChainSolver left_leg, right_leg;
    Scene scene;
    Frame left_arm_target;
    Frame right_arm_target;
    Frame left_leg_target;
    Frame right_leg_target;
    int color;

    public Biped(Scene scene, Frame reference, Vector translation){
        this.scene = scene;
        createBiped(reference, translation);
        color = scene.pApplet().color(0,255,0);
    }

    public void createBiped(Frame reference, Vector translation){
        //Chain Left Arm
        ArrayList<Frame> upper, lower, l_arm, r_arm, l_leg, r_leg;
        upper = new ArrayList<>();
        lower = new ArrayList<>();
        l_arm = new ArrayList<>();
        r_arm = new ArrayList<>();
        l_leg = new ArrayList<>();
        r_leg = new ArrayList<>();

        Frame la_0 = new Frame(scene);
        la_0.setReference(reference);
        la_0.translate(translation);
        Frame la_1 = new Frame(scene);
        la_1.setReference(la_0);
        la_1.translate(-50,-100);
        Frame la_2 = new Frame(scene);
        la_2.setReference(la_1);
        la_2.translate(-25,50);
        Frame la_3 = new Frame(scene);
        la_3.setReference(la_2);
        la_3.translate(-25,50);
        l_arm.add(la_0);
        l_arm.add(la_1);
        l_arm.add(la_2);
        l_arm.add(la_3);
        //Chain right Arm
        Frame ra_0 = new Frame(scene);
        ra_0.setReference(reference);
        ra_0.translate(translation);
        Frame ra_1 = new Frame(scene);
        ra_1.setReference(ra_0);
        ra_1.translate(50,-100);
        Frame ra_2 = new Frame(scene);
        ra_2.setReference(ra_1);
        ra_2.translate(25,50);
        Frame ra_3 = new Frame(scene);
        ra_3.setReference(ra_2);
        ra_3.translate(25,50);
        r_arm.add(ra_0);
        r_arm.add(ra_1);
        r_arm.add(ra_2);
        r_arm.add(ra_3);

        //Chain Left Leg
        Frame ll_0 = new Frame(scene);
        ll_0.translate(translation);
        Frame ll_1 = new Frame(scene);
        ll_1.setReference(ll_0);
        ll_1.translate(-25,50);
        Frame ll_2 = new Frame(scene);
        ll_2.setReference(ll_1);
        ll_2.translate(0,50);
        Frame ll_3 = new Frame(scene);
        ll_3.setReference(ll_2);
        ll_3.translate(0,50);
        l_leg.add(ll_0);
        l_leg.add(ll_1);
        l_leg.add(ll_2);
        l_leg.add(ll_3);

        //Chain Right Leg
        Frame rl_0 = new Frame(scene);
        rl_0.setReference(reference);
        rl_0.translate(translation);
        Frame rl_1 = new Frame(scene);
        rl_1.setReference(rl_0);
        rl_1.translate(25,50);
        Frame rl_2 = new Frame(scene);
        rl_2.setReference(rl_1);
        rl_2.translate(0,50);
        Frame rl_3 = new Frame(scene);
        rl_3.setReference(rl_2);
        rl_3.translate(0,50);
        r_leg.add(rl_0);
        r_leg.add(rl_1);
        r_leg.add(rl_2);
        r_leg.add(rl_3);

        //Upper Loop
        Frame ul_0 = new Frame(scene);
        ul_0.setReference(reference);
        ul_0.translate(translation);
        Frame ul_1 = new Frame(scene);
        ul_1.setReference(ul_0);
        ul_1.translate(-50,-100);
        Frame ul_2 = new Frame(scene);
        ul_2.setReference(ul_0);
        ul_2.translate(50,-100);
        upper.add(ul_0);
        upper.add(ul_1);
        upper.add(ul_2);
        //Lower Loop
        Frame lol_0 = new Frame(scene);
        lol_0.setReference(reference);
        lol_0.translate(translation);
        Frame lol_1 = new Frame(scene);
        lol_1.setReference(lol_0);
        lol_1.translate(-25,50);
        Frame lol_2 = new Frame(scene);
        lol_2.setReference(lol_0);
        lol_2.translate(25,50);
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
        //set number of iterations
        upper_body.maxIter = 5;
        lower_body.maxIter = 5;
        left_arm.maxIter = 5;
        right_arm.maxIter = 5;
        left_leg.maxIter = 5;
        right_leg.maxIter = 5;
    }

    void updateClosedLoopTargets(ClosedLoopChainSolver solver, ChainSolver left, ChainSolver right){
        solver.setUnknown(1, left.chain().get(1).position());
        solver.setUnknown(2, right.chain().get(1).position());
        solver.setChildPosition(1, left.chain().get(2).position());
        solver.setChildPosition(2, right.chain().get(2).position());
        solver.setChildDistance(1, Vector.distance(left.chain().get(1).position(), left.chain().get(2).position()));
        solver.setChildDistance(2, Vector.distance(right.chain().get(1).position(), right.chain().get(2).position()));
    }

    void fixRotation(Frame frame, Vector new_position){
        Frame reference = frame.reference();
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
        left_arm.change_temp = true;
        right_arm.change_temp = true;
        left_leg.change_temp = true;
        right_leg.change_temp = true;

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
        PGraphics pg = scene.frontBuffer();
        pg.hint(PConstants.DISABLE_DEPTH_TEST);
        pg.pushStyle();
        pg.fill(color);
        //draw arms
        for (Frame frame : left_arm.chain()) {
            drawJoint(pg, frame);
        }
        for (Frame frame : right_arm.chain()) {
            drawJoint(pg, frame);
        }
        //draw legs
        for (Frame frame : left_leg.chain()) {
            drawJoint(pg, frame);
        }
        for (Frame frame : right_leg.chain()) {
            drawJoint(pg, frame);
        }
        for (Frame frame : upper_body.chain()) {
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

        for (Frame frame : lower_body.chain()) {
            drawJoint(pg, frame);
        }

        pg.hint(PConstants.ENABLE_DEPTH_TEST);
        pg.popStyle();
    }

    void drawJoint(PGraphics pg, Frame frame){
        pg.pushMatrix();
        scene.applyWorldTransformation(frame);
        pg.noStroke();
        pg.sphere(5);
        pg.stroke(color);
        Vector v = frame.location(new Vector(), frame.reference());
        pg.line(0, 0, 0, v.x(), v.y(), v.z());
        pg.popMatrix();
    }
}
