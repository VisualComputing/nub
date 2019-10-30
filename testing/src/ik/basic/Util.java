package ik.basic;

import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.Constraint;
import nub.core.constraint.Hinge;
import nub.core.constraint.PlanarPolygon;
import nub.ik.solver.geometric.CCDSolver;
import nub.ik.solver.geometric.ChainSolver;
import nub.ik.solver.evolutionary.HAEASolver;
import nub.ik.solver.Solver;
import nub.ik.solver.evolutionary.BioIk;
import nub.ik.solver.evolutionary.GASolver;
import nub.ik.solver.evolutionary.HillClimbingSolver;
import nub.ik.solver.geometric.MySolver;
import nub.ik.solver.geometric.TRIK;
import nub.ik.solver.numerical.PseudoInverseSolver;
import nub.ik.solver.numerical.SDLSSolver;
import nub.ik.solver.numerical.TransposeSolver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.ik.visual.Joint;
import processing.core.PGraphics;
import processing.core.PShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static processing.core.PApplet.*;

public class Util {
    public enum ConstraintType{ NONE, HINGE, CONE_POLYGON, CONE_ELLIPSE, CONE_CIRCLE, MIX }
    public enum SolverType{ HC, FABRIK, FABRIK_H1, FABRIK_H2, FABRIK_H1_H2, HGSA, SDLS, PINV, TRANSPOSE, CCD, CCD_V2, GA, HAEA, MySolver, TRIK}

    public static Solver createSolver(SolverType type, ArrayList<Node> structure){
        switch (type){
            case HC: return new HillClimbingSolver(5, radians(5), structure);
            case HGSA: return new BioIk(structure,10, 4 );
            case CCD: return new CCDSolver(structure);
            case CCD_V2:{
                CCDSolver solver = new CCDSolver(structure, false);
                return solver;
            }
            case PINV: new PseudoInverseSolver(structure);
            case FABRIK:{
                ChainSolver solver = new ChainSolver(structure);
                solver.setKeepDirection(false);
                solver.setFixTwisting(false);
                return solver;
            }
            case FABRIK_H1:{
                ChainSolver solver = new ChainSolver(structure);
                solver.setKeepDirection(true);
                solver.setFixTwisting(false);
                return solver;
            }
            case FABRIK_H2:{
                ChainSolver solver = new ChainSolver(structure);
                solver.setKeepDirection(false);
                solver.setFixTwisting(true);
                return solver;
            }
            case FABRIK_H1_H2:{
                ChainSolver solver = new ChainSolver(structure);
                solver.setKeepDirection(true);
                solver.setFixTwisting(true);
                return solver;
            }
            case TRANSPOSE: return new TransposeSolver(structure);
            case GA: return new GASolver(structure, 10);
            case HAEA: return new HAEASolver(structure, 10, true);
            case SDLS: return new SDLSSolver(structure);
            case MySolver: return  new MySolver(structure);
            case TRIK:{
                TRIK solver = new TRIK(structure);
                solver.setLookAhead(2);
                return solver;
            }
            default: return null;
        }
    }

    public static Node createTarget(Scene scene, float targetRadius){
        PShape redBall;
        if(scene.is3D())
            redBall = scene.context().createShape(SPHERE, targetRadius);
        else
            redBall = scene.context().createShape(ELLIPSE, 0,0, targetRadius, targetRadius);
        return createTarget(scene, redBall, targetRadius);
    }

    public static Node createTarget(Scene scene, PShape shape, float targetRadius){
        PGraphics pg = scene.context();
        return new Node(scene){
            @Override
            public void visit() {
                scene.drawAxes(targetRadius * 2);
                if(scene.trackedNode() == this){
                    shape.setFill(pg.color(0,255,0));
                }else{
                    shape.setFill(pg.color(255,0,0));
                }
                scene.pApplet().shape(shape);
            }
        };
    }

    public static ArrayList<Node> createTargets(int num, Scene scene, float targetRadius){
        PGraphics pg = scene.context();
        ArrayList<Node> targets = new ArrayList<Node>();
        PShape redBall;
        if(scene.is3D())
            redBall = pg.createShape(SPHERE, targetRadius);
        else
            redBall = pg.createShape(ELLIPSE, 0,0, targetRadius, targetRadius);
        redBall.setStroke(false);
        redBall.setFill(pg.color(255,0,0));

        for(int i = 0; i < num; i++) {
            Node target = createTarget(scene, redBall, targetRadius);
            target.setPickingThreshold(targetRadius*2);
            targets.add(target);
        }
        return targets;
    }

    public static ArrayList<Node> generateChain(Scene scene, int numJoints, float radius, float boneLength, Vector translation, int color) {
        return generateChain(scene, numJoints, radius, boneLength, translation, color, -1, 0);
    }

