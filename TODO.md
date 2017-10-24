# Next release: 4.0.0

## Warning

Remember to properly instantiate the twod boolean flag in AbstractScene!

## Key ideas

1. Remove Eye: Move functionality to Scene
2. New scene hierarchy:
Scene -> RasterScene -> P5Scene
Scene is concrete and allows to set a ray-tracing scene.
3. Remove drawing methods from scene: move functionality to Utility drawing class.
4. Rethink EyeConstraint: A constraint defined in terms of an arbitrary frame.
5. Rethink Trackable: idem. Scene.preDraw needs proper handling: eye().setWorldMatrix().
6. Implement Eye.modified()! (Scene.preDraw again)

## AbstractScene

### Ambiguous methods (modified some eye() params)

1. public void setFieldOfView(float fov)
2. public void lookAt(Vec target)
3. public void setViewDirection(Vec direction)
4. public void fitBall(Vec center, float radius)
5. public void fitBoundingBox(Vec min, Vec max)
6. public void fitScreenRegion(Rect rectangle) 


### Methods to be restored

1. add/play/delete/Path_n
2. protected void drawPointUnderPixelHint()
3. public abstract String info()
4. public void displayInfo(boolean onConsole)
5. public boolean pathsVisualHint()
6. public boolean zoomVisualHint()
7. public boolean rotateVisualHint()
8. public void togglePathsVisualHint()
9. protected void toggleZoomVisualHint()
10. protected void toggleRotateVisualHint()
11. public void setPathsVisualHint(boolean draw)
12. public void setPathsVisualHint(boolean draw)
13. public void setZoomVisualHint(boolean draw)
14. public void setRotateVisualHint(boolean draw)
15. protected void drawPathsHint()
16. protected void drawPaths()
17. protected abstract void drawZoomWindowHint()
18. protected abstract void drawScreenRotateHint()

### Methods that were modified

1. public void setPathsVisualHint(boolean draw) 
2. public void preDraw()
3. protected void drawPaths()
4. protected void drawPaths()
5. public void setAvatar(Trackable t)
6. public Trackable resetAvatar()
7. protected void displayVisualHints()

## Scene

### Methods to be restored

1. protected JSONArray toJSONArray(int id)
2. protected void drawPointUnderPixelHint()

### Methods that were modified

1. public void preDraw()
2. public void saveConfig(String fileName)

## Eye

### Methods to be restored

1. public HashMap<Integer, keyFrameInterpolator> keyFrameInterpolatorMap()
2. public keyFrameInterpolator[] keyFrameInterpolatorArray()
3. public List<keyFrameInterpolator> keyFrameInterpolatorList()
4. public keyFrameInterpolator keyFrameInterpolator(int key)
5. public void setKeyFrameInterpolator(int key, KeyFrameInterpolator keyFInterpolator)
6. public void addKeyFrameToPath(int key)
7. protected void detachPaths()
8. protected void detachPath(int key)
9. protected void attachPaths()
10. protected void attachPath(int key)
11. public void playPath(int key)
12. public void deletePath(int key)
13. public void resetPath(int key)
14. public boolean anyInterpolationStarted()
15. public void stopInterpolations()
16. public void interpolateToZoomOnRegion(Rect rectangle)
17. public void interpolateToFitScene()
18. public void interpolateTo(InteractiveFrame fr)
19. public void interpolateTo(InteractiveFrame fr, float duration)
20. public void interpolateToZoomOnPixel(float x, float y)
21. public void interpolateToZoomOnPixel(Point pixel)
22. protected void interpolateToZoomOnTarget(Vec target)
23. protected void unSetTimerFlag()
24. protected void runResetAnchorHintTimer(long period)
25. public abstract boolean setSceneCenterFromPixel(Point pixel)
26. public boolean setSceneCenterFromPixel(float x, float y)
27. public abstract boolean setAnchorFromPixel(Point pixel)
28. public boolean setAnchorFromPixel(float x, float y)

### Methods that were modified

1. public Eye
2. protected Eye
3. protected final void replaceFrame(InteractiveFrame g)
4. public final void setFrame(InteractiveFrame g)
5. public boolean setAnchorFromPixel(Point pixel)

## InteractiveFrame

### Methods to be restored

1. public void zoomOnPixel(TapEvent event)
2. public void anchorFromPixel(TapEvent event)

## Foundation

1. Release inverse kinematics.
2. Perform a deep copy of generic-children frames.
