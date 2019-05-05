package ik.collada.colladaParser.colladaLoader;

import ik.common.Joint;
import nub.core.Node;
import nub.processing.Scene;
import ik.collada.animation.AnimatedModel;
import ik.collada.animation.Animation;
import ik.collada.animation.Mesh;
import ik.collada.animation.SkinningData;
import ik.collada.colladaParser.xmlParser.XmlNode;
import ik.collada.colladaParser.xmlParser.XmlParser;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;

import java.io.File;
import java.util.List;

public class ColladaURDFLoader {
    public static AnimatedModel loadColladaModel(String colladaFile, String dae, Scene scene) {
        XmlNode node = XmlParser.loadXmlFile(colladaFile + dae);
        AnimatedModel model = new AnimatedModel(scene);

        GeometryLoader g = new GeometryLoader(node.getChild("library_geometries"), null);
        List<Mesh> meshes = g.extractURDFModelData();
        int i = 0;
        List<XmlNode> xmlNodes = node.getChild("library_geometries").getChildren("geometry");
        float max = -1;
        for(Mesh mesh : meshes){
            PShape pshape = mesh.generatePShape(scene.context(), null);
            model.addModel(xmlNodes.get(i++).getAttribute("id"), pshape);
            max = max < pshape.getWidth() ? pshape.getWidth() : max;
        }

        SkeletonLoader jointsLoader = new SkeletonLoader(node.getChild("library_visual_scenes"), null);
        jointsLoader.extractBoneData(model, false);

        scene.setRadius(max * 5f);
        for(Node joint : model.getJoints().values()){
            if(joint instanceof Joint) {
                ((Joint)joint).setRadius(scene.radius() * 0.03f);
            }
            joint.setPickingThreshold(-0.0009f);
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