/**
 * InteractiveFish.
 * by Sebastian Chaparro.
 *
 * This example shows how an obj could be animated using
 * Inverse Kinematics and Interpolator methods.
 * The LinearBlendSkinning (LBS) class is used to associate a Skeleton with
 * an obj model. However at this very moment, it must be used with pretty
 * simple models. Otherwise the output will not be a desirable one.
 *
 * Here the skeleton is Just a single Chain, but LBS could be used with
 * any kind of Nodes Hierarchy (i.e Nodes with multiple children).
 *
 * (If the LBS output is not accurate, divide the OBJ by parts, where
 * each one must be related to a unique Joint)
 * Each important step is enumerated
 */

import frames.core.*;
import frames.ik.*;
import frames.primitives.*;
import frames.processing.*;
import frames.primitives.constraint.*;
import frames.input.*;

Scene scene;
Node eye;
OrbitShape shape;
Joint root;
LinearBlendSkinning skinning;
Target target;
Interpolator targetInterpolator;
String shapePath = "TropicalFish01.obj";
String texturePath = "TropicalFish01.jpg";

void setup() {
  size(700, 700, P3D);
  //1. Set a scene and an Eye
  scene = new Scene(this);
  scene.setType(Graph.Type.ORTHOGRAPHIC);
  eye = new OrbitShape(scene);
  scene.setEye(eye);
  scene.setFieldOfView(PI / 3);
  scene.setDefaultGrabber(eye);
  scene.fitBallInterpolation();

  //2. Create the Target
  target = new Target(scene);

  //3. Load a model and a texture
  PShape model = loadShape(shapePath);
  model.setTexture(loadImage(texturePath));

  //4. Adequate the model to fit properly in the scene

  //This step must be modified depending on how do you want to display the model
  //box is an array with two vectors, saving the min and max vector position.
  Vector[] box = getBoundingBox(model);
  float max = max(abs(box[0].x() - box[1].x()), abs(box[0].y() - box[1].y()), abs(box[0].z() - box[1].z()));
  shape = new OrbitShape(scene);
  shape.set(model);

  //Invert Y Axis
  shape.rotate(new Quaternion(new Vector(0,0,1), PI));
  //Scale model
  shape.scale(200.f*1.f/max);

  //5. Create the Skeleton (List or Hierarchy of Joints).
  //Here fishSkeleton method will create the desired Hierarchy
  /*
  It's important that the Skeleton fit properly the model to get a good animation
  */
  root = fishSkeleton(shape);

  //convenient method to get the Hierarchy defined in 5.
  ArrayList<Node> skeleton = scene.branch(root);

  //6. As we have adequate the model (4.) and created the Skeleton (5.) relate them with LBS
  //Here we create a LBS class with a Shape (Node) and the model (PShape)
  skinning = new LinearBlendSkinning(shape, model);
  //relate the model with the Skeleton
  skinning.setup(skeleton);

  //7. set the target position to a desirable place
  /*
  Here we're setting it to be at the fish' tail
  */
  target.setPosition(skeleton.get(skeleton.size()-1).position());


  //8. Making a default Path that target must follow
  /*
  It creates an Interpolator with some KeyFrames
  */
  targetInterpolator = setupTargetInterpolator(target);

  //Adding IK behavior
  //9. Tell the scene that a Solver must be register passing as parameter the root of the Skeleton (chain)
  /*
    The method registerTreeSolver returns a Solver class that could be used to modify some of its Parameters
    Here we use the default Solver configuration
  */
  scene.registerTreeSolver(root);
  //8. Associate the Target with the End Effector of the Skeleton (leaf Node)
  scene.addIKTarget(skeleton.get(skeleton.size()-1), target);
}

void draw(){
  background(0);
  lights();
  //Draw Constraints
  scene.drawAxes();
  shape.draw();
  skinning.applyTransformations();
}

//Get the max and min point that encloses the PShape
Vector[] getBoundingBox(PShape shape) {
  Vector v[] = new Vector[2];
  float minx = 999;  float miny = 999;
  float maxx = -999; float maxy = -999;
  float minz = 999;  float maxz = -999;
  for(int j = 0; j < shape.getChildCount(); j++){
    PShape aux = shape.getChild(j);
    for(int i = 0; i < aux.getVertexCount(); i++){
      float x = aux.getVertex(i).x;
      float y = aux.getVertex(i).y;
      float z = aux.getVertex(i).z;
      minx = minx > x ? x : minx;
      miny = miny > y ? y : miny;
      minz = minz > z ? z : minz;
      maxx = maxx < x ? x : maxx;
      maxy = maxy < y ? y : maxy;
      maxz = maxz < z ? z : maxz;
    }
  }

  v[0] = new Vector(minx,miny, minz);
  v[1] = new Vector(maxx,maxy, maxz);
  return v;
}

/*
Creates a Hierarchy that will be related with the Fish model
*/
Joint fishSkeleton(Node reference){
  Joint j1 = new Joint(scene);
  j1.setReference(reference);
  j1.setPosition(0, 10.8f, 93);
  scene.inputHandler().removeGrabber(j1);
  /*
  consider to use this scaling if the reference scale factor
  has been modified
  */
  j1.setScaling(1.f/reference.scaling());
  Joint j2 = new Joint(scene);
  j2.setReference(j1);
  j2.setPosition(0, 2.3f, 54.7f);
  scene.inputHandler().removeGrabber(j2);
  Joint j3 = new Joint(scene);
  j3.setReference(j2);
  j3.setPosition(0, 0.4f, 22);
  scene.inputHandler().removeGrabber(j3);
  Joint j4 = new Joint(scene);
  j4.setReference(j3);
  j4.setPosition(0, 0, -18);
  scene.inputHandler().removeGrabber(j4);
  Joint j5 = new Joint(scene);
  j5.setReference(j4);
  j5.setPosition(0, 1.8f, -54);
  scene.inputHandler().removeGrabber(j5);
  Joint j6 = new Joint(scene);
  j6.setReference(j5);
  j6.setPosition(0, -1.1f, -95);
  scene.inputHandler().removeGrabber(j6);
  j1.setRoot(true);
  return j1;
}

/*
Plans a target path
*/
Interpolator setupTargetInterpolator(Node target){
  Interpolator targetInterpolator = new Interpolator(target);
  targetInterpolator.setLoop();
  targetInterpolator.setSpeed(3.2f);
  // Create an initial path
  int nbKeyFrames = 10;
  float step = 2.0f*PI/(nbKeyFrames-1);
  for (int i = 0; i < nbKeyFrames; i++) {
    Node node = new Node(scene);
    node.setPosition(new Vector(50*sin(step*i), target.position().y(), target.position().z()));
    targetInterpolator.addKeyFrame(node);
  }
  targetInterpolator.start();
  return targetInterpolator;
}