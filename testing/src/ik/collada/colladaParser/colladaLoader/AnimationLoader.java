package ik.collada.colladaParser.colladaLoader;

import frames.core.Frame;
import frames.primitives.Matrix;
import frames.primitives.Quaternion;
import ik.collada.animation.Animation;
import ik.collada.animation.KeyFrame;
import ik.collada.colladaParser.xmlParser.XmlNode;

import java.util.List;

public class AnimationLoader {

    private XmlNode animationData;
    private XmlNode jointHierarchy;

    public AnimationLoader(XmlNode animationData, XmlNode jointHierarchy){
        this.animationData = animationData;
        this.jointHierarchy = jointHierarchy;
    }

    public Animation extractAnimation(){
        String rootNode = findRootJointName();
        float[] times = getKeyTimes();
        float duration = times[times.length-1];
        KeyFrame[] keyFrames = initKeyFrames(times);
        List<XmlNode> animationNodes = animationData.getChildren("animation");
        for(XmlNode jointNode : animationNodes){
            loadJointTransforms(keyFrames, jointNode, rootNode);
        }
        return new Animation(duration, keyFrames);
    }

    private float[] getKeyTimes(){
        XmlNode timeData = animationData.getChild("animation").getChild("source").getChild("float_array");
        String[] rawTimes = timeData.getData().split(" ");
        float[] times = new float[rawTimes.length];
        for(int i=0;i<times.length;i++){
            times[i] = Float.parseFloat(rawTimes[i]);
        }
        return times;
    }

    private KeyFrame[] initKeyFrames(float[] times){
        KeyFrame[] frames = new KeyFrame[times.length];
        for(int i=0;i<frames.length;i++){
            frames[i] = new KeyFrame(times[i]);
        }
        return frames;
    }

    private void loadJointTransforms(KeyFrame[] frames, XmlNode jointData, String rootNodeId){
        String jointNameId = getJointName(jointData);
        String dataId = getDataId(jointData);
        XmlNode transformData = jointData.getChildWithAttribute("source", "id", dataId);
        String[] rawData = transformData.getChild("float_array").getData().split(" ");
        processTransforms(jointNameId, rawData, frames);
    }

    private String getDataId(XmlNode jointData){
        XmlNode node = jointData.getChild("sampler").getChildWithAttribute("input", "semantic", "OUTPUT");
        return node.getAttribute("source").substring(1);
    }

    private String getJointName(XmlNode jointData){
        XmlNode channelNode = jointData.getChild("channel");
        String data = channelNode.getAttribute("target");
        return data.split("/")[0];
    }

    private void processTransforms(String jointName, String[] rawData, KeyFrame[] keyFrames){
        float[] matrixData = new float[16];
        for(int i=0;i<keyFrames.length;i++){
            for(int j=0;j<16;j++){
                matrixData[j] = Float.parseFloat(rawData[i*16 + j]);
            }
            Matrix mat = new Matrix(matrixData);
            Frame frame = new Frame();
            frame.setTranslation(matrixData[3], matrixData[7], matrixData[11]);
            frame.setRotation(new Quaternion(mat));
            keyFrames[i].getPose().put(jointName, frame);
        }
    }

    private String findRootJointName(){
        XmlNode skeleton = jointHierarchy.getChild("visual_scene").getChildWithAttribute("node", "id", "Armature");
        return skeleton.getChild("node").getAttribute("id");
    }


}