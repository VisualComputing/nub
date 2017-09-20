package constraint;

import processing.core.*;
import remixlab.dandelion.constraint.*;
import remixlab.dandelion.core.*;
import remixlab.dandelion.geom.*;
import remixlab.proscene.*;

public class Hinge2D extends PApplet {
    Scene scene;
    InteractiveFrame f;
    Hinge hinge = new Hinge();

    float q = 5;

    public void settings() {
        size(800, 800, P2D);
    }

    public void setup() {
        // We instantiate our MyScene class defined below
        scene = new Scene(this);
        hinge.setMax(radians(30));
        hinge.setMin(radians(30));
        f = new InteractiveFrame(scene);
        f.rotate(new Rot(radians(45)));
        hinge.setRestRotation(f.rotation().get());
        f.setConstraint(hinge);
    }

    // Make sure to define the draw() method, even if it's empty.
    public void draw() {
        background(0);
        background(0);
        pushMatrix();
        f.applyTransformation();
        scene.drawAxes(40);
        if (scene.motionAgent().defaultGrabber() == frame) {
            fill(0, 255, 255);
            scene.drawTorusSolenoid();
        }
        else if (f.grabsInput()) {
            fill(255, 0, 0);
            scene.drawTorusSolenoid();
        }
        else {
            fill(0, 0, 255, 150);
            scene.drawTorusSolenoid();
        }
        popMatrix();
    }

    public void keyPressed(){
        if(key == 'c'){
            q = -1*q;
        }
        if(key == 'z'){
            f.rotate(new Rot(radians(q)));
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"constraint.Hinge2D"});
    }
}
