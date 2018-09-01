package ik.common;

/**
 * Created by sebchaparr on 21/07/18.
 */

import frames.core.Frame;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;

public class Joint {
    //TODO : Update
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
        PGraphics pg = scene.frontBuffer();
        pg.hint(PConstants.DISABLE_DEPTH_TEST);
        pg.pushStyle();
        pg.fill(color);
        pg.noStroke();
        if (scene.is2D()) pg.ellipse(0, 0, 3, 3);
        else pg.sphere(radius);
        if (!isRoot) {
            pg.strokeWeight(radius/2);
            pg.stroke(color);
            Vector v = frame.location(new Vector(), frame.reference());
            if (scene.is2D()) {
                pg.line(0, 0, v.x(), v.y());
            } else {
                pg.line(0, 0, 0, v.x(), v.y(), v.z());
            }
            pg.popStyle();
        }

        if (frame.constraint() != null) {
            scene.drawConstraint(frame);
        }
        pg.hint(PConstants.ENABLE_DEPTH_TEST);
    }

    public void setRadius(float radius){
        this.radius = radius;
    }
}
