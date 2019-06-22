package nub.ik.loader.collada.data;

import processing.core.*;

import java.util.ArrayList;


/**
 * Created by sebchaparr on 23/07/18.
 */
public class Mesh {
    //TODO : Update
    public static class Vertex{
        int id;
        SkinningData.VertexSkinData skinData;
        PVector vector;
        PVector normal;
        PVector uv;

        public Vertex(Vertex v){
            this.id = v.id;
            this.vector = v.vector != null ? v.vector.copy() : null;
            this.normal = v.normal != null ? v.normal.copy() : null;
            this.uv = v.uv != null ? v.uv.copy() : null;
            this.skinData = v.skinData;
        }

        public Vertex(int id, PVector vector, SkinningData.VertexSkinData skinData){
            this.id = id;
            this.vector = vector;
            this.skinData = skinData;
        }

        public PVector vector(){
            return vector;
        }

        public void setNormal(PVector normal){
            this.normal = normal;
        }

        public void setUV(PVector uv){
            this.uv = uv;
        }
    }

    public static class Face{
        ArrayList<Vertex> vertices;

        public Face(){
            vertices = new ArrayList<Vertex>();
        }

        public ArrayList<Vertex> getVertices(){
            return vertices;
        }

    }

    private ArrayList<Vertex> vertices;
    private ArrayList<Face> faces;

    public ArrayList<Vertex> getVertices(){
        return vertices;
    }

    public ArrayList<Face> getFaces(){
        return faces;
    }

    public Mesh(){
        vertices = new ArrayList<Vertex>();
        faces = new ArrayList<Face>();
    }

    public PShape generatePShape(PGraphics g, String tex){
        g.textureMode(PConstants.NORMAL);
        //TODO: USE PSHAPE IN GROUP MODE
        //Don't use GROUP while https://github.com/processing/processing/issues/5560 is fixed
        //PShape shape = g.createShape(PConstants.GROUP);
        PShape shape = g.createShape();
        shape.beginShape(PConstants.TRIANGLES);
        for(Face face : faces){
            //PShape child = g.createShape();
            //child.beginShape(PConstants.TRIANGLE);
            for(Vertex v : face.vertices){
                //change child by shape
                //add attrib to set weights
                if(v.normal != null) shape.normal(v.normal.x, v.normal.y, v.normal.z);
                if(v.skinData != null) {
                    shape.attrib("joints",
                            v.skinData.jointIds.get(0) * 1.f,
                            v.skinData.jointIds.get(1) * 1.f,
                            v.skinData.jointIds.get(2) * 1.f);
                    shape.attrib("weights",
                            v.skinData.weights.get(0).floatValue(),
                            v.skinData.weights.get(1).floatValue(),
                            v.skinData.weights.get(2).floatValue());
                }
                if(v.uv != null)
                    shape.vertex(v.vector.x, v.vector.y, v.vector.z, v.uv.x, 1 - v.uv.y);
                else
                    shape.vertex(v.vector.x, v.vector.y, v.vector.z);
            }
            //child.endShape(PConstants.CLOSE);
            //shape.addChild(child);
        }
        shape.endShape();
        shape.setStroke(false);
        if(tex != null) {
            PImage texture = g.parent.loadImage(tex);
            shape.setTextureMode(PConstants.NORMAL);
            shape.setTexture(texture);
        } else{
            shape.setStroke(g.color(255));
            shape.setFill(g.color(255));
        }
        return shape;
    }
}
