package nub.ik.loader.collada.data;

import nub.primitives.Matrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapted by sebchaparr on 22/07/18.
 * See https://github.com/TheThinMatrix/OpenGL-Animation
 */
public class SkinningData {
  //TODO : Update
  public static class VertexSkinData {
    List<Integer> jointIds;
    List<Float> weights;

    public VertexSkinData() {
      jointIds = new ArrayList<Integer>();
      weights = new ArrayList<Float>();
    }

    public void addJointEffect(int jointId, float weight) {
      for (int i = 0; i < weights.size(); i++) {
        if (weight > weights.get(i)) {
          jointIds.add(i, jointId);
          weights.add(i, weight);
          return;
        }
      }
      jointIds.add(jointId);
      weights.add(weight);
    }

    public void limitJointNumber(int max) {
      if (jointIds.size() > max) {
        float[] topWeights = new float[max];
        float total = saveTopWeights(topWeights);
        refillWeightList(topWeights, total);
        removeExcessJointIds(max);
      } else if (jointIds.size() < max) {
        fillEmptyWeights(max);
      }
    }

    private void fillEmptyWeights(int max) {
      while (jointIds.size() < max) {
        jointIds.add(0);
        weights.add(0f);
      }
    }

    private float saveTopWeights(float[] topWeightsArray) {
      float total = 0;
      for (int i = 0; i < topWeightsArray.length; i++) {
        topWeightsArray[i] = weights.get(i);
        total += topWeightsArray[i];
      }
      return total;
    }

    private void refillWeightList(float[] topWeights, float total) {
      weights.clear();
      for (int i = 0; i < topWeights.length; i++) {
        weights.add(Math.min(topWeights[i] / total, 1));
      }
    }

    private void removeExcessJointIds(int max) {
      while (jointIds.size() > max) {
        jointIds.remove(jointIds.size() - 1);
      }
    }
  }

  public List<String> jointOrder;
  public List<VertexSkinData> verticesSkinData;
  public List<Matrix> bindMatrices;

  public SkinningData(List<String> joints, List<VertexSkinData> verticesSkinData, List<Matrix> bindMatrices) {
    this.jointOrder = joints;
    this.verticesSkinData = verticesSkinData;
    this.bindMatrices = bindMatrices;
  }
}
