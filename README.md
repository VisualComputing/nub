nub[![All Contributors](https://img.shields.io/badge/all_contributors-1-orange.svg?style=flat-square)](#contributors)
===========================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================

**Table of Contents**

- [Description](#user-content-description)
- [Scene](#user-content-scene)
- [Nodes](#user-content-nodes)
- [Interpolators](#user-content-interpolators)
- [HIDs](#user-content-hids)
- [Control](#user-content-control)
- [Installation](#user-content-installation)
- [Contributors](#user-content-contributors)

## Description

[nub](http://visualcomputing.github.io/Transformations/#/6) is a simple, expressive, language-agnostic, and extensible [(2D/3D) scene graph](https://en.wikipedia.org/wiki/Scene_graph) featuring interaction, visualization and animation frameworks and supporting advanced (onscreen/offscreen) rendering techniques, such as [view frustum culling](http://cgvr.informatik.uni-bremen.de/teaching/cg_literatur/lighthouse3d_view_frustum_culling/index.html).

*nub* is meant to be coupled with third party real and non-real time [renderers](https://en.wikipedia.org/wiki/3D_rendering). Our current [release](https://github.com/VisualComputing/nub/releases) supports all major [Processing](https://processing.org/) desktop renderers: [2D and 3D PGraphicsOpenGL (a.k.a. P2D and P3D, respectively)](https://processing.github.io/processing-javadocs/core/processing/opengl/PGraphicsOpenGL.html), [PGraphicsJava2D (a.k.a. JAVA2D)](https://processing.github.io/processing-javadocs/core/processing/awt/PGraphicsJava2D.html) and [PGraphicsFX2D (a.k.a. FX2D)](https://processing.github.io/processing-javadocs/core/processing/javafx/PGraphicsFX2D.html).

If looking for the API docs, check them [here](https://visualcomputing.github.io/nub-javadocs/).

Readers unfamiliar with geometry transformations may first check the great [Processing 2D transformations tutorial](https://processing.org/tutorials/transform2d/) by _J David Eisenberg_ and this [presentation](http://visualcomputing.github.io/Transformations/#/6) that discusses some related formal foundations.

## Scene

Instantiate your on-screen scene at the [setup()](https://processing.org/reference/setup_.html):

```processing
Scene scene;
void setup() {
  scene = new Scene(this);
}
```

The [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) [context()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#context--) corresponds to the *PApplet* main `PGraphics` instance.

Off-screen scenes should be instantiated upon a [PGraphics](https://processing.org/reference/PGraphics.html) object:

```processing
Scene scene;
void setup() {
  scene = new Scene(this, createGraphics(500, 500, P3D));
  // or use the equivalent but simpler version:
  // scene = new Scene(this, P3D, 500, 500);
}
```

In this case, the [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) [context()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#context--) corresponds to the `PGraphics` instantiated with [createGraphics()](https://processing.org/reference/createGraphics_.html) (which is of course different than the *PApplet* main `PGraphics` instance).

## Nodes

A node may be translated, rotated and scaled (the order is important) and be rendered when it has a shape. [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) instances define each of the nodes comprising a scene graph. To illustrate their use, suppose the following scene graph is being implemented:

```processing
 World
  ^
  |\
  1 eye
  ^
  |\
  2 3
```

To setup the scene hierarchy of _attached_ nodes, i.e., nodes belonging to the scene, use code such as the following:

```processing
Scene scene;
Node n1, n2, n3;
void setup() {
  scene = new Scene(this);
  // To attach a leading-node (those whose parent is the world, such as n1)
  // the scene parameter is passed to the Node constructor:
  n1 = new Node(scene);
  // whereas for the remaining nodes we pass any constructor taking a
  // reference node paramater, such as Node(Node referenceNode)
  n2 = new Node(n1) {
    // immediate mode rendering procedure
    // defines n2 visual representation
    @Override
    public void graphics(PGraphics pg) {
      Scene.drawTorusSolenoid(pg);
    }
  };
  // retained-mode rendering PShape
  // defines n3 visual representation
  n3 = new Node(n1, createShape(BOX, 60));
}
```

Some advantages of using _attached_ nodes are:

* The [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) sets up a default _eye_ node. To set the eye from an arbitrary [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) instance call [setEye(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setEye-frames.core.Node-). To retrieve the scene _eye_ instance call [eye()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#eye--).
* A node shape can be set from an [immediate-mode](https://en.wikipedia.org/wiki/Immediate_mode_(computer_graphics)) rendering Processing procedure (see [graphics(PGraphics)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#graphics-processing.core.PGraphics-)) or from a [retained-mode](https://en.wikipedia.org/wiki/Retained_mode) rendering Processing [PShape](https://processing.org/reference/PShape.html) (see [shape(PShape)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#shape-processing.core.PShape-)). Node shapes can be picked precisely using their projection onto the screen, see [pickingThreshold()](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#pickingThreshold--).
* Even the _eye_ can have a shape which may be useful to depict the viewer in first person camera style.
* The scene topology is set (even at run time) with [setReference(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setReference-frames.core.Node-).
* While the [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) method [screenLocation(Vector, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#screenLocation-frames.primitives.Vector-frames.core.Node-) (or the simpler version [screenLocation(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#screenLocation-nub.core.Node-)) transforms node coordinates to screen space, [location(Vector, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#location-frames.primitives.Vector-frames.core.Node-) performs the inverse transformation (i.e., it transforms screen space coordinates to the node).
* While the [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) method [screenLocation(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#screenLocation-nub.primitives.Vector-) transforms world coordinates to screen space, [location(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#location-nub.primitives.Vector-) performs the inverse transformation (i.e., it transforms screen space coordinates to the world).
* The [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) methods [location(Vector, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#location-frames.primitives.Vector-frames.core.Node-) (or the simpler version [location(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#location-nub.core.Node-)) and [displacement(Vector, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#displacement-frames.primitives.Vector-frames.core.Node-) transforms coordinates and vectors (resp.) from other node instances.
* The [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) methods [location(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#location-nub.primitives.Vector-) and [displacement(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#displacement-nub.primitives.Vector-) transforms world coordinates and vectors (resp.) to the node.
* The [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) methods [worldLocation(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#worldLocation-frames.primitives.Vector-) and [worldDisplacement(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#worldDisplacement-frames.primitives.Vector-) transforms node coordinates and vectors (resp.) to the world.
* The [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) method [setConstraint(Constrain)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setConstraint-frames.core.constraint.Constraint-) applies a [Constraint](https://visualcomputing.github.io/nub-javadocs/nub/core/constraint/Constraint.html) to a node instance limiting its motion.
* The [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) methods [setPosition(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setPosition-frames.primitives.Vector-), [setOrientation(Quaternion)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setOrientation-frames.primitives.Quaternion-), and [setMagnitude(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setMagnitude-float-), globally manipulates a node instance.
* The node methods [setTranslation(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setTranslation-frames.primitives.Vector-), [translate(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#translate-frames.primitives.Vector-), [setRotation(Quaternion)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setRotation-frames.primitives.Quaternion-), [rotate(Quaternion)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#rotate-frames.primitives.Quaternion-), [setScaling(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setScaling-float-) and [scale(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#scale-float-), locally manipulates a node instance.

### Interactivity

To set the scene [tracked-node](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#trackedNode--) (the node the mouse should interact with) call [setTrackedNode(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setTrackedNode-frames.core.Node-) or update it using ray-casting with [cast()](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#cast--), for example:

```processing
void mouseMoved() {
  // the tracked-node is updated using ray-casting from the set of scene attached nodes
  scene.cast();
}
```

To interact with a given node use any [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) method that takes a [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) parameter, such as: [spin(Node)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#spin-frames.core.Node-), [translate(Node)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#translate-frames.core.Node-) or [scale(float, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#scale-float-frames.core.Node-) For example:

```processing
public void mouseDragged() {
  // spin n1
  if (mouseButton == LEFT)
    scene.spin(n1);
  // translate n3
  else if (mouseButton == RIGHT)
    scene.translate(n3);
  // scale n2
  else
    scene.scale(scene.mouseDX(), n2);
}
```

To interact with the [default-node](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#defaultNode--) (which is either the tracked-node updated with the `mouseMoved` above or the scene _eye_ when the tracked-node is null) use the _nodeless_ versions of the above methods, e.g., [spin()](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#spin--), [translate()](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#translate--) or [scale(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#scale-float-). For example:

```processing
public void mouseDragged() {
  // spins the default-node (the eye or the node picked with a mouseMoved)
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
  // translates the default-node (the eye or the node picked with a mouseMoved)
    scene.translate();
  // scales the default-node (the eye or the node picked with a mouseMoved)
  else
    scene.scale(scene.mouseDX());
}
```

See the [CajasOrientadas](https://github.com/VisualComputing/nub/tree/master/examples/basics/CajasOrientadas) example.

### Rendering

Render the node hierarchy onto [context()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#context--) with:

```processing
void draw() {
  // visits each shape drawing it
  scene.render();
}
```

observe that:

* The scene gets rendered respect to the scene [eye()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#eye--) node.
* Call [render(PGraphics)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#render-java.lang.Object-) to render the scene into an arbitrary _PGraphics_ context. See the [PostEffects](https://github.com/VisualComputing/nub/tree/master/examples/demos/PostEffects) example.
* Call [render(PGraphics, Graph.Type, Node, zNear, zFar)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#render-processing.core.PGraphics-frames.core.Graph.Type-frames.core.Node-float-float-) to render the scene into an arbitrary _PGraphics_ context from an arbitrary node point-of-view. See the [DepthMap](https://github.com/VisualComputing/nub/tree/master/examples/demos/DepthMap) and [ShadowMapping](https://github.com/VisualComputing/nub/tree/master/examples/demos/ShadowMapping) examples.
* The role played by a [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) instance during a scene graph traversal is implemented by overriding its [visit()](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#visit--) method.

To bypass the [render()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#render--) algorithm use [detached nodes](detached.md), or cull the node (see [cull(boolean)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#cull-boolean-) and [isCulled()](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#isCulled--)).

#### Drawing functionality

The [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) implements several static drawing functions that complements those already provided by Processing, such as: [drawCylinder(PGraphics, int, float, float)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawCylinder-processing.core.PGraphics-int-float-float-), [drawHollowCylinder(PGraphics, int, float, float, Vector, Vector)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawHollowCylinder-processing.core.PGraphics-int-float-float-frames.primitives.Vector-frames.primitives.Vector-), [drawCone(PGraphics, int, float, float, float, float)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawCone-processing.core.PGraphics-int-float-float-float-float-), [drawCone(PGraphics, int, float, float, float, float, float)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawCone-processing.core.PGraphics-int-float-float-float-float-float-) and [drawTorusSolenoid(PGraphics, int, int, float, float)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawTorusSolenoid-processing.core.PGraphics-int-int-float-float-).

Drawing functions that take a `PGraphics` parameter (including the above static ones), such as [beginHUD(PGraphics)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#beginHUD-processing.core.PGraphics-),
[endHUD(PGraphics)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#endHUD-processing.core.PGraphics-), [drawAxes(PGraphics, float)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawAxes-processing.core.PGraphics-float-), [drawCross(PGraphics, float, float, float)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawCross-processing.core.PGraphics-float-float-float-) and [drawGrid(PGraphics)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawGrid-processing.core.PGraphics-) among others, can be used to set a node shape.

Another scene's eye (different than this one) can be drawn with [drawEye(Graph)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawEye-frames.core.Graph-). Typical usage include interactive [minimaps](https://en.wikipedia.org/wiki/Mini-map) and _visibility culling_ visualization and debugging.

## Interpolators

A node can be animated through a [key-frame](https://en.wikipedia.org/wiki/Key_frame) [Catmull-Rom](https://en.wikipedia.org/wiki/Cubic_Hermite_spline#Catmull%E2%80%93Rom_spline) [interpolator](https://visualcomputing.github.io/nub-javadocs/nub/core/Interpolator.html) path. Use code such as the following:

```processing
Scene scene;
PShape pshape;
Node shape;
Interpolator interpolator;
void setup() {
  ...
  shape = new Node(scene, pshape);
  interpolator = new Interpolator(shape);
  for (int i = 0; i < random(4, 10); i++)
    interpolator.addKeyFrame(scene.randomNode());
  interpolator.start();
}
```

which will create a random interpolator path containing [4..10] key-frames. The interpolation is also started. The interpolator path may be drawn with code like this:

```processing
...
void draw() {
  scene.render();
  scene.drawPath(interpolator);
}
```

while [render()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#render--) will draw the animated shape(s) [drawPath(Interpolator)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawPath-frames.core.Interpolator-) will draw the interpolated path too. See the [Interpolators](https://github.com/VisualComputing/nub/tree/master/examples/basics/Interpolators) example.

## HIDs

Setting up a [Human Interface Device (hid)](https://en.wikipedia.org/wiki/Human_interface_device) (different than the mouse which is provided by default) such as a keyboard or a [space-navigator](https://en.wikipedia.org/wiki/3Dconnexion), is a two step process:

1. Define an _hid_ tracked-node instance, using an arbitrary name for it (see [setTrackedNode(String, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setTrackedNode-java.lang.String-frames.core.Node-)); and,
2. Call any interactivity method that take an _hid_ param (such as [translate(String, float, float, float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#translate-java.lang.String-float-float-), [rotate(String, float, float, float)]() or [scale(String, float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#scale-java.lang.String-float-) following the name convention you defined in 1.

See the [SpaceNavigator](https://github.com/VisualComputing/nub/tree/master/examples/basics/SpaceNavigator) example.

Observations:

1. An _hid_ tracked-node (see [trackedNode(String)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#trackedNode-java.lang.String-)) defines in turn an _hid_ default-node (see [defaultNode(String)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#defaultNode-java.lang.String-)) which simply returns the tracked-node or the scene _eye_ when the _hid_ tracked-node is `null`
2. The _hid_ interactivity methods are implemented in terms of the ones defined previously by simply passing the _hid_ [defaultNode(String)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#defaultNode-java.lang.String-) to them.
3. The default _hid_ is defined with a `null` String parameter (e.g., [scale(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#scale-float-) simply calls `scale(null, delta)`). The _Scene_ default mouse _hid_ presented in the [Nodes](#user-content-frames) section is precisely implemented is this manner.
4. To update an _hid_ tracked-node using ray-casting call [track(String, Point, Node[])](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#track-java.lang.String-frames.primitives.Point-frames.core.Node:A-) (detached or attached nodes), [track(String, Point)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#track-java.lang.String-frames.primitives.Point-) (only attached nodes) or [cast(String, Point)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#cast-java.lang.String-frames.primitives.Point-) (only for attached nodes too). While [track(String, Point, Node[])](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#track-java.lang.String-frames.primitives.Point-frames.core.Node:A-) and [track(String, Point)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#track-java.lang.String-frames.primitives.Point-) update the _hid_ tracked-node synchronously (i.e., they return the _hid_ tracked-node immediately), [cast(String, Point)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#cast-java.lang.String-frames.primitives.Point-) updates it asynchronously (i.e., it optimally updates the _hid_ tracked-node during the next call to the [render()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#render--) algorithm).

## Control

[Application control](https://hal.inria.fr/hal-00789413/) (aka as Post-[WIMP](https://en.wikipedia.org/wiki/WIMP_(computing)) interaction styles) refers to interfaces “containing at least one interaction technique not dependent on classical 2D widgets” [[van Dam]](http://dl.acm.org/citation.cfm?id=253708), such as:  [tangible interaction](https://en.wikipedia.org/wiki/Tangible_user_interface), or perceptual and [affective computing](https://en.wikipedia.org/wiki/Affective_computing).

Implementing an application control for a node is a two step process:

1. Override the node method [interact(Object...)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#interact-java.lang.Object...-) to parse the gesture into a custom (application) control.
2. Send gesture data to the node by calling one of the following scene methods: [defaultHIDControl(Object...)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#defaultHIDControl-java.lang.Object...-), [control(String, Object...)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#control-java.lang.String-java.lang.Object...-) or [control(Node, Object...)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#control-frames.core.Node-java.lang.Object...-).

See the [ApplicationControl](https://github.com/VisualComputing/nub/tree/master/examples/demos/ApplicationControl) example.

## Installation

Import/update it directly from your [PDE](https://processing.org/reference/environment/). Otherwise download your [release](https://github.com/VisualComputing/nub/releases) and extract it to your sketchbook `libraries` folder.

## Contributors

Thanks goes to these wonderful people ([emoji key](https://github.com/kentcdodds/all-contributors#emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tr>
    <td align="center"><a href="https://github.com/nakednous"><img src="https://avatars2.githubusercontent.com/u/645599?v=4" width="100px;" alt="Jean Pierre Charalambos"/><br /><sub><b>Jean Pierre Charalambos</b></sub></a><br /><a href="#blog-nakednous" title="Blogposts">📝</a> <a href="https://github.com/VisualComputing/nub/issues?q=author%3Anakednous" title="Bug reports">🐛</a> <a href="https://github.com/VisualComputing/nub/commits?author=nakednous" title="Code">💻</a> <a href="#design-nakednous" title="Design">🎨</a> <a href="https://github.com/VisualComputing/nub/commits?author=nakednous" title="Documentation">📖</a> <a href="#eventOrganizing-nakednous" title="Event Organizing">📋</a> <a href="#example-nakednous" title="Examples">💡</a> <a href="#financial-nakednous" title="Financial">💵</a> <a href="#fundingFinding-nakednous" title="Funding Finding">🔍</a> <a href="#ideas-nakednous" title="Ideas, Planning, & Feedback">🤔</a> <a href="#platform-nakednous" title="Packaging/porting to new platform">📦</a> <a href="#plugin-nakednous" title="Plugin/utility libraries">🔌</a> <a href="#question-nakednous" title="Answering Questions">💬</a> <a href="#review-nakednous" title="Reviewed Pull Requests">👀</a> <a href="#talk-nakednous" title="Talks">📢</a> <a href="https://github.com/VisualComputing/nub/commits?author=nakednous" title="Tests">⚠️</a> <a href="#tutorial-nakednous" title="Tutorials">✅</a> <a href="#video-nakednous" title="Videos">📹</a></td>
  </tr>
</table>

<!-- markdownlint-enable -->
<!-- prettier-ignore-end -->
<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/kentcdodds/all-contributors) specification. Contributions of any kind welcome!
