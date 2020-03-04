package ik.trik.expressive;

import ik.basic.Util;
import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.ik.solver.trik.heuristic.FinalHeuristic;
import nub.ik.solver.trik.implementations.SimpleTRIK;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.util.List;

public class ExpressiveTest extends PApplet {
    int numJoints = 10;
    float boneLength = 50;
    float targetRadius = 10;

    SimpleTRIK solver;
    Scene delegationScene, mainScene, focus;
    DelegationPanel panel;
    boolean solve = true, enableTask = false;


    public void settings() {
        size(1200, 800, P3D);
    }

    public void setup() {
        //Set Kinematic scene
        mainScene = new Scene(this);
        mainScene.setRadius(numJoints * 1f * boneLength);
        mainScene.fit(1);
        mainScene.setRightHanded();

        //create target
        Node target = Util.createTarget(mainScene, targetRadius);
        List<Node> structure = Util.generateChain(mainScene, numJoints, targetRadius * 0.7f, boneLength, new Vector(), color(0,255, 0));

        //Util.generateConstraints(structure, Util.ConstraintType.CONE_CIRCLE, 0, true);

        solver = new SimpleTRIK(structure, SimpleTRIK.HeuristicMode.EXPRESSIVE_FINAL);
        solver.setTarget(structure.get(numJoints - 1), target);
        //solver.enableSmooth(true);
        solver.context().setSingleStep(!solve);
        solver.setTimesPerFrame(5);
        solver.setMaxIterations(100);
        solver.setMaxError(0.1f);

        //Move the target to any position
        target.setPosition(structure.get(numJoints - 1).position());

        TimingTask task = new TimingTask(mainScene) {
            @Override
            public void execute() {
                if(enableTask){
                    if(!solver.solve()) panel.updateSliders();
                }
            }
        };
        task.run(40);


        //Set eye scene
        mainScene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2.f));
        mainScene.eye().rotate(new Quaternion(new Vector(0,1,0), PI));


        //Set the delegation scene
        delegationScene = new Scene(this, P2D, width, (int)(height * 0.3), 0,(int)(height * 0.7f));
        delegationScene.setRadius( height * 0.3f / 2.f);
        delegationScene.fit();
        //Setting the panel
        panel = new DelegationPanel(delegationScene, solver);

        //set eye constraint
        delegationScene.eye().setConstraint(new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Node node) {
                return new Vector(); //no translation allowed
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Node node) {
                return new Quaternion(); //no rotation is allowed
            }
        });
    }


    public void draw() {
        handleMouse();
        lights();
        mainScene.context().background(0);
        mainScene.drawAxes();
        mainScene.render();

        if( solver.mainHeuristic() instanceof FinalHeuristic){
            FinalHeuristic hig = (FinalHeuristic) (solver.mainHeuristic());
            hig.drawVectors(mainScene);
        }


        noLights();
        mainScene.beginHUD();
        delegationScene.beginDraw();
        delegationScene.context().background(0);
        delegationScene.render();
        delegationScene.endDraw();
        delegationScene.display();
        mainScene.endHUD();
    }

    public void handleMouse(){
        if(!mousePressed) focus = mouseY > 0.7 * height ? delegationScene : mainScene;
    }

    public void mouseMoved(){
        focus.mouseTag();
    }

    public void mouseDragged(){
        if(focus == delegationScene && focus.node() instanceof Slider){
            focus.node().interact("OnMovement", new Vector(focus.mouseX(), focus.mouseY()));
        }else {
            if (mouseButton == LEFT) {
                focus.mouseSpin();
            } else if (mouseButton == RIGHT)
                focus.mouseTranslate();
            else
                focus.moveForward(mouseX - pmouseX);
        }
    }

    public void mouseReleased(){
        if(focus.node() instanceof Slider){
            focus.node().interact("OnFinishedMovement", new Vector(focus.mouseX(), focus.mouseY()));
        }
    }

    public void mouseWheel(MouseEvent event) {
        if(focus != delegationScene && focus.node() == null) focus.scale(event.getCount() * 50);
    }


    public void keyPressed(){
        if(key == 'W' || key == 'w'){
            enableTask = !enableTask;
        }

        if(key == 'S' || key == 's') {
            solver.solve();
            panel.updateSliders();
        }
        if(key == 'C' || key == 'c'){
            solve = !solve;
            solver.context().setSingleStep(!solve);
        }

    }

    public void mouseClicked(MouseEvent event) {
        if(focus == mainScene) {
            if (event.getCount() == 2)
                if (event.getButton() == LEFT)
                    focus.focus();
                else
                    focus.align();
        }
    }

    public static void main(String[] args) {
        PApplet.main(new String[]{"ik.trik.expressive.ExpressiveTest"});
    }
}
