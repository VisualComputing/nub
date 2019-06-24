class Joint extends Node {
    int colour;
    float radius;
    boolean axes = true;
    float constraintFactor = 0.9;
    //set to true only when the joint is the root (for rendering purposes)
    boolean isRoot = false;

    Joint(Scene scene, int colour, float radius){
        super(scene);
        this.colour = colour;
        this.radius = radius;
        setPickingThreshold(-radius*2);
    }

    @Override
    void graphics(PGraphics pg){
        Scene scene = (Scene) this._graph;
        pg.hint(PConstants.DISABLE_DEPTH_TEST);
        pg.pushStyle();
        if (!isRoot) {
            pg.strokeWeight(Math.max(radius/4f, 2));
            pg.stroke(colour);
            Vector v = location(new Vector(), reference());
            float m = v.magnitude();
            if (scene.is2D()) {
                pg.line(radius * v.x() / m, radius * v.y() / m, (m - radius) * v.x() / m, (m - radius) * v.y() / m);
            } else {
                pg.line(radius * v.x() / m, radius * v.y() / m, radius * v.z() / m, (m - radius) * v.x() / m, (m - radius) * v.y() / m, (m - radius) * v.z() / m);
            }
        }
        pg.fill(colour);
        pg.noStroke();
        if (scene.is2D()) pg.ellipse(0, 0, radius*2, radius*2);
        else pg.sphere(radius);
        pg.strokeWeight(radius/4f);
        if (constraint() != null) {
            drawConstraint(pg,constraintFactor);
        }

        pg.hint(PConstants.ENABLE_DEPTH_TEST);
        pg.stroke(255);
        scene.drawBullsEye(this);

        pg.popStyle();

    }
    
    void drawConstraint(PGraphics pGraphics, float factor) {
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
        Node reference = new Node();
        reference.setTranslation(new Vector());
        reference.setRotation(rotation().inverse());

        if (constraint() instanceof BallAndSocket) {
            BallAndSocket constraint = (BallAndSocket) constraint();
            reference.rotate(((BallAndSocket) constraint()).orientation());
            graph().applyTransformation(pGraphics, reference);
            float width = boneLength * factor;
            float max = Math.max(Math.max(Math.max(constraint.up(), constraint.down()), constraint.left()), constraint.right());
            //Max value will define max radius length
            float height = (float) (width / Math.tan(max));
            if (height > boneLength * factor) height = width;
            //get all radius
            float up_r = (float) Math.abs((height * Math.tan(constraint.up())));
            float down_r = (float) Math.abs((height * Math.tan(constraint.down())));
            float left_r = (float) Math.abs((height * Math.tan(constraint.left())));
            float right_r = (float) Math.abs((height * Math.tan(constraint.right())));
            Scene.drawCone(pGraphics, 20, 0, 0, height, left_r, up_r, right_r, down_r);
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
    
    void setRoot(boolean root){
        isRoot = root;
    }
}
