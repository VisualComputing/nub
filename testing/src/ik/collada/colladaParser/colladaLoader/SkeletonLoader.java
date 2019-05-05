package ik.collada.colladaParser.colladaLoader;

import nub.core.Node;
import nub.primitives.Matrix;
import nub.primitives.Quaternion;
import ik.collada.animation.AnimatedModel;
import ik.collada.colladaParser.xmlParser.XmlNode;
import ik.common.Joint;
import nub.primitives.Vector;
import java.util.List;

public class SkeletonLoader {
    private XmlNode armatureData;
    private List<String> boneOrder;
    private int jointCount = 0;
    private float max = -1;

    public SkeletonLoader(XmlNode visualSceneNode, List<String> boneOrder) {
        XmlNode node = visualSceneNode.getChild("visual_scene").getChildWithAttribute("node", "id", "Armature");
        if(node != null) {
            this.armatureData = visualSceneNode.getChild("visual_scene").getChildWithAttribute("node", "id", "Armature");
        } else{
            this.armatureData = visualSceneNode.getChild("visual_scene").getChild("node");
        }
        this.boneOrder = boneOrder;
    }


    public void extractBoneData(AnimatedModel model, boolean blender){
        XmlNode headNode = armatureData.getChild("node");
        Joint root = loadJointData(headNode, model, null, blender);
        root.setRoot(true);
        model.setRootJoint(root);
        model.setJointCount(jointCount);
        System.out.println("max val : " + max);
        model.setScaling(100/max);
        //scale skeleton from children to root
        for(Node j : model.getJoints().values()){
            j.setTranslation(Vector.multiply(j.translation(), model.scaling()));
        }
    }

    private Joint loadJointData(XmlNode jointNode, AnimatedModel model, Joint parent, boolean blender){
        Joint joint = blender ? extractMainJointData(jointNode, model, parent) : extractMainJointTransformationData(jointNode, model, parent);
        float mag = joint.position().magnitude();
        max =  max < mag ? mag : max;
        for(XmlNode childNode : jointNode.getChildren("node")){
            loadJointData(childNode, model, joint, blender);
        }
        return joint;
    }

    private Joint extractMainJointData(XmlNode jointNode, AnimatedModel model, Joint parent){
        String nameId = jointNode.getAttribute("id");
        int index = boneOrder.indexOf(nameId);
        String[] matrixRawData = jointNode.getChild("matrix").getData().split(" ");
        float[] matrixData = convertData(matrixRawData);
        Matrix mat = new Matrix(matrixData);
        Joint joint = new Joint(model.getScene());
        if(parent != null) joint.setReference(parent);
        joint.setTranslation(matrixData[3], matrixData[7], matrixData[11]);
        joint.setRotation(new Quaternion(mat));
        jointCount++;
        model.getJoints().put(nameId, joint);
        model.getIdxs().put(joint.id(), index);
        return joint;
    }

    private Joint extractMainJointTransformationData(XmlNode jointNode, AnimatedModel model, Joint parent){
        Joint joint = new Joint(model.getScene());
        if(parent != null) joint.setReference(parent);

        String nameId = jointNode.getAttribute("id");
        for(XmlNode transformations : jointNode.getChildren()){
            if(transformations.getName().equals("translate")){
                float[] translation = convertData(transformations.getData().split(" "));
                joint.translate(translation[0],translation[1], translation[2]);
            }else if(transformations.getName().equals("rotate")){
                float[] rotation = convertData(transformations.getData().split(" "));
                joint.rotate(new Quaternion(new Vector(rotation[0], rotation[1], rotation[2]), rotation[3]));
            }
        }

        //Related geom
        XmlNode geom = jointNode.getChild("instance_geometry");

        if(geom != null) {
            model.getGeometry().put(geom.getAttribute("url").substring(1), joint);
        }
        jointCount++;
        model.getJoints().put(nameId, joint);
        return joint;
    }

    private float[] convertData(String[] rawData){
        float[] matrixData = new float[Math.min(rawData.length,16)];
        for(int i=0;i<matrixData.length;i++){
            matrixData[i] = Float.parseFloat(rawData[i]);
        }
        return matrixData;
    }

}