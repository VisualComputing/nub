# Detached nodes

**Table of Contents**

- [Description](#user-content-description)
- [Sceneless](#user-content-scene)
- [Scene](#user-content-scene)

## Description

Nodes can also be instantiated without attaching them to a `scene` by simply omitting the `scene` parameter in its constructor: `node = new Node()`. This article discusses how to use them. This is a low-level technique that may be useful to port the library to other frameworks different than [Processing](https://processing.org/).

## Sceneless

To setup the hierarchy of _detached_ nodes, i.e., nodes not belonging to a particular `scene`, use code such as the following:

```processing
Node eye, n1, n2, n3;
void setup() {
  // Note the use of the default node constructor to instantiate a detached 
  // leading node (those whose parent is the world, such as the eye and n1):
  eye =  new Node();
  n1 = new Node();
  // whereas for the remaining nodes we pass any constructor taking a
  // reference node paramater, such as Node(Node referenceNode, float scaling):
  n2 = new Node(n1);
  n3 = new Node(n1);
}
```

then traverse it with:

```processing
void draw() {
  // define a projection matrix
  perspective(fov, width / height, cameraZ / 10.0f, cameraZ * 10.0f);
  // render from the eye poin-of-view
  setMatrix(Scene.toPMatrix(eye.view()));
  // enter n1
  pushMatrix();
  Scene.applyTransformation(g, n1);
  drawN1();
  // enter n2
  pushMatrix();
  Scene.applyTransformation(g, n2);
  drawN2();
  // "return" to n1
  popMatrix();
  // enter n3
  pushMatrix();
  Scene.applyTransformation(g, n3);
  drawN3();
  // return to n1
  popMatrix();
  // return to World
  popMatrix();
}
```

See the [Sceneless example](https://github.com/VisualComputing/nubjs/tree/processing/testing/src/processing/DetachedNodes/Sceneless). Some advantages of using _detached_ nodes without instantiating a `scene` object are:

* The scene gets rendered respect to an `eye` node.
* The graph topology is set (even at run time) with [setReference(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setReference-frames.core.Node-).
* [setTranslation(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setTranslation-frames.primitives.Vector-), [translate(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#translate-frames.primitives.Vector-), [setRotation(Quaternion)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setRotation-frames.primitives.Quaternion-), [rotate(Quaternion)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#rotate-frames.primitives.Quaternion-), [setScaling(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setScaling-float-) and [scale(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#scale-float-), locally manipulates a node instance.
* [setPosition(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setPosition-frames.primitives.Vector-), [setOrientation(Quaternion)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setOrientation-frames.primitives.Quaternion-), and [setMagnitude(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setMagnitude-float-), globally manipulates a node instance.
* [location(Vector, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#location-frames.primitives.Vector-frames.core.Node-) and [displacement(Vector, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#displacement-frames.primitives.Vector-frames.core.Node-) transforms coordinates and vectors (resp.) from other node instances.
* [worldLocation(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#worldLocation-frames.primitives.Vector-) and [worldDisplacement(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#worldDisplacement-frames.primitives.Vector-) transforms node coordinates and vectors (resp.) to the world.
* [setConstraint(Constrain)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setConstraint-frames.core.constraint.Constraint-) applies a [Constraint](https://visualcomputing.github.io/nub-javadocs/nub/core/constraint/Constraint.html) to a node instance limiting its motion.

## Scene

You can also instantiate an eye through a `scene` object which automatically handles the projection and eye transform matrices:

```processing
Scene scene;
Node n1, n2, n3;
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
  // enter n1
  pushMatrix();
  scene.applyTransformation(n1);
  drawF1();
  // enter n2
  pushMatrix();
  scene.applyTransformation(n2);
  drawF2();
  // "return" to n1
  popMatrix();
  // enter n3
  pushMatrix();
  scene.applyTransformation(n3);
  drawF3();
  // return to n1
  popMatrix();
  // return to World
  popMatrix();
}
```

To set the scene [tracked-node](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#trackedNode--) (the node the mouse should interact with) call [setTrackedNode(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setTrackedNode-frames.core.Node-) or update it with [track(Node[])](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#track-frames.core.Node:A-), for example:

```processing
void mouseMoved() {
  // the tracked-node is updated from the array using ray-casting
  scene.track(new Node[]{n1, n2, n3});
}
```

To interact with a given node use any `Scene` method that takes a `node` parameter, such as: [spin(Node)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#spin-frames.core.Node-), [translate(Node)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#translate-frames.core.Node-), [scale(float, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#scale-float-frames.core.Node-) or [zoom(float, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#zoom-float-frames.core.Node-). For example:

```processing
public void mouseDragged() {
  // spin n1
  if (mouseButton == LEFT)
    scene.spin(n1);
  // translate n3
  else if (mouseButton == RIGHT)
    scene.translate(n3);
  // zoom n2
  else
    scene.zoom(scene.mouseDX(), n2);
}
```

To interact with the [default-node](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#defaultNode--) (which is either the tracked-node updated with the `mouseMoved` above or the scene eye when the tracked-node is null) use the _nodeless_ versions of the above methods, e.g., [spin()](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#spin--), [translate()](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#translate--), [scale(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#scale-float-) or [zoom(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#zoom-float-). For example:

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

See the [detached-nodes CajasOrientadas example](https://github.com/VisualComputing/nubjs/tree/processing/testing/src/processing/DetachedNodes/CajasOrientadas). Some advantages of using _detached_ nodes through an instantiated `scene` object:

* Same as with _detached_ nodes without an instantiated `scene` object.
* The `eye` node is automatically handled by the `scene` and may be set from any (attached or detached) [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) instance (see [setEye(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setEye-frames.core.Node-)).
* By default the `scene` object instantiates and attached `eye` node.
* Nodes may be picked using ray-casting and the `scene` provides all sorts of interactivity commands to manipulate them.
* The `scene` methods [location(Vector, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#location-frames.primitives.Vector-frames.core.Node-) and [screenLocation(Vector, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#screenLocation-frames.primitives.Vector-frames.core.Node-) transforms coordinates between node and screen space.

The main disadvantage of using detached nodes is that you need to know the scene hierarchy topology in advanced to be able to traverse it. To enable the scene to handle the traversal algorithm use [attached nodes](README.md) instead.
