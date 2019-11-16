nub[![All Contributors](https://img.shields.io/badge/all_contributors-1-orange.svg?style=flat-square)](#contributors)
===========================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================

**Table of Contents**

- [Description](#description)
- [Scene](#scene)
- [Nodes](#nodes)
  - [Node features](#node-features)
- [Rendering](#rendering)
    - [Drawing functionality](#drawing-functionality)
- [Interactivity](#interactivity)
  - [Eye interaction](#eye-interaction)
  - [Node picking and interaction](#node-picking-and-interaction)
  - [Node interaction](#node-interaction)
- [Timing](#timing)
  - [Timing tasks](#timing-tasks)
  - [Interpolators](#interpolators)
- [Installation](#installation)
- [Contributors](#contributors)

## Description

[nub](http://visualcomputing.github.io/Transformations/#/6) is a simple, expressive, language-agnostic, and extensible [(2D/3D) scene graph](https://en.wikipedia.org/wiki/Scene_graph) featuring interaction, visualization and animation frameworks and supporting advanced (onscreen/offscreen) rendering techniques, such as [view frustum culling](http://cgvr.informatik.uni-bremen.de/teaching/cg_literatur/lighthouse3d_view_frustum_culling/index.html).

_nub_ is meant to be coupled with third party real and non-real time [renderers](https://en.wikipedia.org/wiki/3D_rendering). Our current [release](https://github.com/VisualComputing/nub/releases) supports all major [Processing](https://processing.org/) desktop renderers: [2D and 3D PGraphicsOpenGL (a.k.a. P2D and P3D, respectively)](https://processing.github.io/processing-javadocs/core/processing/opengl/PGraphicsOpenGL.html), [PGraphicsJava2D (a.k.a. JAVA2D)](https://processing.github.io/processing-javadocs/core/processing/awt/PGraphicsJava2D.html) and [PGraphicsFX2D (a.k.a. FX2D)](https://processing.github.io/processing-javadocs/core/processing/javafx/PGraphicsFX2D.html).

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

### Node features

Some of the main features _attached_ nodes are:

1. _Scene topology flexibility_, which may be set (even at run time) with [setReference(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setReference-frames.core.Node-).
2. _Eye_ node instantiation, which is done by the different [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) constructors. To set the eye from an arbitrary [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) instance call [setEye(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setEye-frames.core.Node-), and to retrieve it call [eye()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#eye--).
3. Node _shapes_, which can be set from an [immediate-mode](https://en.wikipedia.org/wiki/Immediate_mode_(computer_graphics)) rendering Processing procedure (see [graphics(PGraphics)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#graphics-processing.core.PGraphics-)) or from a [retained-mode](https://en.wikipedia.org/wiki/Retained_mode) rendering Processing [PShape](https://processing.org/reference/PShape.html) (see [shape(PShape)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#shape-processing.core.PShape-)). Shapes can be picked precisely using their projection onto the screen, see [pickingThreshold()](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#pickingThreshold--). Note that even the _eye_ can have a shape which may be useful to depict the viewer in first person camera style.
4. _Transformations among coordinate systems_
   1. Between screen space and node instances:
      * While the [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) method [location(Vector, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#location-frames.primitives.Vector-frames.core.Node-) transforms screen coordinates to the node, [screenLocation(Vector, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#screenLocation-frames.primitives.Vector-frames.core.Node-) (or the simpler version [screenLocation(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#screenLocation-nub.core.Node-)) performs the inverse transformation (i.e., it transforms node coordinates to screen space).
      * While the [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) method [location(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#location-nub.primitives.Vector-) transforms screen coordinates to the world, [screenLocation(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#screenLocation-nub.primitives.Vector-) performs the inverse transformation (i.e., it transforms world coordinates to screen space).
      * While the [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) method [displacement(Vector, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#displacement-nub.primitives.Vector-nub.core.Node-) transforms screen vector displacements to the node, [screenDisplacement(Vector, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#screenDisplacement-nub.primitives.Vector-nub.core.Node-) performs the inverse transformation (i.e., it transforms node vector displacements to screen space).
      * While the [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) method [displacement(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#displacement-nub.primitives.Vector-) transforms screen vector displacements to the world, [screenDisplacement(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#screenDisplacement-nub.primitives.Vector-) performs the inverse transformation (i.e., it transforms world displacements to screen space).
    1. Between node instances:
        * The [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) methods [location(Vector, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#location-frames.primitives.Vector-frames.core.Node-) (or the simpler version [location(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#location-nub.core.Node-)) and [displacement(Vector, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#displacement-frames.primitives.Vector-frames.core.Node-) transforms coordinates and vectors (resp.) from other node instances.
         * The [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) methods [location(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#location-nub.primitives.Vector-) and [displacement(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#displacement-nub.primitives.Vector-) transforms world coordinates and vectors (resp.) to the node.
         * The [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) methods [worldLocation(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#worldLocation-frames.primitives.Vector-) and [worldDisplacement(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#worldDisplacement-frames.primitives.Vector-) transforms node coordinates and vectors (resp.) to the world.
5. _Constraints_ which limit node motions. Set them with [setConstraint(Constrain)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setConstraint-frames.core.constraint.Constraint-).
6. _Node localization_ which may be achieved globally with [setPosition(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setPosition-frames.primitives.Vector-), [setOrientation(Quaternion)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setOrientation-frames.primitives.Quaternion-), and [setMagnitude(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setMagnitude-float-); or locally with [setTranslation(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setTranslation-frames.primitives.Vector-), [translate(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#translate-frames.primitives.Vector-), [setRotation(Quaternion)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setRotation-frames.primitives.Quaternion-), [rotate(Quaternion)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#rotate-frames.primitives.Quaternion-), [setScaling(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setScaling-float-) and [scale(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#scale-float-), locally localize a node instance.

## Rendering

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

## Interactivity

### Eye interaction

The scene has several methods to position and orient the _eye_ node, such as: [lookAt(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#lookAt-nub.primitives.Vector-), [setFov(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setFOV-float-), [setViewDirection(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setViewDirection-nub.primitives.Vector-), [setUpVector(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setUpVector-nub.primitives.Vector-), [fit()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#fit--) and [fit(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#fit-nub.core.Node-), among others.

The following _eye_ interaction methods are particularly suited for hardware devices with several degrees-of-freedom: [scaleEye(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#scaleEye-float-), [translateEye(float, float, float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#translateEye-float-float-float-), [rotateEye(float, float, float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#rotateEye-float-float-float-), [spinEye(int, int, int, int)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#spinEye-int-int-int-int-), [lookAround(float, float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#lookAround-float-float-) and [rotateCAD(float, float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#rotateCAD-float-float-). Methods such as: [mouseTranslateEye()](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#mouseTranslateEye--), [mouseSpinEye()](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#mouseSpinEye--) and [mouseLookAround()](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#mouseLookAround--), are implemented by simply passing the *Processing* `pmouseX`, `pmouseY`,  `mouseX` and `mouseY` variables as parameters to some of the above ones, and hence their simpler method signatures. The following code snippets show some of them in action:

```processing
// define a mouse-dragged eye interaction
void mouseDragged() {
  if (mouseButton == LEFT)
    scene.mouseSpinEye();
  else if (mouseButton == RIGHT)
    scene.mouseTranslateEye();
  else
    // changes the scene field-of-view
    scene.scaleEye(mouseX - pmouseX);
}
```

```processing
// define a mouse-moved eye interaction
void mouseMoved(MouseEvent event) {
  if (event.isShiftDown())
    scene.mouseTranslateEye();
  else
    scene.mouseLookAround();
}
```

```processing
// define a mouse-wheel eye interaction
void mouseWheel(MouseEvent event) {
  if (scene.is3D())
    scene.moveForward(event.getCount() * 20);
  else
    scene.scaleEye(event.getCount() * 20);
}
```

### Node picking and interaction

Picking a node to interact with it is a two-step process:

1. Tag the node using an arbitrary name (which may be `null`) either with [tag(String, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#tag-nub.core.Node-) or ray-casting: [updateTag(String, int, int, Node[])](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#updateTag-java.lang.String-int-int-nub.core.Node:A-) (detached or attached nodes), [updateTag(String, int, int)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#updateTag-java.lang.String-int-int-) (only attached nodes) or [tag(String, int, int)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#tag-java.lang.String-int-int-) (only for attached nodes too). While ```updateTag(String, int, int, Node[])``` and ```updateTag(String, int, int)``` update the tagged node synchronously (i.e., they return the tagged node immediately), ```tag(String, int, int)``` updates it asynchronously (i.e., it optimally updates the tagged node during the next call to the ```render()``` algorithm); and,
2. Interact with your tagged nodes by calling any of the following methods: [alignTag(String)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#alignTag-java.lang.String-), [focusTag(String)](), [translateTag(String, float, float, float)](), [rotateTag(String, float, float, float)](), [scaleTag(String, float)](), or [spinTag(String, int, int, int, int)]().



 * Observations:
 * <ol>
 * <li>Refer to {@link Node#pickingThreshold()} (and {@link Node#setPickingThreshold(float)}) for the different
 * ray-casting node picking policies.</li>
 * <li>To check if a given node would be picked with a ray casted at a given screen position,
 * call {@link #tracks(Node, int, int)}.</li>
 * <li>To interact with the node that is referred with the {@code null} tag, call any of the following methods:
 * {@link #alignTag()}, {@link #focusTag()}, {@link #translateTag(float, float, float)},
 * {@link #rotateTag(float, float, float)}, {@link #scaleTag(float)} and
 * {@link #spinTag(int, int, int, int)}), allow </li>
 * <li>To directly interact with a given node, call any of the following methods: {@link #alignNode(Node)},
 * {@link #focusNode(Node)}, {@link #translateNode(Node, float, float, float)},
 * {@link #rotateNode(Node, float, float, float)},
 * {@link #scaleNode(Node, float)} and {@link #spinNode(Node, int, int, int, int)}).</li>
 * <li>To either interact with the node referred with a given tag or the eye, when that tag is not in use,
 * call any of the following methods: {@link #align(String)}, {@link #focus(String)},
 * {@link #translate(String, float, float, float)}, {@link #rotate(String, float, float, float)},
 * {@link #scale(String, float)} and {@link #spin(String, int, int, int, int)}.</li>
 * <li>Customize node behaviors by overridden {@link Node#interact(Object...)}
 * and then invoke them by either calling: {@link #interactTag(Object...)},
 * {@link #interactTag(String, Object...)} or {@link #interactNode(Node, Object...)}.

The following code snippets

```processing
  scene.tag("key", node);
```

the "key" is an arbitrary tag (which may be `null`, see below) the scene uses to track the node. To retrieve the node by its tag, use:

```processing
  scene.node("key");// will return node
```

Ray casting picking also employs tags. To synchronously pick a node at pixel `(x, y)` call:

```processing
  scene.updateTag("pixel", x, y);
```

and the `scene.trackedNode("pixel")` will be returned immediately. To pick the node optimally, albeit asynchronously, call:

```processing
  scene.cast("pixel", x, y);
```

and the `scene.tag("pixel")` will be available after the next call to the [render()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#render--) algorithm.

You may also define a *default* picking metaphor by calling the simpler versions of the above methods, which simply use the `null` tag:

```processing
  // Pick a node with the null tag
  scene.tag(node);
  // same as:
  // scene.tag(null, node)
  // Retrieve the node tagged with null
  scene.node();// will return node
  // same as:
  // scene.node(null)
```

### Node interaction

A node interaction is give by the scene command:

```processing
  scene.interact(node, ...gesture);
```

which is the same as:

```processing
  // default implementation is empty
  node.interact(gesture);
```

where `gesture` data is encoded as a [varargs](https://docs.oracle.com/javase/8/docs/technotes/guides/language/varargs.html) of type `Object`. 

Note that whilst scene method [interact(Node, Object...)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#interact-nub.core.Node-java.lang.Object...-) parses user gesture data, the node method [interact(Object...)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#interact-java.lang.Object...-) implements the actual response to it. The scene provides several implementations of the above pattern, such as the following:


## Timing

### Timing tasks



### Interpolators

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
  interpolator.run();
}
```

which will create a random interpolator path containing [4..10] key-frames. The interpolation is also ran. The interpolator path may be drawn with code like this:

```processing
...
void draw() {
  scene.render();
  scene.drawCatmullRom(interpolator);
}
```

while [render()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#render--) will draw the animated shape(s) [drawCatmullRom(Interpolator)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawCatmullRom-nub.core.Interpolator-) will draw the interpolated path too. See the [Interpolators](https://github.com/VisualComputing/nub/tree/master/examples/basics/Interpolators) example.



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
