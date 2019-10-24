package ik.animation.eventParser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import ik.animation.CCDEventVisualizer;
import ik.animation.eventVisualizer.Board;
import ik.animation.eventVisualizer.EventCell;
import ik.animation.eventVisualizer.Slot;
import ik.basic.Util;
import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.ik.animation.InterestingEvent;
import nub.ik.animation.VisualizerMediator;
import nub.ik.solver.Solver;
import nub.ik.solver.geometric.CCDSolver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.function.Consumer;

public class ParserExample extends PApplet {
    //final static String PATH = "/src/nub/ik/solver/geometric/";
    final static String PATH = "/src/nub/ik/solver/evolutionary/";
    //final static String FILE = "FABRIKSolver.java";
    final static String FILE = "GASolver.java";
    int rows = 10, cols = 50;
    Scene eventScene;
    Board board;

    public void settings() {
        size(1200, 800, P2D);
    }

    public void setup() {
        eventScene = new Scene(this);
        eventScene.setRadius(height/2.f);
        eventScene.fit();
        //Setting the board
        board = new Board(eventScene, rows, cols);
        float rh = eventScene.radius(), rw = rh*eventScene.aspectRatio();
        board.setDimension(-rw, -rh, 10*rw, 2*rh);
        //set eye constraint
        eventScene.eye().setConstraint(new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Node node) {
                Vector v = Vector.add(node.translation(), translation);
                if(v.x() < -5) v.setX(-5);
                if(v.y() < -5) v.setY(-5);

                return Vector.subtract(v,node.translation());
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Node node) {
                return new Quaternion(); //no rotation is allowed
            }
        });
        //Adding some cells
        addEventsToBoard(board);
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
        for(MethodEvents method : methodEventsList){
            System.out.println(method);
        }
        return methodEventsList;
    }

    public void addEventsToBoard(Board board){
        List<MethodEvents> methodEventsList = methodEvents();
        for(MethodEvents methodEvents : methodEventsList) {
            for (InterestingEvent event : methodEvents.eventQueue()) {
                new EventCell(board, event.name(), event.startingTime(), event.executionDuration(), event.renderingDuration());
            }
        }
    }

    public static void main(String[] args) {
        PApplet.main(new String[]{"ik.animation.eventParser.ParserExample"});
    }
}
