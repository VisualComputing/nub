/*
 * Eagle.
 * by Sebastian Chaparro Cuevas.
 * 
 * This example shows how to use ik module in order to interact with an .obj model. 
 * First we define a Skeleton and relate it with a .obj (via Skinning class)  
 * Then we register a solver on the Scene and add a Target that the Wings must follow. 
 * We use an Interpolator per Wing in order to define a motion.
 * 
 * Press 's' to enable/disable skeleton visualization.
 * Press 'c' to change between GPU/CPU mode.
 * Press 'i' to show/hide info. 
 * Press 'a' to visualize which joints influence the most a region of the mesh. 
 * Press 'd' to disable Paint mode. 
 */

import nub.core.*;
import nub.core.constraint.*;
import nub.primitives.*;
import nub.processing.*;
import nub.ik.solver.*;
import nub.ik.skinning.*;
import nub.ik.visual.Joint; //Joint provides default way to visualize the skeleton
import java.util.List;

boolean showSkeleton = true;
boolean showInfo = true;
boolean constraintSkeleton = true;
Scene scene;
Node reference;

List<Node> skeleton;
Skinning skinning;

Interpolator[] targetInterpolator = new Interpolator[2];

String shapePath = "EAGLE_2.OBJ";
String texturePath = "EAGLE2.jpg";

//Flag used to visualize which joint influences the most a region of the mesh
int activeRegion = 0;  
boolean gpu = true;

void settings() {
    size(700, 700, P3D);
}

void setup() {
    //1. Create and set the scene
    scene = new Scene(this);
    scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRightHanded();
    scene.fit(1);
    //2. Define the Skeleton
    //2.1 Define a reference node to the skeleton and the mesh
    reference = new Node(scene);
    reference.enableTracking(false); //disable interaction
    //2.2 Use SimpleBuilder example (or a Modelling Sw if desired) and locate each Joint accordingly to mesh
    //2.3 Create the Joints based on 2.2.
    skeleton = buildSkeleton(reference);
    
    //2.4 define constraints (if any)
    //Find axis of rotation via cross product
    if(constraintSkeleton) setConstraints();
    
    //3. Relate the shape with a skinning method (CPU or GPU)
    resetSkinning(gpu);
    //4. Adding IK behavior
    //4.1 Identify root and end effector(s)
    Node root = skeleton.get(0); //root is the fist joint of the structure
    List<Node> endEffectors = new ArrayList<Node>(); //End Effectors are leaf nodes (with no children)
    for(Node node : skeleton) {
        if (node.children().size() == 0) {
            endEffectors.add(node);
        }
    }

    //4.2 relate a skeleton with an IK Solver
    Solver solver = scene.registerTreeSolver(root);
    //Update params
    solver.setMaxError(1f);
    solver.setMaxIterations(15);

    for(Node endEffector : endEffectors){
        //4.3 Create target(s) to relate with End Effector(s)
        PShape redBall = createShape(SPHERE, scene.radius() * 0.02f);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0, 220));
        Node target = new Node(scene, redBall);
        target.setPickingThreshold(0);
        target.setReference(reference); //Target also depends on reference
        target.setPosition(endEffector.position().get());
        //4.4 Relate target(s) with end effector(s)
        scene.addIKTarget(endEffector, target);
        //disable enf effector tracking
        endEffector.enableTracking(false);

        //If desired generates a default Path that target must follow
        if(endEffector == skeleton.get(14)){
          targetInterpolator[0] = setupTargetInterpolator(target, new Vector[]{new Vector(-48,0,0), new Vector(-40,-13,0), new Vector(-32,0,0) , new Vector(-40,20,0), new Vector(-48,0,0)});
        }

        if(endEffector == skeleton.get(18)){
          targetInterpolator[1] = setupTargetInterpolator(target, new Vector[]{new Vector(44,0,0), new Vector(38,-16,0), new Vector(28.5,0,0) , new Vector(38,19,0), new Vector(44,0,0)});
        } 
        
    }
    //use this method to visualize which node influences the most on a region of the mesh.
    if(skinning instanceof LinearBlendSkinningGPU)
        ((LinearBlendSkinningGPU) skinning).paintAllJoints();

}

