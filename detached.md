Detached frames

To setup the hierarchy of _detached_ frames, i.e., frames not belonging to a particular `scene`, use code such as the following:

```processing
Frame eye, f1, f2, f3;
void setup() {
  // Note the use of the default frame constructor to instantiate a
  // detached leading frame (those whose parent is the world, such as the eye and f1):
  eye =  new Frame();
  f1 = new Frame();
  // whereas for the remaining frames we pass any constructor taking a
  // reference frame paramater, such as Frame(Frame referenceFrame, float scaling):
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

See the [Sceneless example](https://github.com/VisualComputing/frames/tree/master/examples/1.DetachedFrames/Sceneless). Some advantages of using _detached_ frames without instantiating a `scene` object are:

* The scene gets rendered respect to an `eye` frame.
* The graph topology is set (even at run time) with [setReference(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setReference-frames.core.Frame-).
* [setTranslation(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setTranslation-frames.primitives.Vector-), [translate(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#translate-frames.primitives.Vector-), [setRotation(Quaternion)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setRotation-frames.primitives.Quaternion-), [rotate(Quaternion)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#rotate-frames.primitives.Quaternion-), [setScaling(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setScaling-float-) and [scale(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#scale-float-), locally manipulates a frame instance.
* [setPosition(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setPosition-frames.primitives.Vector-), [setOrientation(Quaternion)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setOrientation-frames.primitives.Quaternion-), and [setMagnitude(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setMagnitude-float-), globally manipulates a frame instance.
* [location(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#location-frames.primitives.Vector-frames.core.Frame-) and [displacement(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#displacement-frames.primitives.Vector-frames.core.Frame-) transforms coordinates and vectors (resp.) from other frame instances.
* [worldLocation(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#worldLocation-frames.primitives.Vector-) and [worldDisplacement(Vector)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#worldDisplacement-frames.primitives.Vector-) transforms frame coordinates and vectors (resp.) to the world.
* [setConstraint(Constrain)](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html#setConstraint-frames.core.constraint.Constraint-) applies a [Constraint](https://visualcomputing.github.io/frames-javadocs/frames/primitives/constraint/Constraint.html) to a frame instance limiting its motion.

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

To set the scene [tracked-frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#trackedFrame--) (the frame the mouse should interact with) call [setTrackedFrame(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#setTrackedFrame-frames.core.Frame-) or update it with [track(Frame[])](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#track-frames.core.Frame:A-), for example:

```processing
void mouseMoved() {
  // the tracked-frame is updated from the array using ray-casting
  scene.track(new Frame[]{f1, f2, f3});
}
```

To interact with a given frame use any `Scene` method that takes a `frame` parameter, such as: [spin(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#spin-frames.core.Frame-), [translate(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#translate-frames.core.Frame-), [scale(float, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#scale-float-frames.core.Frame-) or [zoom(float, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#zoom-float-frames.core.Frame-). For example:

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

To interact with the [default-frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#defaultFrame--) (which is either the tracked-frame updated with the `mouseMoved` above or the scene eye when the tracked-frame is null) use the _frameless_ versions of the above methods, e.g., [spin()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#spin--), [translate()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#translate--), [scale(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#scale-float-) or [zoom(float)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#zoom-float-). For example:

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

See the [detached-frames CajasOrientadas example](https://github.com/VisualComputing/frames/tree/master/examples/1.DetachedFrames/CajasOrientadas). Some advantages of using _detached_ frames through an instantiated `scene` object:

* Same as with _detached_ frames without an instantiated `scene` object.
* The `eye` frame is automatically handled by the `scene` and may be set from any (attached or detached) [Frame](https://visualcomputing.github.io/frames-javadocs/frames/core/Frame.html) instance (see [setEye(Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#setEye-frames.core.Frame-)).
* By default the `scene` object instantiates and attached `eye` frame.
* Frames may be picked using ray-casting and the `scene` provides all sorts of interactivity commands to manipulate them.
* The `scene` methods [location(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#location-frames.primitives.Vector-frames.core.Frame-) and [screenLocation(Vector, Frame)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#screenLocation-frames.primitives.Vector-frames.core.Frame-) transforms coordinates between frame and screen space.

The main disadvantage of using detached frames is that you need to know the scene hierarchy topology in advanced to be able to traverse it.
