package nub.ik.animation;

import nub.core.Node;
import nub.ik.animation.visualsteps.*;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
    public static VisualStep generateDefaultViz(Visualizer visualizer, InterestingEvent event, HashMap<String, Object> attributes){
        switch(event.type()){
            case "Trajectory":{
                return  generateTrajectory(visualizer, event, attributes);
            }
            case "Message":{
                return generateMessage(visualizer, event, attributes);
            }
            case "NodeRotation":{
                return generateRotateNode(visualizer, event, attributes);
            }
            case "NodeTranslation":{
                return generateTranslateNode(visualizer, event, attributes);
            }
            case "UpdateStructure":{
                return generateUpdateStructure(visualizer, event);
            }
            case "HighlightStructure":{
                return generateHighLightStructure(visualizer, event, attributes, false);
            }
            case "HideStructure":{
                return generateHighLightStructure(visualizer, event, attributes, true);
            }
        }
        return null;
    }


    public static RotateNode generateRotateNode(Visualizer visualizer, InterestingEvent event, HashMap<String, Object> attributes) {
        long executionDuration = event.executionDuration() * visualizer._stepStamp;
        long renderingDuration = event.renderingDuration() * visualizer._stepStamp;
        RotateNode  visualStep = new RotateNode(visualizer._scene, visualizer._nodeToJoint.get(event.getAttribute("node")), visualizer._period, executionDuration, renderingDuration);
        visualStep.setRotation((Quaternion) event.getAttribute("rotation"));
        if(event.getAttribute("enableConstraint") != null){
            visualStep.enableConstraint((boolean) event.getAttribute("enableConstraint"));
        }
        if(event.getAttribute("modifyChildren") != null){
            visualStep.modifyChildren((boolean) event.getAttribute("modifyChildren"));
        }
        if(attributes == null || !attributes.containsKey("radius")) visualStep.setAttribute("radius", visualizer._radius);
        visualStep.setAttributes(attributes);
        return visualStep;
    }

    public static TranslateNode generateTranslateNode(Visualizer visualizer, InterestingEvent event, HashMap<String, Object> attributes) {
        long executionDuration = event.executionDuration() * visualizer._stepStamp;
        long renderingDuration = event.renderingDuration() * visualizer._stepStamp;
        TranslateNode  visualStep = new TranslateNode(visualizer._scene, visualizer._nodeToJoint.get(event.getAttribute("node")), visualizer._period, executionDuration, renderingDuration);
        visualStep.setTranslation((Vector) event.getAttribute("translation"));
        if(event.getAttribute("enableConstraint") != null){
            visualStep.enableConstraint((boolean) event.getAttribute("enableConstraint"));
        }
        if(event.getAttribute("modifyChildren") != null){
            visualStep.modifyChildren((boolean) event.getAttribute("modifyChildren"));
        }
        if(event.getAttribute("useGlobalCoordinates") != null){
            visualStep.useGlobalCoordinates((boolean) event.getAttribute("useGlobalCoordinates"));
        }
        if(attributes == null || !attributes.containsKey("radius")) visualStep.setAttribute("radius", visualizer._radius);
        visualStep.setAttributes(attributes);
        return visualStep;
    }

    public static MessageStep generateMessage(Visualizer visualizer, InterestingEvent event, HashMap<String, Object> attributes) {
        long executionDuration = event.executionDuration() * visualizer._stepStamp;
        long renderingDuration = event.renderingDuration() * visualizer._stepStamp;
        MessageStep visualStep = new MessageStep(visualizer._scene, (String) event.getAttribute("message"), visualizer._period, executionDuration, renderingDuration);
        visualStep.setAttributes(attributes);
        return visualStep;
    }

    public static FollowTrajectoryStep generateTrajectory(Visualizer visualizer, InterestingEvent event, HashMap<String, Object> attributes) {
        long executionDuration = event.executionDuration() * visualizer._stepStamp;
        long renderingDuration = event.renderingDuration() * visualizer._stepStamp;
        FollowTrajectoryStep visualStep = new FollowTrajectoryStep(visualizer._scene, visualizer._period, executionDuration, renderingDuration);
        Vector[] positions = Arrays.asList((Object[]) event.getAttribute("positions")).toArray(new Vector[((Object[])event.getAttribute("positions")).length]);
        visualStep.setTrajectory(visualizer._nodeToJoint.get(event.getAttribute("reference")), positions);
        if(attributes == null || !attributes.containsKey("radius")) visualStep.setAttribute("radius", visualizer._radius);
        visualStep.setAttributes(attributes);
        return visualStep;
    }

    public static UpdateStructure generateUpdateStructure(Visualizer visualizer, InterestingEvent event) {
        long executionDuration = event.executionDuration() * visualizer._stepStamp;
        long renderingDuration = event.renderingDuration() * visualizer._stepStamp;
        List<Node> structure = new ArrayList<Node>();
        for(Node node : (List<? extends Node>) event.getAttribute("structure")){
            structure.add(visualizer._nodeToJoint.get(node));
        }
        UpdateStructure  visualStep = new UpdateStructure(visualizer._scene, structure, visualizer._period, executionDuration, renderingDuration);
        visualStep.setFinalRotations((Quaternion[]) event.getAttribute("rotations"));
        visualStep.setFinalTranslations((Vector[]) event.getAttribute("translations"));
        return visualStep;
    }

    public static HighLightStructure generateHighLightStructure(Visualizer visualizer, InterestingEvent event, HashMap<String, Object> attributes, boolean hide) {
        long executionDuration = event.executionDuration() * visualizer._stepStamp;
        long renderingDuration = event.renderingDuration() * visualizer._stepStamp;
        List<Node> structure = new ArrayList<Node>();
        for(Node node : (List<? extends Node>) event.getAttribute("structure")){
            structure.add(visualizer._nodeToJoint.get(node));
        }
        HighLightStructure  visualStep = new HighLightStructure(visualizer._scene, structure, visualizer._period, executionDuration, renderingDuration);
        if(!hide) visualStep.setHighlight(0,255);
        else visualStep.setHighlight(255,0);
        visualStep.setAttributes(attributes);
        return visualStep;
    }
}
