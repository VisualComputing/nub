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

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        this.g.textureMode(NORMAL);
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.disableBackBuffer();
        model = ColladaLoader.loadColladaModel(sketchPath() + path, dae, tex, this.g, 3);
        //PImage texture = loadImage(sketchPath() + path + tex);
        //model.getModel().setFill(false);
        //model.getModel().setStroke(false);

        //model.getModel().setTextureMode(NORMAL);
        //model.getModel().setTexture(texture);

        for(PShape sh : model.getModel().getChildren()){
            //sh.setTextureMode(NORMAL);
            //sh.setTexture(texture);

            for(int i = 0; i < sh.getVertexCount(); i++){
                System.out.println("Vector : " + sh.getVertex(i));
            }
        }

        //model.getModel().setFill(color(255,0,0));
        scene.setRadius(model.getModel().getWidth()*2);
        scene.fitBallInterpolation();
    }
    public void draw() {
        background(255);
        lights();
        scene.drawAxes();
        scene.traverse();
        shape(model.getModel());
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