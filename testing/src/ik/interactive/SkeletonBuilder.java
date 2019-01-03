package ik.interactive;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.MatrixHandler;
import frames.core.constraint.BallAndSocket;
import frames.core.constraint.Constraint;
import frames.core.constraint.FixedConstraint;
import frames.core.constraint.Hinge;
import frames.primitives.Matrix;
import frames.primitives.Point;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebchaparr on 27/10/18.
 */
public class SkeletonBuilder extends PApplet{
    Scene scene, focus;
    Scene[] views;
    //focus;
    //OptionPanel panel;
    //PGraphics canvas1;

    FitCurve fitCurve;

    float radius = 15;
    int w = 1000, h = 700;
    /*Create different skeletons to interact with*/
    //Choose FX2D, JAVA2D, P2D or P3D
    String renderer = P3D;

    /*Constraint Parameters*/
    float minAngle = radians(60);
    float maxAngle = radians(60);

    List<Target> targets = new ArrayList<Target>();


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.interactive.SkeletonBuilder"});
    }

    public void settings() {
        size(w, h, renderer);
    }

    public void setup(){
        //canvas1 = createGraphics((int)(0.7f*w), h, renderer);
        //canvas1 = createGraphics(w, h, renderer);
        //canvas1 = this.g;
        scene = new Scene(this);
        scene.setRadius(300);
        if(scene.is3D())scene.setType(Graph.Type.ORTHOGRAPHIC);
        new InteractiveJoint(scene, radius).setRoot(true);
        // = new OptionPanel(this, 0.7f * width, 0, (int)(0.3f * width), h );
        //scene.fit(1);
        //create an auxiliary view per Orthogonal Plane
        //create an auxiliary view to look at the XY Plane
        Constraint constraint = new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Frame frame) {
                return translation;
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Frame frame) {
                return new Quaternion();
            }
        };

        views = new Scene[3];

        Frame eyeXY = new Frame();
        eyeXY.setMagnitude(0.5f);
        eyeXY.setPosition(scene.eye().position());
        eyeXY.setOrientation(scene.eye().orientation());
        eyeXY.setConstraint(constraint);
        views[0] = new Scene(this, P3D, w/3, h/3, 0, 2*h/3);
        views[0].setEye(eyeXY);
        views[0].setType(Graph.Type.ORTHOGRAPHIC);
        //create an auxiliary view to look at the XY Plane
        Frame eyeXZ = new Frame();
        eyeXZ.setMagnitude(0.5f);
        eyeXZ.setPosition(0, scene.radius(), 0);
        eyeXZ.setOrientation(new Quaternion(new Vector(1,0,0), -HALF_PI));
        eyeXZ.setConstraint(constraint);
        views[1] = new Scene(this, P3D, w/3, h/3, w/3, 2*h/3);
        views[1].setEye(eyeXZ);
        views[1].setType(Graph.Type.ORTHOGRAPHIC);
        //create an auxiliary view to look at the XY Plane
        Frame eyeYZ = new Frame();
        eyeYZ.setMagnitude(0.5f);
        eyeYZ.setPosition(scene.radius(), 0, 0);
        eyeYZ.setOrientation(new Quaternion(new Vector(0,1,0), HALF_PI));
        eyeYZ.setConstraint(constraint);
        views[2] = new Scene(this, P3D, w/3, h/3, 2*w/3, 2*h/3);
        views[2].setEye(eyeXY);
        views[2].setType(Graph.Type.ORTHOGRAPHIC);

        fitCurve = new FitCurve();
        fitCurve.add(227.0f,439.0f);
        fitCurve.add(229.0f,439.0f);
        fitCurve.add(230.0f,439.0f);
        fitCurve.add(232.0f,439.0f);
        fitCurve.add(235.0f,439.0f);
        fitCurve.add(238.0f,439.0f);
        fitCurve.add(241.0f,439.0f);
        fitCurve.add(250.0f,439.0f);
        fitCurve.add(256.0f,439.0f);
        fitCurve.add(261.0f,439.0f);
        fitCurve.add(273.0f,438.0f);
        fitCurve.add(278.0f,437.0f);
        fitCurve.add(289.0f,435.0f);
        fitCurve.add(295.0f,434.0f);
        fitCurve.add(300.0f,434.0f);
        fitCurve.add(316.0f,432.0f);
        fitCurve.add(324.0f,431.0f);
        fitCurve.add(340.0f,427.0f);
        fitCurve.add(349.0f,425.0f);
        fitCurve.add(357.0f,424.0f);
        fitCurve.add(369.0f,421.0f);
        fitCurve.add(374.0f,420.0f);
        fitCurve.add(382.0f,418.0f);
        fitCurve.add(387.0f,417.0f);
        fitCurve.add(389.0f,417.0f);
        fitCurve.add(392.0f,416.0f);
        fitCurve.add(395.0f,415.0f);
        fitCurve.add(396.0f,415.0f);
        fitCurve.add(402.0f,414.0f);
        fitCurve.add(405.0f,412.0f);
        fitCurve.add(408.0f,411.0f);
        fitCurve.add(417.0f,407.0f);
        fitCurve.add(421.0f,406.0f);
        fitCurve.add(424.0f,404.0f);
        fitCurve.add(432.0f,401.0f);
        fitCurve.add(436.0f,399.0f);
        fitCurve.add(442.0f,397.0f);
        fitCurve.add(455.0f,392.0f);
        fitCurve.add(462.0f,390.0f);
        fitCurve.add(471.0f,386.0f);
        fitCurve.add(478.0f,384.0f);
        fitCurve.add(486.0f,380.0f);
        fitCurve.add(492.0f,377.0f);
        fitCurve.add(498.0f,374.0f);
        fitCurve.add(502.0f,372.0f);
        fitCurve.add(507.0f,369.0f);
        fitCurve.add(513.0f,366.0f);
        fitCurve.add(519.0f,364.0f);
        fitCurve.add(526.0f,360.0f);
        fitCurve.add(532.0f,356.0f);
        fitCurve.add(538.0f,352.0f);
        fitCurve.add(550.0f,343.0f);
        fitCurve.add(556.0f,338.0f);
        fitCurve.add(562.0f,333.0f);
        fitCurve.add(572.0f,324.0f);
        fitCurve.add(576.0f,321.0f);
        fitCurve.add(577.0f,318.0f);
        fitCurve.add(581.0f,313.0f);
        fitCurve.add(583.0f,311.0f);
        fitCurve.add(584.0f,309.0f);
        fitCurve.add(587.0f,304.0f);
        fitCurve.add(589.0f,301.0f);
        fitCurve.add(590.0f,299.0f);
        fitCurve.add(593.0f,295.0f);
        fitCurve.add(595.0f,293.0f);
        fitCurve.add(596.0f,290.0f);
        fitCurve.add(598.0f,288.0f);
        fitCurve.add(599.0f,286.0f);
        fitCurve.add(601.0f,283.0f);
        fitCurve.add(602.0f,281.0f);
        fitCurve.add(603.0f,279.0f);
        fitCurve.add(604.0f,276.0f);
        fitCurve.add(605.0f,273.0f);
        fitCurve.add(607.0f,270.0f);
        fitCurve.add(608.0f,267.0f);
        fitCurve.add(610.0f,262.0f);
        fitCurve.add(614.0f,255.0f);
        fitCurve.add(616.0f,250.0f);
        fitCurve.add(617.0f,245.0f);
        fitCurve.add(620.0f,238.0f);
        fitCurve.add(621.0f,234.0f);
        fitCurve.add(622.0f,230.0f);
        fitCurve.add(623.0f,223.0f);
        fitCurve.add(624.0f,219.0f);
        fitCurve.add(625.0f,213.0f);
        fitCurve.add(625.0f,205.0f);
        fitCurve.add(625.0f,201.0f);
        fitCurve.add(625.0f,195.0f);
        fitCurve.add(625.0f,186.0f);
        fitCurve.add(625.0f,182.0f);
        fitCurve.add(625.0f,173.0f);
        fitCurve.add(625.0f,169.0f);
        fitCurve.add(625.0f,165.0f);
        fitCurve.add(625.0f,159.0f);
        fitCurve.add(625.0f,153.0f);
        fitCurve.add(624.0f,149.0f);
        fitCurve.add(623.0f,146.0f);
        fitCurve.add(621.0f,139.0f);
        fitCurve.add(620.0f,136.0f);
        fitCurve.add(619.0f,132.0f);
        fitCurve.add(617.0f,127.0f);
        fitCurve.add(616.0f,125.0f);
        fitCurve.add(613.0f,121.0f);
        fitCurve.add(613.0f,120.0f);
        fitCurve.add(611.0f,118.0f);
        fitCurve.add(610.0f,117.0f);
        fitCurve.add(608.0f,115.0f);
        fitCurve.add(608.0f,114.0f);
        fitCurve.add(606.0f,113.0f);
        fitCurve.add(605.0f,110.0f);
        fitCurve.add(604.0f,108.0f);
        fitCurve.add(602.0f,107.0f);
        fitCurve.add(599.0f,104.0f);
        fitCurve.add(598.0f,103.0f);
        fitCurve.add(597.0f,102.0f);
        fitCurve.add(594.0f,101.0f);
        fitCurve.add(592.0f,100.0f);
        fitCurve.add(590.0f,99.0f);
        fitCurve.add(587.0f,98.0f);
        fitCurve.add(585.0f,97.0f);
        fitCurve.add(580.0f,96.0f);
        fitCurve.add(576.0f,95.0f);
        fitCurve.add(572.0f,95.0f);
        fitCurve.add(569.0f,94.0f);
        fitCurve.add(565.0f,94.0f);
        fitCurve.add(560.0f,94.0f);
        fitCurve.add(551.0f,94.0f);
        fitCurve.add(547.0f,93.0f);
        fitCurve.add(542.0f,93.0f);
        fitCurve.add(532.0f,93.0f);
        fitCurve.add(526.0f,93.0f);
        fitCurve.add(521.0f,93.0f);
        fitCurve.add(512.0f,94.0f);
        fitCurve.add(508.0f,95.0f);
        fitCurve.add(499.0f,99.0f);
        fitCurve.add(494.0f,100.0f);
        fitCurve.add(488.0f,102.0f);
        fitCurve.add(483.0f,104.0f);
        fitCurve.add(474.0f,108.0f);
        fitCurve.add(469.0f,110.0f);
        fitCurve.add(459.0f,114.0f);
        fitCurve.add(453.0f,116.0f);
        fitCurve.add(447.0f,119.0f);
        fitCurve.add(434.0f,126.0f);
        fitCurve.add(425.0f,132.0f);
        fitCurve.add(418.0f,136.0f);
        fitCurve.add(411.0f,140.0f);
        fitCurve.add(403.0f,145.0f);
        fitCurve.add(396.0f,150.0f);
        fitCurve.add(389.0f,156.0f);
        fitCurve.add(383.0f,160.0f);
        fitCurve.add(377.0f,164.0f);
        fitCurve.add(371.0f,168.0f);
        fitCurve.add(361.0f,174.0f);
        fitCurve.add(358.0f,177.0f);
        fitCurve.add(356.0f,179.0f);
        fitCurve.add(353.0f,184.0f);
        fitCurve.add(352.0f,186.0f);
        fitCurve.add(349.0f,191.0f);
        fitCurve.add(347.0f,195.0f);
        fitCurve.add(346.0f,197.0f);
        fitCurve.add(345.0f,204.0f);
        fitCurve.add(345.0f,207.0f);
        fitCurve.add(347.0f,209.0f);
        fitCurve.add(348.0f,211.0f);
        fitCurve.add(350.0f,214.0f);
        fitCurve.add(351.0f,215.0f);
        fitCurve.add(353.0f,217.0f);
        fitCurve.add(354.0f,218.0f);
        fitCurve.add(356.0f,219.0f);
        fitCurve.add(361.0f,222.0f);
        fitCurve.add(366.0f,224.0f);
        fitCurve.add(372.0f,226.0f);
        fitCurve.add(385.0f,230.0f);
        fitCurve.add(393.0f,232.0f);
        fitCurve.add(400.0f,234.0f);
        fitCurve.add(415.0f,237.0f);
        fitCurve.add(422.0f,238.0f);
        fitCurve.add(429.0f,240.0f);
        fitCurve.add(444.0f,241.0f);
        fitCurve.add(452.0f,241.0f);
        fitCurve.add(467.0f,242.0f);
        fitCurve.add(473.0f,242.0f);
        fitCurve.add(479.0f,242.0f);
        fitCurve.add(483.0f,242.0f);
        fitCurve.add(489.0f,241.0f);
        fitCurve.add(493.0f,240.0f);
        fitCurve.add(498.0f,240.0f);
        fitCurve.add(500.0f,239.0f);
        fitCurve.add(503.0f,237.0f);
        fitCurve.add(510.0f,234.0f);
        fitCurve.add(513.0f,232.0f);
        fitCurve.add(515.0f,231.0f);
        fitCurve.add(523.0f,227.0f);
        fitCurve.add(527.0f,224.0f);
        fitCurve.add(537.0f,218.0f);
        fitCurve.add(543.0f,214.0f);
        fitCurve.add(549.0f,209.0f);
        fitCurve.add(555.0f,205.0f);
        fitCurve.add(561.0f,202.0f);
        fitCurve.add(568.0f,198.0f);
        fitCurve.add(581.0f,190.0f);
        fitCurve.add(589.0f,186.0f);
        fitCurve.add(595.0f,183.0f);
        fitCurve.add(606.0f,178.0f);
        fitCurve.add(612.0f,174.0f);
        fitCurve.add(618.0f,171.0f);
        fitCurve.add(630.0f,165.0f);
        fitCurve.add(636.0f,163.0f);
        fitCurve.add(640.0f,160.0f);
        fitCurve.add(652.0f,155.0f);
        fitCurve.add(658.0f,153.0f);
        fitCurve.add(669.0f,150.0f);
        fitCurve.add(674.0f,148.0f);
        fitCurve.add(680.0f,146.0f);
        fitCurve.add(683.0f,144.0f);
        fitCurve.add(688.0f,141.0f);
        fitCurve.add(690.0f,140.0f);
        fitCurve.add(691.0f,139.0f);
        fitCurve.add(694.0f,137.0f);
        fitCurve.add(695.0f,137.0f);
        fitCurve.add(697.0f,136.0f);
        fitCurve.add(706.0f,135.0f);
        fitCurve.add(711.0f,135.0f);
        fitCurve.add(716.0f,135.0f);
        fitCurve.add(721.0f,135.0f);
        fitCurve.add(727.0f,135.0f);
        fitCurve.add(732.0f,136.0f);
        fitCurve.add(738.0f,137.0f);
        fitCurve.add(744.0f,139.0f);
        fitCurve.add(751.0f,141.0f);
        fitCurve.add(756.0f,143.0f);
        fitCurve.add(763.0f,145.0f);
        fitCurve.add(772.0f,147.0f);
        fitCurve.add(779.0f,149.0f);
        fitCurve.add(787.0f,152.0f);
        fitCurve.add(795.0f,155.0f);
        fitCurve.add(802.0f,157.0f);
        fitCurve.add(810.0f,162.0f);
        fitCurve.add(818.0f,166.0f);
        fitCurve.add(830.0f,172.0f);
        fitCurve.add(835.0f,173.0f);
        fitCurve.add(837.0f,175.0f);
        fitCurve.add(840.0f,178.0f);
        fitCurve.add(842.0f,179.0f);
        fitCurve.add(845.0f,182.0f);
        fitCurve.add(846.0f,184.0f);
        fitCurve.add(848.0f,185.0f);
        fitCurve.add(850.0f,189.0f);
        fitCurve.add(852.0f,190.0f);
        fitCurve.add(853.0f,192.0f);
        fitCurve.add(856.0f,195.0f);
        fitCurve.add(858.0f,198.0f);
        fitCurve.add(859.0f,202.0f);
        fitCurve.add(863.0f,210.0f);
        fitCurve.add(864.0f,215.0f);
        fitCurve.add(865.0f,221.0f);
        fitCurve.add(865.0f,232.0f);
        fitCurve.add(865.0f,237.0f);
        fitCurve.add(865.0f,245.0f);
        fitCurve.add(864.0f,261.0f);
        fitCurve.add(864.0f,269.0f);
        fitCurve.add(863.0f,284.0f);
        fitCurve.add(863.0f,290.0f);
        fitCurve.add(863.0f,295.0f);
        fitCurve.add(863.0f,301.0f);
        fitCurve.add(862.0f,306.0f);
        fitCurve.add(861.0f,311.0f);
        fitCurve.add(860.0f,314.0f);
        fitCurve.add(857.0f,321.0f);
        fitCurve.add(855.0f,326.0f);
        fitCurve.add(853.0f,330.0f);
        fitCurve.add(850.0f,340.0f);
        fitCurve.add(848.0f,344.0f);
        fitCurve.add(846.0f,349.0f);
        fitCurve.add(843.0f,355.0f);
        fitCurve.add(841.0f,357.0f);
        fitCurve.add(839.0f,360.0f);
        fitCurve.add(836.0f,364.0f);
        fitCurve.add(834.0f,366.0f);
        fitCurve.add(830.0f,369.0f);
        fitCurve.add(828.0f,370.0f);
        fitCurve.add(824.0f,372.0f);
        fitCurve.add(813.0f,376.0f);
        fitCurve.add(806.0f,378.0f);
        fitCurve.add(791.0f,381.0f);
        fitCurve.add(784.0f,382.0f);
        fitCurve.add(779.0f,382.0f);
        fitCurve.add(773.0f,382.0f);
        fitCurve.add(769.0f,382.0f);
        fitCurve.add(764.0f,382.0f);
        fitCurve.add(759.0f,381.0f);
        fitCurve.add(755.0f,379.0f);
        fitCurve.add(749.0f,377.0f);
        fitCurve.add(744.0f,375.0f);
        fitCurve.add(738.0f,372.0f);
        fitCurve.add(727.0f,365.0f);
        fitCurve.add(721.0f,362.0f);
        fitCurve.add(715.0f,359.0f);
        fitCurve.add(709.0f,356.0f);
        fitCurve.add(697.0f,349.0f);
        fitCurve.add(691.0f,345.0f);
        fitCurve.add(683.0f,339.0f);
        fitCurve.add(680.0f,337.0f);
        fitCurve.add(677.0f,332.0f);
        fitCurve.add(672.0f,325.0f);
        fitCurve.add(669.0f,319.0f);
        fitCurve.add(666.0f,309.0f);
        fitCurve.add(664.0f,306.0f);
        fitCurve.add(663.0f,303.0f);
        fitCurve.add(662.0f,299.0f);
        fitCurve.add(661.0f,295.0f);
        fitCurve.add(661.0f,293.0f);
        fitCurve.add(661.0f,291.0f);
        fitCurve.add(661.0f,290.0f);
        fitCurve.add(661.0f,289.0f);
        fitCurve.add(661.0f,287.0f);
        fitCurve.add(662.0f,285.0f);
        fitCurve.add(663.0f,284.0f);
        fitCurve.add(665.0f,282.0f);
        fitCurve.add(666.0f,280.0f);
        fitCurve.add(668.0f,277.0f);
        fitCurve.add(669.0f,275.0f);
        fitCurve.add(671.0f,273.0f);
        fitCurve.add(672.0f,270.0f);
        fitCurve.add(674.0f,268.0f);
        fitCurve.add(676.0f,266.0f);
        fitCurve.add(679.0f,262.0f);
        fitCurve.add(680.0f,261.0f);
        fitCurve.add(685.0f,257.0f);
        fitCurve.add(689.0f,255.0f);
        fitCurve.add(691.0f,254.0f);
        fitCurve.add(701.0f,250.0f);
        fitCurve.add(707.0f,247.0f);
        fitCurve.add(713.0f,245.0f);
        fitCurve.add(727.0f,241.0f);
        fitCurve.add(734.0f,239.0f);
        fitCurve.add(747.0f,235.0f);
        fitCurve.add(754.0f,233.0f);
        fitCurve.add(761.0f,231.0f);
        fitCurve.add(774.0f,227.0f);
        fitCurve.add(781.0f,225.0f);
        fitCurve.add(789.0f,223.0f);
        fitCurve.add(806.0f,219.0f);
        fitCurve.add(813.0f,217.0f);
        fitCurve.add(820.0f,215.0f);
        fitCurve.add(831.0f,211.0f);
        fitCurve.add(837.0f,209.0f);
        fitCurve.add(841.0f,208.0f);
        fitCurve.add(846.0f,206.0f);
        fitCurve.add(852.0f,204.0f);
        fitCurve.add(858.0f,201.0f);
        fitCurve.add(862.0f,197.0f);
        fitCurve.add(867.0f,194.0f);
        fitCurve.add(871.0f,191.0f);
        fitCurve.add(874.0f,187.0f);
        fitCurve.add(879.0f,180.0f);
        fitCurve.add(881.0f,177.0f);
        fitCurve.add(883.0f,173.0f);
        fitCurve.add(886.0f,166.0f);
        fitCurve.add(888.0f,161.0f);
        fitCurve.add(891.0f,156.0f);
        fitCurve.add(897.0f,146.0f);
        fitCurve.add(900.0f,140.0f);
        fitCurve.add(902.0f,134.0f);
        fitCurve.add(908.0f,122.0f);
        fitCurve.add(911.0f,116.0f);
        fitCurve.add(914.0f,110.0f);
        fitCurve.add(916.0f,105.0f);
        fitCurve.add(922.0f,94.0f);
        fitCurve.add(924.0f,88.0f);
        fitCurve.add(930.0f,78.0f);
        fitCurve.add(933.0f,72.0f);
        fitCurve.add(936.0f,66.0f);
        fitCurve.add(941.0f,54.0f);
        fitCurve.add(944.0f,48.0f);
        fitCurve.add(946.0f,44.0f);
        fitCurve.add(949.0f,40.0f);
        fitCurve.add(951.0f,36.0f);
        fitCurve.add(952.0f,34.0f);
        fitCurve.add(954.0f,31.0f);
        fitCurve.add(956.0f,29.0f);
        fitCurve.add(957.0f,28.0f);
        fitCurve.add(960.0f,25.0f);
        fitCurve.add(962.0f,23.0f);
        fitCurve.add(963.0f,22.0f);
        fitCurve.add(966.0f,19.0f);
        fitCurve.add(967.0f,18.0f);
        fitCurve.add(969.0f,16.0f);
        fitCurve.add(971.0f,14.0f);
        fitCurve.add(971.0f,13.0f);
        fitCurve.add(972.0f,13.0f);
        fitCurve.fitCurve();
        System.out.println("Curves : " + fitCurve._curves.size());
        System.out.println("Points : " + fitCurve._points.size());
    }

    public void draw() {
        setFocus();
        //handleMouse();
        //scene.beginDraw();
        //canvas1.background(0);
        background(0);
        //canvas1.stroke(255,0,0);
        Joint.setPGraphics(scene.frontBuffer());
        stroke(255,0,0);
        scene.drawAxes();
        scene.traverse();
        for(Target target : targets){
            scene.drawPath(target._interpolator, 5);
        }
        /*
        panel._scene.beginDraw();
        panel._scene.frontBuffer().background(0);
        if(panel._frame != null)
            panel._scene.traverse();
        panel._scene.endDraw();
        panel._scene.display();
        */
        /*
        scene.beginHUD();
        for(AuxiliaryView view : views) {
            InteractiveJoint.setPGraphics(scene.frontBuffer());
            image(view._pGraphics,0,0, width, height);
        }
        scene.endHUD();
        */

        for(int i = 0; i < views.length; i++) {
            scene.shift(views[i]);
            Joint.setPGraphics(views[i].frontBuffer());
            scene.beginHUD();
            views[i].beginDraw();
            views[i].frontBuffer().background(175, 200, 20);
            views[i].drawAxes();
            views[i].traverse();
            views[i].endDraw();
            views[i].display();
            if(fitCurve != null) fitCurve.drawCurves(scene.frontBuffer());
            scene.endHUD();
            views[i].shift(scene);

        }
    }

    public void setFocus(){
        if(mouseY <= 2*h/3) focus = scene;
        else if(mouseX <= w/3) focus =  views[0];
        else if(mouseX <= 2*w/3) focus =  views[1];
        else focus = views[2];
    }

    //mouse events
    @Override
    public void mouseMoved() {
        if(!mousePressed) {
            focus.cast();
        }
    }

    public void mouseDragged(MouseEvent event) {
        Point previous = new Point(pmouseX, pmouseY);
        Point point = new  Point(mouseX, mouseY);
        if (mouseButton == RIGHT && event.isControlDown()) {
            Vector mouse = new Vector(point.x(), point.y());
            if(focus.trackedFrame() != null)
                focus.trackedFrame().interact("OnAdding", mouse);
        } else if (mouseButton == LEFT) {
            if(event.isControlDown() && fitCurve != null){
                fitCurve.add(mouseX, mouseY);
            } else {
                focus.spin(previous, point);
            }
        } else if (mouseButton == RIGHT) {
            focus.translate(point.x() - previous.x(), point.y() - previous.y());
            Target.multipleTranslate();
        } else if (mouseButton == CENTER){
            focus.scale(scene.mouseDX());
        } else if(scene.trackedFrame() != null)
            focus.trackedFrame().interact("Reset");
        //PANEL
        //else {
            //panel._scene.defaultFrame().interact();
        //}
        //if(focus == scene)panel.updateFrameOptions();
        //if(focus == scene && !Target.selectedTargets().contains(focus.trackedFrame())){
        //    Target.clearSelectedTargets();
        //}
        if(!Target.selectedTargets().contains(scene.trackedFrame())){
            Target.clearSelectedTargets();
        }
    }

    public void mousePressed(MouseEvent event){
        if(focus.trackedFrame() == null){
            fitCurve = new FitCurve();
        }
    }

    public void mouseReleased(){
        if(fitCurve != null) {
            fitCurve.fitCurve();
            fitCurve = null;
        }
        Point previous = new Point(pmouseX, pmouseY);
        Point point = new  Point(mouseX, mouseY);
        Vector mouse = new Vector(point.x(), point.y());
        //mouse = scene.location(mouse);
        //mouse = Vector.projectVectorOnPlane(mouse, scene.viewDirection());
        //mouse.add(scene.defaultFrame().position());
        if(focus.trackedFrame() != null)
            focus.trackedFrame().interact("Add", mouse, false);
    }

    public void mouseWheel(MouseEvent event) {
        focus.scale(event.getCount() * 20);
    }

    public void mouseClicked(MouseEvent event) {
        if (event.getButton() == LEFT) {
            if (event.getCount() == 1) {
                //panel.setFrame(scene.trackedFrame());
                if(event.isControlDown()){
                    if(focus.trackedFrame() != null)
                        focus.trackedFrame().interact("KeepSelected");
                }
            }
            else if (event.getCount() == 2) {
                if (event.isShiftDown())
                    if(scene.trackedFrame() != null)
                        scene.trackedFrame().interact("Remove");
                else
                    focus.focus();
            }
            else {
                focus.align();
            }
        }
    }

    public void keyPressed(){
        if(key == '+'){
            new InteractiveJoint(scene, radius).setRoot(true);
        }
        if(key == 'A' || key == 'a'){
            addTreeSolver();
        }
        if(key == 'C' || key == 'c'){
            addConstraint(focus.trackedFrame());
        }
        if(key == 'S' || key == 's'){
            minAngle += radians(5);
            if(minAngle >= radians(170) ) minAngle = radians(170);
            System.out.println("minAngle : " + degrees(minAngle));
        }
        if(key == 'D' || key == 'd'){
            minAngle -= radians(5);
            if(minAngle <= radians(0) ) minAngle = radians(0);
            System.out.println("minAngle : " + degrees(minAngle));
        }
        if(key == 'F' || key == 'f'){
            maxAngle += radians(5);
            if(maxAngle >= radians(170) ) maxAngle = radians(170);
            System.out.println("maxAngle : " + degrees(maxAngle));
        }
        if(key == 'G' || key == 'g'){
            maxAngle -= radians(5);
            if(maxAngle <= radians(0) ) maxAngle = radians(0);
            System.out.println("maxAngle : " + degrees(maxAngle));
        }
        if(key==' '){
            for(Target target : targets){
                target._interpolator.start();
            }
        }

    }

    public void findEndEffectors(Frame frame, List<Frame> endEffectors){
        if(frame.children().isEmpty()){
            endEffectors.add(frame);
            return;
        }
        for(Frame child : frame.children()){
            findEndEffectors(child, endEffectors);
        }
    }

    public void addConstraint(Frame frame){
        //If has a child
        if(frame == null) return;
        if(frame.children().size() != 1) return;
        if(scene.is3D()) {
            BallAndSocket constraint = new BallAndSocket(minAngle, minAngle, maxAngle, maxAngle);
            Vector twist = frame.children().get(0).translation().get();
            constraint.setRestRotation(frame.rotation().get(), Vector.orthogonalVector(twist), twist);
            frame.setConstraint(constraint);
        } else{
            Hinge constraint = new Hinge(true, minAngle, maxAngle);
            constraint.setRestRotation(frame.rotation().get());
            frame.setConstraint(constraint);
        }

    }

    public void addTreeSolver(){
        if(scene.trackedFrame() == null) return;
        scene.registerTreeSolver(scene.trackedFrame());
        //add target
        //get leaf nodes
        ArrayList<Frame> endEffectors = new ArrayList<Frame>();
        findEndEffectors(focus.trackedFrame(), endEffectors);
        for(Frame endEffector : endEffectors) {
            Target target = new Target(scene, endEffector);
            scene.addIKTarget(endEffector, target);
            targets.add(target);
        }
    }
}

