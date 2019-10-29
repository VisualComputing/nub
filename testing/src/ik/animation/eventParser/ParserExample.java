package ik.animation.eventParser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithBody;
import com.github.javaparser.ast.visitor.VoidVisitor;
import ik.animation.eventVisualizer.Board;
import ik.animation.eventVisualizer.EventCell;
import ik.animation.eventVisualizer.Slot;
import nub.ik.animation.InterestingEvent;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.event.MouseEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class ParserExample extends PApplet {
    //final static String PATH = "/src/nub/ik/solver/geometric/";
    final static String PATH = "/src/nub/ik/solver/evolutionary/";
    //final static String FILE = "CCDSolver";
    //final static String FILE = "FABRIKSolver";
    final static String FILE = "GASolver";
    int rows = 3, cols = 50;
    static boolean visualize = false;
    static HashMap<EventCell, InterestingEvent> _events = new HashMap<EventCell, InterestingEvent>();

    Scene eventScene;
    Board board;

    public void settings() {
        size(1200, 800, P2D);
    }

    public void setup() {
        eventScene = new Scene(this);
        eventScene.setRadius(height/2.f);
        eventScene.fit();

        //Setting the boards (one per method - as they are independent flows)
        List<MethodEvents> methodEventsList = methodEvents();
        float rh = eventScene.radius(), rw = rh*eventScene.aspectRatio();
        Vector corner = new Vector(-rw,-rh + 50);

        for(MethodEvents methodEvents : methodEventsList){
            board = new Board(eventScene, rows, cols);
            board.setDimension(corner.x(), corner.y(), 10*rw, rh);
            board.setLabel("ON METHOD: " + methodEvents._name);
            addEventsToBoard(board, methodEvents);
            corner.set(corner.x(), corner.y() + board.height() + 50);
        }
    }

    public void draw() {
        background(0);
        eventScene.render();
    }

    public void keyPressed() {
        if(key == 'r'){
            board.addRows(1, true);
        } else if (key == 'c') {
            board.addCols(1, true);
        }

    }

    public void mouseMoved(){
        eventScene.cast();
    }

    public void mouseDragged(){
        if(eventScene.trackedNode() instanceof Slot){
            eventScene.trackedNode().interact("OnMovement", new Vector(eventScene.mouse().x(), eventScene.mouse().y()));
        } else {
            eventScene.translate();
        }
    }

    public void mouseReleased(){
        if(eventScene.trackedNode() instanceof EventCell){
            //we must align the event to the closest row / col
            ((EventCell) eventScene.trackedNode()).applyMovement();
        } else if(eventScene.trackedNode() instanceof Slot){
            eventScene.trackedNode().interact("OnFinishedMovement", new Vector(eventScene.mouse().x(), eventScene.mouse().y()));
        }
    }

    public void mouseWheel(MouseEvent event) {
        if(eventScene.trackedNode() == null) eventScene.scale(event.getCount() * 50);
    }

    public static List<MethodEvents> methodEvents(){
        CompilationUnit compilationUnit = null;
        try {
            compilationUnit = StaticJavaParser.parse(new File(System.getProperty("user.dir") + PATH + FILE + ".java"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        List<MethodEvents> methodEventsList = new ArrayList<MethodEvents>();
        VoidVisitor<List<MethodEvents>> methodNameVisitor = new EventCollector(visualize);
        methodNameVisitor.visit(compilationUnit, methodEventsList);
        //sort MethodEvents based on their number of events
        methodEventsList.sort((o1, o2) -> {
            int s1 = o1.eventQueue().size();
            int s2 = o2.eventQueue().size();
            return s1 - s2;
        });

        for(MethodEvents m : methodEventsList){
            System.out.println(m);
        }
        return methodEventsList;
    }

    public void addEventsToBoard(Board board, MethodEvents methodEvents){
        for (InterestingEvent event : methodEvents.eventQueue()) {
            EventCell cell = new EventCell(board, event.name(), event.startingTime(), event.executionDuration(), event.renderingDuration());
            if(event.name().equals("AFTER_CONDITION") || event.name().equals("ON_REPEAT") || event.name().equals("LAST_EVENT")) {
                cell.setColorCell(((Scene) cell.graph()).pApplet().color(84,84,197));
            }else if(event.getAttribute("ON_IF") != null){
                cell.setColorCell(((Scene) cell.graph()).pApplet().color(78,204,163));
                cell.setLabel("ON IF: " + event.getAttribute("ON_IF"));
            } else if(event.getAttribute("ON_ELSE") != null){
                cell.setColorCell(((Scene) cell.graph()).pApplet().color(175,4,4));
                cell.setLabel("ON ELSE: " + event.getAttribute("ON_ELSE"));
            }
            if(event.getAttribute("CONTEXT") != null) {
                cell.setLabel("ON LINE: " + event.getAttribute("CONTEXT"));
                cell.label().setColorLabel(color(184,144,248));
            }
            _events.put(cell, event);
        }
    }


    public void saveEvent(JSONArray events, InterestingEvent interestingEvent){
        JSONObject event = new JSONObject();
        event.setString("name", interestingEvent.name());
        event.setString("type", interestingEvent.type());
        event.setString("start_after", (String) interestingEvent.getAttribute("start_after"));
        event.setInt("start_delay", (int) interestingEvent.getAttribute("start_delay"));
        event.setInt("execute_after", (int) interestingEvent.getAttribute("execute_after"));
        event.setInt("render_after", (int) interestingEvent.getAttribute("render_after"));
        event.setInt("execute_delay", (int) interestingEvent.getAttribute("execute_delay"));
        event.setInt("render_delay", (int) interestingEvent.getAttribute("render_delay"));
        events.setJSONObject(events.size(), event);
    }

    public void saveEvents(){
        JSONArray events;
        events = new JSONArray();
        //.. do logic
        saveJSONArray(events, System.getProperty("user.dir") + PATH + FILE);
    }

    public void packageInfo(EventCell cell){
        InterestingEvent interestingEvent = _events.get(cell);
        if(interestingEvent.type().equals("DUMMY")) return;
        //Get previous cell
        EventCell prevEventCell = cell.board().previousCell(cell);
        InterestingEvent prevInterestingEvent = _events.get(prevEventCell);
        //Package info according to prev
        if(prevEventCell.name().equals("ON_REPEAT")){
            EventCell last = lastEventOnLoop(cell.board(), (NodeWithBody) prevInterestingEvent.getAttribute("ON_REPEAT"));
            //.. do some additional things
        }
        //if no edge case
        JSONObject event = new JSONObject();
        event.setString("name", interestingEvent.name());
        event.setString("type", interestingEvent.type());
        event.setString("start_after", prevEventCell.name());
        event.setInt("start_delay", cell.col() - prevEventCell.col());
        event.setInt("execute_after", (int) interestingEvent.getAttribute("execute_after"));
        event.setInt("render_after", (int) interestingEvent.getAttribute("render_after"));
        event.setInt("execute_delay", (int) interestingEvent.getAttribute("execute_delay"));
        event.setInt("render_delay", (int) interestingEvent.getAttribute("render_delay"));
        //events.setJSONObject(events.size(), event);




    }


    public void findStartTime(EventCell current){
        Board board = current.board();
        InterestingEvent currentEvent = _events.get(current);
        Node currentContext = (Node) currentEvent.getAttribute("CONTEXT");
        EventCell prev = null;
        int prevDist = 99999;
        for(int row = current.row() - 1; row >= 0; row--){
            EventCell cell =  board.cellAt(row);
            InterestingEvent event = _events.get(cell);
            if(row == current.row() - 1){
                if(cell.name().equals("ON_REPEAT")){
                    currentEvent.addAttribute("start_after", "last");
                    currentEvent.addAttribute("start_delay", current.col() - prev.col());
                }

            }
            //Must have happened
            if(((Node)event.getAttribute("CONTEXT")).isAncestorOf(currentContext)){
                //Must be previous to this event
                //int dist = cell.row() + cell.executionDuration() - current.row();
                int dist = current.row() - cell.row();
                if(dist >= 0){
                    //Get nearest
                    if(dist < prevDist){
                        prevDist = dist;
                        prev = cell;
                    }
                }
            }
        }
        currentEvent.addAttribute("start_after", prev.name());
        currentEvent.addAttribute("start_delay", current.col() - prev.col());

    }

    public EventCell lastEventOnLoop(Board board, NodeWithBody loop){
        int row = 0;
        EventCell last = null;
        while(row < board.rows()){
            EventCell current = board.cellAt(row);
            if(current != null && _events.get(current) != null){
                if(_events.get(current).getAttribute("LOOP") != null && _events.get(current).getAttribute("LOOP") == loop){
                    last = current;
                }
            }
            row++;
        }
        return last;
    }

    public static void main(String[] args) {
        PApplet.main(new String[]{"ik.animation.eventParser.ParserExample"});
    }
}
