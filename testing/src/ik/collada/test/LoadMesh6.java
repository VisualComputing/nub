package ik.collada.test;

import ik.common.Joint;
import nub.core.Graph;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import ik.collada.animation.AnimatedModel;
import ik.collada.colladaParser.colladaLoader.ColladaBlenderLoader;
import ik.common.SkinningAnimationModel;
import processing.core.*;
import processing.event.MouseEvent;

/**
 * Created by sebchaparr on 18/05/19.
 */
public class LoadMesh6 extends PApplet {
    //TODO : Update
    Scene scene;
    String path = "/testing/data/dae/";
    String dae = "hand_rig.dae";
    String tex = null;
    AnimatedModel model;
    SkinningAnimationModel skinning;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        randomSeed(14);
        Joint.markers = true;
        this.g.textureMode(NORMAL);
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);

        model = ColladaBlenderLoader.loadColladaModel(sketchPath() + path, dae, tex, scene, 3);
        //model = ColladaBlenderLoader.loadColladaModel("C:\\Users\\usuario\\Desktop\\Computer_Graphics_books\\Models\\blender-models\\komodoDragon_FBX_Textures\\", "komodo.dae", "komodoDragon_Diffuse.png", scene, 3);
        //model = ColladaBlenderLoader.loadColladaModel("C:\\Users\\usuario\\Desktop\\Computer_Graphics_books\\Models\\blender-models\\babyElephant_FBX\\", "elephant.dae", null, scene, 3);
        //model = ColladaBlenderLoader.loadColladaModel("C:\\Users\\usuario\\Desktop\\Computer_Graphics_books\\Models\\Simple_models\\dolphin\\dolphin\\", "dolphin.dae", null, scene, 3);
        //model = ColladaBlenderLoader.loadColladaModel("C:\\Users\\usuario\\Desktop\\Computer_Graphics_books\\Models\\blender-models\\More_blender\\Low_dragon\\", "dragon.dae", null, scene, 3);

        scene.setRadius(model.getModels().get(null).getWidth()*2);
        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2));
        scene.eye().rotate(new Quaternion(new Vector(0,0,1), PI));
        scene.fit();
        skinning = new SkinningAnimationModel(model);
    }

    public void draw() {
        skinning.updateParams();
        background(0);
        lights();
        scene.drawAxes();
        shader(skinning.shader);
        shape(model.getModels().get(null));
        resetShader();
        hint(DISABLE_DEPTH_TEST);
        scene.render();
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
            scene.scale(scene.mouseDX());
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
        PApplet.main(new String[]{"ik.collada.test.LoadMesh6"});
    }
}