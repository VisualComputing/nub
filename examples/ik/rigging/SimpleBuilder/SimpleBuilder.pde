/**
 * Simple Builder
 * by Sebastian Chaparro Cuevas.
 * 
 * In this example a mesh is loaded from an .obj file (specified by shapePath) and the idea is to
 * generate a Skeleton Structure over the .obj shape to use later in other sketches. 
 * 
 * To do so, it is possible to interact with a Joint (InteractiveJoint) in different ways:
 * Using the mouse:
 *    Drag with RIGTH button to translate the Joint. 
 *    Drag with LEFT button to rotate the Joint. 
 *    Drag with RIGTH button while pressing CTRL to extrude a Joint from the selected one. Release to create a Joint.      
 *    Double click with LEFT button while pressing SHIFT key to remove the Branch from the selected Joint. 
 * Using the keyboard:
 *    Press 'P' when the mouse is over a Joint to print the branch information on the console (you could require this info in other Sketch) 
 *    Press 'E' when the mouse is over a Joint to set its translation to (0,0,0). It is useful to mantain Chains of a Structure independent.
 */

import nub.core.*;
import nub.primitives.*;
import nub.processing.Scene;
import nub.ik.visual.Joint;
import java.util.List;

//Build easily a Skeleton to relate to a Mesh
Scene scene;
String lastCommand = "None";
//Shape variables
PShape model;

//Set this path to load your objs
String shapePath = "Hand.obj";
String texturePath = "HAND_C.jpg";

float radius = 0;
int w = 1000, h = 700;
/*Create different skeletons to interact with*/
String renderer = P3D;

void settings() {
    size(w, h, renderer);
}

void setup(){
    Joint.markers = true; //Show skeleton markers (disable in case of slow performance)
    //1. Create a scene
    scene = new Scene(this);
    scene.setType(Graph.Type.ORTHOGRAPHIC);
    //2. Import model
    /*
      If you have a mesh with quad faces use:
      model = createShapeQuad(loadShape(shapePath), texturePath, 100);
    */
    model = createShapeTri(loadShape(shapePath), texturePath, 100); 
    //3. Scale scene
    float size = max(model.getHeight(), model.getWidth());
    scene.setRightHanded();
    scene.setRadius(size);
    scene.fit();
    //4. Create a Interactive Joint at the center of the scene
    radius = scene.radius() * 0.01f;
    InteractiveJoint initial = new InteractiveJoint(scene, radius);
    initial.setRoot(true);
    initial.setPickingThreshold(-0.01f);
    textSize(18);
    textAlign(CENTER, CENTER);    
}

void draw() {
    background(0);
    ambientLight(102, 102, 102);
    lightSpecular(204, 204, 204);
    directionalLight(102, 102, 102, 0, 0, -1);
    specular(255, 255, 255);
    shininess(10);
    stroke(255);
    stroke(255,0,0);
    scene.drawAxes();
    shape(model);
    scene.render();
    
    noLights();
    scene.beginHUD();
    text("Last action: " + lastCommand, width/2, 50);
    scene.endHUD();
    
}

//mouse events
void mouseMoved() {
    scene.cast();
}

void mouseDragged(MouseEvent event) {
    if (mouseButton == RIGHT && event.isControlDown()) {
        Vector vector = new Vector(scene.mouse().x(), scene.mouse().y());
        if(scene.trackedNode() != null)
            if(scene.trackedNode() instanceof  InteractiveJoint){
                scene.trackedNode().interact("OnAdding", scene, vector);
                lastCommand = "Extruding from a Joint"; 
            }
            else
                scene.trackedNode().interact("OnAdding", vector);
    } else if (mouseButton == LEFT) {
        scene.spin();
    } else if (mouseButton == RIGHT) {
        scene.translate();
    } else if (mouseButton == CENTER){
        scene.scale(scene.mouseDX());
    }
}

void mouseReleased(MouseEvent event){
    Vector vector = new Vector(scene.mouse().x(), scene.mouse().y());
    if(scene.trackedNode() != null)
        if(scene.trackedNode() instanceof  InteractiveJoint){
            lastCommand = "Adding Joint"; 
            scene.trackedNode().interact("Add", scene, scene, vector);
        }
}

void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
}

void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2) {
        if (event.getButton() == LEFT) {
            if (event.isShiftDown())
                if(scene.trackedNode() != null){
                    lastCommand = "Removing Joint and its children"; 
                    scene.trackedNode().interact("Remove");
                }
                else
                    scene.focus();
        }
        else {
            scene.align();
        }
    }
}

void keyPressed(){
    if(key == 'J' || key == 'j'){
        lastCommand = "Adding Joint on the middle of the scene"; 
        InteractiveJoint initial = new InteractiveJoint(scene, radius);
        initial.setRoot(true);
        initial.setPickingThreshold(-0.01f);
    }else if(key == 'P' || key == 'p'){
        lastCommand = "Printing skeleton information"; 
        printJoints(scene.trackedNode(), "reference", 1);
        saveSkeleton(scene.trackedNode());
    }else if(key == 'A' || key == 'a'){
        Joint.axes = !Joint.axes;
    }else if(key == 'E' || key == 'e'){
        lastCommand = "Setting Joint translation to (0,0,0)"; 
        if(scene.trackedNode() != null){
            scene.trackedNode().setTranslation(new Vector());
            scene.trackedNode().enableTracking(false);
        }
    }
}


