package ik.collada.colladaParser.colladaLoader;

import ik.collada.animation.Mesh;
import ik.collada.animation.SkinningData;
import ik.collada.colladaParser.xmlParser.XmlNode;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Loads the mesh data for a model from a collada XML file.
 * @author Karl
 *
 */
public class GeometryLoader {
    //TODO : Update
    private final XmlNode meshData;

    private final List<SkinningData.VertexSkinData> vertexWeights;

    private float[] verticesArray;
    private float[] normalsArray;
    private float[] texturesArray;
    private int[] indicesArray;
    private int[] jointIdsArray;
    private float[] weightsArray;

    List<Mesh.Vertex> vertices = new ArrayList<Mesh.Vertex>();
    List<PVector> textures = new ArrayList<PVector>();
    List<PVector> normals = new ArrayList<PVector>();

    public GeometryLoader(XmlNode geometryNode, List<SkinningData.VertexSkinData> vertexWeights) {
        this.vertexWeights = vertexWeights;
        this.meshData = geometryNode.getChild("geometry").getChild("mesh");
    }

    public Mesh extractModelData(){
        readRawData();
        return assembleVertices();
    }

    private void readRawData() {
        readPositions();
        readNormals();
        readTextureCoords();
    }

    private void readPositions() {
        String positionsId = meshData.getChild("vertices").getChild("input").getAttribute("source").substring(1);
        XmlNode positionsData = meshData.getChildWithAttribute("source", "id", positionsId).getChild("float_array");
        int count = Integer.parseInt(positionsData.getAttribute("count"));
        String[] posData = positionsData.getData().split(" ");
        for (int i = 0; i < count/3; i++) {
            float x = Float.parseFloat(posData[i * 3])*100;
            float y = Float.parseFloat(posData[i * 3 + 1])*100;
            float z = Float.parseFloat(posData[i * 3 + 2])*100;
            PVector position = new PVector(x, y, z);
            vertices.add(new Mesh.Vertex(vertices.size(), new PVector(position.x, position.y, position.z), vertexWeights.get(vertices.size())));
        }
    }

    private void readNormals() {
        String normalsId = meshData.getChild("polylist").getChildWithAttribute("input", "semantic", "NORMAL")
                .getAttribute("source").substring(1);
        XmlNode normalsData = meshData.getChildWithAttribute("source", "id", normalsId).getChild("float_array");
        int count = Integer.parseInt(normalsData.getAttribute("count"));
        String[] normData = normalsData.getData().split(" ");
        for (int i = 0; i < count/3; i++) {
            float x = Float.parseFloat(normData[i * 3])*100;
            float y = Float.parseFloat(normData[i * 3 + 1])*100;
            float z = Float.parseFloat(normData[i * 3 + 2])*100;
            PVector norm = new PVector(x, y, z);
            normals.add(norm);
        }
    }

    private void readTextureCoords() {
        String texCoordsId = meshData.getChild("polylist").getChildWithAttribute("input", "semantic", "TEXCOORD")
                .getAttribute("source").substring(1);
        XmlNode texCoordsData = meshData.getChildWithAttribute("source", "id", texCoordsId).getChild("float_array");
        int count = Integer.parseInt(texCoordsData.getAttribute("count"));
        String[] texData = texCoordsData.getData().split(" ");
        for (int i = 0; i < count/2; i++) {
            float s = Float.parseFloat(texData[i * 2]);
            float t = Float.parseFloat(texData[i * 2 + 1]);
            PVector uv = new PVector(s,t);
            textures.add(uv);
        }
    }

    private Mesh assembleVertices(){
        XmlNode poly = meshData.getChild("polylist");
        int typeCount = poly.getChildren("input").size();
        String[] rawSides = poly.getChild("vcount").getData().split(" ");
        int[] sides = new int[rawSides.length];
        for(int i = 0; i < rawSides.length; i++){
            sides[i] = Integer.parseInt(rawSides[i]);
        }
        int faceCounter = 0;
        int currentSide = 0;
        String[] indexData = poly.getChild("p").getData().split(" ");

        Mesh mesh = new Mesh();

        Mesh.Face currentFace = new Mesh.Face();
        for(int i=0;i<indexData.length/typeCount; i++){
            int positionIndex = Integer.parseInt(indexData[i * typeCount]);
            int normalIndex = Integer.parseInt(indexData[i * typeCount + 1]);
            int texCoordIndex = Integer.parseInt(indexData[i * typeCount + 2]);
            //Create a Vertex
            Mesh.Vertex v = new Mesh.Vertex(vertices.get(positionIndex));
            v.setNormal(normals.get(normalIndex));
            v.setUV(textures.get(texCoordIndex));
            mesh.getVertices().add(v);
            currentFace.getVertices().add(v);
            //Create a Face
            if(faceCounter == sides[currentSide]-1){
                mesh.getFaces().add(currentFace);
                currentFace = new Mesh.Face();
                faceCounter = 0;
                currentSide++;
            } else{
                faceCounter++;
            }
        }
        return mesh;
    }
}