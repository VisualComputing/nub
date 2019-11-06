package nub.ik.solver.geometric;

import javafx.util.Pair;
import nub.primitives.Matrix;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.ArrayList;
import java.util.List;

/*******************************************************************************
 * This is an adaptation of qcprot.c for fruther information look at https://theobald.brandeis.edu/qcp/
 *
 *  -/_|:|_|_\-
 *
 *  File:           qcprot.c
 *  Version:        1.5
 *
 *  Function:       Rapid calculation of the least-squares rotation using a
 *                  quaternion-based characteristic polynomial and
 *                  a cofactor matrix
 *
 *  Author(s):      Douglas L. Theobald
 *                  Department of Biochemistry
 *                  MS 009
 *                  Brandeis University
 *                  415 South St
 *                  Waltham, MA  02453
 *                  USA
 *
 *                  dtheobald@brandeis.edu
 *
 *                  Pu Liu
 *                  Johnson & Johnson Pharmaceutical Research and Development, L.L.C.
 *                  665 Stockton Drive
 *                  Exton, PA  19341
 *                  USA
 *
 *                  pliu24@its.jnj.com
 *
 *
 *    If you use this QCP rotation calculation method in a publication, please
 *    reference:
 *
 *      Douglas L. Theobald (2005)
 *      "Rapid calculation of RMSD using a quaternion-based characteristic
 *      polynomial."
 *      Acta Crystallographica A 61(4):478-480.
 *
 *      Pu Liu, Dmitris K. Agrafiotis, and Douglas L. Theobald (2009)
 *      "Fast determination of the optimal rotational matrix for macromolecular
 *      superpositions."
 *      Journal of Computational Chemistry 31(7):1561-1563.
 *
 *  Copyright (c) 2009-2016 Pu Liu and Douglas L. Theobald
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are permitted
 *  provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *  * Neither the name of the <ORGANIZATION> nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific prior written
 *    permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  Source:         started anew.
 *
 *  Change History:
 *    2009/04/13      Started source
 *    2010/03/28      Modified FastCalcRMSDAndRotation() to handle tiny qsqr
 *                    If trying all rows of the adjoint still gives too small
 *                    qsqr, then just return identity matrix. (DLT)
 *    2010/06/30      Fixed prob in assigning A[9] = 0 in InnerProduct()
 *                    invalid mem access
 *    2011/02/21      Made CenterCoords use weights
 *    2011/05/02      Finally changed CenterCoords declaration in qcprot.h
 *                    Also changed some functions to static
 *    2011/07/08      Put in fabs() to fix taking sqrt of small neg numbers, fp error
 *    2012/07/26      Minor changes to comments and main.c, more info (v.1.4)
 *    2016/07/13      Fixed normalization of RMSD in FastCalcRMSDAndRotation(), should divide by
 *                    sum of weights (thanks to Geoff Skillman)
 *
 ******************************************************************************/


public class QCP {
    static float InnerProduct(float[] A, List<Vector> coords1, List<Vector> coords2, List<Float> weight){
        float          x1, x2, y1, y2, z1, z2;
        int             i;
        float          G1 = 0.0f, G2 = 0.0f;
        int len = coords1.size();

        A[0] = A[1] = A[2] = A[3] = A[4] = A[5] = A[6] = A[7] = A[8] = 0.0f;

        if (weight != null){
            for (i = 0; i < len; ++i){
                x1 = weight.get(i) * coords1.get(i).x();
                y1 = weight.get(i) * coords1.get(i).y();
                z1 = weight.get(i) * coords1.get(i).z();

                G1 += x1 * coords1.get(i).x() + y1 * coords1.get(i).y() + z1 * coords1.get(i).z();

                x2 = coords2.get(i).x();
                y2 = coords2.get(i).y();
                z2 = coords2.get(i).z();

                G2 += weight.get(i) * (x2 * x2 + y2 * y2 + z2 * z2);

                A[0] +=  (x1 * x2);
                A[1] +=  (x1 * y2);
                A[2] +=  (x1 * z2);

                A[3] +=  (y1 * x2);
                A[4] +=  (y1 * y2);
                A[5] +=  (y1 * z2);

                A[6] +=  (z1 * x2);
                A[7] +=  (z1 * y2);
                A[8] +=  (z1 * z2);
            }
        }
        else {
            for (i = 0; i < len; ++i) {
                x1 = coords1.get(i).x();
                y1 = coords1.get(i).y();
                z1 = coords1.get(i).z();

                G1 += x1 * x1 + y1 * y1 + z1 * z1;

                x2 = coords2.get(i).x();
                y2 = coords2.get(i).y();
                z2 = coords2.get(i).z();

                G2 += (x2 * x2 + y2 * y2 + z2 * z2);

                A[0] +=  (x1 * x2);
                A[1] +=  (x1 * y2);
                A[2] +=  (x1 * z2);

                A[3] +=  (y1 * x2);
                A[4] +=  (y1 * y2);
                A[5] +=  (y1 * z2);

                A[6] +=  (z1 * x2);
                A[7] +=  (z1 * y2);
                A[8] +=  (z1 * z2);
            }
        }

        return (G1 + G2) * 0.5f;
    }


