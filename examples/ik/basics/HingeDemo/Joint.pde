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
        this(scene, color(HingeDemo.this.random(0,255), HingeDemo.this.random(0,255), HingeDemo.this.random(0,255)));
    }

    public Joint(Scene scene, float radius){
        this(scene, color(HingeDemo.this.random(0,255), HingeDemo.this.random(0,255), HingeDemo.this.random(0,255)), radius);
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
            if (_scene.is2D()) {
                pg.line(_radius * v.x() / m, _radius * v.y() / m, (m - _radius) * v.x() / m, (m - _radius) * v.y() / m);
            } else {
                pg.line(_radius * v.x() / m, _radius * v.y() / m, _radius * v.z() / m, (m - _radius) * v.x() / m, (m - _radius) * v.y() / m, (m - _radius) * v.z() / m);
            }
        }

        pg.fill(_color);
        pg.noStroke();
        if (_scene.is2D()) pg.ellipse(0, 0, _radius*2, _radius*2);
        else pg.sphere(_radius);

        pg.strokeWeight(_radius/4f);
        if (constraint() != null) {
            drawConstraint(pg,_constraintFactor);
        }
        if(!depth) pg.hint(PConstants.ENABLE_DEPTH_TEST);
        pg.popStyle();
    }

    public void drawConstraint(PGraphics pGraphics, float factor) {
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




        if (constraint() instanceof Hinge) {
            float radius = boneLength * factor;
            Hinge constraint = (Hinge) constraint();
            reference.rotate(constraint.orientation());
            reference.rotate(new Quaternion(new Vector(1,0,0), new Vector(0,1,0)));
            _scene.applyTransformation(pGraphics,reference);

            _scene.drawArc(pGraphics, radius, -constraint.minAngle() , constraint.maxAngle(), 30);

            //Draw axis
            pGraphics.pushStyle();
            pGraphics.fill(255, 154, 31);
            _scene.drawArrow(new Vector(), new Vector(radius/2,0,0), 1f);
            pGraphics.fill(31, 132, 255);
            _scene.drawArrow(new Vector(), new Vector(0,0,radius/2), 1f);
            pGraphics.popStyle();


            //Write names

            Vector v = new Vector(radius * (float) Math.cos(-constraint.minAngle()) + 5, radius * (float) Math.sin(-constraint.minAngle()));
            //v = this.worldLocation(reference.worldLocation(v));
            //v = graph().screenLocation(v);

            Vector u = new Vector(radius * (float) Math.cos(constraint.maxAngle()) + 5, radius * (float) Math.sin(constraint.maxAngle()));
            //u = this.worldLocation(reference.worldLocation(u));
            //u = graph().screenLocation(u);

            Vector w = new Vector(radius/2,0,0);
            //w = this.worldLocation(reference.worldLocation(w));
            //w = graph().screenLocation(w);

            Vector s = new Vector(0,0,radius/2);
            //s = this.worldLocation(reference.worldLocation(s));
            //s = graph().screenLocation(s);

            //((Scene) graph()).beginHUD(pGraphics);
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
            //((Scene) graph()).endHUD(pGraphics);
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
