Frames
[![All Contributors](https://img.shields.io/badge/all_contributors-0-orange.svg?style=flat-square)](#contributors)
===========================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================

**Table of Contents**

- [Description](#user-content-description)
- [Scene](#user-content-scene)
- [Frames](#user-content-frames)
- [Shapes](#user-content-shapes)
- [Interpolators](#user-content-interpolators)
- [HIDs](#user-content-hids)
- [Control](#user-content-control)
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

```java
Scene scene;
void setup() {
  scene = new Scene(this);
}
```

The [Scene](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html) [frontBuffer()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#frontBuffer--) corresponds to the *PApplet* main `PGraphics` instance.
 
Off-screen scenes should be instantiated upon a [PGraphics](https://processing.org/reference/PGraphics.html) object:

```java
Scene scene;
void setup() {
  scene = new Scene(this, createGraphics(500, 500, P3D));
}
```

In this case, the [Scene](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html) [frontBuffer()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#frontBuffer--) corresponds to the `PGraphics` instantiated with [createGraphics()](https://processing.org/reference/createGraphics_.html) (which is of course different than the *PApplet* main `PGraphics` instance).

## Frames

A frame is a coordinate system that may be translated, rotated and scaled. [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) instances define each of the nodes comprising a scene graph. To customize their role (e.g., a drawing procedure or a culling condition) symply override the [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) [visit()](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#visit--) method. To illustrate their use, suppose the following scene graph is being implemented:

```processing
 World
  ^
  |\
  1 eye
  ^
  |\
  2 3
```

To setup the scene hierarchy of _attached_ frames, i.e., frames belonging to the scene, use code such as the following:

```processing
Scene scene;
Frame f1, f2, f3;
void setup() {
  scene = new Scene(this);
  // To attach a leading-frame (those whose parent is the world, such as f1)
  // the scene parameter is passed to the Frame constructor:
  f1 = new Frame(scene);
  // whereas for the remaining frames we pass any constructor taking a
  // reference frame paramater, such as Frame(Frame referenceFrame)
  f2 = new Frame(f1);
  f3 = new Frame(f1) {
    // note that within visit() the geometry is defined
    // at the frame local coordinate system
    @Override
    public void visit() {
      sphere(50);
    }
  };
}
```

and then traverse it with:

```processing
void draw() {
  // traverse() peforms a top-down traversal of the frame hierarchy, applying the
  // local frame transformation and calling the visit() method on each visited frame
  scene.traverse();
}
```

To set the scene [tracked-frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#trackedFrame--) (the frame the mouse should interact with) call [setTrackedFrame(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#setTrackedFrame-frames.core.Frame-) or update it 
using ray-casting with [cast()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#cast--), for example:

```processing
void mouseMoved() {
  // the tracked-frame is updated using ray-casting from the set of scene attached frames
  scene.cast();
}
```

To interact with a given frame use any [Scene](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html) method that takes a [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) parameter, such as: [spin(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#spin-frames.core.Frame-), [translate(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#translate-frames.core.Frame-), [scale(float, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#scale-float-frames.core.Frame-) or [zoom(float, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#zoom-float-frames.core.Frame-). For example:

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

To interact with the [default-frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#defaultFrame--) (which is either the tracked-frame updated with the `mouseMoved` above or the scene _eye_ when the tracked-frame is null) use the _frameless_ versions of the above methods, e.g., [spin()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#spin--), [translate()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#translate--), [scale(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#scale-float-) or [zoom(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#zoom-float-). For example:

```processing
public void mouseDragged() {
  // spins the default-frame (the eye or the frame picked with a mouseMoved)
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
  // translates the default-frame (the eye or the frame picked with a mouseMoved)
    scene.translate();
  // zooms the default-frame (the eye or the frame picked with a mouseMoved)
  else
    scene.zoom(scene.mouseDX());
}
```

See the [CajasOrientadas example](https://github.com/VisualComputing/frames/tree/master/examples/basics/CajasOrientadas). Some advantages of using _attached_ frames are:

* The scene gets rendered respect to an _eye_ frame which may be set from any [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) instance (see [setEye(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#setEye-frames.core.Frame-)). Note that the [Scene](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html) sets up a default _eye_ frame. To retrieve the scene _eye_ call [eye()](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#eye--).
* The scene topology is set (even at run time) with [setReference(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setReference-frames.core.Frame-).
* Frames may be picked using ray-casting and the [Scene](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html) provides all sorts of interactivity commands to manipulate them.
* The [Scene](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html) methods [location(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#location-frames.primitives.Vector-frames.core.Frame-) and [screenLocation(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#screenLocation-frames.primitives.Vector-frames.core.Frame-) transforms coordinates between frame and screen space.
* The frame methods [setTranslation(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setTranslation-frames.primitives.Vector-), [translate(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#translate-frames.primitives.Vector-), [setRotation(Quaternion)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setRotation-frames.primitives.Quaternion-), [rotate(Quaternion)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#rotate-frames.primitives.Quaternion-), [setScaling(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setScaling-float-) and [scale(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#scale-float-), locally manipulates a frame instance.
* The [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) methods [setPosition(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setPosition-frames.primitives.Vector-), [setOrientation(Quaternion)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setOrientation-frames.primitives.Quaternion-), and [setMagnitude(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setMagnitude-float-), globally manipulates a frame instance.
* The [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) methods [location(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#location-frames.primitives.Vector-frames.core.Frame-) and [displacement(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#displacement-frames.primitives.Vector-frames.core.Frame-) transforms coordinates and vectors (resp.) from other frame instances.
* The [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) methods [worldLocation(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#worldLocation-frames.primitives.Vector-) and [worldDisplacement(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#worldDisplacement-frames.primitives.Vector-) transforms frame coordinates and vectors (resp.) to the world.
* The [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) method [setConstraint(Constrain)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setConstraint-frames.core.constraint.Constraint-) applies a [Constraint](https://visualcomputing.github.io/frames-javadocs/frames/primitives/constraint/Constraint.html) to a frame instance limiting its motion.
* The role played by a [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) instance during a scene graph traversal is implemented by overriding its [visit()](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#visit--) method.

To bypass the [traverse()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#traverse--) algorithm use [detached frames](detached.md), or override [visit()](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#visit--) to setup a _cullingCondition_ for the frame as follows (see [visit()](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#visit--), [cull(boolean)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#cull-boolean-) and [isCulled()](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#isCulled--)):

```processing
Scene scene;
Frame frame;
void setup() {
  scene = new Scene(this);
  frame = new Frame(scene) {
    @Override
    public void visit() {
      // Hierarchical culling is optional and disabled by default. When the cullingCondition
      // (which should be implemented by you) is true, scene.traverse() will prune the branch at the frame
      cull(cullingCondition);
      if(!isCulled())
        // Draw your object here, in the local coordinate system.
    }
  }
}
```

## Shapes

A [Shape](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html) is an attached [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) specialization that can be set from a [retained-mode](https://en.wikipedia.org/wiki/Retained_mode) rendering Processing [PShape](https://processing.org/reference/PShape.html) or from an [immediate-mode](https://en.wikipedia.org/wiki/Immediate_mode_(computer_graphics)) rendering Processing procedure. Shapes can be picked precisely using their projection onto the screen, see [setPrecision(Frame.Precision)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html#setPrecision-frames.core.Frame.Precision-). Use [traverse()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#traverse--) to render all scene-graph shapes or [draw()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html#draw--) to render a specific one instead.

To set a retained-mode shape use `Shape shape = new Shape(Scene scene, PShape shape)` or `Shape shape = new Shape(Scene scene)` and then call [Shape.setGraphics(PShape)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html#set-processing.core.PShape-).

Immediate-mode shapes should override `Shape.setGraphics(PGraphics)`, e.g., using an anonymous inner
[Shape](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html) class instance, such as with the following:
 
```java
...
Shape shape;
void setup() {
  ...
  shape = new Shape(scene) {
    @Override
    protected void setGraphics(PGraphics canvas) {
      //immediate-mode rendering procedure
    }
  };
}
```

and then render it with:

```processing
void draw() {
  // calls visit() on each shape to draw the shape
  scene.traverse();
}
```

See the [DepthOfField example](https://github.com/VisualComputing/frames/tree/master/examples/demos/DepthOfField). Some advantages of using shapes are:

* Same as with _attached_ frames.
* Shapes are picked precisely using ray-tracing against the pixels of their projection. See [setPrecision](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html#setPrecision-frames.core.Frame.Precision-).
* Shapes can be set as the scene _eye_ (see [setEye(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#setEye-frames.core.Frame-)) which may be useful to depict the viewer in first person camera style.

## Interpolators

A frame (and hence a shape) can be animated through a [key-frame](https://en.wikipedia.org/wiki/Key_frame) [Catmull-Rom](https://en.wikipedia.org/wiki/Cubic_Hermite_spline#Catmull%E2%80%93Rom_spline) [interpolator](https://visualcomputing.github.io/frames-javadocs/frames/core/Interpolator.html) path. Use code such as the following:

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
    interpolator.addKeyFrame(scene.randomFrame());
  interpolator.start();
}
```

which will create a random interpolator path containing [4..10] key-frames. The interpolation is also started. The interpolator path may be drawn with code like this:

```java
...
void draw() {
  scene.traverse();
  scene.drawPath(interpolator);
}
```

while [traverse()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#traverse--) will draw the animated shape(s) [drawPath(Interpolator)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawPath-frames.core.Interpolator-) will draw the interpolated path too. See the [Interpolators example](https://github.com/VisualComputing/frames/tree/master/examples/basics/Interpolators).
 
## HIDs

Setting up a [Human Interface Device (hid)](https://en.wikipedia.org/wiki/Human_interface_device) (different than the mouse which is provided by default) such as a keyboard or a [space-navigator](https://en.wikipedia.org/wiki/3Dconnexion), is a two step process:

1. Define an _hid_ tracked-frame instance, using an arbitrary name for it (see [setTrackedFrame(String, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#setTrackedFrame-java.lang.String-frames.core.Frame-)); and,
2. Call any interactivity method that take an _hid_ param (such as [translate(String, float, float, float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#translate-java.lang.String-float-float-), [rotate(String, float, float, float)]() or [scale(String, float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#scale-java.lang.String-float-) following the name convention you defined in 1.

See the [SpaceNavigator example](https://github.com/VisualComputing/frames/tree/master/examples/demos/SpaceNavigator).

Observations:

1. An _hid_ tracked-frame (see [trackedFrame(String)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#trackedFrame-java.lang.String-)) defines in turn an _hid_ default-frame (see [defaultFrame(String)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#defaultFrame-java.lang.String-)) which simply returns the tracked-frame or the scene _eye_ when the _hid_ tracked-frame is `null`
2. The _hid_ interactivity methods are implemented in terms of the ones defined previously by simply passing the _hid_ [defaultFrame(String)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#defaultFrame-java.lang.String-) to them.
3. The default _hid_ is defined with a `null` String parameter (e.g., [scale(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#scale-float-) simply calls `scale(null, delta)`). The _Scene_ default mouse _hid_ presented in the [Frames](#user-content-frames) section is precisely implemented is this manner.
4. To update an _hid_ tracked-frame using ray-casting call [track(String, Point, Frame[])](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#track-java.lang.String-frames.primitives.Point-frames.core.Frame:A-) (detached or attached frames), [track(String, Point)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#track-java.lang.String-frames.primitives.Point-) (only attached frames) or [cast(String, Point)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#cast-java.lang.String-frames.primitives.Point-) (only for attached frames too). While [track(String, Point, Frame[])](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#track-java.lang.String-frames.primitives.Point-frames.core.Frame:A-) and [track(String, Point)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#track-java.lang.String-frames.primitives.Point-) update the _hid_ tracked-frame synchronously (i.e., they return the _hid_ tracked-frame immediately), [cast(String, Point)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#cast-java.lang.String-frames.primitives.Point-) updates it asynchronously (i.e., it optimally updates the _hid_ tracked-frame during the next call to the [traverse()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#traverse--) algorithm).

## Control

[Application control](https://hal.inria.fr/hal-00789413/) (aka as Post-[WIMP](https://en.wikipedia.org/wiki/WIMP_(computing)) interaction styles) refers to interfaces “containing at least one interaction technique not dependent on classical 2D widgets” [[van Dam]](http://dl.acm.org/citation.cfm?id=253708), such as:  [tangible interaction](https://en.wikipedia.org/wiki/Tangible_user_interface), or perceptual and [affective computing](https://en.wikipedia.org/wiki/Affective_computing).

Implementing an application control for a frame is a two step process:

1. Override the frame method [interact(Object...)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#interact-java.lang.Object...-) to parse the gesture into a custom (application) control.
2. Send gesture data to the frame by calling one of the following scene methods: [defaultHIDControl(Object...)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#defaultHIDControl-java.lang.Object...-), [control(String, Object...)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#control-java.lang.String-java.lang.Object...-) or [control(Frame, Object...)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#control-frames.core.Frame-java.lang.Object...-).

See the [ApplicationControl example](https://github.com/VisualComputing/frames/tree/master/examples/demos/ApplicationControl).

## Drawing

The [Scene](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html) implements several static drawing functions that complements those already provided by Processing, such as: [drawCylinder(PGraphics, int, float, float)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawCylinder-processing.core.PGraphics-int-float-float-), [drawHollowCylinder(PGraphics, int, float, float, Vector, Vector)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawHollowCylinder-processing.core.PGraphics-int-float-float-frames.primitives.Vector-frames.primitives.Vector-), [drawCone(PGraphics, int, float, float, float, float)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawCone-processing.core.PGraphics-int-float-float-float-float-), [drawCone(PGraphics, int, float, float, float, float, float)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawCone-processing.core.PGraphics-int-float-float-float-float-float-) and [drawTorusSolenoid(PGraphics, int, int, float, float)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawTorusSolenoid-processing.core.PGraphics-int-int-float-float-).

Drawing functions that take a `PGraphics` parameter (including the above static ones), such as [beginHUD(PGraphics)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#beginHUD-processing.core.PGraphics-),
[endHUD(PGraphics)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#endHUD-processing.core.PGraphics-), [drawAxes(PGraphics, float)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawAxes-processing.core.PGraphics-float-), [drawCross(PGraphics, float, float, float)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawCross-processing.core.PGraphics-float-float-float-) and [drawGrid(PGraphics)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawGrid-processing.core.PGraphics-) among others, can be used to set a [Shape](#user-content-shapes).

Another scene's eye (different than this one) can be drawn with [drawEye(Graph)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#drawEye-frames.core.Graph-). Typical usage include interactive [minimaps](https://en.wikipedia.org/wiki/Mini-map) and _visibility culling_ visualization and debugging.

## Installation

Download the [proof-of-concept implementation](https://github.com/VisualComputing/frames/releases/download/v-0.1.0-pre.1/frames-0.1.0-pre.1.zip) (read the release notes [here](https://github.com/VisualComputing/frames/releases/tag/v-0.1.0-pre.1)) and extract it to your [Processing](https://github.com/processing/processing/wiki/How-to-Install-a-Contributed-Library#manual-install) sketchbook `libraries` folder.

## Contributors

Thanks goes to these wonderful people ([emoji key](https://github.com/kentcdodds/all-contributors#emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore -->
<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/kentcdodds/all-contributors) specification. Contributions of any kind welcome!
