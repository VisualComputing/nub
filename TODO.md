# Next release: 4.0.0

## Key ideas

1. Remove Eye: Move functionality to Scene
2. New scene hierarchy:
Scene -> RasterScene -> P5Scene
Scene is concrete and allows to set a ray-tracing scene.
3. Remove drawing methods from scene: move functionality to Utility drawing class.

## AbstractScene

### Methods to be restored

1. add/play/delete/Path_n

### Methods that were modified

1. public void setPathsVisualHint(boolean draw) 
2. public void preDraw()
3. protected void drawPaths()
4. protected void drawPaths()
5. public void setAvatar(Trackable t)
6. public Trackable resetAvatar()

## Scene

### Methods to be restored

1. protected JSONArray toJSONArray(int id)

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

### Methods that were modified

1. public Eye
2. protected Eye
3. protected final void replaceFrame(InteractiveFrame g)
4. public final void setFrame(InteractiveFrame g)

## InteractiveFrame

### Methods to be restored

1. public void zoomOnPixel(TapEvent event)

## Foundation

1. Release inverse kinematics.
2. Perform a deep copy of generic-children frames.
