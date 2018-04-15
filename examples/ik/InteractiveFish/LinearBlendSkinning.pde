import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sebatian Chaparro on 24/02/18.
 */
public class LinearBlendSkinning {
  public HashMap<PVector, Vertex> vertices;
  public Frame reference = new Frame();
  public PShape shape;

  public LinearBlendSkinning(Frame reference, PShape shape) {
    this.shape = shape;
    this.reference = reference;
  }

  public class Vertex {
    ArrayList<int[]> id;
    ArrayList<Properties> properties;

    public class Properties {
      Node joint;
      Vector local;
      float weight;

      public Properties(Node joint, Vector local, float weight) {
        this.joint = joint;
        this.weight = weight;
        this.local = local;
      }
    }

    public Vertex(int shapeId, int vertexId) {
      id = new ArrayList<int[]>();
      int[] ids = new int[]{vertexId, shapeId};
      id.add(ids);
      properties = new ArrayList<Properties>();
    }

    public void addId(int shapeId, int vertexId) {
      int[] ids = new int[]{vertexId, shapeId};
      id.add(ids);
    }

    public void applyTransformation() {
      Vector newPosition = new Vector(0, 0, 0);
      float total = 0;
      for (Vertex.Properties properties : this.properties) {
        newPosition = Vector.add(newPosition, 
          Vector.multiply(reference.coordinatesOfFrom(properties.local, properties.joint), 
          properties.weight));
        total += properties.weight;
      }
      setVertexPosition(new Vector(newPosition.x() / total, newPosition.y() / total, newPosition.z() / total));
    }

    public void setVertexPosition(Vector position) {
      for (int[] ids : this.id) {
        PShape p = shape.getChild(ids[0]);
        p.setVertex(ids[1], new PVector(position.x(), position.y(), position.z()));
      }
    }

    public void addProperties(Node joint, Vector local, float dist) {
      properties.add(new Vertex.Properties(joint, local, dist));
    }
  }

  public void setup(ArrayList<Node> branch) {
    vertices = new HashMap<PVector, Vertex>();
    for (int i = 0; i < shape.getChildCount(); i++) {
      PShape child = shape.getChild(i);
      for (int j = 0; j < child.getVertexCount(); j++) {
        PVector vector = child.getVertex(j);
        if (!vertices.containsKey(vector)) {
          Vertex vertex = new Vertex(j, i);
          Vector position = new Vector(vector.x, vector.y, vector.z);
          float total_dist = 0.f;
          float max_dist = -999;
          for (Node joint : branch) {
            if (joint.translation().magnitude() < Float.MIN_VALUE) continue;
            float dist = getDistance(position, joint, reference);
            max_dist = dist > max_dist ? dist : max_dist;
          }

          for (Node joint : branch) {
            if (joint == branch.get(0)) continue;
            if (joint.translation().magnitude() < Float.MIN_VALUE) continue;
            float dist = getDistance(position, joint, reference);
            dist = 1 / ((float) Math.pow(dist, 10));
            total_dist += dist;
            vertex.addProperties(joint.reference(), 
              joint.reference().coordinatesOfFrom(position, reference), dist);
          }
          //The more near, the more weight the bone applies
          float sum = 0;
          for (Vertex.Properties properties : vertex.properties) {
            properties.weight = properties.weight / (total_dist * 1.f);
            sum += properties.weight;
          }
          vertices.put(vector, vertex);
        } else {
          Vertex vertex = vertices.get(vector);
          //add the id of the face and the vertex
          vertex.addId(j, i);
        }
      }
    }
  }

  public void applyTransformations() {
    for (Map.Entry<PVector, Vertex> entry : vertices.entrySet()) {
      entry.getValue().applyTransformation();
    }
  }

  /*
   * Get the distance from vertex to line formed by frame and the reference frame of frame
   * Distance will be measure according to root coordinates.
   * In case of reference frame of frame is root, it will return distance from vertex to frame
   * */
  public float getDistance(Vector vertex, Frame frame, Frame root) {
    if (frame == null) return 9999;
    Vector position = root.coordinatesOf(frame.position());
    Vector parentPosition = root.coordinatesOf(frame.reference().position());
    if (frame.reference() == root) {
      return Vector.distance(position, vertex);
    }
    //is the distance between line formed by b and its parent and v
    Vector line = Vector.subtract(position, parentPosition);
    Vector projection = Vector.subtract(vertex, parentPosition);
    float dot = Vector.dot(projection, line);
    float magnitude = line.magnitude();
    float u = dot * (float) 1. / (magnitude * magnitude);
    Vector distance = new Vector();
    if (u >= 0 && u <= 1) {
      distance = new Vector(parentPosition.x() + u * line.x(), parentPosition.y() + u * line.y(), 
        parentPosition.z() + u * line.z());
      distance = Vector.subtract(distance, vertex);
    }
    if (u < 0)
      distance = Vector.subtract(position, vertex);
    if (u > 1)
      distance = Vector.subtract(position, vertex);
    return distance.magnitude();
  }
}
