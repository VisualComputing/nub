class Joint extends Node{
    /*
    * A Joint will be represented as a green ball
    * that is joined by a Line to its reference Node
    * Note that you could override graphics or interaction methods 
    * to generate complex behaviors
    * */
    float radius;
    boolean drawLine;
    
    Joint(Scene scene, Node node, Vector translation, float radius, boolean drawLine){
        super(scene);
        this.radius = radius;
        this.drawLine = drawLine;
        this.setReference(node);
        //Exact picking precision
        this.setPickingThreshold(0);
        this.setTranslation(translation);
    }
    
    @Override
    public void graphics(PGraphics pg){
        Scene scene = (Scene) this._graph;
        pg.pushStyle();
        if (drawLine) {
            pg.stroke(255);
            Vector v = location(new Vector(), reference());
            if (scene.is2D()) {
                pg.line(0, 0, v.x(), v.y());
            } else {
                pg.line(0, 0, 0,  v.x(), v.y(), v.z());
            }
        }
        pg.fill(color(0,255,0));
        pg.noStroke();
        if (scene.is2D()) pg.ellipse(0, 0, radius*2, radius*2);
        else pg.sphere(radius);

        //Invoke drawConstraint method to draw the constraint related with the joint
        if (constraint() != null) {
            scene.drawConstraint(pg,this);
        }
        pg.strokeWeight(5);
        scene.drawAxes(radius*3);
        pg.popStyle();      
    }
}