    static int FastCalcRMSDAndRotation(float[] rot, float[] A, float E0, float len, float minScore)
    {
        float Sxx, Sxy, Sxz, Syx, Syy, Syz, Szx, Szy, Szz;
        float Szz2, Syy2, Sxx2, Sxy2, Syz2, Sxz2, Syx2, Szy2, Szx2,
                SyzSzymSyySzz2, Sxx2Syy2Szz2Syz2Szy2, Sxy2Sxz2Syx2Szx2,
                SxzpSzx, SyzpSzy, SxypSyx, SyzmSzy,
                SxzmSzx, SxymSyx, SxxpSyy, SxxmSyy;
        float[] C = new float[4];
        int i;
        float mxEigenV;
        float oldg = 0.0f;
        float b, a, delta, rms, qsqr;
        float q1, q2, q3, q4, normq;
        float a11, a12, a13, a14, a21, a22, a23, a24;
        float a31, a32, a33, a34, a41, a42, a43, a44;
        float a2, x2, y2, z2;
        float xy, az, zx, ay, yz, ax;
        float a3344_4334, a3244_4234, a3243_4233, a3143_4133,a3144_4134, a3142_4132;
        float evecprec = 1e-6f;
        float evalprec = 1e-11f;

        Sxx = A[0]; Sxy = A[1]; Sxz = A[2];
        Syx = A[3]; Syy = A[4]; Syz = A[5];
        Szx = A[6]; Szy = A[7]; Szz = A[8];

        Sxx2 = Sxx * Sxx;
        Syy2 = Syy * Syy;
        Szz2 = Szz * Szz;

        Sxy2 = Sxy * Sxy;
        Syz2 = Syz * Syz;
        Sxz2 = Sxz * Sxz;

        Syx2 = Syx * Syx;
        Szy2 = Szy * Szy;
        Szx2 = Szx * Szx;

        SyzSzymSyySzz2 = 2.0f*(Syz*Szy - Syy*Szz);
        Sxx2Syy2Szz2Syz2Szy2 = Syy2 + Szz2 - Sxx2 + Syz2 + Szy2;

        C[2] = -2.0f * (Sxx2 + Syy2 + Szz2 + Sxy2 + Syx2 + Sxz2 + Szx2 + Syz2 + Szy2);
        C[1] = 8.0f * (Sxx*Syz*Szy + Syy*Szx*Sxz + Szz*Sxy*Syx - Sxx*Syy*Szz - Syz*Szx*Sxy - Szy*Syx*Sxz);

        SxzpSzx = Sxz + Szx;
        SyzpSzy = Syz + Szy;
        SxypSyx = Sxy + Syx;
        SyzmSzy = Syz - Szy;
        SxzmSzx = Sxz - Szx;
        SxymSyx = Sxy - Syx;
        SxxpSyy = Sxx + Syy;
        SxxmSyy = Sxx - Syy;
        Sxy2Sxz2Syx2Szx2 = Sxy2 + Sxz2 - Syx2 - Szx2;

        C[0] = Sxy2Sxz2Syx2Szx2 * Sxy2Sxz2Syx2Szx2
                + (Sxx2Syy2Szz2Syz2Szy2 + SyzSzymSyySzz2) * (Sxx2Syy2Szz2Syz2Szy2 - SyzSzymSyySzz2)
                + (-(SxzpSzx)*(SyzmSzy)+(SxymSyx)*(SxxmSyy-Szz)) * (-(SxzmSzx)*(SyzpSzy)+(SxymSyx)*(SxxmSyy+Szz))
                + (-(SxzpSzx)*(SyzpSzy)-(SxypSyx)*(SxxpSyy-Szz)) * (-(SxzmSzx)*(SyzmSzy)-(SxypSyx)*(SxxpSyy+Szz))
                + (+(SxypSyx)*(SyzpSzy)+(SxzpSzx)*(SxxmSyy+Szz)) * (-(SxymSyx)*(SyzmSzy)+(SxzpSzx)*(SxxpSyy+Szz))
                + (+(SxypSyx)*(SyzmSzy)+(SxzmSzx)*(SxxmSyy-Szz)) * (-(SxymSyx)*(SyzpSzy)+(SxzmSzx)*(SxxpSyy-Szz));

        /* Newton-Raphson */
        mxEigenV = E0;
        for (i = 0; i < 50; ++i)
        {
            oldg = mxEigenV;
            x2 = mxEigenV*mxEigenV;
            b = (x2 + C[2])*mxEigenV;
            a = b + C[1];
            delta = ((a*mxEigenV + C[0])/(2.0f*x2*mxEigenV + b + a));
            mxEigenV -= delta;
            if (Math.abs(mxEigenV - oldg) < Math.abs(evalprec*mxEigenV))
                break;
        }

        if (i == 50)
            System.err.println("\nMore than " + i + " iterations needed!\n");

        rms = (float) Math.sqrt(Math.abs(2.0f * (E0 - mxEigenV)/len));

        if (minScore > 0)
            if (rms < minScore)
                return (-1); // Don't bother with rotation.

        a11 = SxxpSyy + Szz-mxEigenV; a12 = SyzmSzy; a13 = - SxzmSzx; a14 = SxymSyx;
        a21 = SyzmSzy; a22 = SxxmSyy - Szz-mxEigenV; a23 = SxypSyx; a24= SxzpSzx;
        a31 = a13; a32 = a23; a33 = Syy-Sxx-Szz - mxEigenV; a34 = SyzpSzy;
        a41 = a14; a42 = a24; a43 = a34; a44 = Szz - SxxpSyy - mxEigenV;
        a3344_4334 = a33 * a44 - a43 * a34; a3244_4234 = a32 * a44-a42*a34;
        a3243_4233 = a32 * a43 - a42 * a33; a3143_4133 = a31 * a43-a41*a33;
        a3144_4134 = a31 * a44 - a41 * a34; a3142_4132 = a31 * a42-a41*a32;
        q1 =  a22*a3344_4334-a23*a3244_4234+a24*a3243_4233;
        q2 = -a21*a3344_4334+a23*a3144_4134-a24*a3143_4133;
        q3 =  a21*a3244_4234-a22*a3144_4134+a24*a3142_4132;
        q4 = -a21*a3243_4233+a22*a3143_4133-a23*a3142_4132;

        qsqr = q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4;

        /* The following code tries to calculate another column in the adjoint matrix when the norm of the
           current column is too small.
           Usually this block will never be activated.  To be absolutely safe this should be
           uncommented, but it is most likely unnecessary.
        */
        if (qsqr < evecprec)
        {
            q1 =  a12*a3344_4334 - a13*a3244_4234 + a14*a3243_4233;
            q2 = -a11*a3344_4334 + a13*a3144_4134 - a14*a3143_4133;
            q3 =  a11*a3244_4234 - a12*a3144_4134 + a14*a3142_4132;
            q4 = -a11*a3243_4233 + a12*a3143_4133 - a13*a3142_4132;
            qsqr = q1*q1 + q2 *q2 + q3*q3+q4*q4;

            if (qsqr < evecprec)
            {
                float a1324_1423 = a13 * a24 - a14 * a23, a1224_1422 = a12 * a24 - a14 * a22;
                float a1223_1322 = a12 * a23 - a13 * a22, a1124_1421 = a11 * a24 - a14 * a21;
                float a1123_1321 = a11 * a23 - a13 * a21, a1122_1221 = a11 * a22 - a12 * a21;

                q1 =  a42 * a1324_1423 - a43 * a1224_1422 + a44 * a1223_1322;
                q2 = -a41 * a1324_1423 + a43 * a1124_1421 - a44 * a1123_1321;
                q3 =  a41 * a1224_1422 - a42 * a1124_1421 + a44 * a1122_1221;
                q4 = -a41 * a1223_1322 + a42 * a1123_1321 - a43 * a1122_1221;
                qsqr = q1*q1 + q2 *q2 + q3*q3+q4*q4;

                if (qsqr < evecprec)
                {
                    q1 =  a32 * a1324_1423 - a33 * a1224_1422 + a34 * a1223_1322;
                    q2 = -a31 * a1324_1423 + a33 * a1124_1421 - a34 * a1123_1321;
                    q3 =  a31 * a1224_1422 - a32 * a1124_1421 + a34 * a1122_1221;
                    q4 = -a31 * a1223_1322 + a32 * a1123_1321 - a33 * a1122_1221;
                    qsqr = q1*q1 + q2 *q2 + q3*q3 + q4*q4;

                    if (qsqr < evecprec)
                    {
                        /* if qsqr is still too small, return the identity matrix. */
                        rot[0] = rot[4] = rot[8] = 1.0f;
                        rot[1] = rot[2] = rot[3] = rot[5] = rot[6] = rot[7] = 0.0f;

                        return(0);
                    }
                }
            }
        }

        normq = (float) Math.sqrt(qsqr);
        q1 /= normq;
        q2 /= normq;
        q3 /= normq;
        q4 /= normq;

        a2 = q1 * q1;
        x2 = q2 * q2;
        y2 = q3 * q3;
        z2 = q4 * q4;

        xy = q2 * q3;
        az = q1 * q4;
        zx = q4 * q2;
        ay = q1 * q3;
        yz = q3 * q4;
        ax = q1 * q2;

        rot[0] = a2 + x2 - y2 - z2;
        rot[1] = 2 * (xy + az);
        rot[2] = 2 * (zx - ay);
        rot[3] = 2 * (xy - az);
        rot[4] = a2 - x2 + y2 - z2;
        rot[5] = 2 * (yz + ax);
        rot[6] = 2 * (zx + ay);
        rot[7] = 2 * (yz - ax);
        rot[8] = a2 - x2 - y2 + z2;

        return (1);
    }