void draw(){
    background(0);
    lights();
    scene.drawAxes();
    //Render mesh with respect to the node
    skinning.render(reference);
    if(showSkeleton){
      scene.render();
      pushStyle();
      fill(255);
      stroke(255);
      scene.drawPath(targetInterpolator[0],1);
      scene.drawPath(targetInterpolator[1],1);
      popStyle();
    }
    //Optionally print some info:
    scene.beginHUD();
    text("Mode: " + (gpu ? "GPU " : "CPU") + " Frame rate: " + nf(frameRate, 0, 1), width/2, 50);    
    if(showInfo){
      for(int i = 0; i < skinning.skeleton().size(); i++){
          if(skinning.skeleton().get(i).translation().magnitude() == 0){
              continue;
          }
          fill(255);
          Vector p = scene.screenLocation(skinning.skeleton().get(i).position());
          text("" + i, p.x(), p.y());
          Vector pos = skinning.skeleton().get(i).position();
      }

      for(Interpolator interpolator : targetInterpolator){
        for(Node node : interpolator.keyFrames()){
          pushStyle();
          Vector p = scene.screenLocation(node.position());
          text(round(node.translation().x()) + "," + round(node.translation().y()) + "," + round(node.translation().z()), p.x() - 5, p.y());
          popStyle();
        }
      }

      String msg = activeRegion == 0 ? "Painting all Joints" : activeRegion == -1 ? "" : "Painting Joint : " + activeRegion;
      text(msg,width/2, height - 50);
    }
    scene.endHUD();
    
}

void resetSkinning(boolean gpu){
  //move sekeleton to rest position (in this case is when nodes are all aligned)
  for(Node node : skeleton){
    node.setRotation(new Quaternion());
  }
  
  if(gpu){
    skinning = new LinearBlendSkinningGPU(skeleton, this.g, shapePath, texturePath, scene.radius(), false);    
  } else{
    skinning = new LinearBlendSkinningCPU(skeleton, this.g, shapePath, texturePath, scene.radius(), false);    
  }
}

void mouseMoved() {
    scene.cast();
}

void mouseDragged() {
    if (mouseButton == LEFT){
        scene.spin();
    } else if (mouseButton == RIGHT) {
        scene.translate();
    } else {
        scene.scale(mouseX - pmouseX);
    }
}

void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
}

void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
        if (event.getButton() == LEFT)
            scene.focus();
        else
            scene.align();
}

void keyPressed() {
    if(key == 'S' || key == 's'){
        showSkeleton = !showSkeleton;
    }
    if(key == 'C' || key == 'c'){
        gpu = !gpu;
        resetSkinning(gpu);
    }

    if(key == 'A' || key == 'a') {
        activeRegion = (activeRegion + 1) % skinning.skeleton().size();
        if(skinning instanceof LinearBlendSkinningGPU)
          ((LinearBlendSkinningGPU) skinning).paintJoint(activeRegion);
    }

    if(key == 'd' || key == 'D'){
        if(skinning instanceof LinearBlendSkinningGPU)
          ((LinearBlendSkinningGPU) skinning).disablePaintMode();
    }
        
    if(key == 'i' || key == 'I'){
        showInfo = !showInfo;
    }
}

