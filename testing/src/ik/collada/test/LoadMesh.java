package ik.collada.test;

import frames.core.Graph;
import frames.core.constraint.Hinge;
import frames.core.constraint.PlanarPolygon;
import frames.ik.Solver;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.collada.animation.AnimatedModel;
import ik.collada.colladaParser.colladaLoader.ColladaLoader;
import ik.common.Joint;
import ik.common.Skinning;
import processing.core.*;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sebchaparr on 23/07/18.
 */
public class LoadMesh extends PApplet {
    Scene scene;
    String path = "/testing/data/dae/";
    String dae = "model.dae";
    String tex = "diffuse.png";
    AnimatedModel model;
    Skinning skinning;
    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        this.g.textureMode(NORMAL);
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.disableBackBuffer();
        model = ColladaLoader.loadColladaModel(sketchPath() + path, dae, tex, scene, 3);
        scene.setRadius(model.getModel().getWidth()*2);
        scene.fitBallInterpolation();
        skinning = new Skinning(model);

    }
    public void draw() {
        skinning.updateParams();
        background(0);
        lights();
        shader(skinning.shader);
        shape(model.getModel());
        resetShader();

        scene.drawAxes();
        hint(DISABLE_DEPTH_TEST);
        scene.traverse();
        hint(ENABLE_DEPTH_TEST);
    }

    @Override
    public void mouseMoved() {
        scene.cast();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            scene.spin();
        } else if (mouseButton == RIGHT) {
            scene.translate();
        } else {
            scene.zoom(scene.mouseDX());
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


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.collada.test.LoadMesh"});
    }
}