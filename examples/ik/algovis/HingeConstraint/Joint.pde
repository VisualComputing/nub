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

        if (constraint() instanceof Hinge) {
            float radius = boneLength * factor;
            Hinge constraint = (Hinge) constraint();
            reference.rotate(constraint.orientation());
            reference.rotate(new Quaternion(new Vector(1,0,0), new Vector(0,1,0)));
            graph().applyTransformation(pGraphics,reference);

            ((Scene) graph()).drawArc(pGraphics, radius, -constraint.minAngle() , constraint.maxAngle(), 30);

            //Draw axis
            pGraphics.pushStyle();
            pGraphics.fill(255, 154, 31);
            ((Scene) graph()).drawArrow(new Vector(), new Vector(radius/2,0,0), 1f);
            pGraphics.fill(31, 132, 255);
            ((Scene) graph()).drawArrow(new Vector(), new Vector(0,0,radius/2), 1f);
            pGraphics.popStyle();

            //Write names
            Vector v = new Vector(radius * (float) Math.cos(-constraint.minAngle()) + 5, radius * (float) Math.sin(-constraint.minAngle()));
            Vector u = new Vector(radius * (float) Math.cos(constraint.maxAngle()) + 5, radius * (float) Math.sin(constraint.maxAngle()));
            Vector w = new Vector(radius/2,0,0);
            Vector s = new Vector(0,0,radius/2);

            pGraphics.pushStyle();
            pGraphics.noLights();
            pGraphics.fill(255);
            pGraphics.textFont(font, 12);
            pGraphics.text("\u03B8 " + "min", v.x(), v.y());
            pGraphics.text("\u03B8 " + "max", u.x(), u.y());
            pGraphics.fill(255, 154, 31);
            pGraphics.text("Up vector", w.x() - 10, w.y() - 5, w.z());
            pGraphics.textAlign(RIGHT, BOTTOM);
            pGraphics.fill(31, 132, 255);
            pGraphics.text("Twist vector", s.x() - radius/4, s.y(), s.z());
            pGraphics.lights();
            pGraphics.popStyle();
        }
        pGraphics.popMatrix();
    }


    void setRoot(boolean root){
        isRoot = root;
    }
}