//Skeleton is founded by interacting with SimpleBuilder
/* No Local coordinate has rotation (all are aligned with respect to reference system coordinates)
    J1 |-> Node translation: [ -1.7894811E-7, -1.2377515, -1.5709928 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
        J2 |-> Node translation: [ 6.425498E-7, 1.2980552, 5.463369 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
            J3 |-> Node translation: [ 6.5103023E-7, 0.23802762, 5.4746757 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
        J4 |-> Node translation: [ -4.70038E-7, -2.0343544, -4.0577974 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
            J5 |-> Node translation: [ -4.5386977E-7, -4.236917, -4.046496 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
        J6 |-> Node translation: [ -6.223473E-7, 1.202842, -5.1527314 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
            J7 |-> Node translation: [ -7.298398E-7, -0.33323926, -6.1411514 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                J8 |-> Node translation: [ -6.5542355E-7, -0.4284538, -5.5222764 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
        J9 |-> Node translation: [ -5.269003, 3.248286, -1.3883741 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
            J10 |-> Node translation: [ -12.133301, 5.3501167, 0.6687609 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                J11 |-> Node translation: [ -19.107552, 5.445654, 2.483986 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
        J12 |-> Node translation: [ 8.201833, 3.9170508, -1.8660631 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
            J13 |-> Node translation: [ 11.942226, 5.541193, 1.8152181 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                J14 |-> Node translation: [ 13.184211, 3.8215134, 2.3884451 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
*/

List<Node> buildSkeleton(Node reference){
    Joint j1 = new Joint(scene, scene.radius() * 0.01f);
    j1.setPickingThreshold(-0.01f);
    j1.setReference(reference);
    j1.setTranslation(-1.7894811E-7f, -1.2377515f, -1.5709928f);

    Joint dummy;
    dummy = new Joint(scene, scene.radius() * 0.01f);
    dummy.setReference(j1);
    dummy.enableTracking(false);

    Joint j2 = new Joint(scene, scene.radius() * 0.01f);
    j2.setPickingThreshold(-0.01f);
    j2.setReference(dummy);
    j2.setTranslation(6.425498E-7f, 1.2980552f, 5.463369f);
    Joint j3 = new Joint(scene, scene.radius() * 0.01f);
    j3.setPickingThreshold(-0.01f);
    j3.setReference(j2);
    j3.setTranslation(6.5103023E-7f, 0.23802762f, 5.4746757f);


    dummy = new Joint(scene, scene.radius() * 0.01f);
    dummy.setReference(j1);
    dummy.enableTracking(false);
    Joint j4 = new Joint(scene, scene.radius() * 0.01f);
    j4.setPickingThreshold(-0.01f);
    j4.setReference(dummy);
    j4.setTranslation(-4.70038E-7f, -2.0343544f, -4.0577974f);
    Joint j5 = new Joint(scene, scene.radius() * 0.01f);
    j5.setPickingThreshold(-0.01f);
    j5.setReference(j4);
    j5.setTranslation( -4.5386977E-7f, -4.236917f, -4.046496f);

    dummy = new Joint(scene, scene.radius() * 0.01f);
    dummy.setReference(j1);
    dummy.enableTracking(false);
    Joint j6 = new Joint(scene, scene.radius() * 0.01f);
    j6.setPickingThreshold(-0.01f);
    j6.setReference(dummy);
    j6.setTranslation( -6.223473E-7f, 1.202842f, -5.1527314f);
    Joint j7 = new Joint(scene, scene.radius() * 0.01f);
    j7.setPickingThreshold(-0.01f);
    j7.setReference(j6);
    j7.setTranslation( -7.298398E-7f, -0.33323926f, -6.1411514f);
    Joint j8 = new Joint(scene, scene.radius() * 0.01f);
    j8.setPickingThreshold(-0.01f);
    j8.setReference(j7);
    j8.setTranslation( -6.5542355E-7f, -0.4284538f, -5.5222764f);

    dummy = new Joint(scene, scene.radius() * 0.01f);
    dummy.setReference(j1);
    dummy.enableTracking(false);
    Joint j9 = new Joint(scene, scene.radius() * 0.01f);
    j9.setPickingThreshold(-0.01f);
    j9.setReference(dummy);
    j9.setTranslation( -5.269003f, 3.248286f, -1.3883741f);
    Joint j10 = new Joint(scene, scene.radius() * 0.01f);
    j10.setPickingThreshold(-0.01f);
    j10.setReference(j9);
    j10.setTranslation( -12.133301f, 5.3501167f, 0.6687609f);
    Joint j11 = new Joint(scene, scene.radius() * 0.01f);
    j11.setPickingThreshold(-0.01f);
    j11.setReference(j10);
    j11.setTranslation( -19.107552f, 5.445654f, 2.483986f);

    dummy = new Joint(scene, scene.radius() * 0.01f);
    dummy.setReference(j1);
    dummy.enableTracking(false);
    Joint j12 = new Joint(scene, scene.radius() * 0.01f);
    j12.setPickingThreshold(-0.01f);
    j12.setReference(dummy);
    j12.setTranslation( 8.201833f, 3.9170508f, -1.8660631f);
    Joint j13 = new Joint(scene, scene.radius() * 0.01f);
    j13.setPickingThreshold(-0.01f);
    j13.setReference(j12);
    j13.setTranslation( 11.942226f, 5.541193f, 1.8152181f);
    Joint j14 = new Joint(scene, scene.radius() * 0.01f);
    j14.setPickingThreshold(-0.01f);
    j14.setReference(j13);
    j14.setTranslation( 13.184211f, 3.8215134f, 2.3884451f);
    j1.setRoot(true);
    return scene.branch(j1);
}