void printTree(Node root, String sep){
    if(root == null) return;
    System.out.print(sep + "|-> Node ");
    System.out.println("translation: " + root.translation() + "rotation axis: " + root.rotation().axis() + "rotation angle : " + root.rotation().angle());
    for(Node child : root.children()){
        printTree(child, sep + "\t");
    }
}

int printJoints(Node root, String reference, int i){
    if(root == null) return 0;
    System.out.println("Joint j" + i + " = new Joint(scene, scene.radius() * 0.01f);");
    System.out.println("j" + i + ".setPickingThreshold(-0.01f);");
    System.out.println("j" + i + ".setReference(" + reference + ");");
    System.out.println("j" + i + ".setTranslation(" + root.translation().x() + "f ," +
            root.translation().y() + "f ," + root.translation().z() + "f);");
    System.out.println("j" + i + ".setRotation( new Quaternion( new Vector (" +
            root.rotation().axis().x() + "f ," + root.rotation().axis().y() + "f ,"
            + root.rotation().axis().z() + "f), " + root.rotation().angle() + "f));");
    int idx = i;
    for(Node child : root.children()){
        idx = printJoints(child, "j"+ i, idx + 1);
    }
    return idx;
}


int saveJoints(JSONArray skeleton, Node root, String reference, int i){
    if(root == null) return 0;
    JSONObject joint = new JSONObject();
    joint.setString("reference", reference);
    joint.setString("name", "j"+i);
    joint.setFloat("radius", scene.radius() * 0.01f);
    joint.setFloat("picking", -0.01f);
    joint.setFloat("x", root.translation().x());
    joint.setFloat("y", root.translation().y());
    joint.setFloat("z", root.translation().z());
    joint.setFloat("q_x", root.rotation().x());
    joint.setFloat("q_y", root.rotation().y());
    joint.setFloat("q_z", root.rotation().z());
    joint.setFloat("q_w", root.rotation().w());
    skeleton.setJSONObject(i-1, joint);
    int idx = i;
    for(Node child : root.children()){
        idx = saveJoints(skeleton, child, "j"+ i, idx + 1);
    }
    return idx;
}

void saveSkeleton(Node root){
  JSONArray skeleton;
  skeleton = new JSONArray();
  saveJoints(skeleton, root, "reference", 1);  
  saveJSONArray(skeleton, "data/skeleton.json");
}

List<Node> loadSkeleton(Node reference){
  JSONArray skeleton_data = loadJSONArray("skeleton.json");
  HashMap<String, Joint> dict = new HashMap<String, Joint>();
  List<Node> skeleton = new ArrayList<Node>();
  for(int i = 0; i < skeleton_data.size(); i++){
    JSONObject joint_data = skeleton_data.getJSONObject(i);
    Joint joint = new Joint(scene, joint_data.getFloat("radius"));
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

//Adapted from http://www.cutsquash.com/2015/04/better-obj-model-loading-in-processing/
PShape createShapeTri(PShape r, String texture, float size) {
    float scaleFactor = size / max(r.getWidth(), r.getHeight());
    PImage tex = loadImage(texture);
    PShape s = createShape();
    s.beginShape(TRIANGLES);
    s.noStroke();
    s.texture(tex);
    s.textureMode(NORMAL);
    for (int i=100; i<r.getChildCount (); i++) {
        if (r.getChild(i).getVertexCount() ==3) {
            for (int j=0; j<r.getChild (i).getVertexCount(); j++) {
                PVector p = r.getChild(i).getVertex(j).mult(scaleFactor);
                PVector n = r.getChild(i).getNormal(j);
                float u = r.getChild(i).getTextureU(j);
                float v = r.getChild(i).getTextureV(j);
                s.normal(n.x, n.y, n.z);
                s.vertex(p.x, p.y, p.z, u, v);
            }
        }
    }
    s.endShape();
    return s;
}

PShape createShapeQuad(PShape r, String texture, float size) {
    float scaleFactor = size / max(r.getWidth(), r.getHeight());
    PImage tex = loadImage(texture);
    PShape s = createShape();
    s.beginShape(QUADS);
    s.noStroke();
    s.texture(tex);
    s.textureMode(NORMAL);
    for (int i=100; i<r.getChildCount (); i++) {
        if (r.getChild(i).getVertexCount() ==4) {
            for (int j=0; j<r.getChild (i).getVertexCount(); j++) {
                PVector p = r.getChild(i).getVertex(j).mult(scaleFactor);
                PVector n = r.getChild(i).getNormal(j);
                float u = r.getChild(i).getTextureU(j);
                float v = r.getChild(i).getTextureV(j);
                s.normal(n.x, n.y, n.z);
                s.vertex(p.x, p.y, p.z, u, v);
            }
        }
    }
    s.endShape();
    return s;
}
