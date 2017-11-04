package basics;

import processing.core.PApplet;
import remixlab.proscene.*;
import remixlab.geom.*;

public class FrameInterpolation extends PApplet {
    Scene scene;
    Node keyFrame[];
    Interpolator kfi;
    int nbKeyFrames;

    public void settings() {
        size(640, 360, P3D);
    }

    public void setup() {
        nbKeyFrames = 4;
        scene = new Scene(this);
        //unsets grid and axis altogether
        scene.setVisualHints( Scene.PICKING );
        scene.setRadius(70);
        scene.fitBall();
        kfi = new Interpolator(scene);
        kfi.setLoopInterpolation();

        // An array of interactive (key) frames.
        keyFrame = new Node[nbKeyFrames];
        // Create an initial path
        for (int i=0; i<nbKeyFrames; i++) {
            keyFrame[i] = new Node(scene);
            keyFrame[i].setPosition(-100 + 200*i/(nbKeyFrames-1), 0, 0);
            keyFrame[i].setScaling(random(0.25f, 4.0f));
            kfi.addKeyFrame(keyFrame[i]);
        }

        kfi.startInterpolation();
    }

    public void draw() {
        background(0);
        pushMatrix();
        scene.applyTransformation(kfi.frame());
        scene.drawAxes(30);
        popMatrix();

        pushStyle();
        stroke(255);
        scene.drawPath(kfi, 5, 10);
        popStyle();

        for (int i=0; i<nbKeyFrames; ++i) {
            pushMatrix();
            kfi.keyFrame(i).applyTransformation(scene);

            if ( keyFrame[i].grabsInput() )
                scene.drawAxes(40);
            else
                scene.drawAxes(20);

            popMatrix();
        }
    }

    public void keyPressed() {
        if ((key == ENTER) || (key == RETURN))
            kfi.toggleInterpolation();
        if ( key == 'u')
            kfi.setInterpolationSpeed(kfi.interpolationSpeed()-0.25f);
        if ( key == 'v')
            kfi.setInterpolationSpeed(kfi.interpolationSpeed()+0.25f);
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"basics.FrameInterpolation"});
    }
}
