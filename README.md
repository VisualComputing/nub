nub[![All Contributors](https://img.shields.io/badge/all_contributors-1-orange.svg?style=flat-square)](#contributors)
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

[nub](http://visualcomputing.github.io/Transformations/#/6) is a simple, expressive, language-agnostic, and extensible [visual computing](https://en.wikipedia.org/wiki/Visual_computing) library, featuring interaction, visualization and animation frameworks and supporting advanced (onscreen/offscreen) rendering techniques, such as [view frustum culling](http://cgvr.informatik.uni-bremen.de/teaching/cg_literatur/lighthouse3d_view_frustum_culling/index.html).

_nub_ is meant to be coupled with third party real and non-real time [renderers](https://en.wikipedia.org/wiki/3D_rendering). Our current [release](https://github.com/VisualComputing/nub/releases) supports the [2D and 3D PGraphicsOpenGL (a.k.a. P2D and P3D, respectively)](https://processing.github.io/processing-javadocs/core/processing/opengl/PGraphicsOpenGL.html) [Processing](https://processing.org/) renderers.

If looking for the API docs, check them [here](https://visualcomputing.github.io/nub-javadocs/).

Readers unfamiliar with geometry transformations may first check the great [Processing 2D transformations tutorial](https://processing.org/tutorials/transform2d/) by _J David Eisenberg_ and this [presentation](http://visualcomputing.github.io/Transformations/#/6) that discusses some related formal foundations.

## Scene

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

The [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) [context()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#context--) corresponds to the *PApplet* main `PGraphics` instance.

Off-screen scenes should be instantiated upon a [PGraphics](https://processing.org/reference/PGraphics.html) object:

```processing
import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene offScreenScene;

void setup() {
  offScreenScene = new Scene(this, createGraphics(500, 500, P3D));
  // or use the equivalent but simpler version:
  // offScreenScene = new Scene(this, P3D, 500, 500);
}
```

In this case, the `offScreenScene` [context()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#context--) corresponds to the `PGraphics` instantiated with [createGraphics()](https://processing.org/reference/createGraphics_.html) (which is of course different than the *PApplet* main `PGraphics` instance).

## Nodes

A node may be translated, rotated and scaled (the order is important) and be rendered when it has a shape. [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) instances define each of the nodes comprising the scene tree. To illustrate their use, suppose the following scene hierarchy is being implemented:

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
  n3 = new Node(n1, createShape(BOX, 30));
  // translate the node to make it visible
  n3.translate(50, 50, 50);
}
```

Note that the hierarchy of nodes may be modified with [setReference(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setReference-nub.core.Node-) and the scene [eye()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#eye--) set from an arbitrary node instance with [setEye(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setEye-nub.core.Node-). Calling [setConstraint(Constraint)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setConstraint-nub.core.constraint.Constraint-) will apply a [Constraint](https://visualcomputing.github.io/nub-javadocs/nub/core/constraint/Constraint.html) to a node to limit its motion, see the [ConstrainedEye](https://github.com/VisualComputing/nub/tree/master/examples/basics/ConstrainedEye) and [ConstrainedNode](https://github.com/VisualComputing/nub/tree/master/examples/basics/ConstrainedNode) examples.

#### Localization

A node [position](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#position--), [orientation](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#orientation--) and [magnitude]() may be set with the following methods:

|Node localization|Position                          |Orientation                        |Magnitude                     |
|-----------------|----------------------------------|-----------------------------------|------------------------------|
|Globally         |```setPosition(vector)```         |```setOrientation(quaternion)```   |```setMagnitude(mag)```       |
|Locally          |```setTranslation(vector)```      |```setRotation(quaternion)```      |```setScaling(scl)```         |
|Incrementally    |```translate(vector, [inertia])```|```rotate(quaternion, [inertia])```|```scale(amount, [inertia])```|


The optional `inertia` parameter which should be a value in [0..1], 0 no inertia & 1 no friction. It was inspired by the great [PeasyCam damped actions](https://github.com/jdf/peasycam/blob/master/src/peasy/DampedAction.java) and implemented in terms of [TimingTasks](https://visualcomputing.github.io/nub-javadocs/nub/processing/TimingTask.html).

#### Shapes

Node shapes can be set from an [immediate-mode](https://en.wikipedia.org/wiki/Immediate_mode_(computer_graphics)) rendering Processing procedure (see [graphics(PGraphics)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#graphics-processing.core.PGraphics-)) or from a [retained-mode](https://en.wikipedia.org/wiki/Retained_mode) rendering Processing [PShape](https://processing.org/reference/PShape.html) (see [shape(PShape)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#shape-processing.core.PShape-)). Shapes can be picked precisely using their projection onto the screen, see [pickingThreshold()](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#pickingThreshold--). Note that even the _eye_ can have a shape which may be useful to depict the viewer in first person camera style.

#### Space transformations

The following [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) methods transforms points (_locations_) and vectors (_displacements_) between screen space (a box of `width * height * 1` dimensions where user interaction takes place), [NDC](http://www.songho.ca/opengl/gl_projectionmatrix.html) and nodes (including the world, i.e., the `null` node):

| Space transformation | Points                            | Vectors                                |
|----------------------|-----------------------------------|----------------------------------------|
| NDC to Screen        | ```ndcToScreenLocation(point)```  | ```ndcToScreenDisplacement(vector)```  |
| Screen to NDC        | ```screenToNDCLocation(pixel)```  | ```screenToNDCDisplacement(vector)```  |
| Screen to Node       | ```location(pixel, node)```       | ```displacement(vector, node)```       |
| Node to Screen       | ```screenLocation(point, node)``` | ```screenDisplacement(vector, node)``` |
| Screen to World      | ```location(pixel)```             | ```displacement(vector)```             |
| World to Screen      | ```screenLocation(point)```       | ```screenDisplacement(vector)```       |

The following [Node](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html) methods transforms points (_locations_) and vectors (_displacements_) between different node instances (including the world):

| Space transformation | Points                      | Vectors                          |
|----------------------|-----------------------------|----------------------------------|
| Node to (this) Node  | ```location(point, node)``` | ```displacement(vector, node)``` |
| World to (this) Node | ```location(point)```       | ```displacement(vector)```       |
| (this) Node to World | ```worldLocation(point)```  | ```worldDisplacement(vector)```  |

Note that `points`, `pixels` and `vectors` are all [Vector](https://visualcomputing.github.io/nub-javadocs/nub/primitives/Vector.html) instances.

## Rendering

Render (and display) the scene node hierarchy from its [eye()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#eye--) point-of-view with:

```processing
void draw() {
  scene.render();
}
```

see the [Luxo](https://github.com/VisualComputing/nub/tree/master/examples/basics/Luxo) example, among several others. Render (and display) a scene subtree, like n1-n2, from the scene `eye` point-of-view with:

```processing
// renders n1-n2, discarding n3
void draw() {
  // save space
  pushMatrix();
  // enter n1 space
  scene.applyTransformation(n1);
  scene.draw(n1);
  // save state
  pushMatrix();
  // enter n2 space
  scene.applyTransformation(n2);
  scene.draw(n2);
  // restore space
  popMatrix();
  popMatrix();
}
```

this technique may also be useful when projecting the same subtree among several scenes, but it requires the node hierarchy to be known in advanced. Render the scene node hierarchy from its `eye` point-of-view, onto an arbitrary `PGraphics` with:

```processing
PGraphics pg;

void draw() {
  scene.render(pg);
}
```

see the [PostEffects](https://github.com/VisualComputing/nub/tree/master/examples/demos/PostEffects) example. Render the scene node hierarchy onto an arbitrary `PGraphics`, from an arbitrary node `viewPoint` with:

```processing
PGraphics pg;
Node viewPoint;
// frustum data
Scene.Type frustumType = Scene.Type.PERSPECTIVE;
float zNear, zFar;

void draw() {
  Scene.render(pg, frustumType, viewPoint, zNear, zFar);
}
```

see the [DepthMap](https://github.com/VisualComputing/nub/tree/master/examples/demos/DepthMap) and [ShadowMapping](https://github.com/VisualComputing/nub/tree/master/examples/demos/ShadowMapping) examples.  Render the scene node hierarchy onto an arbitrary `PGraphics`, from given `projection` and `view` matrices with:

```processing
PGraphics pg;
Matrix projection, view;

void draw() {
  Scene.render(pg, projection, view);
}
```

Render (and display) the off-screen scene node hierarchy from its `eye` point-of-view with:

```processing
void draw() {
  offScreenScene.beginDraw();
  offScreenScene.render();
  offScreenScene.endDraw();
  // display the rendered offScreenScene
  offScreenScene.display();
}
```

see the [SceneBuffers](https://github.com/VisualComputing/nub/blob/master/examples/basics/SceneBuffers/SceneBuffers.pde) example. Render (and display) the same node hierarchy among the scene and several off-screen scenes, each one from its own `eye` point-of-view, with:

```processing
void draw() {
  // 1. render onto the scene
  scene.render();
  // 2. render onto the off-screen scene
  offScreenScene.beginDraw();
  offScreenScene.render();
  offScreenScene.endDraw();
  // display the rendered offScreenScene
  offScreenScene.display();
}
```

see the [MiniMap](https://github.com/VisualComputing/nub/blob/master/examples/demos/MiniMap/MiniMap.pde) and the [ViewFrustumCulling](https://github.com/VisualComputing/nub/tree/master/examples/demos/ViewFrustumCulling) examples. Render 2D screen space stuff (such as gui elements and text) on top of a 3D scene with:

```processing
PGraphics pg;

void draw() {
 // save space and setup projection matrix for 2D drawing
 // https://manmohanbishnoi.wordpress.com/2014/12/02/2d-hud-graphics-over-3d-objects-in-modern-opengl/
 scene.beginHUD();
 // Use 2D screen space coordinates from here:
 image(pg, width / 2, height / 2);
 // restore space
 scene.endHUD();
}
```

see the [DepthMap](https://github.com/VisualComputing/nub/tree/master/examples/demos/DepthMap) example, among several others. Customize the rendering traversal algorithm by overriding the node `visit()` method, which will then be just called before its drawing routine, for example:

```processing
Scene.Visibility visibility;

@Override
public void visit() {
  switch (visibility) {
  case VISIBLE:
    for (Node node : children())
      node.cull();
    break;
  case SEMIVISIBLE:
    if (!children().isEmpty()) {
      // don't render the node...
      bypass();
      // ... but don't cull its children either
      for (Node node : children())
        node.cull(false);
    }
    break;
  case INVISIBLE:
    cull();
    break;
  }
}
```

see the [ViewFrustumCulling](https://github.com/VisualComputing/nub/tree/master/examples/demos/ViewFrustumCulling) example.

#### Drawing functionality

The [Scene](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html) implements several static drawing functions that complements those already provided by Processing, such as: [drawCylinder(PGraphics, int, float, float)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawCylinder-processing.core.PGraphics-int-float-float-), [drawHollowCylinder(PGraphics, int, float, float, Vector, Vector)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawHollowCylinder-processing.core.PGraphics-int-float-float-nub.primitives.Vector-nub.primitives.Vector-), [drawCone(PGraphics, int, float, float, float, float)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawCone-processing.core.PGraphics-int-float-float-float-float-), [drawCone(PGraphics, int, float, float, float, float, float)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawCone-processing.core.PGraphics-int-float-float-float-float-float-) and [drawTorusSolenoid(PGraphics, int, int, float, float)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawTorusSolenoid-processing.core.PGraphics-int-int-float-float-).

Drawing functions that take a `PGraphics` parameter (including the above static ones), such as [beginHUD(PGraphics)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#beginHUD-processing.core.PGraphics-),
[endHUD(PGraphics)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#endHUD-processing.core.PGraphics-), [drawAxes(PGraphics, float)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawAxes-processing.core.PGraphics-float-), [drawCross(PGraphics, float, float, float)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawCross-processing.core.PGraphics-float-float-float-) and [drawGrid(PGraphics)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawGrid-processing.core.PGraphics-) among others, can be used to set a node shape.

Another scene's eye (different than this one) can be drawn with [drawFrustum(Scene)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#drawFrustum-nub.core.Graph-). Typical usage include interactive [minimaps](https://en.wikipedia.org/wiki/Mini-map) and _visibility culling_ visualization and debugging.

## Interactivity

### Eye

The scene has several methods to position and orient the _eye_ node, such as: [lookAt(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#lookAt-nub.primitives.Vector-), [setFov(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setFOV-float-), [setViewDirection(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setViewDirection-nub.primitives.Vector-), [setUpVector(Vector)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#setUpVector-nub.primitives.Vector-), [fit()](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#fit--) and [fit(Node)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#fit-nub.core.Node-), among others.

The following scene methods implement _eye_ motion actions particularly suited for input devices, possibly having several degrees-of-freedom ([DOFs](https://en.wikipedia.org/wiki/Degrees_of_freedom_(mechanics))):

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

**n.a.** doesn't mean the mouse action isn't available, but that it can be implemented in several ways (see the code snippets below). The provided mouse actions got _non-ambiguously_ implemented by simply passing the *Processing* `pmouseX`, `pmouseY`,  `mouseX` and `mouseY` variables as parameters to their relative generic input device method counterparts (e.g., `mouseTranslateEye()` is the same as `translateEye(pmouseX - mouseX, pmouseY - mouseY, 0)` and `mouseSpinEye()` is the same as `spinEye(pmouseX, pmouseY, mouseX, mouseY)`), and hence their simpler signatures. 

Mouse and keyboard examples:

```processing
// define a mouse-dragged eye interaction
void mouseDragged() {
  if (mouseButton == LEFT)
    scene.mouseSpinEye();
  else if (mouseButton == RIGHT)
    scene.mouseTranslateEye();
  else
    // drag along x-axis: changes the scene field-of-view
    scene.scaleEye(scene.mouseDX());
}
```

```processing
// define a mouse-moved eye interaction
void mouseMoved(MouseEvent event) {
  if (event.isShiftDown())
    // move mouse along y-axis: roll
    // move mouse along x-axis: pitch
    scene.rotateEye(scene.mouseRADY(), scene.mouseRADX(), 0);
  else
    scene.mouseLookAround();
}
```

```processing
// define a mouse-wheel eye interaction
void mouseWheel(MouseEvent event) {
  if (scene.is3D())
    // move along z
    scene.moveForward(event.getCount() * 20);
  else
    // changes the eye scaling
    scene.scaleEye(event.getCount() * 20);
}
```

```processing
// define a mouse-click eye interaction
void mouseClicked(MouseEvent event) {
  if (event.getCount() == 1)
    scene.alignEye();
  else
    scene.focusEye();
}
```

```processing
// define a key-pressed eye interaction
void keyPressed() {
  // roll with 'x' key
  scene.rotateEye(key == 'x' ? QUARTER_PI / 2 : -QUARTER_PI / 2, 0, 0);
}
```

The [SpaceNavigator](https://github.com/VisualComputing/nub/tree/master/examples/basics/SpaceNavigator) and [CustomEyeInteraction](https://github.com/VisualComputing/nub/tree/master/examples/demos/CustomEyeInteraction) examples illustrate how to set up other hardware such as a keyboard or a full fledged 6-DOFs device like the [space-navigator](https://en.wikipedia.org/wiki/3Dconnexion).

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

Note that the mouse actions are implemented in a similar manner as it has been done with the [eye](#eye).

Mouse and keyboard examples:

```processing
void mouseDragged() {
  // spin n1
  if (mouseButton == LEFT)
    scene.spinNode(n1);
  // translate n3
  else if (mouseButton == RIGHT)
    scene.translateNode(n3);
  // scale n1
  else
    scene.scaleNode(n1, scene.mouseDX());
}
```

```processing
void keyPressed() {
  if (key == CODED)
    if(keyCode == UP)
      scene.translateNode(n2, 0, 10);
    if(keyCode == DOWN)
      scene.translateNode(n2, 0, -10);
}
```

Customize node behaviors by overriding the node [interact(Object...)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#interact-java.lang.Object...-) method and then invoke it with the scene [interactNode(Node, Object...)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#interactNode-nub.core.Node-java.lang.Object...-) method. See the [CustomNodeInteraction](https://github.com/VisualComputing/nub/blob/master/examples/demos/CustomNodeInteraction/CustomNodeInteraction.pde) example.

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
   
   1. **Tagged node**: `interactTag(tag, gesture...)` which simply calls `interactNode(node(tag), gesture)` using [node(String)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#node-java.lang.String-) to resolve the node parameter in the [node methods above](#nodes-1).
   2. **Tagged node or `eye`**: `interact(tag, gesture...)` which is the same as `if (!interactTag(tag, gesture...)) interactEye(gesture...)` i.e., To either interact with the node referred with a given tag (pattern **i.**) or delegate the gesture to the eye ([see above](#eye)) when that tag is not in use.
   
   Generic actions:

   | Action    | Tagged node                                            | Tagged node or `eye`                                |
   |-----------|--------------------------------------------------------|-----------------------------------------------------|
   | Align     | ```alignTag(tag)```                                    | ```align(tag)```                                    |
   | Focus     | ```focusTag(tag)```                                    | ```focus(tag)```                                    |
   | Translate | ```translateTag(tag, dx, dy, dz)```                    | ```translate(tag, dx, dy, dz)```                    |
   | Rotate    | ```rotateTag(tag, roll, pitch, yaw)```                 | ```rotate(tag, roll, pitch, yaw)```                 |
   | Scale     | ```scaleTag(tag, delta)```                             | ```scale(tag, delta)```                             |
   | Spin      | ```spinTag(tag, pixel1X, pixel1Y, pixel2X, pixel2Y)``` | ```spin(tag, pixel1X, pixel1Y, pixel2X, pixel2Y)``` |

   Mouse actions:

   | Action    | Tagged nodes                 | Tagged node or `eye`      |
   |-----------|------------------------------|---------------------------|
   | Translate | ```mouseTranslateTag(tag)``` | ```mouseTranslate(tag)``` |
   | Spin      | ```mouseSpinTag(tag)```      | ```mouseSpin(tag)```      |

Observations:

1. A node can have multiple tags but a given tag cannot be assigned to more than one node, and since the null tag is allowed, signatures of all the above methods lacking the tag parameter are provided for convenience, e.g., `mouseTag()` is equivalent to calling `mouseTag(null)` which in turn is equivalent to `tag(null, mouseX, mouseY)` (and `tag(mouseX, mouseY)`).
2. Refer to [pickingThreshold()](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#pickingThreshold--) and [setPickingThreshold(float)](https://visualcomputing.github.io/nub-javadocs/nub/core/Node.html#setPickingThreshold-float-) for the different ray-casting node picking policies.
3. To check if a given node would be picked with a ray cast at a given screen position, call [tracks(Node, int, int)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#tracks-nub.core.Node-int-int-) or [mouseTracks(Node)](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#mouseTracks-nub.core.Node-).
4. To tag the nodes in a given array with ray casting use [updateTag(String, int, int, Node[])](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#updateTag-java.lang.String-int-int-nub.core.Node:A-) and [updateMouseTag(String, Node[])](https://visualcomputing.github.io/nub-javadocs/nub/processing/Scene.html#updateMouseTag-java.lang.String-nub.core.Node:A-).
5. Invoke custom node behaviors by either calling the scene [interactNode(Node, Object...)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#interactNode-nub.core.Node-java.lang.Object...-), [interactTag(String, Object...)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#interactTag-java.lang.String-java.lang.Object...-) or [interactTag(Object...)](https://visualcomputing.github.io/nub-javadocs/nub/core/Graph.html#interactTag-java.lang.Object...-) methods. See the [CustomNodeInteraction](https://github.com/VisualComputing/nub/blob/master/examples/demos/CustomNodeInteraction/CustomNodeInteraction.pde) example.

Mouse and keyboard examples:

```processing
// pick with mouse-moved
void mouseMoved() {
  scene.mouseTag();
}

// interact with mouse-dragged
void mouseDragged() {
  if (mouseButton == LEFT)
    // spin the picked node or the eye if no node has been picked
    scene.mouseSpin();
  else if (mouseButton == RIGHT)
    // spin the picked node or the eye if no node has been picked
    scene.mouseTranslate();
  else
    // spin the picked node or the eye if no node has been picked
    scene.scale(mouseX - pmouseX);
}
```

```processing
// pick with mouse-clicked
void mouseClicked(MouseEvent event) {
  if (event.getCount() == 1)
    // use the null tag to manipulate the picked node with mouse-moved
    scene.mouseTag();
  if (event.getCount() == 2)
    // use the "key" tag to manipulate the picked node with key-pressed
    scene.mouseTag("key");
}

// interact with mouse-moved
void mouseMoved() {
  // spin the node picked with one click
  scene.mouseSpinTag();
}

// interact with key-pressed
void keyPressed() {
  // focus the node picked with two clicks
  scene.focusTag("key");
}
```

## Timing

### Timing tasks

[Timing tasks](https://visualcomputing.github.io/nub-javadocs/nub/processing/TimingTask.html) are (non)recurrent, (non)concurrent (see [isRecurrent()](https://visualcomputing.github.io/nub-javadocs/nub/timing/Task.html#isRecurrent--) and [isConcurrent()](https://visualcomputing.github.io/nub-javadocs/nub/timing/Task.html#isConcurrent--) resp.) callbacks defined by overriding [execute()](https://visualcomputing.github.io/nub-javadocs/nub/timing/Task.html#execute--). For example:

```processing
Scene scene;
void setup() {
  scene = new Scene(this);
  TimingTask spinningTask = new TimingTask(scene) {
    @Override
    public void execute() {
      scene.eye().orbit(new Vector(0, 1, 0), PI / 100);
    }
  };
  spinningTask.run();
}
```

will run the timing-task at 25Hz (which is its default [frequency()]()). See the [ParticleSystem](https://github.com/VisualComputing/nub/tree/master/examples/basics/ParticleSystem) example.

### Interpolators

An [interpolator](https://visualcomputing.github.io/nub-javadocs/nub/core/Interpolator.html) is a timing-task that allows to define the position, orientation and magnitude a node (including the eye) should have at a particular moment in time, a.k.a., [key-frame](https://en.wikipedia.org/wiki/Key_frame). When the interpolator is run the node is then animated through a [Catmull-Rom](https://en.wikipedia.org/wiki/Cubic_Hermite_spline#Catmull%E2%80%93Rom_spline) spline, matching in space-time the key-frames which defines it. Use code such as the following:

```processing
Scene scene;
PShape pshape;
Node shape;
Interpolator interpolator;
void setup() {
  ...
  shape = new Node(pshape);
  interpolator = new Interpolator(shape);
  for (int i = 0; i < random(4, 10); i++)
    // addKeyFrame(node, elapsedTime) where elapsedTime is defined respect
    // to the previously added key-frame and expressed in seconds.
    interpolator.addKeyFrame(scene.randomNode(), i % 2 == 1 ? 1 : 4);
  interpolator.run();
}
```

which will create a `shape` interpolator containing [4..10] random key-frames. The interpolation is also run. The interpolator trajectory may be drawn with code like this:

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