void setConstraints(){
    Node j11 = skeleton.get(11);
    Vector up11 = j11.children().get(0).translation();//Same as child translation 
    Vector twist11 = Vector.cross(up11, new Vector(0,1,0), null);//Same as child translation 
    Hinge h11 = new Hinge(radians(40), radians(40), j11.rotation(), up11, twist11);
    j11.setConstraint(h11);
    
    
    Node j12 = skeleton.get(12);
    Vector up12 = j12.children().get(0).translation();//Same as child translation 
    Vector twist12 = Vector.cross(up12, new Vector(0,1,0), null);//Same as child translation 
    Hinge h12 = new Hinge(radians(40), radians(40), j12.rotation(), up12, twist12);
    j12.setConstraint(h12);
    
    Node j13 = skeleton.get(13);
    Vector up13 = j13.children().get(0).translation();//Same as child translation 
    Vector twist13 = Vector.cross(up13, new Vector(0,1,0), null);//Same as child translation 
    Hinge h13 = new Hinge(radians(45), radians(5), skeleton.get(13).rotation(), up13, twist13);
    j13.setConstraint(h13);

    
    Node j15 = skeleton.get(15);
    Vector up15 = j15.children().get(0).translation();//Same as child translation 
    Vector twist15 = Vector.cross(up15, new Vector(0,1,0), null);//Same as child translation 
    Hinge h15 = new Hinge(radians(40), radians(40), j15.rotation(), up15, twist15);
    j15.setConstraint(h15);
    
    
    Node j16 = skeleton.get(16);
    Vector up16 = j16.children().get(0).translation();//Same as child translation 
    Vector twist16 = Vector.cross(up16, new Vector(0,1,0), null);//Same as child translation 
    Hinge h16 = new Hinge(radians(40), radians(40), j16.rotation(), up16, twist16);
    j16.setConstraint(h16);
    
    Node j17 = skeleton.get(17);
    Vector up17 = j17.children().get(0).translation();//Same as child translation 
    Vector twist17 = Vector.cross(up17, new Vector(0,1,0), null);//Same as child translation 
    Hinge h17 = new Hinge(radians(45), radians(5), skeleton.get(17).rotation(), up17, twist17);
    j17.setConstraint(h17);
}

Interpolator setupTargetInterpolator(Node target, Vector[] positions) {
    Interpolator targetInterpolator = new Interpolator(target);
    targetInterpolator.setLoop();
    targetInterpolator.setSpeed(1f);
    // Create a path
    for(int i = 0; i < positions.length; i++){
        Node iFrame = new Node(scene);
        iFrame.setPickingThreshold(5);
        iFrame.setReference(target.reference());
        iFrame.setTranslation(positions[i]);
        targetInterpolator.addKeyFrame(iFrame);
    }
    targetInterpolator.start();
    return targetInterpolator;
}
