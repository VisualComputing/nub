package ik.collada.colladaParser.colladaLoader;

import frames.core.Frame;
import frames.primitives.Matrix;
import frames.primitives.Quaternion;
import ik.collada.animation.AnimatedModel;
import ik.collada.colladaParser.xmlParser.XmlNode;
import ik.common.Joint;

import java.util.List;

public class SkeletonLoader {
    //TODO : Update
    private XmlNode armatureData;
    private List<String> boneOrder;
    private int jointCount = 0;

    public SkeletonLoader(XmlNode visualSceneNode, List<String> boneOrder) {
        this.armatureData = visualSceneNode.getChild("visual_scene").getChildWithAttribute("node", "id", "Armature");
        this.boneOrder = boneOrder;
    }

    public void extractBoneData(AnimatedModel model){
        XmlNode headNode = armatureData.getChild("node");
        Joint root = loadJointData(headNode, model, null);
        root.setRoot(true);
        model.setRootJoint(root);
        model.setJointCount(jointCount);
    }

    private Joint loadJointData(XmlNode jointNode, AnimatedModel model, Joint parent){
        Joint joint = extractMainJointData(jointNode, model, parent);
        for(XmlNode childNode : jointNode.getChildren("node")){
            loadJointData(childNode, model, joint);
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
        Frame frame = joint;
        if(parent != null) frame.setReference(parent);
        frame.setTranslation(matrixData[3], matrixData[7], matrixData[11]);
        frame.setRotation(new Quaternion(mat));
        joint.setRadius(0.2f);
        jointCount++;
        model.getJoints().put(nameId, frame);
        model.getIdxs().put(frame.id(), index);
        return joint;
    }

    private float[] convertData(String[] rawData){
        float[] matrixData = new float[16];
        for(int i=0;i<matrixData.length;i++){
            matrixData[i] = Float.parseFloat(rawData[i]);
        }
        return matrixData;
    }

}