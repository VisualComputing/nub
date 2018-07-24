package ik.collada.animation;

import frames.core.Frame;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Represents one keyframe of an animation. This contains the timestamp of the
 * keyframe, which is the time (in seconds) from the start of the animation when
 * this keyframe occurs.
 *
 * It also contains the desired bone-space transforms of all of the joints in
 * the animated entity at this keyframe in the animation (i.e. it contains all
 * the joint transforms for the "pose" at this time of the animation.). The
 * joint transforms are stored in a map, indexed by the name of the joint that
 * they should be applied to.
 *
 * @author Karl
 *
 */
public class KeyFrame {

    private final float timeStamp;
    private final HashMap<String, Frame> pose;

    public KeyFrame(float timeStamp){
        this.timeStamp = timeStamp;
        this.pose = new HashMap<String, Frame>();
    }

    /**
     * @param timeStamp
     *            - the time (in seconds) that this keyframe occurs during the
     *            animation.
     * @param jointKeyFrames
     *            - the local-space transforms for all the joints at this
     *            keyframe, indexed by the name of the joint that they should be
     *            applied to.
     */

    public KeyFrame(float timeStamp, HashMap<String, Frame> jointKeyFrames) {
        this.timeStamp = timeStamp;
        this.pose = jointKeyFrames;
    }

    public HashMap<String, Frame> getPose(){
        return pose;
    }

    /**
     * @return The time in seconds of the keyframe in the animation.
     */
    protected float getTimeStamp() {
        return timeStamp;
    }

    /**
     * @return The desired bone-space transforms of all the joints at this
     *         keyframe, of the animation, indexed by the name of the joint that
     *         they correspond to. This basically represents the "pose" at this
     *         keyframe.
     */
    protected Map<String, Frame> getJointKeyFrames() {
        return pose;
    }
}