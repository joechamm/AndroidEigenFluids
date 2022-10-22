
/*
 * MIT License
 *
 * Copyright (c) 2022 Joseph Cunningham
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.joechamm.eigenfluids;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;

import java.lang.Math;

public class ParticleRenderer implements GLSurfaceView.Renderer {

    public ParticleRenderer(int gridResolution, int N, boolean useTranslucentBackground) {
        mTranslucentBackground = useTranslucentBackground;
        mParticles = new ParticleArray(1000, true);

        mX = gridResolution;
        mY = gridResolution;

        mN = N;
        mVelocityField = new double[2][mX + 1][mY + 1];
        mCoefficients = new double[mN];
        mForcesDW = new double[mN];

        this.fillLookupTable();
        this.precomputeBasisFields();
        this.precomputeDynamics();

        mCoefficients[0] = 1.0;
        mForcesDW[0] = 1.0;
    }

    public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        mParticles.draw(gl);

        this.step();
        this.advectParticles();
        mParticles.update();
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrthof(0.0f, (float) Math.PI, 0.0f, (float) Math.PI, 0.0f, 1.0f);
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glDisable(GL10.GL_DITHER);

        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

        if (mTranslucentBackground) {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        } else {
            gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glEnable(GL10.GL_DEPTH_TEST);
    }

    public void step() {
        // Advance the simulation
        double[] dw = new double[mN];

        // Calculate current energy
        double previousEnergy = currentEnergy();

        double[][] dwt = new double[4][mN];
        double[][] qn = new double[4][mN];

        qn[0] = mCoefficients;

        for (int k = 0; k < mN; k++) {
            // Calculate Ck matrix vector products
            dwt[0][k] = dot(qn[0], mCk[k].mult(qn[0]));
            qn[1][k] = qn[0][k] + 0.5 * dwt[0][k] * mDt;
        }

        for (int k = 0; k < mN; k++) {
            dwt[1][k] = dot(qn[1], mCk[k].mult(qn[1]));
            qn[2][k] = qn[0][k] + 0.5 * dwt[1][k] * mDt;
        }

        for (int k = 0; k < mN; k++) {
            dwt[2][k] = dot(qn[2], mCk[k].mult(qn[2]));
            qn[3][k] = qn[0][k] + dwt[2][k] * mDt;
        }

        for (int k = 0; k < mN; k++) {
            dwt[3][k] = dot(qn[3], mCk[k].mult(qn[3]));
            dw[k] = (dwt[0][k] + 2.0 * dwt[1][k] + 2.0 * dwt[2][k] + dwt[3][k]) / 6.0;
        }

        // Take the explicit step
        for (int k = 0; k < mN; k++) {
            mCoefficients[k] += dw[k] * mDt;
        }

        // Re-normalize energy
        if (previousEnergy > 1e-5) {
            setEnergy(previousEnergy);
        }

        // Dissipate energy for viscosity
        for (int k = 0; k < mN; k++) {
            mCoefficients[k] *= Math.exp(-1.0 * mEigenvalues[k] * mDt * mViscosity);
            // Add external forces
            mCoefficients[k] += mForcesDW[k];
            mForcesDW[k] = 0.0;
        }

        // Reconstruct velocity field
        this.expandBasis();
    }

    public void attractParticles() {
        // Bunch up particles so we can watch them advect
        for (int i = 0; i < mParticles.mNumParticles; i++) {
            mParticles.mParticlesXY[i * 2] *= 0.2;
            mParticles.mParticlesXY[i * 2] += 0.1;
            mParticles.mParticlesXY[i * 2 + 1] *= 0.2;
            mParticles.mParticlesXY[i * 2 + 1] += 0.4;
        }
    }

    public void advectParticles() {
        // Advect particles using RK4 and cubic velocity interpolation

        double pdt = mDt * mPDTMult;

        boolean RK4 = true;
        boolean RK2 = false;
        boolean Euler = false;

        for (int i = 0; i < mParticles.mNumParticles; i++) {
            double x = mParticles.mParticlesXY[i * 2];
            double y = mParticles.mParticlesXY[i * 2 + 1];

            double nx = 0.0;
            double ny = 0.0;
            if (RK4) {
                double[] v0 = bilinearVelocity(x, y);
                double[] v1 = bilinearVelocity(x + 0.5 * pdt * v0[0], y + 0.5 * pdt * v0[1]);
                double[] v2 = bilinearVelocity(x + 0.5 * pdt * v1[0], y + 0.5 * pdt * v1[1]);
                double[] v3 = bilinearVelocity(x + pdt * v2[0], y + pdt * v2[1]);

                nx = x + pdt * (v0[0] + 2.0 * v1[0] + 2.0 * v2[0] + v3[0]) / 6.0;
                ny = y + pdt * (v0[1] + 2.0 * v1[1] + 2.0 * v2[1] + v3[1]) / 6.0;
            } else if (RK2) {
                double[] v0 = bilinearVelocity(x, y);
                double[] v1 = bilinearVelocity(x - 0.666 * pdt * v0[0], y - 0.666 * pdt * v0[1]);

                nx = x + pdt * (v0[0] + 3.0 * v1[0]) / 4.0;
                ny = y + pdt * (v0[1] + 3.0 * v1[1]) / 4.0;
            } else if (Euler) {
                double[] v0 = bilinearVelocity(x, y);
                nx = x + pdt * v0[0];
                ny = y + pdt * v0[1];
            }

            nx = clampDbl(nx, mMargin, 1.0 - mMargin);
            ny = clampDbl(ny, mMargin, 1.0 - mMargin);

            mParticles.mParticlesXY[(i + 1) * 2] = (float) nx;
            mParticles.mParticlesXY[(i + 1) * 2 + 1] = (float) ny;
        }
    }

    public void precomputeBasisFields() {
        mVelocityBasis = new double[mN][][][];

        for (int i = 0; i < mN; i++) {
            int k1 = this.basisLookup(i, 0);
            int k2 = this.basisLookup(i, 1);

            mVelocityBasis[i] = this.basisField2DRect(k1, k2, 1.0);
        }
    }

    public void precomputeDynamics() {
        // Precomputes structure coefficients for 2-D rectangle basis functions.

        mCk = new SparseMatrix[mN];

        // Allocate sparse matrices
        for (int i = 0; i < mN; i++) {
            mCk[i] = new SparseMatrix(mN, mN);
        }

        // Calculate the eigenvalues of each basis field.
        mEigenvalues = new double[mN];
        mInverseEigenvalues = new double[mN];
        mInverseEigenvaluesSqrt = new double[mN];

        for (int i = 0; i < mN; i++) {
            int k1 = this.basisLookup(i, 0);
            int k2 = this.basisLookup(i, 1);
            mEigenvalues[i] = k1 * k1 + k2 * k2;
            mInverseEigenvalues[i] = 1.0 / (k1 * k1 + k2 * k2);
            mInverseEigenvaluesSqrt[i] = 1.0 / Math.sqrt(k1 * k1 + k2 * k2);
        }

        for (int d1 = 0; d1 < mN; d1++) {
            int a1 = this.basisLookup(d1, 0);
            int a2 = this.basisLookup(d1, 1);

            double lambdaA = -1.0 * (a1 * a1 + a2 * a2);

            for (int d2 = 0; d2 < mN; d2++) {
                int b1 = this.basisLookup(d2, 0);
                int b2 = this.basisLookup(d2, 1);

                double lambdaB = -1.0 * (b1 * b1 + b2 * b2);
                double invLambdaB = -1.0 / (b1 * b1 + b2 * b2);

                int k1 = this.basisReverseLookup(a1, a2);
                int k2 = this.basisReverseLookup(b1, b2);

                int[][] antipairs = new int[4][2];
                antipairs[0][0] = a1 - b1;
                antipairs[0][1] = a2 - b2;
                antipairs[1][0] = a1 - b1;
                antipairs[1][1] = a2 + b2;
                antipairs[2][0] = a1 + b1;
                antipairs[2][1] = a2 - b2;
                antipairs[3][0] = a1 + b1;
                antipairs[3][1] = a2 + b2;

                for (int c = 0; c < 4; c++) {
                    int i = antipairs[c][0];
                    int j = antipairs[c][1];

                    int idx = this.basisReverseLookup(i, j);

                    if (idx != -1) {
                        double coefficient = invLambdaB * this.coefficientDensity(a1, a2, b1, b2, c, 0);
                        mCk[idx].set(k1, k2, -coefficient);
                        mCk[idx].set(k2, k1, coefficient * lambdaB / lambdaA);
                    }
                }
            }
        }
    }

    public double coefficientDensity(int a1, int b1, int a2, int b2, int c, int tt) {
        if (tt == 0) {
            // SS x SS
            if (c == 0)
                return -0.25 * (a1 * b2 - a2 * b1); // --
            if (c == 1)
                return 0.25 * (a1 * b2 + a2 * b1); // -+
            if (c == 2)
                return -0.25 * (a1 * b2 + a2 * b1); // +-
            if (c == 3)
                return 0.25 * (a1 * b2 - a2 * b1); // ++
        } else if (tt == 1) {
            // SC x SS
            if (c == 0)
                return -0.25 * (a1 * b2 - a2 * b1); // --
            if (c == 1)
                return -0.25 * (a1 * b2 + a2 * b1); // -+
            if (c == 2)
                return 0.25 * (a1 * b2 + a2 * b1); // +-
            if (c == 3)
                return 0.25 * (a1 * b2 - a2 * b1); // ++
        } else if (tt == 2) {
            // CS x SS
            if (c == 0)
                return -0.25 * (a1 * b2 - a2 * b1); // --
            if (c == 1)
                return -0.25 * (a1 * b2 + a2 * b1); // -+
            if (c == 2)
                return 0.25 * (a1 * b2 + a2 * b1); // +-
            if (c == 3)
                return 0.25 * (a1 * b2 - a2 * b1); // ++
        } else if (tt == 3) {
            // CS x SS
            if (c == 0)
                return -0.25 * (a1 * b2 - a2 * b1); // --
            if (c == 1)
                return -0.25 * (a1 * b2 + a2 * b1); // -+
            if (c == 2)
                return 0.25 * (a1 * b2 + a2 * b1); // +-
            if (c == 3)
                return 0.25 * (a1 * b2 - a2 * b1); // ++
        }

        return 0;
    }

    public double[][][] basisField2DRect(int n, int m, double amp) {
        // Calculate Laplacian eigenfunction for eigenvalue (k1,k2) on 2D Rectangle

        int a = n;
        int b = m;

        double xFactor = 1.0;
        double yFactor = 1.0;

        if (n != 0)
            xFactor = -1.0 / (a * a + b * b);

        if (m != 0)
            yFactor = -1.0 / (a * a + b * b);

        double[][][] velField = new double[2][mX + 1][mY + 1];

        double dx = Math.PI / mX;
        double dy = Math.PI / mY;

        for (int i = 0; i < mX + 1; i++) {
            for (int j = 0; j < mY + 1; j++) {
                double x = (double) i * dx;
                double y = (double) j * dy;

                velField[0][i][j] = -b * amp * xFactor * Math.sin(a * x) * Math.cos(b * (y + 0.5 * dy));
                velField[1][i][j] = a * amp * yFactor * Math.cos(a * (x + 0.5 * dx)) * Math.sin(b * y);
            }
        }

        return velField;
    }

    public double currentEnergy() {
        // calculate current energy, sum of squares of coefficients since Laplacian eigenfunction basis is orthogonal
        double energy = 0.0;
        for (int i = 0; i < mN; i++) {
            energy += mInverseEigenvalues[i] * (mCoefficients[i] * mCoefficients[i]);
        }

        return energy;
    }

    public void setEnergy(double desiredEnergy) {
        double currentEnergy = this.currentEnergy();
        double factor = Math.sqrt(desiredEnergy) / Math.sqrt(currentEnergy);

        for (int i = 0; i < mN; i++) {
            mCoefficients[i] *= factor;
        }
    }

    public int basisLookup(int index, int component) {
        return mBasisLookupTable[index][component];
    }

    public int basisReverseLookup(int k1, int k2) {
        if (k1 > mNSqrt || k1 < 1 || k2 > mNSqrt || k2 < 1) {
            // these fields do not exist
            return -1;
        }

        return mBasisReverseLookupTable[k1][k2];
    }

    public void expandBasis() {
        // Calculate superposition of basis fields

        mVelocityField = new double[2][mX + 1][mY + 1];

        for (int k = 0; k < mN; k++) {
            for (int i = 0; i < mX + 1; i++) {
                for (int j = 0; j < mY + 1; j++) {
                    mVelocityField[0][i][j] += mCoefficients[k] * mVelocityBasis[k][0][i][j];
                    mVelocityField[1][i][j] += mCoefficients[k] * mVelocityBasis[k][1][i][j];
                }
            }
        }
    }

    public void fillLookupTable() {
        // Assume that mN is a perfect square, and use all basis fields with eigenvalues (k1,k2) up to (sqrt(mN), sqrt(mN))

        mNSqrt = (int) Math.floor(Math.sqrt(mN));

        mBasisLookupTable = new int[mN][2];
        mBasisReverseLookupTable = new int[mNSqrt + 1][mNSqrt + 1];

        // Initialize lookup table to -1, meaning this (k1,k2) basis field does not exist
        for (int k1 = 0; k1 < mNSqrt + 1; k1++) {
            for (int k2 = 0; k2 < mNSqrt + 1; k2++) {
                mBasisReverseLookupTable[k1][k2] = -1;
            }
        }

        int idx = 0;
        for (int k1 = 0; k1 < mNSqrt + 1; k1++) {
            for (int k2 = 0; k2 < mNSqrt + 1; k2++) {
                if (k1 > mNSqrt || k1 < 1 || k2 > mNSqrt || k2 < 1) {
                    // these fields do not exist
                    continue;
                }

                mBasisLookupTable[idx][0] = k1;
                mBasisLookupTable[idx][1] = k2;

                mBasisReverseLookupTable[k1][k2] = idx++;
            }
        }
    }

    public double dot(double[] v, double[] w) {
        double sum = 0.0;
        for (int i = 0; i < v.length; i++) {
            sum += v[i] * w[i];
        }

        return sum;
    }

    public double[] projectForces(double[][] forcePath) {
        double[] dw = new double[mN];

        for (int i = 0; i < mN; i++) {
            double tot = 0.0;

            int a = this.basisLookup(i, 0);
            int b = this.basisLookup(i, 1);

            double xFactor = 1.0;
            double yFactor = 1.0;

            if (a != 0)
                xFactor = -1.0 / (a * a + b * b);

            if (b != 0)
                yFactor = -1.0 / (a * a + b * b);

            for (int j = 0; j < forcePath.length - 1; j++) {
                double x = forcePath[j][0];
                double y = forcePath[j][1];
                double fx = forcePath[j][2];
                double fy = forcePath[j][3];

                if (x >= 1.00001 || x <= -0.00001 || y >= 1.00001 || y <= -0.00001)
                    continue;

                x *= Math.PI;
                y *= Math.PI;

                double vx = -b * xFactor * Math.sin(a * x) * Math.cos(b * y) * mDt;
                double vy = a * yFactor * Math.cos(a * x) * Math.sin(b * y) * mDt;

                tot += (vx * fx + vy * fy);
            }
            dw[i] = tot;
        }
        return dw;
    }

    public void stir(double[][] forcePath) {
        // Calculate the projected forces, and incorporate them on the next timestep
        double[] dw = this.projectForces(forcePath);
        for (int i = 0; i < mN; i++) {
            mForcesDW[i] += dw[i];
        }
    }

    public double getInterpolatedValue(double x, double y, int index) {
        int i = (int) Math.floor(x);
        int j = (int) Math.floor(y);

        double tot = 0.0;
        int den = 0;

        if (i >= 0 && i <= mX && j >= 0 && j <= mY) {
            tot += (i + 1 - x) * (j + 1 - y) * mVelocityField[index][i][j];
            den++;
        }
        if (i + 1 >= 0 && i + 1 <= mX && j >= 0 && j <= mY) {
            tot += (x - i) * (j + 1 - y) * mVelocityField[index][i + 1][j];
            den++;
        }
        if (i >= 0 && i <= mX && j + 1 >= 0 && j + 1 <= mY) {
            tot += (i + 1 - x) * (y - j) * mVelocityField[index][i][j + 1];
            den++;
        }
        if (i + 1 >= 0 && i + 1 <= mX && j + 1 >= 0 && j + 1 <= mY) {
            tot += (x - i) * (y - j) * mVelocityField[index][i + 1][j + 1];
            den++;
        }

        if (den == 0)
            return 0;

        return tot / (double) den;
    }

    public double[] bilinearVelocity(double xx, double yy) {
        double[] v = new double[2];

        xx *= mX;
        yy *= mY;

        v[0] = getInterpolatedValue(xx, yy - 0.5, 0);
        v[1] = getInterpolatedValue(xx - 0.5, yy, 1);

        return v;
    }

    public double[] cubicVelocity(double xx, double yy) {
        double[] v = new double[2];

        double[] f = new double[4];
        double tk;

        xx *= mX;
        yy *= mY;

        // calculate velocity at x, y
        int k = 1;

        int[] x = new int[4];
        x[k] = clampInt((int) Math.floor(xx), 0, mX);
        x[k + 1] = clampInt(x[k] + 1, 0, mX);
        x[k + 2] = clampInt(x[k] + 2, 0, mX);
        x[k - 1] = clampInt(x[k] - 1, 0, mX);

        int[] y = new int[4];
        y[k] = clampInt((int) Math.floor(yy), 0, mY);
        y[k + 1] = clampInt(y[k] + 1, 0, mY);
        y[k + 2] = clampInt(y[k] + 2, 0, mY);
        y[k - 1] = clampInt(y[k] - 1, 0, mY);

        // x component
        f[k - 1] = mVelocityField[0][x[k - 1]][y[k]];
        f[k] = mVelocityField[0][x[k]][y[k]];
        f[k + 1] = mVelocityField[0][x[k + 1]][y[k]];
        f[k + 2] = mVelocityField[0][x[k + 2]][y[k]];

        tk = xx - x[k];

        v[0] = f[k - 1] * (-0.5 * tk + tk * tk - 0.5 * tk * tk * tk) + f[k]
                * (1.0 - (5.0 / 2.0) * tk * tk + (3.0 / 2.0) * tk * tk * tk)
                + f[k + 1]
                * (0.5 * tk + 2 * tk * tk - (3.0 / 2.0) * tk * tk * tk)
                + f[k + 2] * (-0.5 * tk * tk + 0.5 * tk * tk * tk);

        // y component
        f[k - 1] = mVelocityField[1][x[k]][y[k - 1]];
        f[k] = mVelocityField[1][x[k]][y[k]];
        f[k + 1] = mVelocityField[1][x[k]][y[k + 1]];
        f[k + 2] = mVelocityField[1][x[k]][y[k + 2]];

        tk = yy - y[k];
        v[1] = f[k - 1] * (-0.5 * tk + tk * tk - 0.5 * tk * tk * tk) + f[k]
                * (1.0 - (5.0 / 2.0) * tk * tk + (3.0 / 2.0) * tk * tk * tk)
                + f[k + 1]
                * (0.5 * tk + 2 * tk * tk - (3.0 / 2.0) * tk * tk * tk)
                + f[k + 2] * (-0.5 * tk * tk + 0.5 * tk * tk * tk);

        return v;
    }

    public int clampInt(int val, int min, int max) {
        if (val < min)
            return min;
        return Math.min(val, max);
    }

    public double clampDbl(double val, double min, double max) {
        if (val < min)
            return min;
        return Math.min(val, max);
    }

    private final boolean mTranslucentBackground;
    private final ParticleArray mParticles;

    public int mX;
    public int mY;
    public double[][][][] mVelocityBasis;
    public double[] mCoefficients;
    public double[][][] mVelocityField;
    public double[] mEigenvalues;
    public double[] mInverseEigenvalues;
    public double[] mInverseEigenvaluesSqrt;
    public SparseMatrix[] mCk;
    public double mViscosity = 0.0;
    public double mDt = 0.1;
    public double mPDTMult = 1.0;
    public double mMargin = 1e-7;
    public int mN;
    public int mNSqrt;
    public int[][] mBasisLookupTable;
    public int[][] mBasisReverseLookupTable;
    public double[] mForcesDW;
    public boolean mForcesPending;
}
