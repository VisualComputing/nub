//This Tab contains methods to generate different kind of chain structures with constraints 

/*
  Choose between:
  NONE : Unconstrained chain.
  BALL_AND_SOCKET : Constrained chain with random Ball and Socket constraints
  HINGE : Constrained chain with random hinge constriants
  MIX : Constrained chain with constrained and unconstrained joints
*/
enum ConstraintType{ NONE, HINGE, BALL_AND_SOCKET, MIX }

/*
  Choose between:
  FIXED : Distances between joints (bones) have the same length
  RANDOM : Distances between joints (bones) have different length
*/
enum LengthType{ FIXED, RANDOM}

/*
  Choose between:
  All_SAME : All Joints are aligned.
  All_DIFFERENT : Joints are randomly aligned.
*/
enum AlignType{ FIXED, RANDOM}

/*
Convenient method that generates a chain structure.
  - Scene: Specify a scene that contains the strucure
  - numJoint: set the number of joint of the structure
  - radius: set the radius of a joint
  - translation: Define the initial position of the chain
  - chainColor: Define the color of the whole structure
  - randRotation: set to -1 to align each joint rotation with world axis. A positive number will generate random rotations.  
  - randLength: set to -1 to fix the same distance between joints rotation. A posite number will generate random lengths.  
*/
ArrayList<Node> generateChain(Scene scene, int numJoints, float radius, float boneLength, Vector translation, LengthType lengthType, AlignType alignType, color chainColor) {
    Joint prevJoint = null;
    Joint chainRoot = null;
    for (int i = 0; i < numJoints; i++) {
        Joint joint;
        joint = new Joint(scene, chainColor, radius);
        if (i == 0)
            chainRoot = joint;
        if (prevJoint != null) joint.setReference(prevJoint);

        float x = 0;
        float y = 1;
        float z = 0;

        if(alignType == AlignType.RANDOM) {
            x = 2 * random(0,1) - 1;
            z = random(0,1);
            y = 2 * random(0,1) - 1;
        }

        Vector translate = new Vector(x,y,z);
        translate.normalize();
        
        if(lengthType == LengthType.RANDOM)
            translate.multiply(boneLength * (1 - 0.4f*random(0,1)));
        else
            translate.multiply(boneLength);
        joint.setTranslation(translate);
        prevJoint = joint;
    }
    //Consider Standard Form: Parent Z Axis is Pointing at its Child
    chainRoot.setTranslation(translation);
    //chainRoot.setupHierarchy();
    chainRoot.setRoot(true);
    return (ArrayList) scene.branch(chainRoot);
}

void generateConstraints(ArrayList<Node> structure, ConstraintType type, boolean is3D){
    int numJoints = structure.size();
    for (int i = 0; i < numJoints - 1; i++) {
        Vector twist = structure.get(i + 1).translation().get();
        //Quaternion offset = new Quaternion(new Vector(0, 1, 0), radians(random(-90, 90)));
        Quaternion offset = new Quaternion();//Quaternion.random();
        Constraint constraint = null;
        ConstraintType current = type;
        if(type == ConstraintType.MIX){
            int r = int(random(0,ConstraintType.values().length));
            r = is3D ? r : r % 2;
            current = ConstraintType.values()[r];
        }
        switch (current){
            case NONE:{
                break;
            }
            case BALL_AND_SOCKET:{
                if(!is3D) break;
                float down = radians(random(10,40));
                float up = radians(random(10,40));
                float left = radians(random(10,40));
                float right = radians(random(10,40));
                constraint = new BallAndSocket(down, up, left, right);
                Quaternion rest = Quaternion.compose(structure.get(i).rotation().get(), offset);
                ((BallAndSocket) constraint).setRestRotation(rest, new Vector(0, 1, 0), twist);
                break;
            }
            case HINGE:{
                Vector vector = new Vector(random(-1,1), random(-1,1), random(-1,1));
                vector.normalize();
                vector = Vector.projectVectorOnPlane(vector, structure.get(i + 1).translation());
                if(Vector.squaredNorm(vector) == 0) {
                    constraint = null;
                }
                constraint = new Hinge(radians(random(10,160)), radians(random(10,160)),
                        structure.get(i).rotation().get(), structure.get(i + 1).translation(), vector);
            }
        }
        structure.get(i).setConstraint(constraint);
    }
}
