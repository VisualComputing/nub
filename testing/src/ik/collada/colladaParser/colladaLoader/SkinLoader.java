package ik.collada.colladaParser.colladaLoader;

import ik.collada.animation.Mesh;
import ik.collada.animation.SkinningData;
import ik.collada.colladaParser.xmlParser.XmlNode;

import java.util.ArrayList;
import java.util.List;


public class SkinLoader {

    private final XmlNode skinningData;
    private final int maxWeights;

    public SkinLoader(XmlNode controllersNode, int maxWeights) {
        this.skinningData = controllersNode.getChild("controller").getChild("skin");
        this.maxWeights = maxWeights;
    }

    public SkinningData extractSkinData() {
        List<String> jointsList = loadJointsList();
        float[] weights = loadWeights();
        XmlNode weightsDataNode = skinningData.getChild("vertex_weights");
        int[] effectorJointCounts = getEffectiveJointsCounts(weightsDataNode);
        List<SkinningData.VertexSkinData> vertexWeights = getSkinData(weightsDataNode, effectorJointCounts, weights);
        return new SkinningData(jointsList, vertexWeights);
    }

    private List<String> loadJointsList() {
        XmlNode inputNode = skinningData.getChild("vertex_weights");
        String jointDataId = inputNode.getChildWithAttribute("input", "semantic", "JOINT").getAttribute("source")
                .substring(1);
        XmlNode jointsNode = skinningData.getChildWithAttribute("source", "id", jointDataId).getChild("Name_array");
        String[] names = jointsNode.getData().split(" ");
        List<String> jointsList = new ArrayList<String>();
        for (String name : names) {
            jointsList.add(name);
        }
        return jointsList;
    }

    private float[] loadWeights() {
        XmlNode inputNode = skinningData.getChild("vertex_weights");
        String weightsDataId = inputNode.getChildWithAttribute("input", "semantic", "WEIGHT").getAttribute("source")
                .substring(1);
        XmlNode weightsNode = skinningData.getChildWithAttribute("source", "id", weightsDataId).getChild("float_array");
        String[] rawData = weightsNode.getData().split(" ");
        float[] weights = new float[rawData.length];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = Float.parseFloat(rawData[i]);
        }
        return weights;
    }

    private int[] getEffectiveJointsCounts(XmlNode weightsDataNode) {
        String[] rawData = weightsDataNode.getChild("vcount").getData().split(" ");
        int[] counts = new int[rawData.length];
        for (int i = 0; i < rawData.length; i++) {
            counts[i] = Integer.parseInt(rawData[i]);
        }
        return counts;
    }

    private List<SkinningData.VertexSkinData> getSkinData(XmlNode weightsDataNode, int[] counts, float[] weights) {
        String[] rawData = weightsDataNode.getChild("v").getData().split(" ");
        List<SkinningData.VertexSkinData> skinningData = new ArrayList<SkinningData.VertexSkinData>();
        int pointer = 0;
        for (int count : counts) {
            SkinningData.VertexSkinData skinData = new SkinningData.VertexSkinData();
            for (int i = 0; i < count; i++) {
                int jointId = Integer.parseInt(rawData[pointer++]);
                int weightId = Integer.parseInt(rawData[pointer++]);
                skinData.addJointEffect(jointId, weights[weightId]);
            }
            skinData.limitJointNumber(maxWeights);
            skinningData.add(skinData);
        }
        return skinningData;
    }

}