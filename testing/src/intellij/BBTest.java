package intellij;

import nub.core.*;
import nub.primitives.*;
import nub.processing.*;
import nub.timing.*;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.event.MouseEvent;

public class BBTest extends PApplet {
    Scene scene;
    boolean debug = false;

    public void settings(){
        size(800, 600, P3D);
    }

    public void setup() {
        scene = new Scene(createGraphics(width / 2, height, P3D));
        Node node = new Node(createShape(SPHERE, 20));
    }

    public void draw() {
        scene.display(0);
    }

    public void mouseMoved() {
        if(debug) {
            println("On debug");
        }
        scene.updateTag();
    }

    public void mouseDragged() {
        if (Scene.pApplet.mouseButton == PConstants.LEFT) {
            scene.spin();
        } else if (Scene.pApplet.mouseButton == PConstants.RIGHT) {
            scene.shift();
        } else
            scene.zoom(Scene.pApplet.mouseX - Scene.pApplet.pmouseX);
    }

    public void mouseReleased() {

    }

    public void mouseWheel(MouseEvent event) {
        scene.moveForward(event.getCount() * 5);
    }

    public void keyPressed(){
        debug = !debug;
    }


    public static void main(String args[]) {
        PApplet.main(new String[]{"intellij.BBTest"});
    }
}