    static Vector CalculateCentroid(List<Vector> coords, List<Float> weight){
        int             i, len = coords.size();
        float          xsum, ysum, zsum, wsum;
        xsum = ysum = zsum = 0.0f;
        if (weight != null)
        {
            wsum = 0.0f;
            for (i = 0; i < len; ++i)
            {
                xsum += weight.get(i) * coords.get(i).x();
                ysum += weight.get(i) * coords.get(i).y();
                zsum += weight.get(i) * coords.get(i).z();
                wsum += weight.get(i);
            }

            xsum /= wsum;
            ysum /= wsum;
            zsum /= wsum;
        }
        else
        {
            for (i = 0; i < len; ++i)
            {
                xsum += coords.get(i).x();
                ysum += coords.get(i).y();
                zsum += coords.get(i).z();
            }

            xsum /= len;
            ysum /= len;
            zsum /= len;
        }

        return new Vector(xsum, ysum, zsum);
    }

    static Vector[] CenterCoords(List<Vector> coords, Vector centroid) {
        int             i, len = coords.size();
        Vector[] center_coords = new Vector[coords.size()];

        for (i = 0; i < len; ++i)
        {
            center_coords[i] = new Vector(coords.get(i).x() - centroid.x(), coords.get(i).y() - centroid.y(), coords.get(i).z() - centroid.z());
        }
        return center_coords;
    }

