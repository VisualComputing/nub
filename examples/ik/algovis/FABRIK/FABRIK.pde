/**
 * Fish.
 * by Sebastian Chaparro Cuevas.
 * 
 * This example shows an interactive explanation of Forward and Backward Reaching Inverse Kinematics (FABRIK).
 * For more info look at http://www.andreasaristidou.com/publications/papers/FABRIK.pdf
 *
 * Here it is generated a Chain Structure and a Target (interactive) that the End Effector must reach. 
 * Then a ChainSolver (FABRIK) is instantiated and related to the structure. 
 * 
 * Press 's' to reach the Target using FABRIK.
 * Press 'e' to see step by step how FABRIK works.
 * Press ' ' to change between Explanation scene and Interactive scene.
 * Press any number to set the duration of each step.
 */

import nub.core.*;
import nub.core.constraint.*;
import nub.ik.solver.geometric.*;
import nub.ik.visual.*;
import nub.ik.animation.IKAnimation;
import nub.primitives.*;
import nub.processing.*;
import nub.timing.*;
import processing.core.*;
import java.util.Random;

Scene scene, auxiliar, focus;
boolean displayAuxiliar = false;

int numJoints = 7;
float radius = 5;  
float boneLength = 50;
color chainColor = color(212,0,255);


ChainSolver solver;
ArrayList<Node> structure = new ArrayList<Node>(); //Keep Structures
Node target;

IKAnimation.FABRIKAnimation FABRIKAnimator = null;

void settings() {
    size(700, 700, P3D);
}

void setup() {
    //1. Setup the main Scene
    scene = new Scene(this);
    scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRadius(numJoints * boneLength * 0.8f);    
    scene.setRightHanded();
    scene.fit(1);

    //2. Setup the auxiliar Scene (will be used to explain how CCD Algorithm works)
    auxiliar = new Scene(this, P3D, width, height , 0, 0);
    auxiliar.setType(Graph.Type.ORTHOGRAPHIC);
    auxiliar.setRadius(numJoints * boneLength * 0.8f);    
    auxiliar.setRightHanded();
    auxiliar.fit();
    
    //3. Generate the Chain (look at Util tab)
    structure = generateChain(scene, numJoints, radius, boneLength, new Vector(), LengthType.RANDOM, AlignType.FIXED, chainColor);

    //4. Create the target
    PShape redBall = createShape(SPHERE, 10);
    redBall.setStroke(false);
    redBall.setFill(color(255,0,0, 220));
    target = new Node(scene, redBall);
    target.setPickingThreshold(0);
    
    //5. Instantiate and setup a Solver
    solver = new ChainSolver(structure);
    
    //6. Enable history (to keep info about each Step of the algorithm)
    solver.enableHistory(true);
    solver.setMaxError(0.001f);
    solver.setTimesPerFrame(5);
    solver.setMaxIterations(20);
    
    //7. Relate target with solver
    solver.setTarget(structure.get(numJoints - 1), target);
    target.setPosition(structure.get(numJoints - 1).position());

    //Define Text Font
    textFont(createFont("Zapfino", 38));
}

void draw() {
    focus = displayAuxiliar ? auxiliar : scene;
    background(0);
    lights();
    scene.drawAxes();
    scene.render();
    scene.beginHUD();

    //Draw explanation process
    if(displayAuxiliar) {
        auxiliar.beginDraw();
        auxiliar.context().lights();
        auxiliar.context().background(0);
        auxiliar.drawAxes();
        auxiliar.render();
        if(FABRIKAnimator != null)  FABRIKAnimator.draw();
        auxiliar.endDraw();
        auxiliar.display();
    }
    scene.endHUD();
}

void mouseMoved() {
    focus.cast();
}

void mouseDragged() {
    if (mouseButton == LEFT){
        focus.spin();
    } else if (mouseButton == RIGHT) {
        focus.translate();
    } else {
        focus.scale(mouseX - pmouseX);
    }
}

void mouseWheel(MouseEvent event) {
    focus.scale(event.getCount() * 20);
}

void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
        if (event.getButton() == LEFT)
            focus.focus();
        else
            focus.align();
}

void keyPressed(){
    if(key == 'e'){
        displayAuxiliar = true;
        solver.solve();
        if(FABRIKAnimator == null) FABRIKAnimator = new IKAnimation.FABRIKAnimation(auxiliar, solver, radius, chainColor);
        else FABRIKAnimator.reset();
    } else if(key == ' '){
        displayAuxiliar = !displayAuxiliar;
    } else if(key == 's'){
        solver.solve();
    } else if(Character.isDigit(key)){
        if(FABRIKAnimator != null) FABRIKAnimator.setPeriod(max(Integer.valueOf("" + key) * 1000, 100));
    }
}