    public static ArrayList<Node> generateChain(Scene scene, int numJoints, float radius, float boneLength, Vector translation, int color, int randRotation, int randLength) {
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

    public static void generateConstraints(List<? extends Node> structure, ConstraintType type, int seed, boolean is3D){
        Random random = new Random(seed);
        int numJoints = structure.size();
        for (int i = 0; i < numJoints - 1; i++) {
            Vector twist = structure.get(i + 1).translation().get();
            //Quaternion offset = new Quaternion(new Vector(0, 1, 0), radians(random(-90, 90)));
            Quaternion offset = new Quaternion();//Quaternion.random();
            Constraint constraint = null;
            ConstraintType current = type;
            if(type == ConstraintType.MIX){
                int r = random.nextInt(ConstraintType.values().length - 1);
                r = is3D ? r : r % 2;
                current = ConstraintType.values()[r];
            }
            switch (current){
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
                case CONE_CIRCLE:{
                    if(!is3D) break;
                    float r = radians(random.nextFloat()*40 + 10);
                    constraint = new BallAndSocket(r,r,r,r);
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
                    Vector vector = new Vector(2*random.nextFloat() - 1, 2*random.nextFloat() - 1, 2*random.nextFloat() - 1);
                    vector.normalize();
                    vector = Vector.projectVectorOnPlane(vector, structure.get(i + 1).translation());
                    if(Vector.squaredNorm(vector) == 0) {
                        constraint = null;
                    }
                    constraint = new Hinge(radians(random.nextFloat()*160 + 10),
                            radians(radians(random.nextFloat()*160 + 10)),
                            structure.get(i).rotation().get(), structure.get(i + 1).translation(), vector);
                }
            }
            structure.get(i).setConstraint(constraint);
        }
    }

    public static void printInfo(Scene scene, Solver solver, Vector basePosition){
        PGraphics pg = scene.context();
        pg.pushStyle();
        pg.fill(255);
        pg.textSize(15);
        Vector pos = scene.screenLocation(basePosition);
        if(solver instanceof BioIk){
            pg.text("HGSA \n Algorithm" + "\n Error: " + String.format( "%.3f", solver.error()) + "\n iter : " + solver.lastIteration(), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }
        if(solver instanceof ChainSolver){
            ChainSolver s = (ChainSolver) solver;
            String heuristics = "";
            if(s.keepDirection()) heuristics += "\n Keep directions";
            if(s.fixTwisting()) heuristics += "\n Fix Twisting";
            pg.text("FABRIK" + heuristics + "\n Error: " + String.format( "%.3f", solver.error()) + "\n Exploration : " + s.explorationTimes() + "\n iter : " + solver.lastIteration(), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }
        if(solver instanceof CCDSolver){
            pg.text("CCD" + "\n Error: " + String.format( "%.3f", solver.error()) + "\n iter : " + solver.lastIteration(), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }
        if(solver instanceof TransposeSolver){
            pg.text("Transpose" + "\n Error: " + String.format( "%.3f", solver.error()) + "\n iter : " + solver.lastIteration(), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }
        if(solver instanceof SDLSSolver){
            pg.text("SDLS" + "\n Error: " + String.format( "%.3f", solver.error()) + "\n iter : " + solver.lastIteration(), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }
        if(solver instanceof PseudoInverseSolver){
            pg.text("PseudoInv" + "\n Error: " + String.format( "%.3f", solver.error()) + "\n iter : " + solver.lastIteration(), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
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
            pg.text("HAEA \n Algorithm" + "\n Error: " + String.format( "%.3f", solver.error()) + "\n iter : " + solver.lastIteration(), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }
        if(solver instanceof MySolver){
            pg.text("MySolver" + "\n Error: " + String.format( "%.3f", solver.error()) + "\n iter : " + solver.lastIteration(), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }
        if(solver instanceof TRIK){
            pg.text("TRIK" + "\n Error: " + String.format( "%.3f", solver.error()) + "\n iter : " + solver.lastIteration(), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
        }

        pg.popStyle();
    }

    public static ArrayList<Node> copy(List<? extends Node> chain) {
        ArrayList<Node> copy = new ArrayList<Node>();
        Node reference = chain.get(0).reference();
        if (reference != null) {
            reference = new Node(reference.position().get(), reference.orientation().get(), 1);
        }
        for (Node joint : chain) {
            Node newJoint = new Node();
            newJoint.setReference(reference);
            newJoint.setPosition(joint.position().get());
            newJoint.setOrientation(joint.orientation().get());
            newJoint.setConstraint(joint.constraint());
            copy.add(newJoint);
            reference = newJoint;
        }
        return copy;
    }

    public static void drawPositions(PGraphics pg, ArrayList<Vector> positions, int color, float str) {
        pg.sphereDetail(5);
        if(positions == null) return;
        Vector prev = null;
        for(Vector p : positions){
            pg.pushMatrix();
            pg.pushStyle();
            pg.stroke(color);
            pg.strokeWeight(str);
            if(prev != null) pg.line(prev.x(),prev.y(),prev.z(), p.x(),p.y(),p.z());
            pg.noStroke();
            pg.fill(color);
            pg.translate(p.x(),p.y(),p.z());
            pg.sphere(3);
            pg.popStyle();
            pg.popMatrix();
            prev = p;
        }
        pg.sphereDetail(40);
    }
}