    //Do not consider coords that are quite near to the origin
    static void filter(List<Vector> coords1, List<Vector> coords2, List<Float> weights){
        for(int i = coords1.size() - 1; i >= 0; i--){
            if(coords1.get(i).magnitude() < 0.1) {
                coords1.remove(i);
                coords2.remove(i);
                if(weights != null) weights.remove(i);
            }
        }
    }

    /* Superposition coords2 onto coords1 -- in other words, coords2 is rotated, coords1 is held fixed */
    static Quaternion CalcRMSDRotationalMatrix(List<Vector> coords1, List<Vector> coords2, List<Float> weight)
    {
        int i;
        float[] A = new float[9];
        float wsum;

        /* center the structures -- if precentered you can omit this step */
        //HERE WE DON'T CENTER THE COORDINATES CAUSE THEY'RE ALREADY IN A COMMON FRAME
        //Vector centroid1 = CalculateCentroid(coords1, weight);
        //Vector centroid2 = CalculateCentroid(coords2, weight);
        //coords1_centered = CenterCoords(coords1, centroid1);
        //coords2_centered = CenterCoords(coords2, centroid2);

        List<Vector> coords1_centered = new ArrayList<>(coords1);
        List<Vector> coords2_centered = new ArrayList<>(coords2);
        List<Float> filteredWeights = weight == null ? null : new ArrayList<>(weight);
        filter(coords1_centered, coords2_centered, filteredWeights);

        if (filteredWeights == null)
        {
            wsum = coords1_centered.size();
        }
        else
        {
            wsum = 0.0f;
            for (i = 0; i < coords1_centered.size(); ++i)
            {
                wsum += weight.get(i);
            }
        }

        /* calculate the (weighted) inner product of two structures */
        float E0 = InnerProduct(A, coords1_centered, coords2_centered, weight);

        float[] rot = new float[9];

        /* calculate the RMSD & rotational matrix */
        FastCalcRMSDAndRotation(rot, A, E0, wsum, -1);

        Vector x = new Vector((float) rot[0], (float) rot[3], (float) rot[6]);
        Vector y = new Vector((float) rot[1], (float) rot[4], (float) rot[7]);
        Vector z = new Vector((float) rot[2], (float) rot[5], (float) rot[8]);

        //Rotation
        Quaternion q = new Quaternion();
        q.fromRotatedBasis(x,y,z);
        return q;
    }


}

