package ik.interactiveSkeleton;

import common.InteractiveNode;
import common.InteractiveShape;
import frames.core.Graph;
import frames.core.Interpolator;
import frames.core.Node;
import frames.ik.Solver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.common.Joint;
import ik.common.Target;
import processing.core.PApplet;
import processing.core.PShape;
import processing.opengl.PShader;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 11/03/18.
 */
public class InteractiveFish extends PApplet {
    Scene scene;
    Node eye;
    InteractiveShape shape;
    Joint root;
    LinearBlendSkinning skinning;
    Target target;
    Interpolator targetInterpolator;
    String shapePath = "/testing/data/objs/TropicalFish01.obj";
    String texturePath = "/testing/data/objs/TropicalFish01.jpg";

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        eye = new InteractiveNode(scene);
        scene.setEye(eye);
        scene.setFieldOfView(PI / 3);
        scene.setDefaultGrabber(eye);
        scene.fitBallInterpolation();
        target = new Target(scene);

        PShape model = loadShape(sketchPath() + shapePath);
        model.setTexture(loadImage(sketchPath() + texturePath));

        Vector[] box = getBoundingBox(model);
        //Scale model
        float max = max(abs(box[0].x() - box[1].x()), abs(box[0].y() - box[1].y()), abs(box[0].z() - box[1].z()));
        model.scale(200.f*1.f/max);
        //Invert Y Axis and set Fill
        shape = new InteractiveShape(scene, model);
        shape.setPrecision(Node.Precision.FIXED);
        shape.setPrecisionThreshold(1);
        shape.rotate(new Quaternion(new Vector(0,0,1), PI));
        root = fishSkeleton(shape);
        //Apply skinning
        skinning = new LinearBlendSkinning(shape, model);
        ArrayList<Node> skeleton = scene.branch(root);
        skinning.setup(skeleton);
        //Adding IK behavior
        target.setPosition(skeleton.get(skeleton.size()-1).position());
        targetInterpolator = setupTargetInterpolator(target);

        Solver solver = scene.registerTreeSolver(root);
        scene.addIKTarget(skeleton.get(skeleton.size()-1), target);
        setSkinning();
    }

    public void draw(){
        updateParams();
        background(0);
        lights();
        //Draw Constraints
        scene.drawAxes();
        for(Node frame : scene.nodes()){
            if(frame == shape) shader(shader);
            if(frame instanceof Shape){
                ((Shape) frame).draw();
                pushMatrix();
                frame.applyWorldTransformation();
                scene.drawAxes(10);
                popMatrix();
            }
            if(frame == shape) resetShader();
        }
        //skinning.applyTransformations();
    }

    public static  Vector[] getBoundingBox(PShape shape) {
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

    public Joint fishSkeleton(Node reference){
        Joint j1 = new Joint(scene, true);
        j1.setReference(reference);
        j1.setPosition(0, 10.8f, 93);
        Joint j2 = new Joint(scene, false);
        j2.setReference(j1);
        j2.setPosition(0, 2.3f, 54.7f);
        Joint j3 = new Joint(scene, false);
        j3.setReference(j2);
        j3.setPosition(0, 0.4f, 22);
        Joint j4 = new Joint(scene, false);
        j4.setReference(j3);
        j4.setPosition(0, 0, -18);
        Joint j5 = new Joint(scene, false);
        j5.setReference(j4);
        j5.setPosition(0, 1.8f, -54);
        Joint j6 = new Joint(scene, false);
        j6.setReference(j5);
        j6.setPosition(0, -1.1f, -95);
        return j1;
    }

    public Interpolator setupTargetInterpolator(Node target){
        Interpolator targetInterpolator = new Interpolator(target);
        targetInterpolator.setLoop();
        targetInterpolator.setSpeed(3.2f);
        // Create an initial path
        int nbKeyFrames = 10;
        float step = 2.0f*PI/(nbKeyFrames-1);
        for (int i = 0; i < nbKeyFrames; i++) {
            InteractiveNode iFrame = new InteractiveNode(scene);
            iFrame.setPosition(new Vector(50*sin(step*i), target.position().y(), target.position().z()));
            targetInterpolator.addKeyFrame(iFrame);
        }
        targetInterpolator.start();
        return targetInterpolator;
    }

    public void printSkeleton(Node root){
        int i = 0;
        for(Node node : scene.branch(root)){
            System.out.println("Node " + i + " : " + node.position());
            i++;
        }
    }

    public void keyPressed(){
        if(key == ' '){
            printSkeleton(root);
        }
    }

    //testing skinning on GPU
    PShader shader;
    Quaternion[] boneQuat = new Quaternion[120];
    float[] bonePositionOrig = new float[120];
    float[] bonePosition = new float[120];

    public void setSkinning(){
        ArrayList<Node> skeleton = scene.branch(root);
        shader = loadShader(sketchPath() + "/testing/src/ik/interactiveSkeleton/frag.glsl",
                sketchPath() + "/testing/src/ik/interactiveSkeleton/skinning.glsl");
        int i = 0, j = 0;
        for(Node node : skeleton){
            Vector position;
            boneQuat[j++] = node.orientation().get();
            position = shape.coordinatesOfFrom(new Vector(), node);
            bonePositionOrig[i+0] = position.x();
            bonePositionOrig[i+1] = position.y();
            bonePositionOrig[i+2] = position.z();
            bonePosition[i++] = position.x();
            bonePosition[i++] = position.y();
            bonePosition[i++] = position.z();

        }
        shader.set("bonePositionOrig", bonePositionOrig);
        shader.set("bonePosition", bonePosition);
        shader.set("boneLength", i);

    }

    public void updateParams(){
        ArrayList<Node> skeleton = scene.branch(root);
        float[] boneRotation = new float[120];
        int i = 0, j = 0, k =0;
        //System.out.println("Begin--------------------");
        //System.out.println("Begin--------------------");
        for(Node node : skeleton){
            Vector position;
            Quaternion rotation;
            position = shape.coordinatesOfFrom(new Vector(), node);
            bonePosition[i++] = position.x();
            bonePosition[i++] = position.y();
            bonePosition[i++] = position.z();
            rotation = Quaternion.compose(boneQuat[k++].inverse(),node.orientation());
            //System.out.println("rota: " + rotation.axis() + " ang : " + rotation.angle());
            boneRotation[j++] = rotation.x();
            boneRotation[j++] = rotation.y();
            boneRotation[j++] = rotation.z();
            boneRotation[j++] = rotation.w();
        }
        //System.out.println("End--------------------");
        //System.out.println("End--------------------");

        shader.set("bonePosition", bonePosition);
        shader.set("boneRotation", boneRotation);
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.interactiveSkeleton.InteractiveFish"});
    }
}