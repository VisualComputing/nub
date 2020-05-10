package nub.ik.visualization;

import nub.core.Node;
import nub.ik.solver.Solver;
import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is responsible of deal with interactions between
 * a collection of visualizers and a solver.
 * <p>
 * Usually we provide a visualizer with information from solver
 * i.e we add the interesting events occured during execution.
 * <p>
 * Although it is less common, it is also possible to interact with
 * the visualizer and update the subscribed solver-
 */

public class VisualizerMediator {
  protected List<Visualizer> _visualizers = new ArrayList<>();
  protected Solver _solver;
  protected List<InterestingEvent> _eventQueue = new ArrayList<>();

  protected long _firstEvent = 0;
  protected final long MAX_EVENT_SIZE = 5000;


  public VisualizerMediator(Solver solver, Visualizer... visualizers) {
    _solver = solver;
    _solver.setMediator(this);
    for (Visualizer v : visualizers) {
      addVisualizer(v);
    }
  }

  public int finishingTime() {
    return !_eventQueue.isEmpty() ? _eventQueue.get(_eventQueue.size() - 1).finishingTime() : 0;
  }

  public long lastEvent() {
    return _firstEvent + _eventQueue.size();
  }

  public InterestingEvent event(long id) {
    return _eventQueue.get((int) (id - _firstEvent));
  }


  public List<InterestingEvent> eventQueue() {
    return _eventQueue;
  }

  public void addVisualizer(Visualizer v) {
    _visualizers.add(v);
    v.setMediator(this);
    _solver.registerStructure(this);
    v._time = (_firstEvent + _eventQueue.size()) * v._period;
  }

  //Methods for facilitating event insertion on events queue
  public void addEvent(InterestingEvent event) {
    //Insert the event at its corresponding position o(n)
    if (_eventQueue.isEmpty()) {
      _eventQueue.add(event);
      return;
    }

    boolean added = false;
    for (int i = _eventQueue.size() - 1; i >= 0; i--) {
      InterestingEvent current = _eventQueue.get(i);
      if (event.startingTime() > current.startingTime() || (event.startingTime() == current.startingTime() && event.finishingTime() >= current.finishingTime())) {
        if (i + 1 < _eventQueue.size()) _eventQueue.add(i + 1, event);
        else _eventQueue.add(event);
        added = true;
        break;
      }
    }
    if (!added) _eventQueue.add(0, event);
    if (_eventQueue.size() > MAX_EVENT_SIZE) {
      _limitEventQueue();
    }
  }

  public InterestingEvent lastEventWithName(String name) {
    for (int i = _eventQueue.size() - 1; i >= 0; i--) {
      if (_eventQueue.get(i).name().equals(name)) return _eventQueue.get(i);
    }
    return null;
  }

  public InterestingEvent addEvent(String name, String type, int startingTime, int executionDuration, int renderingDuration) {
    InterestingEvent event = new InterestingEvent(name, type);
    event.startAt(startingTime);
    event.setDuration(executionDuration, renderingDuration);
    addEvent(event);
    return event;
  }

  public InterestingEvent addEventStartingAfter(InterestingEvent previous, String name, String type, int executionDuration, int renderingDuration, int wait) {
    int start = previous.finishingTime() + wait;
    return addEvent(name, type, start, executionDuration, renderingDuration);
  }

  public InterestingEvent addEventStartingAfter(InterestingEvent previous, String name, String type, int executionDuration, int renderingDuration) {
    return addEventStartingAfter(previous, name, type, executionDuration, renderingDuration, 0);
  }

  public InterestingEvent addEventStartingAfter(InterestingEvent previous, String name, String type, int executionDuration) {
    return addEventStartingAfter(previous, name, type, executionDuration, executionDuration, 0);
  }

  //Starts the event right after the last event with the given name (if there's no one an error will be thrown)
  public InterestingEvent addEventStartingAfter(String previousEventName, String name, String type, int executionDuration, int renderingDuration, int wait) {
    InterestingEvent event = lastEventWithName(previousEventName);
    if (event == null) throw new RuntimeException("There is no event with name " + previousEventName);
    return addEventStartingAfter(event, name, type, executionDuration, renderingDuration, wait);
  }

  public InterestingEvent addEventStartingAfter(String previousEventName, String name, String type, int executionDuration, int renderingDuration) {
    return addEventStartingAfter(previousEventName, name, type, executionDuration, renderingDuration, 0);
  }

  public InterestingEvent addEventStartingAfter(String previousEventName, String name, String type, int executionDuration) {
    return addEventStartingAfter(previousEventName, name, type, executionDuration, executionDuration, 0);
  }

  public InterestingEvent addEventStartingAfterLast(String name, String type, int executionDuration, int renderingDuration, int wait) {
    return addEvent(name, type, finishingTime() + wait, executionDuration, renderingDuration);
  }

  public InterestingEvent addEventStartingAfterLast(String name, String type, int executionDuration, int renderingDuration) {
    return addEvent(name, type, finishingTime(), executionDuration, renderingDuration);
  }

  public InterestingEvent addEventStartingAfterLast(String name, String type, int executionDuration) {
    return addEvent(name, type, finishingTime(), executionDuration, executionDuration);
  }

  public InterestingEvent addEventStartingWith(InterestingEvent event, String name, String type, int executionDuration, int renderingDuration) {
    int start = event.startingTime();
    return addEvent(name, type, start, executionDuration, renderingDuration);
  }

  public InterestingEvent addEventStartingWith(String concurrentEventName, String name, String type, int executionDuration, int renderingDuration) {
    InterestingEvent event = lastEventWithName(concurrentEventName);
    if (event == null) throw new RuntimeException("There is no event with name " + concurrentEventName);
    return addEventStartingWith(event, name, type, executionDuration, renderingDuration);
  }

