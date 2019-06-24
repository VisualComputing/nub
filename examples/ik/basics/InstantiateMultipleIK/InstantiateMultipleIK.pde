/*
 * Instantiate IK
 * by Sebastian Chaparro Cuevas.
 *
 * In this example we will compare the performance of two common IK solver working 
 * over a pretty simple chain structure on the XY-Plane: 
 * 
 *                         World
 *                           ^
 *                           | \
 *                           0 eye
 *                           ^
 *                           |
 *                           1
 *                           ^
 *                           |
 *                           2
 *                           ^
 *                           |
 *                           3
 *                           ^
 *                           |
 *                           4
 * As Node 4 is the End effector of the structure (leaf node) we will attach a Target to it.
 * Press 'S' to stop/restart IK Solver
 */
 
import nub.primitives.*;
import nub.core.*;
import nub.processing.*;
import nub.ik.solver.geometric.*;
import nub.timing.*;


Scene scene;
//Set the scene as P3D or P2D
String renderer = P3D;
float jointRadius = 5;
float length = 50;
boolean enableSolver = true;
//Skeleton structure defined above
ArrayList<Node> skeleton1 = new ArrayList<Node>();
ArrayList<Node> skeleton2 = new ArrayList<Node>();


public void settings() {
    size(700, 700, renderer);
}

public void setup() {
    //Setting the scene
    scene = new Scene(this);
    if(scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRadius(280);
    scene.fit(1);
    //1. Create the Skeletons (chains described above)
    skeleton1 = createSkeleton(new Vector(-scene.radius()/2 , -scene.radius()/2 , 0));
    skeleton2 = createSkeleton(new Vector( scene.radius()/2 , -scene.radius()/2 , 0));
    //2. Lets create a Target per chain (a bit bigger than a Joint structure)
    Node target1 = new Target(scene, jointRadius * 1.5f);
    Node target2 = new Target(scene, jointRadius * 1.5f);

    //Locate the Targets on same spot of end effectors
    target1.setPosition(skeleton1.get(skeleton1.size()-1).position());
    target2.setPosition(skeleton2.get(skeleton2.size()-1).position());

    //3. Relate the structure with a Solver. In this example we instantiate a solver per chain
    
    //A Chain solver constructor receives List containing the Skeleton structure
    final ChainSolver solver1 = new ChainSolver(skeleton1);

    //Optionally you could modify the following parameters of the Solver:
    //Maximum distance between end effector and target, If is below maxError, then we stop executing IK solver (Default value is 0.01)
    solver1.setMaxError(1);
    //Number of iterations to perform in order to reach the target (Default value is 50)
    solver1.setMaxIterations(15);
    //Times a solver will iterate on a single Frame (Default value is 5)
    solver1.setTimesPerFrame(5);
    //Minimum distance between previous and current solution to consider that Solver converges (Default value is 0.01)
    solver1.setMinDistance(0.5f);

    //A CCD solver is another quite known solver, it is only allowed for chain structures
    //A CCD solver constructor receives an List containing the Skeleton structure
    final CCDSolver solver2 = new CCDSolver(skeleton2);

    //Optionally you could modify the following parameters of the Solver:
    //Maximum distance between end effector and target, If is below maxError, then we stop executing IK solver (Default value is 0.01)
    solver2.setMaxError(1);
    //Number of iterations to perform in order to reach the target (Default value is 50)
    solver2.setMaxIterations(15);
    //Times a solver will iterate on a single Frame (Default value is 5)
    solver2.setTimesPerFrame(5);
    //Minimum distance between previous and current solution to consider that Solver converges (Default value is 0.01)
    solver2.setMinDistance(0.5f);

    //4. relate targets with end effectors
    solver1.setTarget(skeleton1.get(skeleton1.size() - 1), target1);
    solver2.setTarget(skeleton2.get(skeleton2.size() - 1), target2);

    //5. Create a Timing Task for each solver such that the solver executes each amount of time
    TimingTask solverTask1 = new TimingTask() {
        @Override
        public void execute() {
            //a solver perform an iteration when solve method is called
            if(enableSolver){
                solver1.solve();
            }
        }
    };

    TimingTask solverTask2 = new TimingTask() {
        @Override
        public void execute() {
            //a solver perform an iteration when solve method is called
            if(enableSolver){
                solver2.solve();
            }
        }
    };

    scene.registerTask(solverTask1); //Add solverTask to the Graph scene
    scene.registerTask(solverTask2); //Add solverTask to the Graph scene
    solverTask1.run(40); //Execute the solverTask each 40 ms
    solverTask2.run(40); //Execute the solverTask each 40 ms

    //Define Text Properties
    textAlign(CENTER);
    textSize(24);
}

public void draw() {
    background(0);
    if(scene.is3D()) lights();
    scene.drawAxes();
    scene.render();
    scene.beginHUD();

    for (int i = 0; i < skeleton1.size(); i++) {
        //Print Node names
        Vector screenLocation1 = scene.screenLocation(skeleton1.get(i).position());
        text("Node " + i, screenLocation1.x(), screenLocation1.y());
        Vector screenLocation2 = scene.screenLocation(skeleton2.get(i).position());
        text("Node " + i, screenLocation2.x(), screenLocation2.y());
        if(i == 0){
            text("Chain Solver", screenLocation1.x(), screenLocation1.y() - 50);
            text("CCD Solver", screenLocation2.x(), screenLocation2.y() - 50);
        }

    }
    scene.endHUD();
}

public ArrayList<Node> createSkeleton(Vector translation ){
    ArrayList<Node> skeleton = new ArrayList<Node>();
    skeleton.add(new Joint(scene,null, translation, jointRadius, false));
    skeleton.add(new Joint(scene,skeleton.get(0), new Vector(0, length), jointRadius, true));
    skeleton.add(new Joint(scene,skeleton.get(1), new Vector(0, length), jointRadius, true));
    skeleton.add(new Joint(scene,skeleton.get(2), new Vector(0, length), jointRadius, true));
    skeleton.add(new Joint(scene,skeleton.get(3), new Vector(0, length), jointRadius, true));
    //End Effector
    Node endEffector = new Joint(scene,skeleton.get(4), new Vector(0, length), jointRadius, true);
    skeleton.add(endEffector);
    //As target and effector lie on the same spot, is preferable to disable End Effector tracking
    endEffector.enableTracking(false);
    return skeleton;
}

void mouseMoved() {
    scene.cast();
}

void mouseDragged() {
    if (mouseButton == LEFT){
        scene.spin();
    } else if (mouseButton == RIGHT) {
        scene.translate();
    } else {
        scene.scale(mouseX - pmouseX);
    }
}

void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
}

void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
        if (event.getButton() == LEFT)
            scene.focus();
        else
            scene.align();
}

void keyPressed(){
    if(key == 'S' || key == 's'){
        enableSolver = !enableSolver;
    }
}
