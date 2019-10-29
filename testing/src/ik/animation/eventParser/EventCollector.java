package ik.animation.eventParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithBody;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import nub.ik.animation.InterestingEvent;

import java.util.*;
import java.util.function.Consumer;

public class EventCollector extends VoidVisitorAdapter<List<MethodEvents>> {

    protected boolean _visualize = false;

    public EventCollector(boolean visualize){
        _visualize = visualize;
    }


    @Override
    public void visit(MethodDeclaration md, List<MethodEvents> methodEventsList) {
        super.visit(md, methodEventsList);
        MethodEvents methodEvents = new MethodEvents(md.getName().toString());
        Set<Node> edgeConditions = new HashSet<Node>();
        Set<NodeWithBody> edgeLoops = new HashSet<NodeWithBody>();
        Consumer<Expression> consumer = expr -> {
            if (expr.isNameExpr() && expr.asNameExpr().getName().toString().equals("_mediator") || (expr.isMethodCallExpr() && expr.asMethodCallExpr().getName().toString().equals("mediator"))) {
                //get the addEvent Method
                Optional<MethodCallExpr> inner = expr.findAncestor(MethodCallExpr.class, methodCallExpr -> methodCallExpr != expr);
                if(inner.isPresent()) {
                    MethodCallExpr methodCall = inner.get();
                    //Create the InterestingEvent according to the method signature
                    InterestingEvent interestingEvent = null;
                    switch(methodCall.getName().toString()){
                        case "addEventStartingAfterLast":{
                            interestingEvent = addBasedOnLast(methodCall, methodEvents, true, true);
                            break;
                        }
                        case "addEventStartingWithLast": {
                            interestingEvent = addBasedOnLast(methodCall, methodEvents, true, false);
                            break;
                        }
                        case "addEventStartingAfter":{
                            interestingEvent = addBasedOnLast(methodCall, methodEvents, false, true);
                            break;
                        }
                        case "addEventStartingWith": {
                            interestingEvent = addBasedOnLast(methodCall, methodEvents, false, false);
                            break;
                        }
                    }
                    if(interestingEvent == null) return;

                    //consider edge cases: AFTER_CONDITION, ON_REPEAT
                    //IF THE EVENT WAS CREATED IMMEDIATELY AFTER A CONDITION
                    Optional<IfStmt> condition = methodCall.findAncestor(IfStmt.class, stmt -> !stmt.getCondition().toString().contains("enableMediator")); //If is surrounded by non dummy IF CLAUSE
                    if(condition.isPresent()){
                        edgeConditions.add(condition.get());
                        //EVENT IS CONTAINED IN AL IF/ELSE STRUCTURE
                        if(condition.get().hasThenBlock() && condition.get().getThenStmt().isAncestorOf(methodCall)){
                            interestingEvent.addAttribute("ON_IF", condition.get());
                        } else if(condition.get().hasElseBlock() && condition.get().getElseStmt().get().isAncestorOf(methodCall)){
                            interestingEvent.addAttribute("ON_ELSE", condition.get());
                        }
                    }

                    //Add dummy last event at startingof method
                    if(methodEvents.eventQueue().size() == 1) {
                        addDummyLastEventBefore("LAST_EVENT", methodEvents, interestingEvent);
                    }

                    //Add dummy after condition
                    boolean addDummyCondition = false;
                    Iterator<Node> iterator = edgeConditions.iterator();
                    while (iterator.hasNext()) {
                        Node next = iterator.next();
                        //Has been traversed completely
                        if(!next.isAncestorOf(methodCall)){
                            iterator.remove();
                            addDummyCondition = true;
                        }
                    }
                    if(addDummyCondition){
                        //A dummy last event is required
                        addDummyLastEventBefore("AFTER_CONDITION", methodEvents, interestingEvent);
                    }

                    //IF EVENT IS INSIDE A LOOP
                    Optional<NodeWithBody> loop = methodCall.findAncestor(NodeWithBody.class); //If is surrounded by loop
                    boolean addDummyLoop = false;

                    if(loop.isPresent()){
                        if(!edgeLoops.contains(loop.get())){
                            addDummyLoop = true;
                            methodEvents.eventQueue().get(methodEvents.eventQueue().size() - 1).addAttribute("BEFORE_LOOP", interestingEvent.name());
                        }
                        edgeLoops.add(loop.get());
                        interestingEvent.addAttribute("LOOP", loop.get());
                    }

                    if(addDummyLoop){
                        //A dummy last event is required
                        InterestingEvent dummy = addDummyLastEventBefore("ON_REPEAT",methodEvents, interestingEvent);
                        dummy.addAttribute("ON_REPEAT", loop.get());
                        dummy.addAttribute("CONTEXT", methodCall);
                    }
                    interestingEvent.addAttribute("CONTEXT", methodCall);
                }
            }
        };
        md.findAll(Expression.class).forEach(consumer);
        if(!methodEvents._eventQueue.isEmpty())methodEventsList.add(methodEvents);
    }

    protected InterestingEvent addDummyLastEventBefore(String name, MethodEvents methodEvents, InterestingEvent event){
        methodEvents.eventQueue().remove(event);
        InterestingEvent dummy = methodEvents.addEventStartingAfterLast(name, "DUMMY", 1);
        methodEvents.addEventStartingAfterLast(event);
        return dummy;
    }

    protected  InterestingEvent addBasedOnLast(MethodCallExpr methodCall, MethodEvents methodEvents, boolean use_last, boolean after){
        int counter = 0;
        String reference = "last";
        if(!use_last){
            reference = methodCall.getArgument(counter++).toString().replace("\"", "");
            boolean exist = false;
            for(InterestingEvent ev : methodEvents.eventQueue()){
                if(ev.name().equals(reference)){
                    exist = true;
                    break;
                }
            }
            if(!exist) use_last = true;
        }
        String name = methodCall.getArgument(counter++).toString().replace("\"", "");
        String type = methodCall.getArgument(counter++).toString().replace("\"", "");
        int duration = 1;
        int renderingDuration = 1;
        int waitTime = 0;
        if(methodCall.getArguments().size() >= counter) {
            try {
                duration = Integer.valueOf(methodCall.getArgument(counter++).toString());
                renderingDuration = duration;
            } catch (Exception exception) {
                //...
            }
        }
        if(methodCall.getArguments().size() >= counter){
            try{
                renderingDuration = Integer.valueOf(methodCall.getArgument(counter++).toString());
            } catch(Exception exception){
                //...
            }
        }
        if(after && methodCall.getArguments().size() == counter){
            try{
                waitTime = Integer.valueOf(methodCall.getArgument(counter++).toString());
            } catch(Exception exception){
                //...
            }
        }
        if(!_visualize) return methodEvents.addEventStartingAfterLast(name, type, duration, renderingDuration, waitTime);
        if(after && use_last) return methodEvents.addEventStartingAfterLast(name, type, duration, renderingDuration, waitTime);
        if(!after && use_last) return methodEvents.addEventStartingWithLast(name, type, duration, renderingDuration);

        if(after) return methodEvents.addEventStartingAfter(reference, name, type, duration, renderingDuration, waitTime);
        else return methodEvents.addEventStartingWith(reference, name, type, duration, renderingDuration);
    }
}

