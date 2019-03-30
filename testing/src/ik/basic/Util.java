package ik.basic;

import frames.core.Frame;
import frames.core.constraint.BallAndSocket;
import frames.core.constraint.Constraint;
import frames.core.constraint.Hinge;
import frames.core.constraint.PlanarPolygon;
import frames.ik.CCDSolver;
import frames.ik.ChainSolver;
import frames.ik.HAEASolver;
import frames.ik.Solver;
import frames.ik.evolution.BioIk;
import frames.ik.evolution.GASolver;
import frames.ik.evolution.HillClimbingSolver;
import frames.ik.jacobian.PseudoInverseSolver;
import frames.ik.jacobian.SDLSSolver;
import frames.ik.jacobian.TransposeSolver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import ik.common.Joint;
import processing.core.PGraphics;
import processing.core.PShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static processing.core.PApplet.*;

public class Util {
    public enum ConstraintType{ NONE, HINGE, CONE_POLYGON, CONE_ELLIPSE, MIX }
    public enum SolverType{ HC, FABRIK, HGSA, SDLS, PINV, TRANSPOSE, CCD, GA, HAEA }

    public static Solver createSolver(SolverType type, ArrayList<Frame> structure){
        switch (type){
            case HC: return new HillClimbingSolver(5, radians(5), structure);
            case HGSA: return new BioIk(structure,10, 4 );
            case CCD: return new CCDSolver(structure);
            case PINV: new PseudoInverseSolver(structure);
            case FABRIK: return new ChainSolver(structure);
            case TRANSPOSE: return new TransposeSolver(structure);
            case GA: return new GASolver(structure, 10);
            case HAEA: return new HAEASolver(structure, 10, true);
            case SDLS: return new SDLSSolver(structure);
            default: return null;
        }
    }

    public static Frame createTarget(Scene scene, float targetRadius){
        PShape redBall;
        if(scene.is3D())
            redBall = scene.frontBuffer().createShape(SPHERE, targetRadius);
        else
            redBall = scene.frontBuffer().createShape(ELLIPSE, 0,0, targetRadius, targetRadius);
        return createTarget(scene, redBall, targetRadius);
    }

    public static Frame createTarget(Scene scene, PShape shape, float targetRadius){
        PGraphics pg = scene.frontBuffer();
        return new Frame(scene){
            @Override
            public void visit() {
                scene.drawAxes(targetRadius * 2);
                if(scene.trackedFrame() == this){
                    shape.setFill(pg.color(0,255,0));
                }else{
                    shape.setFill(pg.color(255,0,0));
                }
                scene.pApplet().shape(shape);
            }
        };
    }

    public static ArrayList<Frame> createTargets(int num, Scene scene, float targetRadius){
        PGraphics pg = scene.frontBuffer();
        ArrayList<Frame> targets = new ArrayList<Frame>();
        for(int i = 0; i < num; i++) {
            PShape redBall;
            if(scene.is3D())
                redBall = pg.createShape(SPHERE, targetRadius);
            else
                redBall = pg.createShape(ELLIPSE, 0,0, targetRadius, targetRadius);
            redBall.setStroke(false);
            redBall.setFill(pg.color(255,0,0));
            Frame target = createTarget(scene, redBall, targetRadius);
            target.setPickingThreshold(targetRadius*2);
            target.setHighlighting(Frame.Highlighting.FRONT);
            targets.add(target);
        }
        return targets;
    }

    public static ArrayList<Frame> generateChain(Scene scene, int numJoints, float radius, float boneLength, Vector translation, int color) {
        return generateChain(scene, numJoints, radius, boneLength, translation, color, -1, 0);
    }

    public static ArrayList<Frame> generateChain(Scene scene, int numJoints, float radius, float boneLength, Vector translation, int color, int randRotation, int randLength) {
        Random r1 = randRotation != -1 ? new Random(randRotation) : null;
        Random r2 = randLength != -1 ? new Random(randLength) : null;

        Joint prevJoint = null;
        Joint chainRoot = null;
        for (int i = 0; i < numJoints; i++) {
            Joint joint;
            joint = new Joint(scene, color, radius);
            if (i == 0)
                chainRoot = joint;
            if (prevJoint != null) joint.setReference(prevJoint);

            float x = 0;
            float y = 1;
            float z = 0;

            if(r1 != null) {
                x = 2 * r1.nextFloat() - 1;
                z = r1.nextFloat();
                y = 2 * r1.nextFloat() - 1;
            }

            Vector translate = new Vector(x,y,z);
            translate.normalize();
            if(r2 != null)
                translate.multiply(boneLength * (1 - 0.4f*r2.nextFloat()));
            else
                translate.multiply(boneLength);
            joint.setTranslation(translate);
            prevJoint = joint;
        }
        //Consider Standard Form: Parent Z Axis is Pointing at its Child
        chainRoot.setTranslation(translation);
        //chainRoot.setupHierarchy();
        chainRoot.setRoot(true);
        return (ArrayList) scene.branch(chainRoot);
    }

