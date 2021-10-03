package intellij;

import nub.core.MatrixHandler;
import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

public class MaxDepthTest extends PApplet {
    Scene scene;

    public void settings() {
        size(1000, 700, P3D);
    }

    public void setup() {
        MatrixHandler.STACK_DEPTH = 55;
        scene = new Scene(this);
        recursiveDummy(null, 50);
    }

    public void recursiveDummy(Node parent, int m){
        if(m <= 0) return;
        Node curr = new Node(parent);
        int col = color(random(255), random(255), random(255));
        curr.setShape( (pg) -> {
            pg.noStroke();
            pg.fill(col);
            pg.sphere(5);
        });
        curr.setHUD( pg -> {
            pg.pushStyle();
            pg.fill(255);
            pg.text("" + curr.id(), 0,0);
            pg.popStyle();
        });


        curr.randomize(new Vector(), 100, true);
        curr.setMagnitude(1);
        curr.enableHint(Node.SHAPE);
        recursiveDummy(curr, m - 1);
    }

    public void draw() {
        background(0);
        lights();
        scene.render();
    }

    public void mouseMoved() {
        scene.tag();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT)
            scene.spin();
        else if (mouseButton == RIGHT) {
            scene.shift();
        }
        else
            scene.zoom(mouseX - pmouseX);
    }

    public void mouseWheel(MouseEvent event) {
        scene.moveForward(event.getCount() * 20);
    }

    public static void main(String[] args) {
        PApplet.main(new String[]{"intellij.MaxDepthTest"});
    }
}