package ik.common;

import common.InteractiveShape;
import frames.processing.Scene;
import processing.core.PGraphics;

/**
 * Created by sebchaparr on 28/01/18.
 */
public class Target extends InteractiveShape {
    public Target(Scene scene) {
        super(scene);
    }

    public void set(PGraphics pg) {
        pg.pushStyle();
        pg.noStroke();
        pg.fill(255, 0, 0, 200);
        if(pg.is2D())
            pg.ellipse(0, 0, 5, 5);
        else
            pg.sphere(5);
        pg.popStyle();
        graph().drawAxes(5);

    }
}