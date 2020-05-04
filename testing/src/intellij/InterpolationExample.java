package intellij;

import nub.core.Interpolator;
import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public class InterpolationExample extends PApplet{
    Scene scene;
    Node reference;
    Node node;
    Interpolator interpolator;
    List<Node> chain, other;

    boolean sameRef = false;
    int nodeRef = 0;
    int keyRef = 0;
    int nodes = 3;

    public void settings() {
            size(700, 700, P3D);
        }
    public void setup() {
        //1. Create and set the scene
        scene = new Scene(this);
        scene.setRightHanded();
        scene.setRadius(200);
        scene.fit(1);
        reference = new Node();
        chain = chain(reference, color(0,255,0), 20, nodes);
        other = chain(reference, color(0,0,255), 20,nodes);

        PShape shape = createShape(BOX, 10);
        shape.setStroke(false);
        shape.setFill(color(255,0,0));
        node = new Node(shape);
        node.setPickingThreshold(0);
        nodeRef  = (int) random(chain.size() - 1);
        node.setReference(chain.get(nodeRef));

        //Generate key frames in other branch
        Node keyReference;
        if(sameRef) {
            keyRef = nodeRef;
            keyReference = node.reference();
        } else {
            keyRef = (int) random(other.size() - 1);
            keyReference = other.get(keyRef); //any other node
        }

        interpolator = new Interpolator(node);
        interpolator.enableRecurrence();
        // Create an initial path
        int nbKeyFrames = 50;
        float step = 2.0f * PI / (nbKeyFrames - 1);

        PShape shape2 = createShape(BOX, 5);
        shape2.setStroke(false);
        shape2.setFill(color(255,255,0));

        for (int i = 0; i < nbKeyFrames; i++) {
            Node key = new Node(shape2);

            key.setReference(keyReference);
            key.setPickingThreshold(10);
            Vector v = keyReference.location(node);
            key.setTranslation(new Vector(100 * sin(step * i) + v.x() + 50, 100 * cos(step * i) + v.y() + 50, v.z()));
            key.setRotation(Quaternion.random());
            interpolator.addKeyFrame(key);
        }
        interpolator.setSpeed(5);
        interpolator.run();
        textSize(16);
    }

    public void draw() {
        background(0);
        lights();
        scene.drawAxes();
        scene.render();
        //Render mesh with respect to the node
        stroke(255);
        //scene.drawCatmullRom(interpolator);
        scene.beginHUD();
        int i = 0;
        for(Node node : chain){
            Vector v = scene.screenLocation(node);
            text("A " + i++, v.x() - 20, v.y() - 20);
        }

        int j = 0;
        for(Node node : other){
            Vector v = scene.screenLocation(node);
            text("B " + j++, v.x() - 20, v.y() - 20);
        }

        if(!sameRef) text("reference of the Key Frames is B : " + keyRef, 50, 50);
        else text("reference of the Key Frames  is A : " + nodeRef, 50, 50);
        text("reference of Node to interpolate  is A : " + nodeRef, 50, 100);
        text("Press any to make both references lie on the same place", 50, 150);

        scene.endHUD();
    }

    public List<Node> chain(Node reference, int col, float l, int nodes) {
        List<Node> list = new ArrayList<>();
        Node prev = reference;
        PShape shape = createShape(BOX, 10);
        shape.setStroke(col);
        shape.setFill(col);
        for(int i = 0; i < nodes; i++){
            Node node = new Node(shape);
            node.setReference(prev);
            node.setTranslation(Vector.multiply(Vector.random(), l));
            node.setRotation(Quaternion.random());
            node.setPickingThreshold(0);
            prev = node;
            list.add(node);
        }
        return list;
    }

    @Override
    public void mouseMoved() {
        scene.mouseTag();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            scene.mouseSpin();
        } else if (mouseButton == RIGHT) {
            scene.mouseTranslate();
        } else {
            scene.scale(scene.mouseDX());
        }
    }

    public void mouseWheel(MouseEvent event) {
        scene.scale(event.getCount() * 20);
    }

    public void mouseClicked(MouseEvent event) {
        if (event.getCount() == 2)
            if (event.getButton() == LEFT)
                scene.focus();
            else
                scene.align();
    }

    public void keyPressed(){
        other.get(keyRef).set(chain.get(nodeRef));
    }


    public static void main(String args[]) {
        PApplet.main(new String[]{"intellij.InterpolationExample"});
    }
}
