//This Tab contains methods to generate different kind of chain structures with constraints 
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
