package ik;

import processing.core.*;
import remixlab.dandelion.constraint.*;
import remixlab.dandelion.core.*;
import remixlab.dandelion.geom.*;
import remixlab.dandelion.ik.Solver;
import remixlab.proscene.*;

import java.util.ArrayList;

public class BasicIK extends PApplet {
    Scene scene;
    InteractiveFrame target;

    int num_joints = 12;

    //ballandsocket
    float constraint_factor = 50;
    //hinge
    float max = 0;
    float min = 10;
    //boolean auto = true;
    int TimesPerFrame = 1;

    boolean constrained = true;
    //Choose one of P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
    String renderer = P3D;

    public void settings() {
        size(700, 700, renderer);
    }

    public void setup() {
        scene = new Scene(this);
        target = new InteractiveFrame(scene, "targetGraphics");
        target.translate(new Vec(50, 50*noise(0)));
        if (scene.is3D())
            scene.setCameraType(Camera.Type.ORTHOGRAPHIC);
        scene.setAxesVisualHint(true);
        InteractiveFrame prevFrame = null;
        InteractiveFrame chainRoot = null;
        for (int i = 0; i < num_joints; i++) {
            InteractiveFrame iFrame;
            iFrame = new InteractiveFrame(scene, "frameGraphics");
            if (i == 0)
                chainRoot = iFrame;
            if (prevFrame != null) iFrame.setReferenceFrame(prevFrame);
            iFrame.setTranslation(new Vec(10, 10, 10));
            iFrame.setPickingPrecision(InteractiveFrame.PickingPrecision.FIXED);
            prevFrame = iFrame;
        }

        ArrayList<GenericFrame> branch = scene.branch(chainRoot, false);

        //Fix hierarchy
        //TODO: no entiendo el proposito de esta linea:
        //creo que esto deberia hacerse automaticamente al instanciar el solver
        chainRoot.setupHierarchy();
        if (constrained)
            for (int i = 0; i < branch.size(); i++)
                if (scene.is3D()) {
                    BallAndSocket constraint = new BallAndSocket(radians(constraint_factor), radians(constraint_factor), radians(constraint_factor), radians(constraint_factor));
                    constraint.setRestRotation((Quat) branch.get(i).rotation().get());
                    //branch.get(i).setConstraint(constraint);
                } else {
                    Hinge hinge = new Hinge();
                    hinge.setRestRotation(branch.get(i).rotation());
                    hinge.setMax(radians(max));
                    hinge.setMin(radians(min));
                    hinge.setAxis(branch.get(i).transformOf(new Vec(1, -1, 0)));
                    branch.get(i).setConstraint(hinge);
                }
        Solver solver = scene.setIKStructure(chainRoot);
        scene.addIKTarget(branch.get(branch.size()-1), target);
        solver.setTIMESPERFRAME(TimesPerFrame);
        solver.setMINCHANGE(999);
    }

    public void frameGraphics(InteractiveFrame iFrame, PGraphics pg) {
        pg.pushStyle();
        scene.drawAxes(pg, 3);
        pg.fill(0, 255, 0);
        pg.strokeWeight(5);
        pg.stroke(0, 100, 100, 100);
        if (iFrame.referenceFrame() != null) {
            Vec v = iFrame.coordinatesOfFrom(new Vec(), iFrame.referenceFrame());
            if(pg.is2D())
                pg.line(0, 0, v.x(), v.y());
            else
                pg.line(0, 0, 0, v.x(), v.y(), v.z());

        }
        pg.popStyle();
    }

    public void targetGraphics(PGraphics pg) {
        pg.pushStyle();
        pg.noStroke();
        pg.fill(255, 0, 0, 200);
        if(pg.is2D())
            pg.ellipse(0, 0, 5, 5);
        else
            pg.sphere(5);
        pg.popStyle();
    }

    public void draw() {
        background(0);
        scene.drawFrames();
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.BasicIK"});
    }
}
