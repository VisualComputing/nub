package ik.animation.eventParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import nub.ik.animation.InterestingEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class EventCollector extends VoidVisitorAdapter<List<MethodEvents>> {
    @Override
    public void visit(MethodDeclaration md, List<MethodEvents> methodEventsList) {
        super.visit(md, methodEventsList);
        MethodEvents methodEvents = new MethodEvents(md.getName().toString());
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
                            interestingEvent = addBasedOnLast(methodCall, methodEvents, true);
                            break;
                        }
                        case "addEventStartingWithLast": {
                            interestingEvent = addBasedOnLast(methodCall, methodEvents, false);
                            break;
                        }
                    }
                    if(interestingEvent == null) return;

                    //consider edge cases: ON_COND, ON_WHILE, ON_METHOD
                    Optional<IfStmt> condition = methodCall.findAncestor(IfStmt.class, stmt -> !stmt.getCondition().toString().contains("enableMediator")); //If is surrounded by non dummy IF CLAUSE
                    if(condition.isPresent()){
                        //EVENT IS CONTAINED IN AL IF/ELSE STRUCTURE
                        if(condition.get().hasThenBlock() && condition.get().getThenStmt().isAncestorOf(methodCall)){
                            interestingEvent.addAttribute("ON_IF", condition.get().getCondition());
                        } else if(condition.get().hasElseBlock() && condition.get().getElseStmt().get().isAncestorOf(methodCall)){
                            interestingEvent.addAttribute("ON_ELSE", condition.get().getCondition());
                        }
                    }
                    if(methodEvents._eventQueue.isEmpty()){
                        //FIRST EVENT IN THE GIVEN METHOD

                    }

                    //InterestingEvent ev = new InterestingEvent(methodCall.getArgument(0).toString(), methodCall.getArgument(1).toString());
                    //methodEvents.addEvent(ev);
                }
            }
        };
        md.findAll(Expression.class).forEach(consumer);
        if(!methodEvents._eventQueue.isEmpty())methodEventsList.add(methodEvents);
    }

    protected  InterestingEvent addBasedOnLast(MethodCallExpr methodCall, MethodEvents methodEvents, boolean after){
        String name = methodCall.getArgument(0).toString().replace("\"", "");
        String type = methodCall.getArgument(1).toString().replace("\"", "");
        int duration = 1;
        int renderingDuration = 1;
        int waitTime = 0;
        if(methodCall.getArguments().size() >= 3) {
            try {
                duration = Integer.valueOf(methodCall.getArgument(2).toString());
                renderingDuration = duration;
            } catch (Exception exception) {
                //...
            }
        }
        if(methodCall.getArguments().size() >= 4){
            try{
                renderingDuration = Integer.valueOf(methodCall.getArgument(3).toString());
            } catch(Exception exception){
                //...
            }
        }
        if(after && methodCall.getArguments().size() == 5){
            try{
                waitTime = Integer.valueOf(methodCall.getArgument(4).toString());
            } catch(Exception exception){
                //...
            }
        }
        if(after) return methodEvents.addEventStartingAfterLast(name, type, duration, renderingDuration, waitTime);
        else return methodEvents.addEventStartingWithLast(name, type, duration, renderingDuration);
    }
}

