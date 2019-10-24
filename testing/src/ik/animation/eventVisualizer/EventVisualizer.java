package ik.animation.eventVisualizer;

import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public class EventVisualizer extends PApplet {
    int rows = 15, cols = 100;
    int _gray1, _gray2, _gray3, _gray4, _red, _blue1, _blue2, _green1, _green2, _yellow, _white;
    PFont font36;

    Scene scene;
    Board board;

    public void settings() {
        size(1200, 800, P2D);
    }

    public void setup() {
         _gray1 = color(82,82,82); _gray2 = color(65,65,65); _gray3 = color(49,49,49); _gray4 = color(179,179,179);
        _red = color(202,62,71); _blue1 = color(23,34,59); _blue2 = color(38,56,89); _green1 = color(0,129,138);
        _yellow = color(249,210,118);
        _white = color(240,236,226);
        _green2 = color(33,152,151);
        scene = new Scene(this);
        scene.setRadius(height/2.f);
        scene.fit();

        //Setting the board
        board = new Board(scene, rows, cols);
        float rh = scene.radius(), rw = rh*scene.aspectRatio();
        board.setDimension(-rw, -rh, 10*rw, 5*rh);
        font36 = loadFont("FreeSans-36.vlw");

        //Adding some cells
        EventCell cell = new EventCell(board, "Evento con nombre largo", 2, 3, 3, 3);
        cell = new EventCell(board, "Evento 2", 1, 5, 5, 5);
        cell = new EventCell(board, "Evento 3", 3, 7, 6, 6);

        //set eye constraint
        scene.eye().setConstraint(new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Node node) {
                Vector v = Vector.add(node.translation(), translation);
                if(v.x() < -5) v.setX(-5);
                if(v.y() < -5) v.setY(-5);

                return Vector.subtract(v,node.translation());
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Node node) {
                return new Quaternion(); //no rotation is allowed
            }
        });

    }


    public void draw() {
        background(0);
        scene.render();
    }

    public void keyPressed() {
        if(key == 'r'){
            board.addRows(1, true);
        } else if (key == 'c') {
            board.addCols(1, true);
        }

    }

    public void mouseMoved(){
        scene.cast();
    }

    public void mouseDragged(){
        if(scene.trackedNode() instanceof Slot){
            scene.trackedNode().interact("OnMovement", new Vector(scene.mouse().x(), scene.mouse().y()));
        } else {
            scene.translate();
        }
    }

    public void mouseReleased(){
        if(scene.trackedNode() instanceof EventCell){
            //we must align the event to the closest row / col
            ((EventCell) scene.trackedNode()).applyMovement();
        } else if(scene.trackedNode() instanceof Slot){
            scene.trackedNode().interact("OnFinishedMovement", new Vector(scene.mouse().x(), scene.mouse().y()));
        }
    }

    public void mouseWheel(MouseEvent event) {
        if(scene.trackedNode() == null) scene.scale(event.getCount() * 50);
    }

    public static void main(String[] args) {
        PApplet.main(new String[]{"ik.animation.eventVisualizer.EventVisualizer"});
    }

}
