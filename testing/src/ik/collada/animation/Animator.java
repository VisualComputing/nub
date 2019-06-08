package ik.collada.animation;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.Map;



/**
 *
 * This class contains all the functionality to apply an animation to an
 * animated entity. An Animator instance is associated with just one
 * {@link Model}. It also keeps track of the running time (in seconds)
 * of the current animation, along with a reference to the currently playing
 * animation for the corresponding entity.
 *
 * An Animator instance needs to be updated every frame, in order for it to keep
 * updating the animation pose of the associated entity. The currently playing
 * animation can be changed at any time using the doAnimation() method. The
 * Animator will keep looping the current animation until a new animation is
 * chosen.
 *
 * The Animator calculates the desired current animation pose by interpolating
 * between the previous and next keyframes of the animation (based on the
 * current animation time). The Animator then updates the transforms all of the
 * joints each frame to match the current desired animation pose.
 *
 * @author Karl
 *
 */
public class Animator {
    //TODO : Update
    private final Model entity;

    private Animation currentAnimation;
    private float previousTime = 0;
    private float animationTime = 0;

    /**
     * @param entity
     *            - the entity which will by animated by this animator.
     */
    public Animator(Model entity) {
        this.entity = entity;
    }

    /**
     * Indicates that the entity should carry out the given animation. Resets
     * the animation time so that the new animation starts from the beginning.
     *
     * @param animation
     *            - the new animation to carry out.
     */
    public void doAnimation(Animation animation, float start) {
        this.previousTime = start;
        this.animationTime = 0;
        this.currentAnimation = animation;
    }

    /**
     * This method should be called each frame to update the animation currently
     * being played. This increases the animation time (and loops it back to
     * zero if necessary), finds the pose that the entity should be in at that
     * time of the animation, and then applies that pose to all the model's
     * joints by setting the joint transforms.
     */
    public void update(float deltaTime) {
        if (currentAnimation == null) {
            return;
        }
        increaseAnimationTime(deltaTime);
        Map<String, Node> currentPose = calculateCurrentAnimationPose();
    }

    /**
     * Increases the current animation time which allows the animation to
     * progress. If the current animation has reached the end then the timer is
     * reset, causing the animation to loop.
     */
    private void increaseAnimationTime(float ellapsedTime) {
        animationTime += (ellapsedTime - previousTime)/1000.f;
        previousTime = ellapsedTime;
        if (animationTime > currentAnimation.getLength()) {
            this.animationTime %= currentAnimation.getLength();
        }
    }

    /**
     * This method returns the current animation pose of the entity. It returns
     * the desired local-space transforms for all the joints in a map, indexed
     * by the name of the joint that they correspond to.
     *
     * The pose is calculated based on the previous and next keyframes in the
     * current animation. Each keyframe provides the desired pose at a certain
     * time in the animation, so the animated pose for the current time can be
     * calculated by interpolating between the previous and next keyframe.
     *
     * This method first finds the preious and next keyframe, calculates how far
     * between the two the current animation is, and then calculated the pose
     * for the current animation time by interpolating between the transforms at
     * those keyframes.
     *
     * @return The current pose as a map of the desired local-space transforms
     *         for all the joints. The transforms are indexed by the name ID of
     *         the joint that they should be applied to.
     */
    private Map<String, Node> calculateCurrentAnimationPose() {
        KeyFrame[] frames = getPreviousAndNextFrames();
        float progression = calculateProgression(frames[0], frames[1]);
        return interpolatePoses(frames[0], frames[1], progression);
    }


    /**
     * Finds the previous keyframe in the animation and the next keyframe in the
     * animation, and returns them in an array of length 2. If there is no
     * previous frame (perhaps current animation time is 0.5 and the first
     * keyframe is at time 1.5) then the first keyframe is used as both the
     * previous and next keyframe. The last keyframe is used for both next and
     * previous if there is no next keyframe.
     *
     * @return The previous and next keyframes, in an array which therefore will
     *         always have a length of 2.
     */
    private KeyFrame[] getPreviousAndNextFrames() {
        KeyFrame[] allFrames = currentAnimation.getKeyFrames();
        KeyFrame previousFrame = allFrames[0];
        KeyFrame nextFrame = allFrames[0];
        for (int i = 1; i < allFrames.length; i++) {
            nextFrame = allFrames[i];
            if (nextFrame.getTimeStamp() > animationTime) {
                break;
            }
            previousFrame = allFrames[i];
        }
        return new KeyFrame[] { previousFrame, nextFrame };
    }

    /**
     * Calculates how far between the previous and next keyframe the current
     * animation time is, and returns it as a value between 0 and 1.
     *
     * @param previousFrame
     *            - the previous keyframe in the animation.
     * @param nextFrame
     *            - the next keyframe in the animation.
     * @return A number between 0 and 1 indicating how far between the two
     *         keyframes the current animation time is.
     */
    private float calculateProgression(KeyFrame previousFrame, KeyFrame nextFrame) {
        float totalTime = nextFrame.getTimeStamp() - previousFrame.getTimeStamp();
        float currentTime = animationTime - previousFrame.getTimeStamp();
        return currentTime / totalTime;
    }

    /**
     * Calculates all the local-space joint transforms for the desired current
     * pose by interpolating between the transforms at the previous and next
     * keyframes.
     *
     * @param previousFrame
     *            - the previous keyframe in the animation.
     * @param nextFrame
     *            - the next keyframe in the animation.
     * @param progression
     *            - a number between 0 and 1 indicating how far between the
     *            previous and next keyframes the current animation time is.
     * @return The local-space transforms for all the joints for the desired
     *         current pose. They are returned in a map, indexed by the name of
     *         the joint to which they should be applied.
     */
    private Map<String, Node> interpolatePoses(KeyFrame previousFrame, KeyFrame nextFrame, float progression) {
        Map<String, Node> currentPose = entity.skeleton();
        for (String jointName : previousFrame.getJointKeyFrames().keySet()) {
            Node previous = previousFrame.getJointKeyFrames().get(jointName);
            Node next = nextFrame.getJointKeyFrames().get(jointName);
            //interpolate position
            Node frame = entity.skeleton().get(jointName);
            Vector translation = Vector.subtract(next.translation(), previous.translation());
            translation.multiply(progression);
            previous.translate(next.translation());
            Quaternion rotation = Quaternion.slerp(previous.rotation(), next.rotation(), progression);
            //interpolate rotation
            frame.setRotation(rotation);
        }
        return currentPose;
    }
}