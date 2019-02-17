package ik.interactive;

import frames.core.Frame;
import frames.core.Interpolator;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

/*
Adapted from an Algorithm for Automatically Fitting Digitized Curves
by Philip J. Schneider from "Graphics Gems 1st Edition"
*/

public class FitCurve {

    public static class Bezier {
        protected Vector[] _points = new Vector[4];

        protected void addPoint(int i, Vector point){
            _points [i] = point;
        }


        //Bernstein Equations
        public static float B03(float u){
            float mu = (1-u);
            return mu * mu * mu;
        }

        public static float B13(float u){
            float mu = (1-u);
            return 3 * u * mu * mu;
        }

        public static float B23(float u){
            float mu = (1-u);
            return 3 * u * u * mu;
        }

        public static float B33(float u){
            return u * u * u;
        }

        public Vector evaluate(float u){
            return  evaluate(_points, u);
        }

        /*Casteljau's Algorithm*/
        public static Vector evaluate(Vector[] points, float u){
            if(points.length == 1)
                return points[0];
            Vector[] new_points = new Vector[points.length - 1];
            for(int i = 0; i < new_points.length; i++){
                new_points[i] = Vector.add(Vector.multiply(points[i], 1 - u),
                        Vector.multiply(points[i + 1],  u));
            }
            return evaluate(new_points, u);
        }
    }

    public static float PIXEL_ERROR = 5.f;
    public static float SQ_ERROR = 50.f;
    public static float SQ_ITERATION_ERROR = SQ_ERROR * 4.f;
    public static int MAX_ITERATIONS = 4;

    protected List<Vector> _points = new ArrayList<Vector>();
    protected List<Float> _distances = new ArrayList<Float>();
    protected List<Bezier> _curves = new ArrayList<Bezier>();

    protected int _split = -1;
    protected boolean _changed = false;
    protected boolean _started = false;

    public boolean started(){
        return _started;
    }

    public void setStarted(boolean started){
        _started = started;
    }

    public void add(float x, float y){
        Vector point = new Vector(x,y);
        //Add only if point is far enough
        int n = _points.size();
        if(n < 1){
            if(_points == null) _points = new ArrayList<Vector>();
            _points.add(point);
            _distances.add(0.f);
            _changed = true;
            return;
        }
        float dist = Vector.distance(point, _points.get(n-1));
        if(dist < PIXEL_ERROR) return;
        _points.add(point);
        _distances.add(_distances.get(_distances.size()-1) + dist);
        _changed = true;
    }

    protected float[] _findParameters(int first, int last){
        float[] u = new float[last - first + 1];
        float length = _distances.get(last) - _distances.get(first);
        for (int i = first; i <= last; i++) {
            u[i - first] = (_distances.get(i) -  _distances.get(first))/ length;
        }
        return u;
    }

    public void fitCurve(){
        if(!_changed || _points.size() < 2) return;

        _curves = new ArrayList<Bezier>();
        Vector t1 = Vector.subtract(_points.get(1), _points.get(0)).normalize(null);
        Vector t2 = Vector.subtract(_points.get(_points.size() - 2), _points.get(_points.size() - 1)).normalize(null);
        fitCubic(0, _points.size() - 1, t1, t2);
    }

    public void fitCubic(int first, int last, Vector t1, Vector t2){
        int n = last - first + 1;
        //In case the number of points is 2 then use recommended heuristic
        if(n == 2){
            float dist = Vector.distance(_points.get(last), _points.get(first)) / 3.f;
            Bezier curve = new Bezier();
            curve.addPoint(0, _points.get(first));
            curve.addPoint(1, Vector.add(_points.get(first), Vector.multiply(t1, dist)));
            curve.addPoint(2, Vector.add(_points.get(last), Vector.multiply(t2, dist)));
            curve.addPoint(3, _points.get(last));
            _curves.add(curve);
            return;
        }
        //Compute chord length parametrization
        float[] u = _findParameters(first, last);
        //Find cubic bezier using least squares optimization
        Bezier curve = generateBezier(first, last, u, t1, t2);
        //Get error
        float max_error = maxError(first, last, curve, u);
        int center = _split;

        if(max_error < SQ_ERROR){
            _curves.add(curve);
            return;
        }
        /*  If error not too large, try some reparameterization  */
        if (SQ_ERROR < SQ_ITERATION_ERROR) {
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                u = reparameterize(first, last, u, curve);
                curve = generateBezier(first, last, u, t1, t2);
                max_error = maxError(first, last, curve, u);
                if (max_error < SQ_ERROR) {
                    _curves.add(curve);
                    return;
                }
            }
        }

