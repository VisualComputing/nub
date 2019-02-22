FramesJS
[![All Contributors](https://img.shields.io/badge/all_contributors-2-orange.svg?style=flat-square)](#contributors)
===========================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================

**Table of Contents**

- [Description](#user-content-description)
- [Scene](#user-content-scene)
- [Frames](#user-content-frames)
- [Interpolators](#user-content-interpolators)
- [HIDs](#user-content-hids)
- [Control](#user-content-control)
- [IK](#user-content-ik)
- [Drawing](#user-content-drawing)
- [Installation](#user-content-installation)
- [Contributors](#user-content-contributors)

## Description

[Frames](http://visualcomputing.github.io/Transformations/#/6) is a simple, expressive, language-agnostic, and extensible [(2D/3D) scene graph](https://en.wikipedia.org/wiki/Scene_graph) featuring interaction, visualization and animation frameworks and supporting advanced (onscreen/offscreen) rendering techniques, such as [view frustum culling](http://cgvr.informatik.uni-bremen.de/teaching/cg_literatur/lighthouse3d_view_frustum_culling/index.html).

*Frames* is meant to be coupled with third party real and non-real time [renderers](https://en.wikipedia.org/wiki/3D_rendering). Our current [release](https://github.com/VisualComputing/frames/releases) supports all major [Processing](https://processing.org/) desktop renderers: [2D and 3D PGraphicsOpenGL (a.k.a. P2D and P3D, respectively)](https://processing.github.io/processing-javadocs/core/processing/opengl/PGraphicsOpenGL.html), [PGraphicsJava2D (a.k.a. JAVA2D)](https://processing.github.io/processing-javadocs/core/processing/awt/PGraphicsJava2D.html) and [PGraphicsFX2D (a.k.a. FX2D)](https://processing.github.io/processing-javadocs/core/processing/javafx/PGraphicsFX2D.html).

If looking for the API docs, check them [here](https://visualcomputing.github.io/frames-javadocs/).

Readers unfamiliar with geometry transformations may first check the great [Processing 2D transformations tutorial](https://processing.org/tutorials/transform2d/) by _J David Eisenberg_ and this [presentation](http://visualcomputing.github.io/Transformations/#/6) that discusses some related formal foundations.

## Scene

Instantiate your on-screen scene at the [setup()](https://processing.org/reference/setup_.html):

```processing
Scene scene;
void setup() {
  scene = new Scene(this);
}
```

The [Scene](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html) [frontBuffer()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#frontBuffer--) corresponds to the *PApplet* main `PGraphics` instance.

Off-screen scenes should be instantiated upon a [PGraphics](https://processing.org/reference/PGraphics.html) object:

```processing
Scene scene;
void setup() {
  scene = new Scene(this, createGraphics(500, 500, P3D));
}
```

In this case, the [Scene](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html) [frontBuffer()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#frontBuffer--) corresponds to the `PGraphics` instantiated with [createGraphics()](https://processing.org/reference/createGraphics_.html) (which is of course different than the *PApplet* main `PGraphics` instance).

## Frames

A node is a coordinate system that may be translated, rotated and scaled. [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) instances define each of the nodes comprising a scene graph. To illustrate their use, suppose the following scene graph is being implemented:

```processing
 World
  ^
  |\
  1 eye
  ^
  |\
  2 3
```

To setup the scene hierarchy of _attached_ nub, i.e., nub belonging to the scene, use code such as the following:

```processing
Scene scene;
Frame f1, f2, f3;
void setup() {
  scene = new Scene(this);
  // To attach a leading-node (those whose parent is the world, such as f1)
  // the scene parameter is passed to the Frame constructor:
  f1 = new Frame(scene);
  // whereas for the remaining nub we pass any constructor taking a
  // reference node paramater, such as Frame(Frame referenceFrame)
  f2 = new Frame(f1) {
    // immediate mode rendering procedure
    // defines f2 visual representation
    @Override
    public boolean graphics(PGraphics pg) {
      Scene.drawTorusSolenoid(pg);
      return true;
    }
  };
  // retained-mode rendering PShape
  // defines f3 visual representation
  f3 = new Frame(f1, createShape(BOX, 60));
}
```

Some advantages of using _attached_ nub are:

* The [Scene](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html) sets up a default _eye_ node. To set the eye from an arbitrary [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) instance call [setEye(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#setEye-frames.core.Frame-). To retrieve the scene _eye_ instance call [eye()](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#eye--).
* A node shape can be set from an [immediate-mode](https://en.wikipedia.org/wiki/Immediate_mode_(computer_graphics)) rendering Processing procedure (see [graphics(PGraphics)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#graphics-processing.core.PGraphics-)) or from a [retained-mode](https://en.wikipedia.org/wiki/Retained_mode) rendering Processing [PShape](https://processing.org/reference/PShape.html) (see [shape(PShape)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#shape-processing.core.PShape-)). Frame shapes can be picked precisely using their projection onto the screen, see [pickingThreshold()](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#pickingThreshold--).
* Even the _eye_ can have a shape which may be useful to depict the viewer in first person camera style.
* The scene topology is set (even at run time) with [setReference(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setReference-frames.core.Frame-).
* The [Scene](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html) methods [location(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#location-frames.primitives.Vector-frames.core.Frame-) and [screenLocation(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#screenLocation-frames.primitives.Vector-frames.core.Frame-) transforms coordinates between node and screen space.
* The node methods [setTranslation(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setTranslation-frames.primitives.Vector-), [translate(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#translate-frames.primitives.Vector-), [setRotation(Quaternion)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setRotation-frames.primitives.Quaternion-), [rotate(Quaternion)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#rotate-frames.primitives.Quaternion-), [setScaling(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setScaling-float-) and [scale(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#scale-float-), locally manipulates a node instance.
* The [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) methods [setPosition(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setPosition-frames.primitives.Vector-), [setOrientation(Quaternion)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setOrientation-frames.primitives.Quaternion-), and [setMagnitude(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setMagnitude-float-), globally manipulates a node instance.
* The [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) methods [location(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#location-frames.primitives.Vector-frames.core.Frame-) and [displacement(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#displacement-frames.primitives.Vector-frames.core.Frame-) transforms coordinates and vectors (resp.) from other node instances.
* The [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) methods [worldLocation(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#worldLocation-frames.primitives.Vector-) and [worldDisplacement(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#worldDisplacement-frames.primitives.Vector-) transforms node coordinates and vectors (resp.) to the world.
* The [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) method [setConstraint(Constrain)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setConstraint-frames.core.constraint.Constraint-) applies a [Constraint](https://visualcomputing.github.io/frames-javadocs/frames/primitives/constraint/Constraint.html) to a node instance limiting its motion.

### Interactivity

To set the scene [tracked-node](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#trackedFrame--) (the node the mouse should interact with) call [setTrackedFrame(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#setTrackedFrame-frames.core.Frame-) or update it using ray-casting with [cast()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#cast--), for example:

```processing
void mouseMoved() {
  // the tracked-node is updated using ray-casting from the set of scene attached nub
  scene.cast();
}
```

To interact with a given node use any [Scene](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html) method that takes a [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) parameter, such as: [spin(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#spin-frames.core.Frame-), [translate(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#translate-frames.core.Frame-) or [scale(float, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#scale-float-frames.core.Frame-) For example:

```processing
public void mouseDragged() {
  // spin f1
  if (mouseButton == LEFT)
    scene.spin(f1);
  // translate f3
  else if (mouseButton == RIGHT)
    scene.translate(f3);
  // scale f2
  else
    scene.scale(scene.mouseDX(), f2);
}
```

To interact with the [default-node](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#defaultFrame--) (which is either the tracked-node updated with the `mouseMoved` above or the scene _eye_ when the tracked-node is null) use the _frameless_ versions of the above methods, e.g., [spin()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#spin--), [translate()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#translate--) or [scale(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#scale-float-). For example:

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

See the [CajasOrientadas example](https://github.com/VisualComputing/frames/tree/master/examples/basics/CajasOrientadas).

### Rendering

Render the node hierarchy into the [frontBuffer()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#frontBuffer--) with:

```processing
void draw() {
  // calls visit() on each shape to draw the shape
  scene.render();
}
```

observe that:

* The scene gets rendered respect to the scene [eye()](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#eye--) node.
* Call [render(PGraphics)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#render-java.lang.Object-) to render the scene into an arbitrary _PGraphics_ context. See the [DepthOfField example](https://github.com/VisualComputing/framesjs/tree/master/examples/basics/DepthOfField).
* Call [render(PGraphics, Graph.Type, Frame, zNear, zFar)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#render-processing.core.PGraphics-frames.core.Graph.Type-frames.core.Frame-float-float-) to render the scene into an arbitrary _PGraphics_ context from an arbitrary node point-of-view. See the [ShadowMap example](https://github.com/VisualComputing/framesjs/tree/processing/examples/demos/ShadowMap).
* The role played by a [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) instance during a scene graph traversal is implemented by overriding its [visit()](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#visit--) method.

To bypass the [render()](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#render--) algorithm use [detached nub](detached.md), or override [visit()](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#visit--) to setup a _cullingCondition_ for the node as follows (see [visit()](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#visit--), [cull(boolean)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#cull-boolean-) and [isCulled()](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#isCulled--)):

```processing
Scene scene;
Frame node;
void setup() {
  scene = new Scene(this);
  node = new Frame(scene) {
    @Override
    public void visit() {
      // Hierarchical culling is optional and disabled by default. When the cullingCondition
      // (which should be implemented by you) is true, scene.render() will prune the branch at the node
      cull(cullingCondition);
      if(!isCulled())
        // Draw your object here, in the local coordinate system.
    }
  }
}
```

## Interpolators

A node can be animated through a [key-node](https://en.wikipedia.org/wiki/Key_frame) [Catmull-Rom](https://en.wikipedia.org/wiki/Cubic_Hermite_spline#Catmull%E2%80%93Rom_spline) [interpolator](https://visualcomputing.github.io/frames-javadocs/frames/core/Interpolator.html) path. Use code such as the following:

```processing
Scene scene;
PShape pshape;
Shape shape;
Interpolator interpolator;
void setup() {
  ...
  shape = new Shape(scene, pshape);
  interpolator = new Interpolator(shape);
  for (int i = 0; i < random(4, 10); i++)
    interpolator.addKeyFrame(scene.randomFrame());
  interpolator.start();
}
```

which will create a random interpolator path containing [4..10] key-nub. The interpolation is also started. The interpolator path may be drawn with code like this:

```processing
...
void draw() {
  scene.render();
  scene.drawPath(interpolator);
}
```

while [render()](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#render--) will draw the animated shape(s) [drawPath(Interpolator)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawPath-frames.core.Interpolator-) will draw the interpolated path too. See the [Interpolators example](https://github.com/VisualComputing/frames/tree/master/examples/basics/Interpolators).

## HIDs

Setting up a [Human Interface Device (hid)](https://en.wikipedia.org/wiki/Human_interface_device) (different than the mouse which is provided by default) such as a keyboard or a [space-navigator](https://en.wikipedia.org/wiki/3Dconnexion), is a two step process:

1. Define an _hid_ tracked-node instance, using an arbitrary name for it (see [setTrackedFrame(String, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#setTrackedFrame-java.lang.String-frames.core.Frame-)); and,
2. Call any interactivity method that take an _hid_ param (such as [translate(String, float, float, float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#translate-java.lang.String-float-float-), [rotate(String, float, float, float)]() or [scale(String, float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#scale-java.lang.String-float-) following the name convention you defined in 1.

See the [SpaceNavigator example](https://github.com/VisualComputing/frames/tree/master/examples/demos/SpaceNavigator).

Observations:

1. An _hid_ tracked-node (see [trackedFrame(String)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#trackedFrame-java.lang.String-)) defines in turn an _hid_ default-node (see [defaultFrame(String)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#defaultFrame-java.lang.String-)) which simply returns the tracked-node or the scene _eye_ when the _hid_ tracked-node is `null`
2. The _hid_ interactivity methods are implemented in terms of the ones defined previously by simply passing the _hid_ [defaultFrame(String)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#defaultFrame-java.lang.String-) to them.
3. The default _hid_ is defined with a `null` String parameter (e.g., [scale(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#scale-float-) simply calls `scale(null, delta)`). The _Scene_ default mouse _hid_ presented in the [Frames](#user-content-frames) section is precisely implemented is this manner.
4. To update an _hid_ tracked-node using ray-casting call [track(String, Point, Frame[])](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#track-java.lang.String-frames.primitives.Point-frames.core.Frame:A-) (detached or attached nub), [track(String, Point)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#track-java.lang.String-frames.primitives.Point-) (only attached nub) or [cast(String, Point)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#cast-java.lang.String-frames.primitives.Point-) (only for attached nub too). While [track(String, Point, Frame[])](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#track-java.lang.String-frames.primitives.Point-frames.core.Frame:A-) and [track(String, Point)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#track-java.lang.String-frames.primitives.Point-) update the _hid_ tracked-node synchronously (i.e., they return the _hid_ tracked-node immediately), [cast(String, Point)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#cast-java.lang.String-frames.primitives.Point-) updates it asynchronously (i.e., it optimally updates the _hid_ tracked-node during the next call to the [render()](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#render--) algorithm).

## Control

[Application control](https://hal.inria.fr/hal-00789413/) (aka as Post-[WIMP](https://en.wikipedia.org/wiki/WIMP_(computing)) interaction styles) refers to interfaces “containing at least one interaction technique not dependent on classical 2D widgets” [[van Dam]](http://dl.acm.org/citation.cfm?id=253708), such as:  [tangible interaction](https://en.wikipedia.org/wiki/Tangible_user_interface), or perceptual and [affective computing](https://en.wikipedia.org/wiki/Affective_computing).

Implementing an application control for a node is a two step process:

1. Override the node method [interact(Object...)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#interact-java.lang.Object...-) to parse the gesture into a custom (application) control.
2. Send gesture data to the node by calling one of the following scene methods: [defaultHIDControl(Object...)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#defaultHIDControl-java.lang.Object...-), [control(String, Object...)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#control-java.lang.String-java.lang.Object...-) or [control(Frame, Object...)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#control-frames.core.Frame-java.lang.Object...-).

See the [ApplicationControl example](https://github.com/VisualComputing/frames/tree/master/examples/demos/ApplicationControl).

## IK

## Drawing

The [Scene](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html) implements several static drawing functions that complements those already provided by Processing, such as: [drawCylinder(PGraphics, int, float, float)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawCylinder-processing.core.PGraphics-int-float-float-), [drawHollowCylinder(PGraphics, int, float, float, Vector, Vector)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawHollowCylinder-processing.core.PGraphics-int-float-float-frames.primitives.Vector-frames.primitives.Vector-), [drawCone(PGraphics, int, float, float, float, float)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawCone-processing.core.PGraphics-int-float-float-float-float-), [drawCone(PGraphics, int, float, float, float, float, float)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawCone-processing.core.PGraphics-int-float-float-float-float-float-) and [drawTorusSolenoid(PGraphics, int, int, float, float)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawTorusSolenoid-processing.core.PGraphics-int-int-float-float-).

Drawing functions that take a `PGraphics` parameter (including the above static ones), such as [beginHUD(PGraphics)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#beginHUD-processing.core.PGraphics-),
[endHUD(PGraphics)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#endHUD-processing.core.PGraphics-), [drawAxes(PGraphics, float)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawAxes-processing.core.PGraphics-float-), [drawCross(PGraphics, float, float, float)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawCross-processing.core.PGraphics-float-float-float-) and [drawGrid(PGraphics)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawGrid-processing.core.PGraphics-) among others, can be used to set a ([Shape](#user-content-shapes)).

Another scene's eye (different than this one) can be drawn with [drawEye(Graph)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawEye-frames.core.Graph-). Typical usage include interactive [minimaps](https://en.wikipedia.org/wiki/Mini-map) and _visibility culling_ visualization and debugging.

## Installation

Import/update it directly from your PDE. Otherwise download your [release](https://github.com/VisualComputing/frames/releases) and extract it to your sketchbook `libraries` folder.

## Contributors

Thanks goes to these wonderful people ([emoji key](https://github.com/kentcdodds/all-contributors#emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore -->
| [<img src="https://avatars2.githubusercontent.com/u/9769647?v=4" width="100px;"/><br /><sub><b>sechaparroc</b></sub>](https://github.com/sechaparroc)<br />[📝](#blog-sechaparroc "Blogposts") [🐛](https://github.com/VisualComputing/framesjs/issues?q=author%3Asechaparroc "Bug reports") [💻](https://github.com/VisualComputing/framesjs/commits?author=sechaparroc "Code") [🎨](#design-sechaparroc "Design") [📖](https://github.com/VisualComputing/framesjs/commits?author=sechaparroc "Documentation") [📋](#eventOrganizing-sechaparroc "Event Organizing") [💡](#example-sechaparroc "Examples") [💵](#financial-sechaparroc "Financial") [🔍](#fundingFinding-sechaparroc "Funding Finding") [🤔](#ideas-sechaparroc "Ideas, Planning, & Feedback") [📦](#platform-sechaparroc "Packaging/porting to new platform") [🔌](#plugin-sechaparroc "Plugin/utility libraries") [💬](#question-sechaparroc "Answering Questions") [👀](#review-sechaparroc "Reviewed Pull Requests") [📢](#talk-sechaparroc "Talks") [⚠️](https://github.com/VisualComputing/framesjs/commits?author=sechaparroc "Tests") [✅](#tutorial-sechaparroc "Tutorials") [📹](#video-sechaparroc "Videos") | [<img src="https://avatars2.githubusercontent.com/u/645599?v=4" width="100px;"/><br /><sub><b>Jean Pierre Charalambos</b></sub>](https://github.com/nakednous)<br />[📝](#blog-nakednous "Blogposts") [🐛](https://github.com/VisualComputing/framesjs/issues?q=author%3Anakednous "Bug reports") [💻](https://github.com/VisualComputing/framesjs/commits?author=nakednous "Code") [🎨](#design-nakednous "Design") [📖](https://github.com/VisualComputing/framesjs/commits?author=nakednous "Documentation") [📋](#eventOrganizing-nakednous "Event Organizing") [💡](#example-nakednous "Examples") [💵](#financial-nakednous "Financial") [🔍](#fundingFinding-nakednous "Funding Finding") [🤔](#ideas-nakednous "Ideas, Planning, & Feedback") [📦](#platform-nakednous "Packaging/porting to new platform") [🔌](#plugin-nakednous "Plugin/utility libraries") [💬](#question-nakednous "Answering Questions") [👀](#review-nakednous "Reviewed Pull Requests") [📢](#talk-nakednous "Talks") [⚠️](https://github.com/VisualComputing/framesjs/commits?author=nakednous "Tests") [✅](#tutorial-nakednous "Tutorials") [📹](#video-nakednous "Videos") |
| :---: | :---: |
<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/kentcdodds/all-contributors) specification. Contributions of any kind welcome!
