package ik.collada.colladaParser.colladaLoader;

import ik.collada.animation.Mesh;
import ik.collada.animation.SkinningData;
import ik.collada.colladaParser.xmlParser.XmlNode;
import nub.primitives.Matrix;
import nub.primitives.Vector;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Adapted by sebchaparr on 22/07/18.
 * See https://github.com/TheThinMatrix/OpenGL-Animation
 * Loads the mesh data for a model from a collada XML file.
 *
 */
public class GeometryLoader {
    private XmlNode meshData;

    private Matrix bind;
    private final List<SkinningData.VertexSkinData> vertexWeights;
    List<PVector> textures = new ArrayList<PVector>();
    List<PVector> normals = new ArrayList<PVector>();

    public GeometryLoader(XmlNode geometryNode, List<SkinningData.VertexSkinData> vertexWeights){
        this(geometryNode, vertexWeights, null);
    }

    public GeometryLoader(XmlNode geometryNode, List<SkinningData.VertexSkinData> vertexWeights, Matrix bind) {
        this.vertexWeights = vertexWeights;
        this.meshData = geometryNode;
        this.bind = bind;
    }

    public Mesh extractBlenderModelData(float scaling){
        meshData = meshData.getChild("geometry").getChild("mesh");
        String positionsId = meshData.getChild("vertices").getChild("input").getAttribute("source").substring(1);
        XmlNode positionsData = meshData.getChildWithAttribute("source", "id", positionsId).getChild("float_array");
        List<Mesh.Vertex> vertices = readPositions(true , positionsData, scaling);
        if(meshData.getChild("polylist") != null) {
            readNormals();
            readTextureCoords();
            return assembleVertices(vertices);
        }
        return assembleTriangles(meshData, vertices);
    }

    public List<Mesh> extractURDFModelData(float scaling){
        List<Mesh> meshes = new ArrayList<>();
        for(XmlNode xmlNode : meshData.getChildren("geometry")){
            XmlNode positionsData = xmlNode.getChild("mesh").getChild("source").getChild("float_array");
            List<Mesh.Vertex> vertices = readPositions(false, positionsData, scaling);
            meshes.add(assembleTriangles(xmlNode.getChild("mesh"), vertices));
        }
        return meshes;
    }

    private List<Mesh.Vertex> readPositions(boolean blender, XmlNode positionsData, float scaling) {
        int count = Integer.parseInt(positionsData.getAttribute("count"));
        String[] posData = positionsData.getData().split(" ");
        List<Mesh.Vertex> vertices = new ArrayList<>();
        for (int i = 0; i < posData.length/3; i++) {
            float x = Float.parseFloat(posData[i * 3]) * 1;
            float y = Float.parseFloat(posData[i * 3 + 1]) * 1;
            float z = Float.parseFloat(posData[i * 3 + 2]) * 1;
            Vector position = new Vector(x, y, z);
            if(bind != null) position = this.bind.multiply(position);
            position.multiply(scaling);

            vertices.add(new Mesh.Vertex(vertices.size(), new PVector(position.x(), position.y(), position.z()), blender ? vertexWeights.get(vertices.size()) : null));
        }
        return vertices;
    }

    private void readNormals() {
        String normalsId = meshData.getChild("polylist").getChildWithAttribute("input", "semantic", "NORMAL")
                .getAttribute("source").substring(1);
        XmlNode normalsData = meshData.getChildWithAttribute("source", "id", normalsId).getChild("float_array");

        int count = Integer.parseInt(normalsData.getAttribute("count"));
        String[] normData = normalsData.getData().split(" ");
        for (int i = 0; i < count/3; i++) {
            float x = Float.parseFloat(normData[i * 3]);
            float y = Float.parseFloat(normData[i * 3 + 1]);
            float z = Float.parseFloat(normData[i * 3 + 2]);
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

    private Mesh assembleTriangles(XmlNode geom, List<Mesh.Vertex> vertices){
        Mesh mesh = new Mesh();
        for(XmlNode triangles : geom.getChildren("triangles")){
            int total = -1;
            for(XmlNode node : triangles.getChildren("input")){
                int next = Integer.parseInt(node.getAttribute("offset"));
                if(total < next) total = next;

            }
            total  += 1;
            int offset = Integer.parseInt(triangles.getChildWithAttribute("input", "semantic", "VERTEX").getAttribute("offset"));
            String[] rawIndexData = triangles.getChild("p").getData().split(" ");

            int[] indexData = new int[rawIndexData.length / total];

            for(int i = offset; i < indexData.length; i++){
                indexData[i] = Integer.parseInt(rawIndexData[i * total]);
            }
            for(int i=0;i<indexData.length; i+=3){
                Mesh.Face face = new Mesh.Face();

                int i1 = indexData[i];
                int i2 = indexData[i + 1];
                int i3 = indexData[i + 2];
                //Create a Vertex
                Mesh.Vertex v1 = new Mesh.Vertex(vertices.get(i1));
                Mesh.Vertex v2 = new Mesh.Vertex(vertices.get(i2));
                Mesh.Vertex v3 = new Mesh.Vertex(vertices.get(i3));

                mesh.getVertices().add(v1);
                mesh.getVertices().add(v2);
                mesh.getVertices().add(v3);

                face.getVertices().add(v1);
                face.getVertices().add(v2);
                face.getVertices().add(v3);

                mesh.getFaces().add(face);
            }
        }

        return mesh;
    }

    private Mesh assembleVertices(List<Mesh.Vertex> vertices){
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