    public static void generateConstraints(List<? extends Frame> structure, ConstraintType type, int seed, boolean is3D){
        Random random = new Random(seed);
        int numJoints = structure.size();
        for (int i = 0; i < numJoints - 1; i++) {
            Vector twist = structure.get(i + 1).translation().get();
            //Quaternion offset = new Quaternion(new Vector(0, 1, 0), radians(random(-90, 90)));
            Quaternion offset = new Quaternion();//Quaternion.random();
            Constraint constraint = null;
            if(type == ConstraintType.MIX){
                int r = random.nextInt(ConstraintType.values().length);
                r = is3D ? r : r % 2;
                type = ConstraintType.values()[r];
            }
            switch (type){
                case NONE:{
                    break;
                }
                case CONE_ELLIPSE:{
                    if(!is3D) break;
                    float down = radians(random.nextFloat()*40 + 10);
                    float up = radians(random.nextFloat()*40 + 10);
                    float left = radians(random.nextFloat()*40 + 10);
                    float right = radians(random.nextFloat()*40 + 10);
                    constraint = new BallAndSocket(down, up, left, right);
                    Quaternion rest = Quaternion.compose(structure.get(i).rotation().get(), offset);
                    ((BallAndSocket) constraint).setRestRotation(rest, new Vector(0, 1, 0), twist);
                    break;
                }
                case CONE_POLYGON:{
                    if(!is3D) break;
                    ArrayList<Vector> vertices = new ArrayList<Vector>();
                    float v = 20;
                    float w = 20;
                    vertices.add(new Vector(-w, -v));
                    vertices.add(new Vector(w, -v));
                    vertices.add(new Vector(w, v));
                    vertices.add(new Vector(-w, v));
                    constraint = new PlanarPolygon(vertices);
                    Quaternion rest = Quaternion.compose(structure.get(i).rotation().get(), offset);
                    ((PlanarPolygon) constraint).setRestRotation(rest, new Vector(0, 1, 0), twist);
                    ((PlanarPolygon) constraint).setAngle(radians(random.nextFloat()*20 + 30));
                    break;
                }
                case HINGE:{
                    constraint = new Hinge(radians(random.nextFloat()*160 + 10), radians(radians(random.nextFloat()*160 + 10)));
                    ((Hinge) constraint).setRestRotation(structure.get(i).rotation().get());
                    ((Hinge) constraint).setAxis(Vector.projectVectorOnPlane(Vector.random(), structure.get(i + 1).translation()));
                    if(Vector.squaredNorm(((Hinge) constraint).axis()) == 0) {
                        constraint = null;
                    }
                }
            }
            structure.get(i).setConstraint(constraint);
        }
    }

    public static void printInfo(Scene scene, Solver solver, Vector basePosition){
        PGraphics pg = scene.frontBuffer();
        pg.pushStyle();
        pg.fill(255);
        pg.textSize(15);
        Vector pos = scene.screenLocation(basePosition);
        if(solver instanceof BioIk){
            pg.text("HGSA \n Algorithm" + "\n Error: " + String.format( "%.3f", solver.error()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }
        if(solver instanceof ChainSolver){
            ChainSolver s = (ChainSolver) solver;
            String heuristics = "";
            if(s.keepDirection()) heuristics += "\n Keep directions";
            if(s.fixTwisting()) heuristics += "\n Fix Twisting";
            pg.text("FABRIK" + heuristics + "\n Error: " + String.format( "%.3f", solver.error()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }
        if(solver instanceof CCDSolver){
            pg.text("CCD" + "\n Error: " + String.format( "%.3f", solver.error()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }
        if(solver instanceof TransposeSolver){
            pg.text("Transpose" + "\n Error: " + String.format( "%.3f", solver.error()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }
        if(solver instanceof SDLSSolver){
            pg.text("SDLS" + "\n Error: " + String.format( "%.3f", solver.error()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }
        if(solver instanceof PseudoInverseSolver){
            pg.text("PseudoInv" + "\n Error: " + String.format( "%.3f", solver.error()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }
        if(solver instanceof HillClimbingSolver){
            HillClimbingSolver s = (HillClimbingSolver) solver;
            if(s.powerLaw()){
                pg.text("Power Law  \n Sigma: " + String.format( "%.2f", s.sigma()) + "\n Alpha: " + String.format( "%.2f", s.alpha()) + "\n Error: " + String.format( "%.3f", s.error()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
            } else{
                pg.text("Gaussian  \n Sigma: " + String.format( "%.2f", s.sigma()) + "\n Error: " + String.format( "%.3f", s.error()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
            }
        }
        if(solver instanceof GASolver){
            pg.text("Genetic \n Algorithm" + "\n Error: " + String.format( "%.3f", solver.error()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }
        if(solver instanceof HAEASolver){
            pg.text("HAEA \n Algorithm" + "\n Error: " + String.format( "%.3f", solver.error()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }
        pg.popStyle();
    }
}
