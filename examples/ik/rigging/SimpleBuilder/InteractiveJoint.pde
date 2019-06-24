public class InteractiveJoint extends Joint {
    protected Vector _desiredTranslation;
    public InteractiveJoint(Scene scene, int colour, float radius) {
        super(scene, colour, radius);
    }
    public InteractiveJoint(Scene scene, float radius) {
        super(scene, radius);
    }

    @Override
    public void interact(Object... gesture){
        String command = (String) gesture[0];
        if(command.matches("Add")){
            if(_desiredTranslation != null) {
                addChild((Scene) gesture[1], (Scene) gesture[2], (Vector) gesture[3]);
            }
            _desiredTranslation = null;
        } else if(command.matches("OnAdding")){
            _desiredTranslation = translateDesired((Scene) gesture[1],(Vector) gesture[2]);
        } else if(command.matches("Reset")){
            _desiredTranslation = null;
        } else if(command.matches("Remove")){
            removeChild();
        }
    }
    @Override
    public void graphics(PGraphics pg) {
        super.graphics(pg);
        //Draw desired position
        if(!depth)pg.hint(PConstants.DISABLE_DEPTH_TEST);
        if(_desiredTranslation != null){
            pg.pushStyle();
            pg.stroke(0,255,0);
            pg.strokeWeight(_radius/4f);
            pg.line(0,0,0, _desiredTranslation.x()/this.scaling(), _desiredTranslation.y()/this.scaling(), _desiredTranslation.z()/this.scaling());
            pg.popStyle();
        }
        if(!depth)pg.hint(PConstants.ENABLE_DEPTH_TEST);
    }

    public void addChild(Scene scene, Scene focus, Vector mouse){
        InteractiveJoint joint = new InteractiveJoint(scene, this.radius());
        joint.setPickingThreshold(this.pickingThreshold());
        joint.setReference(this);
        joint.setTranslation(joint.translateDesired(focus, mouse));
    }

    public void removeChild(){
        _graph.pruneBranch(this);
    }

    //------------------------------------
    //Interactive actions - same method found in Graph Class (duplicated cause of visibility)
    protected Vector _translateDesired(Scene scene, float dx, float dy, float dz, int zMax, Node node) {
        if (scene.is2D() && dz != 0) {
            System.out.println("Warning: graph is 2D. Z-translation reset");
            dz = 0;
        }
        dx = scene.isEye(node) ? -dx : dx;
        dy = scene.isRightHanded() ^ scene.isEye(node) ? -dy : dy;
        dz = scene.isEye(node) ? dz : -dz;
        // Scale to fit the screen relative vector displacement
        if (scene.type() == Graph.Type.PERSPECTIVE) {
            float k = (float) Math.tan(scene.fov() / 2.0f) * Math.abs(
                    scene.eye().location(scene.isEye(node) ? scene.anchor() : node.position())._vector[2] * scene.eye().magnitude());
            //TODO check me weird to find height instead of width working (may it has to do with fov?)
            dx *= 2.0 * k / (scene.height() * scene.eye().magnitude());
            dy *= 2.0 * k / (scene.height() *scene. eye().magnitude());
        }
        // this expresses the dz coordinate in world units:
        //Vector eyeVector = new Vector(dx, dy, dz / eye().magnitude());
        Vector eyeVector = new Vector(dx, dy, dz * 2 * scene.radius() / zMax);
        return node.reference() == null ? scene.eye().worldDisplacement(eyeVector) : node.reference().displacement(eyeVector, scene.eye());
    }


    public Vector translateDesired(Scene scene, Vector point){
        Vector delta = Vector.subtract(point, scene.screenLocation(position()));
        return _translateDesired(scene, delta.x(), delta.y(), 0, Math.min(scene.width(), scene.height()), this);
    }
}
