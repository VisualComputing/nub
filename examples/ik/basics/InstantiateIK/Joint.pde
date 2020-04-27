class Joint extends Node{
    /*
    * A Joint will be represented as a green ball
    * that is joined by a cone / triangle to its reference Node
    * Note that you could override graphics or interaction methods 
    * to generate complex behaviors
    * */
    float radius;
    boolean drawBone;
    
    Joint(Node node, Vector translation, float radius, boolean drawBone){
        super();
        this.radius = radius;
        this.drawBone = drawBone;
        this.setReference(node);
        //Exact picking precision
        this.setPickingThreshold(0);
        this.setTranslation(translation);
    }
    
    @Override
    public void graphics(PGraphics pg){
        pg.pushStyle();
        if (drawBone) {
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
        pg.popStyle();
    }
}
