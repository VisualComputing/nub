package ik.animation.eventParser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import ik.animation.eventVisualizer.Board;
import ik.animation.eventVisualizer.EventCell;
import ik.animation.eventVisualizer.Slot;
import nub.ik.animation.InterestingEvent;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class ParserExample extends PApplet {
    //final static String PATH = "/src/nub/ik/solver/geometric/";
    final static String PATH = "/src/nub/ik/solver/evolutionary/";
    //final static String FILE = "FABRIKSolver.java";
    final static String FILE = "GASolver.java";
    int rows = 3, cols = 50;
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
            compilationUnit = StaticJavaParser.parse(new File(System.getProperty("user.dir") + PATH + FILE));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        List<MethodEvents> methodEventsList = new ArrayList<MethodEvents>();
        VoidVisitor<List<MethodEvents>> methodNameVisitor = new EventCollector();
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
            if(event.getAttribute("ON_IF") != null){
                cell.setColorCell(((Scene) cell.graph()).pApplet().color(78,204,163));
                cell.setLabel("ON IF: " + event.getAttribute("ON_IF"));
            }
        }
    }

    public static void main(String[] args) {
        PApplet.main(new String[]{"ik.animation.eventParser.ParserExample"});
    }
}
