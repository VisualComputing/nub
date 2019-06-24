package nub.ik.loader.collada;

import nub.core.Node;
import nub.ik.loader.collada.data.Mesh;
import nub.ik.loader.collada.data.Model;
import nub.ik.loader.collada.xml.XmlNode;
import nub.ik.loader.collada.xml.XmlParser;
import nub.ik.visual.Joint;
import nub.processing.Scene;
import processing.core.PShape;

import java.util.List;

public class URDFLoader {
  public static Model loadColladaModel(String colladaFile, String dae, Scene scene) {
    XmlNode node = XmlParser.loadXmlFile(colladaFile + dae);
    Model model = new Model(scene);

    SkeletonLoader jointsLoader = new SkeletonLoader(node.getChild("library_visual_scenes"), null);
    jointsLoader.extractBoneData(model, false);

    GeometryLoader g = new GeometryLoader(node.getChild("library_geometries"), null);
    List<Mesh> meshes = g.extractURDFModelData(model.scaling());
    int i = 0;
    List<XmlNode> xmlNodes = node.getChild("library_geometries").getChildren("geometry");
    float max = -1;
    for (Mesh mesh : meshes) {
      PShape pshape = mesh.generatePShape(scene.context(), null);
      String id = xmlNodes.get(i++).getAttribute("id");
      model.addModel(id, pshape);
      max = max < pshape.getWidth() ? pshape.getWidth() : max;
      Joint joint = ((Joint) model.meshMap().get(id));
      pshape.setFill(joint.color());
      joint.addMesh(pshape);
    }


    scene.setRadius(max * 5f);
    for (Node joint : model.skeleton().values()) {
      if (joint instanceof Joint) {
        ((Joint) joint).setRadius(scene.radius() * 0.03f);
      }
      joint.setPickingThreshold(-0.03f);
    }

    return model;
  }
}