<!-- omit in toc -->nub[![All Contributors](https://img.shields.io/badge/all_contributors-1-orange.svg?style=flat-square)](#contributors)
===========================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================

**Table of Contents**

- [Description](#description)
- [Scene](#scene)
- [Nodes](#nodes)
    - [Localization](#localization)
    - [Shapes](#shapes)
    - [Space transformations](#space-transformations)
- [Rendering](#rendering)
    - [Drawing functionality](#drawing-functionality)
- [Interactivity](#interactivity)
  - [Eye](#eye)
  - [Nodes](#nodes-1)
  - [Picking](#picking)
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
  // the scene object creates a default eye node
  scene = new Scene(this);
  // To attach a leading-node (those whose parent is the world, such as n1)
  // the scene parameter is passed to the Node constructor:
  n1 = new Node(scene);
  // whereas for the remaining nodes we pass any constructor taking a
  // reference node parameter, such as Node(Node referenceNode)
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

Note that the hierarchy of nodes may be modified with [setReference(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setReference-frames.core.Node-) and the scene [eye()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#eye--) set from an arbitrary node instance with [setEye(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setEye-frames.core.Node-). Calling [setConstraint(Constrain)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setConstraint-frames.core.constraint.Constraint-) will apply a [Constraint](https://visualcomputing.github.io/nub-javadocs/nub/core/constraint/Constraint.html) to a node to limit its motion.

#### Localization

A node [position](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#position--), [orientation](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#orientation--) and [magnitude]() may be set with the following methods:

| Node localization | Position                     | Orientation                      | Magnitude               |
|-------------------|------------------------------|----------------------------------|-------------------------|
| Globally          | ```setPosition(vector)```    | ```setOrientation(quaternion)``` | ```setMagnitude(mag)``` |
| Locally           | ```setTranslation(vector)``` | ```setRotation(quaternion)```    | ```setScaling(scl)```   |
| Incrementally     | ```translate(vector)```      | ```rotate(quaternion)```         | ```scale(amount)```     |

#### Shapes

Node shapes can be set from an [immediate-mode](https://en.wikipedia.org/wiki/Immediate_mode_(computer_graphics)) rendering Processing procedure (see [graphics(PGraphics)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#graphics-processing.core.PGraphics-)) or from a [retained-mode](https://en.wikipedia.org/wiki/Retained_mode) rendering Processing [PShape](https://processing.org/reference/PShape.html) (see [shape(PShape)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#shape-processing.core.PShape-)). Shapes can be picked precisely using their projection onto the screen, see [pickingThreshold()](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#pickingThreshold--). Note that even the _eye_ can have a shape which may be useful to depict the viewer in first person camera style.

#### Space transformations

The following [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) methods transforms points (_locations_) and vectors (_displacements_) between screen space (a box of `width * height * 1` dimensions, where user interaction takes place) and nodes (including the world, i.e., the `null` node):

| Space transformation | Points                            | Vectors                                |
|----------------------|-----------------------------------|----------------------------------------|
| Screen to Node       | ```location(pixel, node)```       | ```displacement(vector, node)```       |
| Node to Screen       | ```screenLocation(point, node)``` | ```screenDisplacement(vector, node)``` |
| Screen to World      | ```location(pixel)```             | ```displacement(vector, node)```       |
| World to Screen      | ```screenLocation(point)```       | ```screenDisplacement(vector)```       |

The following [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) methods transforms points (_locations_) and vectors (_displacements_) between different node instances (including the world) to this node:

| Space transformation | Points                      | Vectors                          |
|----------------------|-----------------------------|----------------------------------|
| Node to (this) Node  | ```location(point, node)``` | ```displacement(vector, node)``` |
| World to Node        | ```location(point)```       | ```displacement(vector)```       |
| Node to World        | ```worldLocation(point)```  | ```worldDisplacement(vector)```  |

Note that `points`, `pixels` and `vectors` are all [Vector](https://visualcomputing.github.io/nub-javadocs/nub/primitives/Vector.html) instances.

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

### Eye

The scene has several methods to position and orient the _eye_ node, such as: [lookAt(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#lookAt-nub.primitives.Vector-), [setFov(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setFOV-float-), [setViewDirection(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setViewDirection-nub.primitives.Vector-), [setUpVector(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setUpVector-nub.primitives.Vector-), [fit()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#fit--) and [fit(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#fit-nub.core.Node-), among others.

The following scene methods implement _eye_ motion actions particularly suited for input devices, possibly having several degrees-of-freedom:

| Action       | Generic input device                              | Mouse                     |
|--------------|---------------------------------------------------|---------------------------|
| Align        | ```alignEye()```                                  | n.a.                      |
| Focus        | ```focusEye()```                                  | n.a.                      |
| Translate    | ```translateEye(dx, dy, dz)```                    | ```mouseTranslateEye()``` |
| Rotate       | ```rotateEye(roll, pitch, yaw)```                 | n.a.                      |
| Scale        | ```scaleEye(delta)```                             | n.a.                      |
| Spin         | ```spinEye(pixel1X, pixel1Y, pixel2X, pixel2Y)``` | ```mouseSpinEye()```      |
| Move forward | ```moveForward(dz)```                             | n.a.                      |
| Rotate CAD   | ```rotateCAD(roll, pitch)```                      | ```mouseRotateCAD()```    |
| Look around  | ```lookAround(deltaX, deltaY)```                  | ```mouseLookAround()```   |

Note that the mouse actions follows the [delegation pattern](https://en.wikipedia.org/wiki/Delegation_pattern), simply passing the *Processing* `pmouseX`, `pmouseY`,  `mouseX` and `mouseY` as parameters to their relative delegates (the generic input device method counterparts), and hence their simpler signatures. Mouse examples:

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

The [SpaceNavigator](https://github.com/VisualComputing/nub/tree/master/examples/basics/SpaceNavigator) and [CustomEyeInteraction](https://github.com/VisualComputing/nub/tree/master/examples/demos/CustomEyeInteraction) examples illustrate how to set up other hardware such as a keyboard or a [space-navigator](https://en.wikipedia.org/wiki/3Dconnexion).

### Nodes

To directly interact with a given node, call any of the following scene methods:

| Action       | Generic input device                                     | Mouse                          |
|--------------|----------------------------------------------------------|--------------------------------|
| Align        | ```alignNode(node)```                                    | n.a.                           |
| Focus        | ```focusNode(node)```                                    | n.a.                           |
| Translate    | ```translateNode(node, dx, dy, dz)```                    | ```mouseTranslateNode(node)``` |
| Rotate       | ```rotateNode(node, roll, pitch, yaw)```                 | n.a.                           |
| Scale        | ```scaleNode(node, delta)```                             | n.a.                           |
| Spin         | ```spinNode(node, pixel1X, pixel1Y, pixel2X, pixel2Y)``` | ```mouseSpinNode(node)```      |

Note that the mouse actions are implemented using the _delegation pattern_, in a similar manner as it has been done with the [eye](#eye). Mouse examples:

```processing
public void mouseDragged() {
  // spin n1
  if (mouseButton == LEFT)
    scene.spinNode(n1);
  // translate n3
  else if (mouseButton == RIGHT)
    scene.translateNode(n3);
  // scale n2
  else
    scene.scaleNode(n2, scene.mouseDX());
}
```

### Picking

Picking a node (which should be different than the scene eye) to interact with it is a two-step process:

1. Tag the node using an arbitrary name either with [tag(String, Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#tag-nub.core.Node-) or ray-casting:
   
   | Ray casting | Synchronously :small_blue_diamond:   | Asynchronously :small_orange_diamond: |
   |-------------|--------------------------------------|---------------------------------------|
   | Generic     | ```updateTag(tag, pixelX, pixelY)``` | ```tag(tag, pixelX, pixelY)```        |
   | Mouse       | ```updateMouseTag(tag)```            | ```mouseTag(tag)```                |
   
   :small_blue_diamond: The tagged node (see [node(String)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#node-java.lang.String-)) is returned immediately
   :small_orange_diamond: The tagged node is returned during the next call to the ```render()``` algorithm
   
2. Interact with your _tagged_ nodes using one of the following patterns:
   
   1. **Tagged node**: `interactTag(tag, gesture...)` which simply calls `interactNode(node(tag))` using [node(String)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#node-java.lang.String-) to resolve the node parameter in the [node methods above](#nodes-1).
   2. **Tagged node or `eye`**: `interact(tag, gesture...)` which is the same as `if (!interactTag(tag, gesture...)) interactEye(gesture....)` i.e., To either interact with the node referred with a given tag (pattern **i.**) or the eye ([see above](#eye)), when that tag is not in use.
   
   Generic actions:

   | Action    | Tagged node                                            | Tagged node or `eye`                                   |
   |-----------|--------------------------------------------------------|--------------------------------------------------------|
   | Align     | ```alignTag(tag)```                                    | ```align(tag)```                                       |
   | Focus     | ```focusTag(tag)```                                    | ```focus(tag)```                                       |
   | Translate | ```translateTag(tag, dx, dy, dz)```                    | ```translate(tag, dx, dy, dz)```                       |
   | Rotate    | ```rotateTag(tag, roll, pitch, yaw)```                 | ```rotate(tag, roll, pitch, yaw)```                    |
   | Scale     | ```scaleTag(tag, delta)```                             | ```scale(tag, delta)```                                |
   | Spin      | ```spinTag(tag, pixel1X, pixel1Y, pixel2X, pixel2Y)``` | ```spin(String, pixel1X, pixel1Y, pixel2X, pixel2Y)``` |

   Mouse actions:

   | Action    | Tagged nodes                 | Tagged node or `eye`      |
   |-----------|------------------------------|---------------------------|
   | Translate | ```mouseTranslateTag(tag)``` | ```mouseTranslate(tag)``` |
   | Spin      | ```mouseSpinTag(tag)```      | ```mouseSpin(tag)```      |

Observations:

1. Since thee `null` tag is allowed you can pass the `null` tag to any of the above methods or use the stringless versions of them provided for convenience, e.g., `mouseTag(null)` is equivalent to `mouseTag()`.
2. Refer to [pickingThreshold()](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#pickingThreshold--) and [setPickingThreshold(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setPickingThreshold-float-) for the different ray-casting node picking policies.
3. To check if a given node would be picked with a ray casted at a given screen position, call [tracks(Node, int, int)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#tracks-nub.core.Node-int-int-) or [mouseTracks(Node)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#mouseTracks-nub.core.Node-).
4. Customize node behaviors by overridden the node method [interact(Object...)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#interact-java.lang.Object...-) and then invoke them by either calling: [interactNode(Node, Object...)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#interactNode-nub.core.Node-java.lang.Object...-), [interactTag(String, Object...)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#interactTag-java.lang.String-java.lang.Object...-) or [interactTag(Object...)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#interactTag-java.lang.Object...-). See the [CustomNodeInteraction](https://github.com/VisualComputing/nub/blob/master/examples/demos/CustomNodeInteraction/CustomNodeInteraction.pde) example.

Mouse examples:

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
