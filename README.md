nub[![All Contributors](https://img.shields.io/badge/all_contributors-1-orange.svg?style=flat-square)](#contributors)
===========================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================

- [Description](#description)
- [Scene](#scene)
- [Nodes](#nodes)
  - [Localization](#localization)
  - [Motion filters](#motion-filters)
  - [Visual hints](#visual-hints)
  - [Space transformations](#space-transformations)
  - [Keyframes](#keyframes)
  - [Behaviors](#behaviors)
- [Rendering](#rendering)
  - [Drawing functionality](#drawing-functionality)
- [Interactivity](#interactivity)
  - [Node interaction methods](#node-interaction-methods)
    - [Eye mouse and keyboard code snippets](#eye-mouse-and-keyboard-code-snippets)
    - [Node mouse and keyboard code snippets](#node-mouse-and-keyboard-code-snippets)
  - [Picking](#picking)
    - [Mouse and keyboard code snippets](#mouse-and-keyboard-code-snippets)
- [Installation](#installation)
- [Contributors](#contributors)

# Description

[nub](http://visualcomputing.github.io/Transformations/#/6) is a simple, expressive, language-agnostic, and extensible [visual computing](https://en.wikipedia.org/wiki/Visual_computing) library, featuring interaction, visualization and animation frameworks and supporting advanced (onscreen/offscreen) rendering techniques, such as [view frustum culling](http://cgvr.informatik.uni-bremen.de/teaching/cg_literatur/lighthouse3d_view_frustum_culling/index.html).

_nub_ is meant to be coupled with third party real and non-real time [renderers](https://en.wikipedia.org/wiki/3D_rendering). Our current [release](https://github.com/VisualComputing/nub/releases) supports the [2D and 3D PGraphicsOpenGL (a.k.a. P2D and P3D, respectively)](https://processing.github.io/processing-javadocs/core/processing/opengl/PGraphicsOpenGL.html) [Processing](https://processing.org/) renderers.

If looking for the API docs, check them [here](https://visualcomputing.github.io/nub-javadocs/).

Readers unfamiliar with geometry transformations may first check the great [Processing 2D transformations tutorial](https://processing.org/tutorials/transform2d/) by _J David Eisenberg_ and the [affine transformations](http://visualcomputing.github.io/Transformations) and [scene-graphs](http://visualcomputing.github.io/SceneGraphs) presentations that discuss some related formal foundations.

# Scene

Instantiate your on-screen scene at the [setup()](https://processing.org/reference/setup_.html):

```processing
// import all nub classes
import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene;

void setup() {
  scene = new Scene(this);
}
```

The `Scene.context()` corresponds to the *PApplet* main `PGraphics` instance.

Off-screen scenes should be instantiated upon a [PGraphics](https://processing.org/reference/PGraphics.html) object:

```processing
import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene offScreenScene;

void setup() {
  offScreenScene = new Scene(createGraphics(w, h / 2, P3D));
}
```

In this case, the `offScreenScene.context()` corresponds to the `PGraphics` instantiated with [createGraphics()](https://processing.org/reference/createGraphics_.html) (which is of course different than the *PApplet* main `PGraphics` instance).

# Nodes

A node may be translated, rotated and scaled (the order is important) and be rendered when it has a shape. `Node` instances define each of the nodes comprising the scene tree. To illustrate their use, suppose the following scene hierarchy is being implemented:

```processing
World
  ^
  |\
 n1 eye
  ^
  |\
 n2 n3
```

To setup the scene hierarchy of nodes use code such as the following:

```processing
import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene;
Node n1, n2, n3;

void setup() {
  size(720, 480, P3D);
  // the scene object creates a default eye node
  scene = new Scene(this);
  // Create a top-level node (i.e., a node whose reference is null) with:
  n1 = new Node();
  // whereas for the remaining nodes we pass any constructor taking a
  // reference node parameter, such as Node(Node referenceNode), or
  // Node(Node referenceNode, shape). Here the shape parameter defines
  // an immediate mode rendering procedure node visual representation
  n2 = new Node(n1, (PGraphics pg) -> {
    pg.fill(255, 0, 255);
    pg.box(20, 10, 30);
  });
  // here the shape paramenter defines a retained mode rendering
  // procedure node visual representation
  n3 = new Node(n1, createShape(BOX, 30));
  // translate the node to make it visible
  n3.translate(50, 50, 50);
}
```

Note that the hierarchy of nodes may be modified with `setReference(Node)` and the scene `eye()` set from an arbitrary node instance with `setEye(Node)`.

## Localization

A node `position`, `orientation` and `magnitude` may be set with the following methods:

|Node localization|Position                      |Orientation                       |Magnitude                   |
|-----------------|------------------------------|----------------------------------|----------------------------|
|Globally         |`setWorldPosition(vector)`    |`setWorldOrientation(quaternion)` |`setWorldMagnitude(scalar)` |
|Locally          |`setPosition(vector)`         |`setOrientation(quaternion)`      |`setMagnitude(scalar)`      |
|Incrementally    |`translate(vector, [inertia])`|`rotate(quaternion, [inertia])`,  |`scale(scalar, [inertia])`  |

Note that the optional `inertia` parameter should be a value in [0..1], `0` no inertia (which is the default value) & `1` no friction. Its implementation was inspired by the great [PeasyCam damped actions](https://github.com/jdf/peasycam/blob/master/src/peasy/DampedAction.java).

## Motion filters

Calling `setTranslationFilter(filter params)`, `setRotationFilter(filter, params)` and/or `setScalingFilter(filter, params)` will apply a `filter` which is a function used to limit the node motion when calling any of the methods found in the previous section. The node provides the following default filters: `translationalAxisFilter`, `translationalPlaneFilter` and `rotationalAxisFilter` (see the [NodeFilters](https://github.com/VisualComputing/nub/tree/master/examples/basics/NodeFilters/NodeFilters.pde) and the [Luxo](https://github.com/VisualComputing/nub/tree/master/examples/basics/Luxo/Luxo.pde) examples).

## Visual hints

The node visual representation may be configured using the following hints:

* `BULLSEYE` which displays a bullseye centered at the node `worldPosition()` screen projection and is used for [picking](#picking). Call `setBullsEyeSize(value)` to set the size of the hint.
* `SHAPE` which displays the node shape set with `setShape(shape)` which is either an [immediate-mode](https://en.wikipedia.org/wiki/Immediate_mode_(computer_graphics)) rendering Processing procedure or a [retained-mode](https://en.wikipedia.org/wiki/Retained_mode) rendering Processing [PShape](https://processing.org/reference/PShape.html).
* `HUD` which displays the node _Heads-Up-Display_ set with `setHUD(shape)`.
* `BOUNDS` which displays the bounding volume of each scene for which this node is the eye. Only meaningful if there's a second scene perspective to look at this eye node from.
* `KEYFRAMES` which displays the [Catmull Rom spline](https://en.wikipedia.org/wiki/Cubic_Hermite_spline#Catmull%E2%80%93Rom_spline) defining the current node animation path.
* `AXES` which displays an axes hint centered at the node `worldPosition()` an oriented according to the node `worldOrientation()`.
* `TORUS` which displays a torus solenoid.
* `CAMERA` which displays a camera hint centered at the node screen projection.

Observations:

1. The actual node visual `hint()` is a bitwise-or mask of a subset of the above hints enabled at a given time, and so it does the hint parameter in methods to enable (`node.enableHint(hint)`), disable (`node.disableHint(hint)`) or toggle (`node.toggleHint(hint)`) them, e.g., `node.enableHint(Node.SHAPE | Node.KEYFRAMES)` enables altogether the `SHAPE` and `KEYFRAMES` node hints.
2. Displaying the hint requires first to enabling it (`enableHint(mask)`) and then calling a scene [rendering algorithm](#rendering).

### Hints configuration

The node `configHint(hint, params)` configures `hint` using [varargs](https://en.wikipedia.org/wiki/Variadic_function) params as follows:

* `BOUNDS`: `configHint(Node.BOUNDS, boundsWeight)`.
* `KEYFRAMES`: `configHint(Node.KEYFRAMES, keyframesMask)` or `configHint(Node.KEYFRAMES, keyframesMask, steps)` or `configHint(Node.KEYFRAMES, keyframesMask, steps, splineStroke)`, or `configHint(Node.KEYFRAMES, keyframesMask, steps, splineStroke, splineWeight)`.
* `AXES`: `configHint(Node.AXES, axesLength)`.
* `BULLSEYE`: `configHint(Node.BULLSEYE, bullseyeStroke)`, `configHint(Node.BULLSEYE, bullseyeShape)`, or `configHint(Node.BULLSEYE, bullseyeStroke, bullseyeShape)`.
* `TORUS`: `configHint(Node.TORUS, torusStroke)`, or `configHint(Node.TORUS, torusStroke, torusFaces)`.
* `CAMERA`: `configHint(Node.CAMERA, cameraStroke)` or `configHint(Node.CAMERA, cameraStroke, cameraLength)`.

e.g., `node.configHint(Node.BULLSEYE, color(255, 0, 0))` colors red the `BULLSEYE` hint.

## Space transformations

The following `Scene` methods transform points (_locations_) and vectors (_displacements_) between screen space (a box of `width * height * 1` dimensions where user interaction takes place), [NDC](http://www.songho.ca/opengl/gl_projectionmatrix.html) and nodes (including the world, i.e., the `null` node):

| Space transformation  | Points                        | Vectors                            |
|-----------------------|-------------------------------|------------------------------------|
| NDC to Screen         | `ndcToScreenLocation(point)`  | `ndcToScreenDisplacement(vector)`  |
| Screen to NDC         | `screenToNDCLocation(pixel)`  | `screenToNDCDisplacement(vector)`  |
| Screen to Node        | `location(pixel, node)`       | `displacement(vector, node)`       |
| Node to Screen        | `screenLocation(point, node)` | `screenDisplacement(vector, node)` |
| Screen to World       | `location(pixel)`             | `displacement(vector)`             |
| World to Screen       | `screenLocation(point)`       | `screenDisplacement(vector)`       |

Note that `point`, `pixel` and `vector` are `Vector` instances.

The following `Node` methods transform points (_locations_) and scalars / vectors/ quaternions (_displacements_) between different node instances (including the world):

| Space transformation     | Points                     |Scalars / Vectors / Quaternions   |
|--------------------------|----------------------------|----------------------------------|
| Node to (this) Node      | `location(point, node)`    | `displacement(element, node)`    |
| (this) Node to Reference | `referenceLocation(point)` | `referenceDisplacement(element)` |
| Reference to (this) Node | `localLocation(point)`     | `localDisplacement(element)` |
| World to (this) Node     | `location(point)`          | `displacement(element)`          |
| (this) Node to World     | `worldLocation(point)`     | `worldDisplacement(element)`     |

Note that `point` is a `Vector` instance and `element` is either a `float` (scalar), `Vector` or `Quaternion` one.

## Keyframes

[Keyframes](https://en.wikipedia.org/wiki/Key_frame) allow to define the position, orientation and magnitude a node (including the eye) should have at a particular moment in time. The node may then be animated through a [Catmull-Rom](https://en.wikipedia.org/wiki/Cubic_Hermite_spline#Catmull%E2%80%93Rom_spline) spline, matching in space-time the key-frames which defines it. Use code such as the following:

```processing
Scene scene;
PShape pshape;
Node shape;

void setup() {
  // ...
  shape = new Node(pshape);
  for (int i = 0; i < random(4, 10); i++) {
    scene.randomize(shape);
    // addKeyFrame(hint, elapsedTime) where elapsedTime is defined respect
    // to the previously added key-frame and expressed in milliseconds.
    shape.addKeyFrame(Node.AXES | Node.SHAPE, i % 2 == 1 ? 1000 : 4000);
  }
  shape.animate();
}
```

which will create a `shape` interpolator containing [4..10] random key-frames. See the [KeyFrames](https://github.com/VisualComputing/nub/tree/master/examples/basics/KeyFrames) example.

## Behaviors

Custom `node` behaviors to be executed every iteration of the `scene` `render()` algorithm may be added with code such as the following:

```processing
Scene scene;
Node node;

void setup() {
  Scene scene = new Scene();
  Node node = new Node();
  scene.addBehavior(node, this::behavior);
}

void behavior(Scene scene, Node node) {
  // Custom node behavior for the scene.render() algorithm
}
```

See the [CajasOrientadas](https://github.com/VisualComputing/nub/tree/master/examples/basics/CajasOrientadas) and the the [ViewFrustumCulling](https://github.com/VisualComputing/nub/tree/master/examples/demos/ViewFrustumCulling) examples.

# Rendering

Render the scene node hierarchy from its `eye` point-of-view with `render(subtree)` and display it with `image(cornerX, cornerY)`, e.g.,

```processing
void draw() {
  scene.openContext();
  // drawing commands here take place at the world coordinate system
  // use scene.context() to access the scene Processing PGraphics instance,
  // e.g., reset the background with:
  scene.context().background(100);
  scene.render(subtree); // the subtree param is optional
  // drawing commands here also take place at the world coordinate system
  scene.closeContext();
  scene.image(cornerX, cornerY);
}
```

Onscreen scenes may just call `render(subtree)`, e.g.,

```processing
void draw() {
  background(125);
  scene.render(subtree); // the subtree param is optional
  // drawing commands here take place at the world coordinate system
  scene.drawAxes();
}
```

Or use the higher level `display()` algorithm to achive the same results for both of the above onscreen and offscreen scenes:

```processing
void draw() {
  // the offscreen scene subtree is rendered at (cornerX, cornerY)
  scene.display(color(100), subtree, cornerX, cornerY);
}
```

```processing
void draw() {
  // the onscreen scene subtree is rendered with the world axes
  scene.display(color(125), true, subtree);
}
```

Observations:

1. Enclose 2D screen space with `scene.beginHUD()` and `scene.endHUD()` stuff (such as gui elements and text) with to render it on top of a 3D scene.
2. Customize the rendering traversal algorithm by setting a node custom behavior with `scene.addBehavior(node, behavior)`, see the [ViewFrustumCulling](https://github.com/VisualComputing/nub/tree/master/examples/demos/ViewFrustumCulling) example.

## Drawing functionality

The `Scene` implements several static drawing functions that complements those already provided by Processing, such as: `drawCylinder(PGraphics, int, float, float)`, `drawHollowCylinder(PGraphics, int, float, float, Vector, Vector)`, `drawCone(PGraphics, int, float, float, float, float)`, `drawCone(PGraphics, int, float, float, float, float, float)` and `drawTorusSolenoid(PGraphics, int, int, float, float)`.

Drawing functions that take a `PGraphics` parameter (including the above static ones), such as `beginHUD(PGraphics)`,
`endHUD(PGraphics)`, `drawAxes(PGraphics, float)`, `drawCross(PGraphics, float, float, float)` and `drawGrid(PGraphics)` among others, can be used to set a node shape.

Another scene's eye (different than this one) can be drawn with `drawFrustum(Scene)`. Typical usage include interactive [minimaps](https://en.wikipedia.org/wiki/Mini-map) and _visibility culling_ visualization and debugging.

# Interactivity

## Node interaction methods

To directly interact with a given `node` (or the _eye_ when no `node` param is given), call any of the following screen space scene methods:

| Scene screen space method                                             |
|-----------------------------------------------------------------------|
| `align([node])`                                                       |
| `focus([node])`                                                       |
| `shift([node], [dx], [dy], [dz], [inertia])`                          |
| `turn([node], roll, pitch, yaw, [inertia])`                           |
| `zoom([node], delta, [inertia])`                                      |
| `spin([node], [pixel1X], [pixel1Y], [pixel2X], [pixel2Y], [inertia])` |

Customize node behaviors by registering a user gesture data parser `function_object` with the node `setInteraction(function_object)` method, and then send gesture data to the node by calling one of the scene custom interaction invoking methods: `interact(gesture)`, `interact(tag, gesture)` or `interact(node, gesture)`, where gesture type is `Object....`. See the [CustomNodeInteraction](https://github.com/VisualComputing/nub/blob/master/examples/demos/CustomNodeInteraction/CustomNodeInteraction.pde) example.

### Eye mouse and keyboard code snippets

```processing
// define a mouse-dragged eye interaction
void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
    scene.shift();
  else
    scene.zoom(scene.mouseDX());
}
```

```processing
// define a mouse-moved eye interaction
void mouseMoved(MouseEvent event) {
  if (event.isShiftDown())
    scene.turn(scene.mouseRADY(), scene.mouseRADX(), 0);
  else
    scene.lookAround();
}
```

```processing
// define a mouse-wheel eye interaction
void mouseWheel(MouseEvent event) {
  if (scene.is3D())
    scene.moveForward(event.getCount() * 20);
  else
    scene.zoom(event.getCount() * 20);
}
```

```processing
// define a mouse-click eye interaction
void mouseClicked(MouseEvent event) {
  if (event.getCount() == 1)
    scene.align();
  else
    scene.focus();
}
```

```processing
// define a key-pressed eye interaction
void keyPressed() {
  // roll with 'x' key
  scene.turn(key == 'x' ? QUARTER_PI / 2 : -QUARTER_PI / 2, 0, 0);
}
```

The [SpaceNavigator](https://github.com/VisualComputing/nub/tree/master/examples/basics/SpaceNavigator) and [CustomEyeInteraction](https://github.com/VisualComputing/nub/tree/master/examples/demos/CustomEyeInteraction) examples illustrate how to set up other hardware such as a keyboard or a full fledged 6-DOFs device like the [space-navigator](https://en.wikipedia.org/wiki/3Dconnexion).

### Node mouse and keyboard code snippets

```processing
void mouseDragged() {
  // spin n1
  if (mouseButton == LEFT)
    scene.spin(n1);
  // shift n3
  else if (mouseButton == RIGHT)
    scene.shift(n3);
  // zoom n1
  else
    scene.zoom(n1, scene.mouseDX());
}
```

```processing
void keyPressed() {
  if (key == CODED)
    if(keyCode == UP)
      // shift n2 up
      scene.shift(n2, 0, 10);
    if(keyCode == DOWN)
      // shift n2 down
      scene.shift(n2, 0, -10);
}
```

## Picking

Picking a node (which should be different than the scene eye) to interact with it is a two-step process:

1. Tag the `node` using an arbitrary `tag` either with `tag(tag, node)` or ray-casting:
   
   | Synchronously :small_blue_diamond:     | Asynchronously :small_orange_diamond:|
   |----------------------------------------|--------------------------------------|
   | `updateTag([tag], [pixelX], [pixelY])` | `tag([tag], [pixelX], [pixelY])`     |
   
   :small_blue_diamond: The tagged node is returned immediately
   :small_orange_diamond: The tagged node is returned during the next call to the `render()` algorithm
   
2. Interact with your _tagged_ nodes by calling any `scene` method implementing the `interact(tag, gesture...)` which resolves the node param in the [methods above](#node-interaction-methods) (using the scene `node(tag)` method):

   | Scene screen space method                                            |
   |----------------------------------------------------------------------|
   | `align([tag])`                                                       |
   | `focus([tag])`                                                       |
   | `shift([tag], [dx], [dy], [dz], [inertia])`                          |
   | `turn([tag], roll, pitch, yaw, [inertia])`                           |
   | `zoom([tag], delta, [inertia])`                                      |
   | `spin([tag], [pixel1X], [pixel1Y], [pixel2X], [pixel2Y], [inertia])` |

Observations:

1. A node can have multiple tags but a given tag cannot be assigned to more than one node, and since the null tag is allowed, signatures of all the above methods lacking the tag parameter are provided for convenience.
2. The `BULLSEYE` hint is used to pick the node with ray-casting. Disabling the hint will thus disable ray-casting.
3. To check if a given node would be picked with a ray cast at a given screen position, call `tracks(node, [pixelX], [pixelY])`.
4. To tag the nodes in a given array with ray casting use `updateTag([tag], [pixelX], [pixelY], Node[])`.
5. Set `Scene.inertia` in  [0..1] (`0` no inertia & `1` no friction) to change the default `inertia` value globally. It is initially set to `0.8`. See the [CajasOrientadas](https://github.com/VisualComputing/nub/tree/master/examples/basics/CajasOrientadas) example.
6. Invoke custom node behaviors by either calling the scene `interact(node, gesture)`, `interact(gesture)` or `interact(tag, gesture)` methods. See the [CustomNodeInteraction](https://github.com/VisualComputing/nub/blob/master/examples/demos/CustomNodeInteraction/CustomNodeInteraction.pde) example.

### Mouse and keyboard code snippets

```processing
// pick with mouse-moved
void mouseMoved() {
  scene.tag();
}

// interact with mouse-dragged
void mouseDragged() {
  if (mouseButton == LEFT)
    // spin the picked node or the eye if no node has been picked
    scene.spin();
  else if (mouseButton == RIGHT)
    // shift the picked node or the eye if no node has been picked
    scene.shift();
  else
    // zoom the picked node or the eye if no node has been picked
    scene.zoom(mouseX - pmouseX);
}
```

```processing
// pick with mouse-clicked
void mouseClicked(MouseEvent event) {
  if (event.getCount() == 1)
    // use the null tag to manipulate the picked node with mouse-moved
    scene.tag();
  if (event.getCount() == 2)
    // use the "key" tag to manipulate the picked node with key-pressed
    scene.tag("key");
}

// interact with mouse-moved
void mouseMoved() {
  // spin the node picked with one click
  scene.spin();
}

// interact with key-pressed
void keyPressed() {
  // focus the node that has been picked with two clicks
  scene.focus("key");
}
```

# Installation

Import/update it directly from your [PDE](https://processing.org/reference/environment/). Otherwise download your [release](https://github.com/VisualComputing/nub/releases) and extract it to your sketchbook `libraries` folder.

# Contributors

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
