class Joint extends Node {
    boolean depth = false;
    int _color;
    float _radius;
    boolean _axes = true;
    float _constraintFactor = 0.9f;
    //set to true only when the joint is the root (for rendering purposes)
    boolean _isRoot = false;
    Scene _scene;

    public Joint(Scene scene, int col, float radius){
        super();
        _scene = scene;
        _color = col;
        _radius = radius;
        setPickingThreshold(-_radius*2);
    }

    public Joint(Scene scene, int col){
        this(scene, col, 5);
    }

    public Joint(Scene scene){
        this(scene, color(BallAndSocketDemo.this.random(0,255), BallAndSocketDemo.this.random(0,255), BallAndSocketDemo.this.random(0,255)));
    }

    public Joint(Scene scene, float radius){
        this(scene, color(BallAndSocketDemo.this.random(0,255), BallAndSocketDemo.this.random(0,255), BallAndSocketDemo.this.random(0,255)), radius);
    }

    @Override
    public void graphics(PGraphics pg){
        if(!depth)pg.hint(PConstants.DISABLE_DEPTH_TEST);
        pg.pushStyle();
        if (!_isRoot) {
            pg.strokeWeight(Math.max(_radius/4f, 2));
            pg.stroke(_color);
            Vector v = location(new Vector(), reference());
            float m = v.magnitude();
            if (pg.is2D()) {
                pg.line(_radius * v.x() / m, _radius * v.y() / m, (m - _radius) * v.x() / m, (m - _radius) * v.y() / m);
            } else {
                pg.line(_radius * v.x() / m, _radius * v.y() / m, _radius * v.z() / m, (m - _radius) * v.x() / m, (m - _radius) * v.y() / m, (m - _radius) * v.z() / m);
            }
        }
        pg.fill(_color);
        pg.noStroke();
        if (pg.is2D()) pg.ellipse(0, 0, _radius*2, _radius*2);
        else pg.sphere(_radius);
        pg.strokeWeight(_radius/4f);
        if (constraint() != null) {
            drawConstraint(_scene, pg, _constraintFactor);
        }
        if(_axes) Scene.drawAxes(pg,_radius*2);
        if(!depth) pg.hint(PConstants.ENABLE_DEPTH_TEST);
        pg.popStyle();

    }

    public void drawConstraint(Scene scene, PGraphics pGraphics, float factor) {

        if (this.constraint() == null) return;
        float boneLength = 0;
        if (!children().isEmpty()) {
            for (Node child : children())
                boneLength += child.translation().magnitude();
            boneLength = boneLength / (1.f * children().size());
        } else
            boneLength = translation().magnitude();
        if (boneLength == 0) return;

        pGraphics.pushMatrix();
        pGraphics.pushStyle();
        pGraphics.noStroke();

        pGraphics.fill(62, 203, 55, 150);
        Node reference = Node.detach(new Vector(), new Quaternion(), 1f);
        reference.setTranslation(new Vector());
        reference.setRotation(rotation().inverse());

        if (constraint() instanceof BallAndSocket) {
            BallAndSocket constraint = (BallAndSocket) constraint();
            reference.rotate(((BallAndSocket) constraint()).orientation());
            scene.applyTransformation(pGraphics, reference);
            float width = boneLength * factor;
            float max = Math.max(Math.max(Math.max(constraint.up(), constraint.down()), constraint.left()), constraint.right());
            //Max value will define max radius length
            float height = (float) (width / Math.tan(max));
            if (height > boneLength * factor) height = width;
            //drawAxes(pGraphics,height*1.2f);
            //get all radius
            float up_r = (float) Math.abs((height * Math.tan(constraint.up())));
            float down_r = (float) Math.abs((height * Math.tan(constraint.down())));
            float left_r = (float) Math.abs((height * Math.tan(constraint.left())));
            float right_r = (float) Math.abs((height * Math.tan(constraint.right())));
            scene.drawCone(pGraphics, 20, 0, 0, height, left_r, up_r, right_r, down_r);
            //Draw Up - Down Triangle
            pGraphics.pushStyle();
            //Draw offset
            Quaternion q = Quaternion.compose(constraint.restRotation().inverse(), constraint.offset());
            q.compose(constraint.restRotation());
            Vector off = q.rotate(new Vector(0,0,boneLength));
            pGraphics.stroke(255,255,0);
            pGraphics.line(0,0,0,off.x(), off.y(), off.z());
            pGraphics.noStroke();
            pGraphics.fill(255, 154, 31, 100);
            pGraphics.beginShape(PConstants.TRIANGLES);
                pGraphics.vertex(0,0,0);
                pGraphics.vertex(-left_r,0,height);
                pGraphics.vertex(right_r,0,height);
            pGraphics.endShape(CLOSE);
            pGraphics.popStyle();

            //Draw Left - Right Triangle
            pGraphics.pushStyle();
            pGraphics.fill(31, 132, 255, 100);
            pGraphics.beginShape(PConstants.TRIANGLES);
            pGraphics.vertex(0,0,0);
            pGraphics.vertex(0,-down_r,height);
            pGraphics.vertex(0,up_r,height);
            pGraphics.endShape(CLOSE);
            pGraphics.popStyle();

            //Write names
            pGraphics.pushStyle();
            pGraphics.noLights();
            pGraphics.fill(255);
            pGraphics.textFont(font, 12);
            pGraphics.textAlign(RIGHT);
            pGraphics.text("Left", -left_r, 0, height);
            pGraphics.textAlign(LEFT);
            pGraphics.text("Right", right_r, 0, height);
            pGraphics.textAlign(CENTER, BOTTOM);
            pGraphics.text("Down", 0,-down_r, height);
            pGraphics.textAlign(CENTER, TOP);
            pGraphics.text("Up", 0, up_r, height);
            pGraphics.textAlign(CENTER, CENTER);
            pGraphics.text("Offset", off.x(), off.y(), off.z());
            pGraphics.lights();
            pGraphics.popStyle();
        }
        pGraphics.popMatrix();
    }


    public void setRadius(float radius){
        _radius = radius;
        setPickingThreshold(-_radius*2);
    }
    public void setRoot(boolean isRoot){
        _isRoot = isRoot;
    }
    public float radius(){
        return _radius;
    }
}