  public InterestingEvent addEventStartingWith(String concurrentEventName, String name, String type, int executionDuration) {
    return addEventStartingWith(concurrentEventName, name, type, executionDuration, executionDuration);
  }

  public InterestingEvent addEventStartingWithLast(String name, String type, int executionDuration, int renderingDuration) {
    if (_eventQueue.isEmpty()) return addEvent(name, type, finishingTime(), executionDuration, renderingDuration);
    return addEventStartingWith(_eventQueue.get(_eventQueue.size() - 1), name, type, executionDuration, renderingDuration);
  }

  public InterestingEvent addEventStartingWithLast(String name, String type, int executionDuration) {
    return addEventStartingWithLast(name, type, executionDuration, executionDuration);
  }

  protected void _limitEventQueue() {
    //How many events must be removed ?
    long firstEvent = _firstEvent + _eventQueue.size() - MAX_EVENT_SIZE;
    //Make sure that each visualizer is updated to new first event
    for (Visualizer v : _visualizers) {
      v.jumpTo(firstEvent);
    }
    _firstEvent = firstEvent;
    //remove first events
    while (_eventQueue.size() > MAX_EVENT_SIZE) _eventQueue.remove(0);
  }

  public void registerStructure(Object structure) {
    //Due to the flow of execution, the only visualizer that must be updated is the last one
    Visualizer v = _visualizers.get(_visualizers.size() - 1);
    if (structure instanceof Node) {
      v.registerStructure((Node) structure);
    } else if (structure instanceof List) {
      v.registerStructure((List<? extends Node>) structure);
    }
  }

  public void hide(String eventName) {
    hide(lastEventWithName(eventName));
  }

  public void hide(InterestingEvent event) {
    event.setRenderingDuration(finishingTime());
  }


  //Testing with JSON
  protected File _configFile;
  protected HashMap<String, EventConfig> _config;


  protected class EventConfig {
    protected int _wait, _waitRender, _waitExecution;
    protected String _startEvent, _stopRenderingEvent, _stopExecutionEvent;
    //this is useful only for events inside loops
    protected String _beforeLoopEvent, _startEventOnFirstIteration;
    protected boolean _first = false;
  }

  public void loadJSONConfiguration(PApplet pApplet, String configFile) {
    _config = new HashMap<String, EventConfig>();
    JSONArray events_data = pApplet.loadJSONArray(_configFile);
    for (int i = 0; i < events_data.size(); i++) {
      EventConfig eventConfig = new EventConfig();
      JSONObject event_data = events_data.getJSONObject(i);
      eventConfig._startEvent = event_data.getString("start_event");
      eventConfig._wait = event_data.getInt("start_wait");
      eventConfig._stopRenderingEvent = event_data.getString("stop_rendering");
      eventConfig._waitRender = event_data.getInt("rendering_wait");
      eventConfig._stopExecutionEvent = event_data.getString("stop_execution");
      eventConfig._waitRender = event_data.getInt("execution_wait");
      eventConfig._waitRender = event_data.getInt("execution_wait");
      if (event_data.hasKey("before_loop")) eventConfig._beforeLoopEvent = event_data.getString("before_loop");
      if (event_data.hasKey("first_iteration")) eventConfig._beforeLoopEvent = event_data.getString("first_iteration");
      _config.put(event_data.getString("name"), eventConfig);
    }
  }

  public void addEventUsingJSON(String name, String type) {
    EventConfig config = _config.get(name);
    InterestingEvent prev = null;
    if (config._beforeLoopEvent != null) {
      _config.get(config._beforeLoopEvent)._first = true;
    }

    if (config._first == true && config._startEventOnFirstIteration != null) { // is within a loop
      if (config._first) {
        prev = lastEventWithName(config._startEventOnFirstIteration);
        config._first = false;
      }
    } else if (config._startEvent.equals("last")) {
      prev = eventQueue().get(eventQueue().size() - 1);
    } else {
      prev = lastEventWithName(config._startEvent);
    }
    //Execution duration and rendering will be defined later
    addEvent(name, type, prev.startingTime() + config._wait, 1, 1);
  }

  public void setDurationUsingJSON(InterestingEvent event) {
    //find first occurrence of future event
    int i = eventQueue().indexOf(event);
    EventConfig config = _config.get(event);

    while (eventQueue().get(i).startingTime() >= event.startingTime()) {
      i--;
    }
    //from i find first occurrence of render and exec
    InterestingEvent stopExec = null, stopRender = null;
    for (int index = i + 1; i < eventQueue().size(); i++) {
      InterestingEvent next = eventQueue().get(index);
      if (stopExec == null && next.name().equals(config._stopExecutionEvent)) {
        stopExec = next;
      }
      if (stopRender == null && next.name().equals(config._stopRenderingEvent)) {
        stopRender = next;
      }
      if (stopRender != null && stopExec != null) break;
    }
    //set the values accordingly
    event.setRenderingDuration(stopExec.startingTime() - event.startingTime() + config._waitExecution);
    event.setRenderingDuration(stopExec.startingTime() - event.startingTime() + config._waitRender);
    //sort the eventQueue
    i = eventQueue().indexOf(event);
    while (event.startingTime() <= eventQueue().get(i).startingTime() && event.executionDuration() >= eventQueue().get(i).executionDuration()) {
      i++;
    }
    eventQueue().remove(event);
    if (i < eventQueue().size()) eventQueue().add(i, event);
    else eventQueue().add(event);
  }
}
