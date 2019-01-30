# Detached frames

**Table of Contents**

- [Description](#user-content-description)
- [Sceneless](#user-content-scene)
- [Scene](#user-content-scene)

## Description

Frames can also be instantiated without attaching them to a `scene` by simply omitting the `scene` parameter in its constructor: `node = new Frame()`. This article discusses how to use them. This is a low-level technique that may be useful to port the library to other frameworks different than [Processing](https://processing.org/).

## Sceneless

To setup the hierarchy of _detached_ frames, i.e., frames not belonging to a particular `scene`, use code such as the following:

```processing
Frame eye, f1, f2, f3;
void setup() {
  // Note the use of the default node constructor to instantiate a
  // detached leading node (those whose parent is the world, such as the eye and f1):
  eye =  new Frame();
  f1 = new Frame();
  // whereas for the remaining frames we pass any constructor taking a
  // reference node paramater, such as Frame(Frame referenceFrame, float scaling):
  f2 = new Frame(f1, 1);
  f3 = new Frame(f1, 1);
}
```

then traverse it with:

```processing
void draw() {
  // define a projection matrix
  perspective(fov, width / height, cameraZ / 10.0f, cameraZ * 10.0f);
  // render from the eye poin-of-view
  setMatrix(Scene.toPMatrix(eye.view()));
  // enter f1
  pushMatrix();
  // g happens to be the name of the
  // PApplet man PGraphics instance
  Scene.applyTransformation(g, f1);
  drawF1();
  // enter f2
  pushMatrix();
  Scene.applyTransformation(g, f2);
  drawF2();
  // "return" to f1
  popMatrix();
  // enter f3
  pushMatrix();
  Scene.applyTransformation(g, f3);
  drawF3();
  // return to f1
  popMatrix();
  // return to World
  popMatrix();
}
```

See the [Sceneless example](https://github.com/VisualComputing/frames/tree/master/testing/src/processing/DetachedFrames/Sceneless). Some advantages of using _detached_ frames without instantiating a `scene` object are:

* The scene gets rendered respect to an `eye` node.
* The graph topology is set (even at run time) with [setReference(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setReference-frames.core.Frame-).
* [setTranslation(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setTranslation-frames.primitives.Vector-), [translate(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#translate-frames.primitives.Vector-), [setRotation(Quaternion)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setRotation-frames.primitives.Quaternion-), [rotate(Quaternion)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#rotate-frames.primitives.Quaternion-), [setScaling(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setScaling-float-) and [scale(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#scale-float-), locally manipulates a node instance.
* [setPosition(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setPosition-frames.primitives.Vector-), [setOrientation(Quaternion)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setOrientation-frames.primitives.Quaternion-), and [setMagnitude(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setMagnitude-float-), globally manipulates a node instance.
* [location(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#location-frames.primitives.Vector-frames.core.Frame-) and [displacement(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#displacement-frames.primitives.Vector-frames.core.Frame-) transforms coordinates and vectors (resp.) from other node instances.
* [worldLocation(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#worldLocation-frames.primitives.Vector-) and [worldDisplacement(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#worldDisplacement-frames.primitives.Vector-) transforms node coordinates and vectors (resp.) to the world.
* [setConstraint(Constrain)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setConstraint-frames.core.constraint.Constraint-) applies a [Constraint](https://visualcomputing.github.io/frames-javadocs/frames/primitives/constraint/Constraint.html) to a node instance limiting its motion.

## Scene

You can also instantiate an eye through a `scene` object which automatically handles the projection and eye transform matrices:

```processing
Scene scene;
Frame f1, f2, f3;
void setup() {
  // Define a 3D perspective scene
  // Note that the scene.eye() is also instantiated
  scene = new Scene(this);
  // ... same as before
}
```

then traverse the hierarchy with:

```processing
void draw() {
  // enter f1
  pushMatrix();
  scene.applyTransformation(f1);
  drawF1();
  // enter f2
  pushMatrix();
  scene.applyTransformation(f2);
  drawF2();
  // "return" to f1
  popMatrix();
  // enter f3
  pushMatrix();
  scene.applyTransformation(f3);
  drawF3();
  // return to f1
  popMatrix();
  // return to World
  popMatrix();
}
```

To set the scene [tracked-node](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#trackedFrame--) (the node the mouse should interact with) call [setTrackedFrame(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#setTrackedFrame-frames.core.Frame-) or update it with [track(Frame[])](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#track-frames.core.Frame:A-), for example:

```processing
void mouseMoved() {
  // the tracked-node is updated from the array using ray-casting
  scene.track(new Frame[]{f1, f2, f3});
}
```

To interact with a given node use any `Scene` method that takes a `node` parameter, such as: [spin(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#spin-frames.core.Frame-), [translate(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#translate-frames.core.Frame-), [scale(float, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#scale-float-frames.core.Frame-) or [zoom(float, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#zoom-float-frames.core.Frame-). For example:

```processing
public void mouseDragged() {
  // spin f1
  if (mouseButton == LEFT)
    scene.spin(f1);
  // translate f3
  else if (mouseButton == RIGHT)
    scene.translate(f3);
  // zoom f2
  else
    scene.zoom(scene.mouseDX(), f2);
}
```

To interact with the [default-node](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#defaultFrame--) (which is either the tracked-node updated with the `mouseMoved` above or the scene eye when the tracked-node is null) use the _frameless_ versions of the above methods, e.g., [spin()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#spin--), [translate()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#translate--), [scale(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#scale-float-) or [zoom(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#zoom-float-). For example:

```processing
public void mouseDragged() {
  // spins the default-node (the eye or the node picked with a mouseMoved)
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
  // translates the default-node (the eye or the node picked with a mouseMoved)
    scene.translate();
  // zooms the default-node (the eye or the node picked with a mouseMoved)
  else
    scene.zoom(scene.mouseDX());
}
```

See the [detached-frames CajasOrientadas example](https://github.com/VisualComputing/frames/tree/master/testing/src/processing/DetachedFrames/CajasOrientadas). Some advantages of using _detached_ frames through an instantiated `scene` object:

* Same as with _detached_ frames without an instantiated `scene` object.
* The `eye` node is automatically handled by the `scene` and may be set from any (attached or detached) [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) instance (see [setEye(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#setEye-frames.core.Frame-)).
* By default the `scene` object instantiates and attached `eye` node.
* Frames may be picked using ray-casting and the `scene` provides all sorts of interactivity commands to manipulate them.
* The `scene` methods [location(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#location-frames.primitives.Vector-frames.core.Frame-) and [screenLocation(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#screenLocation-frames.primitives.Vector-frames.core.Frame-) transforms coordinates between node and screen space.

The main disadvantage of using detached frames is that you need to know the scene hierarchy topology in advanced to be able to traverse it. To enable the scene to handle the traversal algorithm use [attached frames](README.md) instead.
