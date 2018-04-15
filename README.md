frames
[![All Contributors](https://img.shields.io/badge/all_contributors-2-orange.svg?style=flat-square)](#contributors)
===========================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================

**Table of Contents**

- [Description](#user-content-description)
- [Usage](#user-content-usage)
- [Interpolators](#user-content-interpolators)
- [Interactivity](#user-content-interactivity)
- [IK](#user-content-ik)
- [Drawing](#user-content-drawing)
- [Installation](#user-content-installation)
- [Contributors](#user-content-contributors)

## Description

*Frames* (like in [here](https://en.wikipedia.org/wiki/Frame_of_reference)) is a simple, expressive, language-agnostic, and extensible [(2D/3D) scene graph](https://en.wikipedia.org/wiki/Scene_graph) featuring interaction, inverse kinematics, visualization and animation frameworks and supporting advanced (onscreen/offscreen) rendering techniques, such as [view frustum culling](http://cgvr.informatik.uni-bremen.de/teaching/cg_literatur/lighthouse3d_view_frustum_culling/index.html).

*Frames* is meant to be coupled with third party real and non-real time [renderers](https://en.wikipedia.org/wiki/3D_rendering). The _processing git branch_ (the one you're looking at) supports all major [Processing](https://processing.org/) desktop renderers: [2D and 3D PGraphicsOpenGL (a.k.a. P2D and P3D, respectively)](https://processing.github.io/processing-javadocs/core/processing/opengl/PGraphicsOpenGL.html), [PGraphicsJava2D (a.k.a. JAVA2D)](https://processing.github.io/processing-javadocs/core/processing/awt/PGraphicsJava2D.html) and [PGraphicsFX2D (a.k.a. FX2D)](https://processing.github.io/processing-javadocs/core/processing/javafx/PGraphicsFX2D.html).

If looking for the API docs, check them [here](https://visualcomputing.github.io/frames-javadocs/).

## Usage

Typical usage comprises three steps: scene instantiation, setting an eye and setting some shapes.

### Scene instantiation

Instantiate your on-screen scene at the [setup()](https://processing.org/reference/setup_.html):

```java
Scene scene;
void setup() {
  scene = new Scene(this);
}
```

The `scene` [frontBuffer()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#frontBuffer--) corresponds to the *PApplet* main canvas.
 
Off-screen scenes should be instantiated upon a [PGraphics](https://processing.org/reference/PGraphics.html) object:

```java
Scene scene;
PGraphics canvas;
void setup() {
  canvas = createGraphics(500, 500, P3D);
  scene = new Scene(this, canvas);
}
```

In this case, the `scene` [frontBuffer()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#frontBuffer--) corresponds to the `canvas`.

### The eye

The default scene eye is a [Frame](https://visualcomputing.github.io/frames-javadocs/frames/primitives/Frame.html) instance, but it can also be a [Node](https://visualcomputing.github.io/frames-javadocs/frames/core/Node.html) one.

To set the eye from whatever `frame` instance, different than the default one, use code such as the following:

```java
...
Frame eye;
void setup() {
  ...
  eye = new Frame();
  scene.setEye(eye);
}
```

The eye can be controlled programmatically using the powerful [Frame](https://visualcomputing.github.io/frames-javadocs/frames/primitives/Frame.html) API.

To set the eye from a node instance use code such as the following:

```java
...
Node eye;
void setup() {
  ...
  eye = new Node(scene) {
    @Override
    public void interact(Event event) {
      // translate the node from a LEFT mouse button drag
      if (event.shortcut().matches(new Shortcut(PApplet.LEFT)))
        translate(event);
    }
  };
  scene.setEye(eye);
  scene.setDefaultGrabber(eye);
}
```

The eye can be controlled both programmatically (since a [Node](https://visualcomputing.github.io/frames-javadocs/frames/core/Node.html) is a [Frame](https://visualcomputing.github.io/frames-javadocs/frames/primitives/Frame.html) specialization) and interactively (using the mouse, see [mouse()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#mouse--) and [Mouse](https://visualcomputing.github.io/frames-javadocs/frames/processing/Mouse.html)). Note that the node [interactivity](#user-content-interactivity) itself is programmable.

Observe the anonymous inner [Node](https://visualcomputing.github.io/frames-javadocs/frames/core/Node.html) class intance which is used to define how the node will behave: actions are bound to the node using the event `Shortcut` attribute which identifies it. For instance `Shortcut(PApplet.LEFT))` tells us the `event` is a `MotionEvent2` mouse left drag (refer to the [Node](https://visualcomputing.github.io/frames-javadocs/frames/core/Node.html) API for details).

Note also the [setDefaultGrabber(Grabber)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#setDefaultGrabber-frames.input.Grabber-) call which will direct user input (e.g., mouse) to the eye when no other node is being picked.

### Shapes

A [Shape](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html) is a [Node](https://visualcomputing.github.io/frames-javadocs/frames/core/Node.html) specialization that can be set from a [retained-mode](https://en.wikipedia.org/wiki/Retained_mode) rendering Processing [PShape](https://processing.org/reference/PShape.html) or from an [immediate-mode](https://en.wikipedia.org/wiki/Immediate_mode_(computer_graphics)) rendering Processing procedure. Shapes can be picked precisely using their projection onto the screen, see [setPrecision(Node.Precision)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html#setPrecision-frames.core.Node.Precision-). Use [traverse()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#traverse--) to render all scene-graph shapes or [draw()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html#draw--) to render a specific one instead.

#### Retained-mode shapes

To set a retained-mode shape use `Shape shape = new Shape(Scene scene, PShape shape)` or `Shape shape = new Shape(Scene scene)` and then call [Shape.set(PShape)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html#set-processing.core.PShape-).

#### Immediate-mode shapes

Immediate-mode shapes should override `Shape.set(PGraphics)`, e.g., using an anonymous inner [Shape](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html#set-processing.core.PShape-) class intance, such as with the following:
 
```java
...
Shape shape;
void setup() {
  ...
  shape = new Shape(scene) {
    @Override
    protected void set(PGraphics canvas) {
      //immediate-mode rendering procedure
    }
  };
}
```

Note that shapes like nodes can be controlled interactively by overriding [interact(Event)](https://visualcomputing.github.io/frames-javadocs/frames/core/Node.html#interact-frames.input.Event-) (like it has been done above) and, like frames, they can be set as the scene eye which may be useful to depict the viewer in first person camera style.

Note also that shapes override the `Node` [visit](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html#visit--) method to actually render its contents.

## Interpolators

A frame (and hence a node or a shape) can be animated through a [key-frame](https://en.wikipedia.org/wiki/Key_frame) [Catmull-Rom](https://en.wikipedia.org/wiki/Cubic_Hermite_spline#Catmull%E2%80%93Rom_spline) interpolator path. Use code such as the following:

```java
Scene scene;
PShape pshape;
Shape shape;
Interpolator interpolator;
void setup() {
  ...
  shape = new Shape(scene, pshape);
  interpolator = new Interpolator(shape);
  for (int i = 0; i < random(4, 10); i++)
    interpolator.addKeyFrame(Node.random(scene));
  interpolator.start();
}
```

which will create a random interpolator path containing [4..10] key-frames. The interpolation is also started. The interpolator path may be drawn with code like this:

```java
...
void draw() {
  scene.traverse();
  scene.drawPath(interpolator, 5);
}
```

while `traverse()` will draw the animated shape(s) `drawPath(Interpolator, int)` will draw the interpolated path too.
 
## Interactivity
 
To control your scene nodes by [means](https://en.wikipedia.org/wiki/Human_interface_device) different than the [mouse()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#mouse--) (see [Mouse](https://visualcomputing.github.io/frames-javadocs/frames/processing/Mouse.html)), implement an [Agent](https://visualcomputing.github.io/frames-javadocs/frames/input/Agent.html) and call [registerAgent(Agent)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#registerAgent-frames.input.Agent-).

## IK

The goal of Inverse Kinematics (IK for short) is to animate a [Skeleton](https://en.wikipedia.org/wiki/Skeletal_animation), which is a rigid body system composed of Joints and Bones.

As a Skeleton could be built using a hierarchical tree structure where each Node keeps information about Joints and Bones configuration by means of relative geometric transformations (at least rotation and translation) we could consider a Skeleton as a subset of the scene graph.

There are some positions of interest at the Skeleton called End Effectors which depends on the Joint configuration and that an animator would like to manipulate in order to reach some given Target positions and obtain the desired pose.

[Forward Kinematics](https://en.wikipedia.org/wiki/Forward_kinematics#Computer_animation) attempts to obtain the position of an end-effector from some given values for the joint parameters (usually rotation information). This task could be done easily thanks to the scene graph structure provided by Frames.

[Inverse Kinematics](https://en.wikipedia.org/wiki/Inverse_kinematics#Inverse_kinematics_and_3D_animation) on the other hand is a harder problem that attempts to obtain the joint parameters given the desired pose.

This short [video](https://www.youtube.com/watch?v=euFe1S0OltQ) summarizes the difference between this two problems.

Whenever IK is required to be solved follow the next steps:

#### Define the Skeleton (List or Hierarchy of Joints) 

The Skeleton must be a branch from a `Graph` (for instance see [`branch(Node)`](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#branch-frames.core.Node-)). Where each of the leaf nodes (i.e Nodes without children) could be treated as an End Effector.

#### Define the Target(s)
Create the Target(s) that the End Effector(s) must Follow. A Target is a `Node` that indicates which is de desired position of an End Effector. It's important to instantiate the Target before the Skeleton.

#### Register a Solver
Once you have specified a Skeleton and the Target(s). It is required to register a Solver task managed by the `scene`. To do this call `registerTreeSolver(Node)` method, where `Node` is the root of the Skeleton.

#### Relate Target(s) and End Effector(s)
You must specify explicitly which leaf `node` (End Effector) is related to which target `node`. To do this call scene method `addIKTarget(Node, Node)`. Where the first node is the End Effector, whereas the second one is the Target.

Optionally, it is usual to set initial target(s) position to be the same as end effector(s) position.

Assuming that the Skeleton is determined by `branch(Node)` and it is a single chain (i.e each `Node` has as much one child) the code must look like:

```java
...
void setup() {
  Node target = new Node(scene);
  Node root = new Node(scene);
  ...
  ArrayList<Node> chain = scene.branch(root);
  Node endEffector = chain.get(chain.size()-1);
  ...
  scene.registerTreeSolver(root);
  target.setPosition(endEffector.position());  
  scene.addIKTarget(endEffector, target);
}
```
#### Using constraints

It is desirable to obtain natural, predictable and intuitive poses when End Effectors are manipulated, however it could exist many solutions (i.e many different poses) that satisfy the IK problem. In such cases you could add constraints to some `nodes` (see [`setConstraint(Constraint)`](https://visualcomputing.github.io/frames-javadocs/frames/primitives/Frame.html#setConstraint-frames.primitives.constraint.Constraint-)) in the Skeleton to improve the `Solver` performance (e.g see [Human Skeleton](https://en.wikipedia.org/wiki/Synovial_joint#/media/File:909_Types_of_Synovial_Joints.jpg)). Currently the supported rotation constraints for Kinematics are:

* **`Hinge`**:   1-DOF constraint. i.e the joint will rotate just in one direction defined by a given Axis (Called Twist Axis).
* **`BallAndSocket`**: 3-DOF constraint. i.e the joint could rotate in any direction but the twist axis (defined by the user) must remain inside a cone with an elliptical base. User must specify ellipse semi-axis to constraint the movement. (for further info look [here](http://wiki.roblox.com/index.php?title=Inverse_kinematics))
* **`PlanarPolygon`**: As Ball and Socket, is a 3-DOF constraint. i.e the joint could rotate in any direction but the twist axis (defined by the user) must remain inside a cone with a polygonal base. A set of vertices on XY plane in Clockwise or Counter Clockwise order must be given to constraint the movement.
 * **`SphericalPolygon`**: As Ball and Socket, is a 3-DOF constraint. i.e the joint could rotate in any direction but the twist axis (defined by the user) must remain inside a cone defined by an spherical polygon. A set of vertices that lies in a unit sphere must be given in Counter Clockwise order  to constraint the movement. (for further info look [here]( https://pdfs.semanticscholar.org/d535/e562effd08694821ea6a8a5769fe10ffb5b6.pdf))

## Drawing

The [Scene](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html) implements several static drawing functions that complements those already provided by Processing, such as: `drawCylinder(PGraphics, int, float, float)}`, `drawHollowCylinder(PGraphics, int, float, float, Vector, Vector)`, `drawCone(PGraphics, int, float, float, float, float)`, `drawCone(PGraphics, int, float, float, float, float, float)` and `drawTorusSolenoid(PGraphics, int, int, float, float)`.

Drawing functions that take a `PGraphics` parameter (including the above static ones), such as `beginScreenCoordinates(PGraphics)`,
`endScreenCoordinates(PGraphics)`, `drawAxes(PGraphics, float)`, `drawCross(PGraphics, float, float, float)` and `drawGrid(PGraphics)` among others, can be used to set a `Shape` (see [set(PGraphics)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html#set-processing.core.PShape-)).

Another scene's eye (different than this one) can be drawn with `drawEye(Graph)`. Typical usage include interactive [minimaps](https://en.wikipedia.org/wiki/Mini-map) and _visibility culling_ visualization and debugging.

## Installation

Import/update it directly from your PDE. Otherwise download your release from [here](https://github.com/VisualComputing/framesjs/releases) and extract it to your sketchbook `libraries` folder.


## Contributors

Thanks goes to these wonderful people ([emoji key](https://github.com/kentcdodds/all-contributors#emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore -->
| [<img src="https://avatars2.githubusercontent.com/u/9769647?v=4" width="100px;"/><br /><sub><b>sechaparroc</b></sub>](https://github.com/sechaparroc)<br />[📝](#blog-sechaparroc "Blogposts") [🐛](https://github.com/VisualComputing/framesjs/issues?q=author%3Asechaparroc "Bug reports") [💻](https://github.com/VisualComputing/framesjs/commits?author=sechaparroc "Code") [🎨](#design-sechaparroc "Design") [📖](https://github.com/VisualComputing/framesjs/commits?author=sechaparroc "Documentation") [📋](#eventOrganizing-sechaparroc "Event Organizing") [💡](#example-sechaparroc "Examples") [💵](#financial-sechaparroc "Financial") [🔍](#fundingFinding-sechaparroc "Funding Finding") [🤔](#ideas-sechaparroc "Ideas, Planning, & Feedback") [📦](#platform-sechaparroc "Packaging/porting to new platform") [🔌](#plugin-sechaparroc "Plugin/utility libraries") [💬](#question-sechaparroc "Answering Questions") [👀](#review-sechaparroc "Reviewed Pull Requests") [📢](#talk-sechaparroc "Talks") [⚠️](https://github.com/VisualComputing/framesjs/commits?author=sechaparroc "Tests") [✅](#tutorial-sechaparroc "Tutorials") [📹](#video-sechaparroc "Videos") | [<img src="https://avatars2.githubusercontent.com/u/645599?v=4" width="100px;"/><br /><sub><b>Jean Pierre Charalambos</b></sub>](https://github.com/nakednous)<br />[📝](#blog-nakednous "Blogposts") [🐛](https://github.com/VisualComputing/framesjs/issues?q=author%3Anakednous "Bug reports") [💻](https://github.com/VisualComputing/framesjs/commits?author=nakednous "Code") [🎨](#design-nakednous "Design") [📖](https://github.com/VisualComputing/framesjs/commits?author=nakednous "Documentation") [📋](#eventOrganizing-nakednous "Event Organizing") [💡](#example-nakednous "Examples") [💵](#financial-nakednous "Financial") [🔍](#fundingFinding-nakednous "Funding Finding") [🤔](#ideas-nakednous "Ideas, Planning, & Feedback") [📦](#platform-nakednous "Packaging/porting to new platform") [🔌](#plugin-nakednous "Plugin/utility libraries") [💬](#question-nakednous "Answering Questions") [👀](#review-nakednous "Reviewed Pull Requests") [📢](#talk-nakednous "Talks") [⚠️](https://github.com/VisualComputing/framesjs/commits?author=nakednous "Tests") [✅](#tutorial-nakednous "Tutorials") [📹](#video-nakednous "Videos") |
| :---: | :---: |
<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/kentcdodds/all-contributors) specification. Contributions of any kind welcome!
