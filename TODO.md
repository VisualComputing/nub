# Next release: 4.0.0

## Key ideas

1. Remove reflect stuff, i.e., Profiles and iFrame.setShape(method).
2. Make Graph (previously AbstractScene) instantiable. Refactor GenericFrames as Nodes.
3. Make eye nodes indistinguible from other nodes, i.e., they can interchangeably be used.
4. Rethink third person camera by removing Trackable in favor of 3.
5. Better 2D and 3D merging by removing Eye hierarchy and move its functionality into the Graph. 
6. Rethink constraints to cope with ik framework.
7. Port the framework to JS (from all the previous points).
8. Implement proscene package to simplify all the examples which should be ported and update the API docs.

## JS port (@sechaparroc)

### Methodology

Port one by one the java packages in the following order:

1. [Timing](https://github.com/VisualComputing/proscene.js/tree/processing/src/remixlab/timing). Status: API is completed and tested. Expect some (occasional) API docs updates.
2. [Input](https://github.com/VisualComputing/proscene.js/tree/processing/src/remixlab/input). Status: API is completed and tested. Expect some API (occasional) docs updates.
3. [Primitives](https://github.com/VisualComputing/proscene.js/tree/processing/src/remixlab/primitives). Status: API is completed and tested. Expect some API docs updates.
4. [Core](https://github.com/VisualComputing/proscene.js/tree/processing/src/remixlab/core). Status: API is completed and (mostly completed) tested. Expect lots of API docs updates.
5. [Proscene](https://github.com/VisualComputing/proscene.js/tree/processing/src/remixlab/proscene). Status: API is uncomplete (see below).

Observations:

1. Timing and Input packages should be ported at their own repos (_master_ branches): [here](https://github.com/VisualComputing/fpstiming.js) and [here](https://github.com/VisualComputing/bias.js). Note that the _processing_ branches contain the Java code with the examples to be tested.
2. Primitives, Core and Proscene packages should be ported at the [proscene.js](https://github.com/VisualComputing/proscene.js) repo master branch. Note that the _processing_ branch is synced with the repos in 1 and that it contains the examples [here](https://github.com/VisualComputing/proscene.js/tree/processing/testing).

### Code conventions

* [ECMAScript 2015](http://es6-features.org) (a.k.a., ECMAScript6 or [ES6](https://en.wikipedia.org/wiki/ECMAScript#6th_Edition_-_ECMAScript_2015)) compatibility with [class support](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Classes).
* vars (class attributes) and 'private' methods are prefixed with _underscore_ (_). Convention adopted from [here](https://developer.mozilla.org/en-US/docs/Archive/Add-ons/Add-on_SDK/Guides/Contributor_s_Guide/Private_Properties). Note that Java protected methods aren't prefixed (the same goes for their JS counterparts).
* Method _params_ should be named as explicit as possible (in order to cope with JS dynamic and weak typed features).

## Proscene (@nakednous)

* Merge Node and Frame in the API, e.g., `setEye(Frame)`, `setDefaultNode(Node)`.
* Restored `save/loadConfig` (json).
* Make all drawing _static_.
* Restore `PickingPrecision.EXACT` using the `pickingBuffer`.
* Restore *Luxo*, *MiniMap*, *Flock* and the remaining advanced examples with the book (and slides) in mind.