        /* Fitting failed -- split at max error point and fit recursively */
        Vector t_center = Vector.subtract(_points.get(center - 1), _points.get(center));
        t_center.add(Vector.subtract(_points.get(center), _points.get(center + 1)));
        t_center.multiply(0.5f);
        t_center.normalize();
        fitCubic(first, center, t1, t_center);
        t_center.multiply(-1);
        fitCubic(center, last, t_center, t2);
    }

    public float maxError(int first, int last, Bezier curve, float[] u) {
        float max = 0.f;
        _split =  (last - first + 1)/2;
        for (int i = first + 1; i < last; i++) {
            Vector point = curve.evaluate(u[i - first]);
            float dist = Vector.subtract(point, _points.get(i)).squaredMagnitude();
            if (dist > max) {
                max = dist;
                _split = i;
            }

        }
        return max;
    }

    /*
     * See An algorithm for automatically fitting digitized curves: https://dl.acm.org/citation.cfm?id=90941
     */
    public Bezier generateBezier(int first, int last, float[] u, Vector t1, Vector t2){
        int n = last - first + 1;
        Vector[][] 	A = new Vector[n][2];	        /* Precomputed rhs for eqn	*/
        float[][] 	C = new float [2][2];			/* Matrix C		*/
        float[] 	X = new float [2];			    /* Matrix X		*/
        float 	det_C0_C1, det_C0_X, det_X_C1;      /* Determinants of matrices	*/
        float 	alpha_l, alpha_r;                   /* Alpha values, left and right	*/
        Bezier curve = new Bezier();

        /* Compute the A's, C's and X*/
        for (int i = 0; i < n; i++) {
            A[i][0] = Vector.multiply(t1, Bezier.B13(u[i]));
            A[i][1] = Vector.multiply(t2, Bezier.B23(u[i]));
            C[0][0] += Vector.dot(A[i][0],A[i][0]);
            C[0][1] += Vector.dot(A[i][0],A[i][1]);
            C[1][0] = C[0][1];
            C[1][1] += Vector.dot(A[i][1],A[i][1]);
            Vector rhs = _points.get(first + i).get();
            rhs.subtract(Vector.multiply(_points.get(first), Bezier.B03(u[i])));
            rhs.subtract(Vector.multiply(_points.get(first), Bezier.B13(u[i])));
            rhs.subtract(Vector.multiply(_points.get(last), Bezier.B23(u[i])));
            rhs.subtract(Vector.multiply(_points.get(last), Bezier.B33(u[i])));
            X[0] += Vector.dot(rhs, A[i][0]);
            X[1] += Vector.dot(rhs, A[i][1]);
        }

        /* Compute the determinants of C and X	*/
        det_C0_C1 = C[0][0] * C[1][1] - C[1][0] * C[0][1];
        det_C0_X  = C[0][0] * X[1]    - C[1][0] * X[0];
        det_X_C1  = X[0]    * C[1][1] - X[1]    * C[0][1];

        /* Finally, derive alpha values	*/
        alpha_l = (det_C0_C1 == 0) ? 0.f : det_X_C1 / det_C0_C1;
        alpha_r = (det_C0_C1 == 0) ? 0.f : det_C0_X / det_C0_C1;

        /* If alpha negative, use the Wu/Barsky heuristic (see text) */
        /* (if alpha is 0, you get coincident control points that lead to
         * divide by zero in any subsequent NewtonRaphsonRootFind() call. */
        float segLength = Vector.distance(_points.get(first), _points.get(last));
        float epsilon = 1.0e-6f * segLength;
        if (alpha_l < epsilon || alpha_r < epsilon)
        {
            /* fall back on standard (probably inaccurate) formula, and subdivide further if needed. */
            float dist = segLength / 3.f;
            curve.addPoint(0, _points.get(first).get());
            curve.addPoint(1, Vector.add(_points.get(first), Vector.multiply(t1, dist)));
            curve.addPoint(2, Vector.add(_points.get(last), Vector.multiply(t2, dist)));
            curve.addPoint(3, _points.get(last).get());
            return curve;
        }

        /*  First and last control points of the Bezier curve are */
        /*  positioned exactly at the first and last data points */
        /*  Control points 1 and 2 are positioned an alpha distance out */
        /*  on the tangent vectors, left and right, respectively */
        curve.addPoint(0, _points.get(first).get());
        curve.addPoint(1, Vector.add(_points.get(first), Vector.multiply(t1, alpha_l)));
        curve.addPoint(2, Vector.add(_points.get(last), Vector.multiply(t2, alpha_r)));
        curve.addPoint(3, _points.get(last).get());
        return curve;
    }


    /*
     *  Reparameterize:
     *	Given set of points and their parameterization, try to find
     *   a better parameterization.
     */
    public float[] reparameterize(int first, int last, float[] u, Bezier curve){
        int n = last-first+1;
        float[] new_u = new float[n];

        for (int i = first; i <= last; i++) {
            new_u[i-first] = newtonRaphsonRootFind(curve, _points.get(i), u[i - first]);
        }
        return new_u;
    }

    /*
     *  NewtonRaphsonRootFind :
     *	Use Newton-Raphson iteration to find better root.
     */
    public float newtonRaphsonRootFind(Bezier Q, Vector P, float u){
        float numerator, denominator;
        Vector[] Q1 = new Vector[3], Q2 = new Vector[2];	/*  Q' and Q''			*/
        Vector Q_u, Q1_u, Q2_u; /*u evaluated at Q, Q', & Q''	*/
        float u_prime;		/*  Improved u  */
        Q_u = Q.evaluate(u);

        /* Generate control vertices for Q'	*/
        for (int i = 0; i <= 2; i++)
            Q1[i] = Vector.multiply(Vector.subtract(Q._points[i+1], Q._points[i]), 3);

        /* Generate control vertices for Q'' */
        for (int i = 0; i <= 1; i++) {
            Q2[i] = Vector.multiply(Vector.subtract(Q1[i+1], Q1[i]), 2);
        }

        /* Compute Q'(u) and Q''(u)	*/
        Q1_u = Bezier.evaluate(Q1, u);
        Q2_u = Bezier.evaluate(Q2, u);

        /* Compute f(u)/f'(u) */
        Vector diff = Vector.subtract(Q_u, P);
        numerator = Vector.dot(diff, Q1_u);
        denominator =  Vector.dot(Q1_u, Q1_u) + Vector.dot(diff, Q2_u);
        if (denominator == 0.0f) return u;

        /* u = u - f(u)/f'(u) */
        u_prime = u - (numerator/denominator);
        return u_prime;
    }


    /*From Cubic Bezier to Catmull Rom*/
    Interpolator _interpolator;
    public void getCatmullRomCurve(Scene scene, float depth){
        _interpolator = new Interpolator(scene);
        for(int i = 0; i < _curves.size(); i++){
            Bezier curve = _curves.get(i);
            Vector v = curve.evaluate(0);
            v.setZ(depth);
            v = scene.location(v);
            Frame f = new Frame(scene);
            f.setPosition(v);
            _interpolator.addKeyFrame(f);
            if(i == 0) {
                v = curve.evaluate(0.15f);
                v.setZ(depth);
                v = scene.location(v);
                f = new Frame(scene);
                f.setPosition(v);
                _interpolator.addKeyFrame(f);
            }
            v = curve.evaluate(0.333f);
            v.setZ(depth);
            v = scene.location(v);
            f = new Frame(scene);
            f.setPosition(v);
            _interpolator.addKeyFrame(f);
            v = curve.evaluate(0.666f);
            v.setZ(depth);
            v = scene.location(v);
            f = new Frame(scene);
            f.setPosition(v);
            _interpolator.addKeyFrame(f);
            if(i == _curves.size() - 1) {
                v = curve.evaluate(1);
                v.setZ(depth);
                v = scene.location(v);
                f = new Frame(scene);
                f.setPosition(v);
                _interpolator.addKeyFrame(f);
            }
        }
    }


    public void drawCurves(PGraphics pg){
        if(_curves == null) return;
        pg.pushStyle();
        pg.noFill();
        for(Bezier curve : _curves){
            pg.stroke(255);
            pg.strokeWeight(4);
            pg.bezier(curve._points[0].x(), curve._points[0].y(),
                    curve._points[1].x(), curve._points[1].y(),
                    curve._points[2].x(), curve._points[2].y(),
                    curve._points[3].x(), curve._points[3].y());
            pg.strokeWeight(10);
            pg.stroke(255,0,0, 100);
            pg.point(curve._points[0].x(), curve._points[0].y());
            pg.point(curve._points[1].x(), curve._points[1].y());
            pg.point(curve._points[2].x(), curve._points[2].y());
            pg.point(curve._points[3].x(), curve._points[3].y());

            pg.strokeWeight(12);
            pg.stroke(255,255,0, 50);
            Vector v = curve.evaluate(0);
            pg.point(v.x(), v.y());
            v = curve.evaluate(0.333f);
            pg.point(v.x(), v.y());
            v = curve.evaluate(0.666f);
            pg.point(v.x(), v.y());
            v = curve.evaluate(1);
            pg.point(v.x(), v.y());

            /*pg.point(curve._points[3].x() + 6*(curve._points[0].x() - curve._points[1].x()),
                    curve._points[3].y() + 6*(curve._points[0].y() - curve._points[1].y()));
            pg.point(curve._points[0].x(), curve._points[0].y());
            pg.point(curve._points[3].x(), curve._points[3].y());
            pg.point(curve._points[0].x() + 6*(curve._points[3].x() - curve._points[2].x()),
                    curve._points[0].y() + 6*(curve._points[3].y() - curve._points[2].y()));*/


        }
        pg.popStyle();
    }

    public void printCurves(){
        for(Bezier curve : _curves) {
            System.out.print("[");
            for(Vector p : curve._points){
                System.out.print( p + ", ");

            }
            System.out.println("]");
        }

    }
}
