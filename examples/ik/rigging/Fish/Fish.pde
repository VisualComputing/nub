/**
 * Fish.
 * by Sebastian Chaparro Cuevas.
 * 
 * This example shows how to use ik module in order to interact with an .obj model. 
 * First we define a Skeleton and relate it with a .obj (via Skinning class)  
 * Then we register a solver on the Scene and add a Target that the fish tail must follow. 
 * Finally we use an Interpolator to define the Target Trajectory.   
 * 
 * Press 's' to enable/disable skeleton visualization.
 * Press 'c' to change between GPU/CPU mode.
 */


import nub.core.*;
import nub.primitives.*;
import nub.processing.*;
import nub.ik.solver.*;
import nub.ik.skinning.*;
import nub.ik.animation.Joint; //Joint provides default way to visualize the skeleton
import java.util.List;

boolean showSkeleton = true;
Scene scene;
Node reference;

List<Node> skeleton;
Skinning skinning;

Node target;
Interpolator targetInterpolator;

String shapePath = "fish0.obj";
String texturePath = "fish0.jpg";

boolean gpu = true;
float targetRadius = 7;

void settings() {
    size(700, 700, P3D);
}

public void setup() {
    //1. Create and set the scene
    scene = new Scene(this);
    scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRightHanded();
    scene.setRadius(200);
    scene.fit(1);
    //2. Define the Skeleton
    //2.1 Define a reference node to the skeleton and the mesh
    reference = new Node();
    //2.2 Use SimpleBuilder example (or a Modelling Sw if desired) and locate each Joint accordingly to mesh
    //2.3 Create the Joints based on 2.2.
    skeleton = loadSkeleton(reference);
    //3. Relate the shape with a skinning method (CPU or GPU)
    resetSkinning(gpu);
    //4. Adding IK behavior
    //4.1 Identify root and end effector(s) (first and last joint on the skeleton)
    Node root = skeleton.get(0);
    Node endEffector = skeleton.get(skeleton.size()- 1);
    //4.2 relate a skeleton with an IK Solver
    Solver solver = scene.registerTreeSolver(root);
    //Update params
    solver.setMaxError(1f);
    //4.3 Create target(s) to relate with End Effector(s)
    target = createTarget(targetRadius);
    //Target also depends on reference
    target.setReference(reference);
    //Make target to be on same position/orientation as endEffector
    target.setPosition(endEffector.position());
    //4.4 Relate target(s) with end effector(s)
    scene.addIKTarget(endEffector, target);
    //Generates a default Path that target must follow
    targetInterpolator = setupTargetInterpolator(target);
    
    textAlign(CENTER, CENTER);
    textSize(24);
}

void draw() {
    background(0);
    lights();
    scene.drawAxes();
    //Render mesh with respect to the node
    skinning.render(scene, reference);
    if(showSkeleton) scene.render();
    scene.beginHUD();
    text("Mode: " + (gpu ? "GPU " : "CPU") + " Frame rate: " + nf(frameRate, 0, 1), width/2, 50);
    scene.endHUD();
}

Node createTarget(float radius){
    PShape redBall = createShape(SPHERE, radius);
    redBall.setStroke(false);
    redBall.setFill(color(255,0,0));
    Node target = new Node(redBall);
    target.setPickingThreshold(0);
    return target;
}

void resetSkinning(boolean gpu){
  //move sekeleton to rest position (in this case is when nodes are all aligned)
  for(Node node : skeleton){
    node.setRotation(new Quaternion());
  }
  
  if(gpu){
    skinning = new GPULinearBlendSkinning(skeleton, this.g, shapePath, texturePath, 200, true);    
  } else{
    skinning = new CPULinearBlendSkinning(skeleton, this.g, shapePath, texturePath, 200, true);    
  }
}

List<Node> loadSkeleton(Node reference){
  JSONArray skeleton_data = loadJSONArray("skeleton.json");
  HashMap<String, Joint> dict = new HashMap<String, Joint>();
  List<Node> skeleton = new ArrayList<Node>();
  for(int i = 0; i < skeleton_data.size(); i++){
    JSONObject joint_data = skeleton_data.getJSONObject(i);
    Joint joint = new Joint(joint_data.getFloat("radius"));
    joint.setPickingThreshold(joint_data.getFloat("picking"));
    if(i == 0){
      joint.setRoot(true);
      joint.setReference(reference);
    }else{
      joint.setReference(dict.get(joint_data.getString("reference")));
    }
    joint.setTranslation(joint_data.getFloat("x"), joint_data.getFloat("y"), joint_data.getFloat("z"));
    joint.setRotation(joint_data.getFloat("q_x"), joint_data.getFloat("q_y"), joint_data.getFloat("q_z"), joint_data.getFloat("q_w"));
    skeleton.add(joint);
    dict.put(joint_data.getString("name"), joint);
  }  
  return skeleton;
}

Interpolator setupTargetInterpolator(Node target) {
    Interpolator targetInterpolator = new Interpolator(target);
    targetInterpolator.enableRecurrence();
    targetInterpolator.setSpeed(1f);
    // Create an initial path
    int nbKeyFrames = 7;
    float step = 2.0f * PI / (nbKeyFrames - 1);
    for (int i = 0; i < nbKeyFrames; i++) {
        Node iFrame = new Node();
        iFrame.setReference(target.reference());
        iFrame.setTranslation(new Vector(100 * sin(step * i), target.translation().y(), target.translation().z() + 25 * abs(sin(step * i))));
        targetInterpolator.addKeyFrame(iFrame);
    }
    targetInterpolator.run();
    return targetInterpolator;
}

void keyPressed() {
    if(key == 'S' || key == 's'){
        showSkeleton = !showSkeleton;
    }
    if(key == 'C' || key == 'c'){
        gpu = !gpu;
        resetSkinning(gpu);
    }
}

void mouseMoved() {
    scene.mouseTag();
}

void mouseDragged() {
    if (mouseButton == LEFT){
        scene.mouseSpin();
    } else if (mouseButton == RIGHT) {
        scene.mouseTranslate();
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
