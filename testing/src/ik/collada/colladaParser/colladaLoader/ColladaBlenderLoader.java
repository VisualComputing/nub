package ik.collada.colladaParser.colladaLoader;

import ik.common.Joint;
import nub.core.Node;
import nub.primitives.Matrix;
import nub.processing.Scene;
import ik.collada.animation.Model;
import ik.collada.animation.Animation;
import ik.collada.animation.Mesh;
import ik.collada.animation.SkinningData;
import ik.collada.colladaParser.xmlParser.XmlNode;
import ik.collada.colladaParser.xmlParser.XmlParser;
import processing.core.PImage;

public class ColladaBlenderLoader {
    /**
     * Load a Collada model from Blender
     * This parser is usable only for quite simple structures
     * */

    public static Model loadColladaModel(String colladaFile, String dae, String tex, Scene scene, int maxWeights){
        XmlNode node = XmlParser.loadXmlFile(colladaFile + dae);

        Model model = new Model(scene);

        SkinLoader skinLoader = new SkinLoader(node.getChild("library_controllers"), maxWeights);
        SkinningData skinningData = skinLoader.extractSkinData();
        SkeletonLoader jointsLoader = new SkeletonLoader(node.getChild("library_visual_scenes"), skinningData.jointOrder, skinningData.bindMatrices);
        jointsLoader.extractBoneData(model,  true);

        XmlNode bind = node.getChild("library_controllers").getChild("controller").getChild("skin").getChild("bind_shape_matrix");
        String[] bind_data = bind.getData().split(" ");
        float[] mat = new float[bind_data.length];

        for(int i = 0; i < bind_data.length; i++){
            mat[i] = Float.parseFloat(bind_data[i]);
        }
        Matrix m = new Matrix(mat, false);

        GeometryLoader g = new GeometryLoader(node.getChild("library_geometries"), skinningData.verticesSkinData, m);
        Mesh meshData = g.extractBlenderModelData(model.scaling());
        model.addModel(null,meshData.generatePShape(scene.context(), tex == null ? null : colladaFile + tex));


        if(tex == null){
            //set a dummy texture
            PImage img = scene.context().parent.createImage(1,1, scene.context().parent.ARGB);
            img.loadPixels();
            for (int i = 0; i < img.pixels.length; i++) {
                img.pixels[i] = scene.context().color(222, 184, 135);
            }
            model.mesh().get(null).setTexture(img);
        }

        scene.setRadius(100);
        for(Node joint : scene.nodes()){
            if(joint instanceof Joint) {
                ((Joint)joint).setRadius(scene.radius() * 0.03f);
            }
            joint.setPickingThreshold(-0.03f);
        }

        return model;
    }

    public static Animation loadColladaAnimation(String colladaFile) {
        XmlNode node = XmlParser.loadXmlFile(colladaFile);
        XmlNode animNode = node.getChild("library_animations");
        XmlNode jointsNode = node.getChild("library_visual_scenes");
        AnimationLoader loader = new AnimationLoader(animNode, jointsNode);
        Animation animData = loader.extractAnimation();
        return animData;
    }


}