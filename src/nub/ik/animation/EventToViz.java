package nub.ik.animation;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class includes common implementations to map an Interesting Event into a Visual Step
 * Use this methods when building your custom Algorithm Visualizer.
 *
 * You are welcome to add your own ones
 *
 */

public class EventToViz {
    //TODO: Rename
    // TODO: look Factory Pattern
    /*
    * Common events have a default mapping that ideally works in most of the cases.
    * If so, then the mapping between events and visual steps is handled automatically
    * */
    public static VisualStep generateDefaultViz(Visualizer visualizer, InterestingEvent event){
        switch(event.type()){
            case "Trajectory":{
                return  generateTrajectory(visualizer, event);
            }
            case "Message":{
                return generateMessage(visualizer, event);
            }
            case "NodeRotation":{
                return generateRotateNode(visualizer, event);
            }
            case "UpdateStructure":{
                return generateUpdateStructure(visualizer, event);
            }
        }
        return null;
    }


    public static VisualSteps.RotateNode generateRotateNode(Visualizer visualizer, InterestingEvent event) {
        return generateRotateNode(visualizer, visualizer._radius, event);
    }
    public static VisualSteps.RotateNode generateRotateNode(Visualizer visualizer, float radius, InterestingEvent event){
        long executionDuration = event.executionDuration() * visualizer._stepStamp;
        long renderingDuration = event.renderingDuration() * visualizer._stepStamp;
        System.out.println(visualizer._nodeToJoint.size());
        System.out.println(visualizer._nodeToJoint.get(event.getAttribute("node")));
        VisualSteps.RotateNode  visualStep = new VisualSteps.RotateNode(visualizer._scene, visualizer._nodeToJoint.get(event.getAttribute("node")), radius, visualizer._period, executionDuration, renderingDuration);
        visualStep.setRotation((Quaternion) event.getAttribute("rotation"));
        return visualStep;
    }

    public static VisualSteps.MessageStep generateMessage(Visualizer visualizer, InterestingEvent event){
        return generateMessage(visualizer, 24, event);
    }

    public static VisualSteps.MessageStep generateMessage(Visualizer visualizer, float size, InterestingEvent event){
        long executionDuration = event.executionDuration() * visualizer._stepStamp;
        long renderingDuration = event.renderingDuration() * visualizer._stepStamp;
        VisualSteps.MessageStep visualStep = new VisualSteps.MessageStep(visualizer._scene, (String) event.getAttribute("message"), visualizer._period, executionDuration, renderingDuration);
        visualStep.setSize(size);
        return visualStep;
    }

    public static VisualSteps.FollowTrajectoryStep generateTrajectory(Visualizer visualizer, InterestingEvent event) {
        return generateTrajectory(visualizer, visualizer._radius, event);
    }
    public static VisualSteps.FollowTrajectoryStep generateTrajectory(Visualizer visualizer, float radius, InterestingEvent event){
        long executionDuration = event.executionDuration() * visualizer._stepStamp;
        long renderingDuration = event.renderingDuration() * visualizer._stepStamp;
        VisualSteps.FollowTrajectoryStep visualStep = new VisualSteps.FollowTrajectoryStep(visualizer._scene, radius, visualizer._period, executionDuration, renderingDuration);
        Vector[] positions = Arrays.asList((Object[]) event.getAttribute("positions")).toArray(new Vector[((Object[])event.getAttribute("positions")).length]);
        visualStep.setTrajectory(visualizer._nodeToJoint.get(event.getAttribute("reference")), positions);
        return visualStep;
    }

    public static VisualSteps.UpdateStructure generateUpdateStructure(Visualizer visualizer, InterestingEvent event) {
        long executionDuration = event.executionDuration() * visualizer._stepStamp;
        long renderingDuration = event.renderingDuration() * visualizer._stepStamp;
        List<Node> structure = new ArrayList<Node>();
        for(Node node : (List<? extends Node>) event.getAttribute("structure")){
            structure.add(visualizer._nodeToJoint.get(node));
        }
        VisualSteps.UpdateStructure  visualStep = new VisualSteps.UpdateStructure(visualizer._scene, structure, visualizer._period, executionDuration, renderingDuration);
        visualStep.setFinalRotations((Quaternion[]) event.getAttribute("rotations"));
        visualStep.setFinalTranslations((Vector[]) event.getAttribute("translations"));
        return visualStep;
    }

}
