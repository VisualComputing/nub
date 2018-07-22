package ik.common;

/**
 * Created by sebchaparr on 21/07/18.
 */

import frames.core.Frame;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;

public class Joint {
    Scene scene;
    PApplet pApplet;
    public Frame frame;
    int color;
    float radius = 5;
    //set to true only when the joint is the root (for rendering purposes)
    public boolean isRoot = false;

    public Joint(Scene scn, int color){
        scene = scn;
        pApplet = scene.pApplet();
        this.color = color;
        frame = new Frame(scene){
            @Override
            public void visit(){
                render();
            }
        };
    }

    public Joint(Scene scn){
        this(scn, scn.pApplet().color(scn.pApplet().random(0,255),scn.pApplet().random(0,255), scn.pApplet().random(0,255)));
    }

    public void render(){
        pApplet.pushStyle();
        pApplet.fill(color);
        pApplet.noStroke();
        if (scene.is2D()) pApplet.ellipse(0, 0, 3, 3);
        else pApplet.sphere(radius);
        if (!isRoot) {
            pApplet.strokeWeight(radius/2);
            pApplet.stroke(color);
            Vector v = frame.location(new Vector(), frame.reference());
            if (scene.is2D()) {
                pApplet.line(0, 0, v.x(), v.y());
            } else {
                pApplet.line(0, 0, 0, v.x(), v.y(), v.z());
            }
            pApplet.popStyle();
        }

        if (frame.constraint() != null) {
            scene.drawConstraint(frame);
        }
    }
}
