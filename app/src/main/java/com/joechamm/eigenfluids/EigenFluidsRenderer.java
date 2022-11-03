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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class EigenFluidsRenderer implements GLSurfaceView.Renderer {

    public EigenFluidsRenderer ( int gridRes, int N, int densRes, float visc, float dt ) {
        mFs = new LEFuncs ( gridRes, N, densRes );
        mFs.visc = visc;
        mFs.dt = dt;
        mFs.expand_basis ();

        mIsPressed = false;
//		mDispVel = true;
        mDispVel = false;
        mDispDens = true;
        mForceMode = 2;
        mDensityMode = 1;

        mX = 0.5f;
        mY = 0.5f;
        mWinW = 1;
        mWinH = 1;
        mForce = 100.0f;
        mSource = 1.0f;

        p1 = new float[ 2 ];
        p2 = new float[ 2 ];

//		initVelocityBuffers();
//		initDensityBuffers();

//		resizeVelocityGrid();
//		resizeDensityGrid();
    }

    public void onDrawFrame ( GL10 gl ) {
        mFs.step ();

        gl.glClear ( GL10.GL_COLOR_BUFFER_BIT );

        gl.glMatrixMode ( GL10.GL_MODELVIEW );
        gl.glLoadIdentity ();
        // gl.glScalef(1.0f, 2.0f, 1.0f);

        gl.glEnableClientState ( GL10.GL_VERTEX_ARRAY );
        gl.glEnableClientState ( GL10.GL_COLOR_ARRAY );

        if ( mDispDens ) {
            fillDensityBuffers ();
            renderDensity ( gl );
        }

        if ( mDispVel ) {

            fillVelocityBuffers ();
            renderVelocity ( gl );
        }

    }

    public void onSurfaceChanged ( GL10 gl, int w, int h ) {
        mWinW = w;
        mWinH = h;

        gl.glClearColor ( 0.0f, 0.0f, 0.0f, 1.0f );
        gl.glClear ( GL10.GL_COLOR_BUFFER_BIT );
        gl.glLineWidth ( 1.0f );
        gl.glFrontFace ( GL10.GL_CCW );

//		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
//		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        gl.glViewport ( 0, 0, mWinW, mWinH );
        gl.glMatrixMode ( GL10.GL_PROJECTION );
        gl.glLoadIdentity ();
        gl.glOrthof ( 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f );

        /*
         * float ratio = (float)w / h;
         * gl.glMatrixMode(GL10.GL_PROJECTION);
         * gl.glLoadIdentity();
         * gl.glFrustumf(- ratio, ratio, - 1.0f, 1.0f, 1.0f, 10.0f);
         *
         */
    }

    public void onSurfaceCreated ( GL10 gl, EGLConfig config ) {

        initVelocityBuffers ();
        initDensityBuffers ();

        resizeVelocityGrid ();
        resizeDensityGrid ();
    }

    public void renderVelocity ( GL10 gl ) {
        gl.glVertexPointer ( 2, GL10.GL_FLOAT, 0, mFVelVertexBuffer );
        gl.glColorPointer ( 4, GL10.GL_UNSIGNED_BYTE, 0, mBVelColorBuffer );
        gl.glDrawArrays ( GL10.GL_LINES, 0, mFs.mx * mFs.my * 2 );
    }

    public void renderDensity ( GL10 gl ) {
        int count = ( mFs.dmx - 1 ) * ( mFs.dmy - 1 ) * 6;
        gl.glVertexPointer ( 2, GL10.GL_FLOAT, 0, mFDensVertexBuffer );
        gl.glColorPointer ( 4, GL10.GL_UNSIGNED_BYTE, 0, mBDensColorBuffer );
        gl.glDrawElements ( GL10.GL_TRIANGLES, count, GL10.GL_SHORT, mUSDensIndexBuffer );
    }

    public void initVelocityBuffers () {
        final byte r0 = (byte) 255;
        final byte g0 = (byte) 0;
        final byte b0 = (byte) 0;

        final byte r1 = (byte) 255;
        final byte g1 = (byte) 0;
        final byte b1 = (byte) 0;

        float x0, x1, y0, y1, dx, dy;
        int j0, j1;

        dx = 1.0f / (float) mFs.mx;
        dy = 1.0f / (float) mFs.my;

        mVelVertices = new float[ mFs.mx * mFs.my * 2 * 2 ];
        mVelColors = new byte[ mFs.mx * mFs.my * 2 * 4 ];

        for ( int row = 0; row < mFs.my; ++ row ) {
            for ( int col = 0; col < mFs.mx; ++ col ) {
                int i = row * mFs.mx + col;

                j0 = i * 2;
                j1 = j0 + 1;

                x0 = (float) col * dx;
                y0 = (float) row * dy;

                x1 = x0;
                y1 = y0;

                mVelVertices[ j0 * 2 ] = x0;
                mVelVertices[ j0 * 2 + 1 ] = y0;

                mVelVertices[ j1 * 2 ] = x1;
                mVelVertices[ j1 * 2 + 1 ] = y1;

                mVelColors[ j0 * 4 ] = r0;
                mVelColors[ j0 * 4 + 1 ] = g0;
                ;
                mVelColors[ j0 * 4 + 2 ] = b0;
                mVelColors[ j0 * 4 + 3 ] = (byte) 255;

                mVelColors[ j1 * 4 ] = r1;
                mVelColors[ j1 * 4 + 1 ] = g1;
                mVelColors[ j1 * 4 + 2 ] = b1;
                mVelColors[ j1 * 4 + 3 ] = (byte) 255;
            }
        }

        ByteBuffer velVbb = ByteBuffer.allocateDirect ( mVelVertices.length * 4 );
        velVbb.order ( ByteOrder.nativeOrder () );
        mFVelVertexBuffer = velVbb.asFloatBuffer ();
        mFVelVertexBuffer.put ( mVelVertices );
        mFVelVertexBuffer.position ( 0 );

        mBVelColorBuffer = ByteBuffer.allocateDirect ( mVelColors.length );
        mBVelColorBuffer.order ( ByteOrder.nativeOrder () );
        mBVelColorBuffer.put ( mVelColors );
        mBVelColorBuffer.position ( 0 );
    }

    public void resizeVelocityGrid () {
        float x0, x1, y0, y1, dx, dy;
        int j0, j1;

        dx = 1.0f / (float) mFs.mx;
        dy = 1.0f / (float) mFs.my;

        for ( int row = 0; row < mFs.my; ++ row ) {
            for ( int col = 0; col < mFs.mx; ++ col ) {
                int i = row * mFs.mx + col;

                j0 = i * 2;
                j1 = j0 + 1;

                x0 = (float) col * dx;
                y0 = (float) row * dy;

                x1 = x0;
                y1 = y0;

                mVelVertices[ j0 * 2 ] = x0;
                mVelVertices[ j0 * 2 + 1 ] = y0;

                mVelVertices[ j1 * 2 ] = x1;
                mVelVertices[ j1 * 2 + 1 ] = y1;
            }
        }

        mFVelVertexBuffer.put ( mVelVertices );
        mFVelVertexBuffer.position ( 0 );
    }

    public void fillVelocityBuffers () {
        final float dispScale = 5.0f;

        float dx, dy, vx, vy;

        dx = 1.0f / (float) mFs.mx;
        dy = 1.0f / (float) mFs.my;

        for ( int row = 0; row < mFs.my; ++ row ) {
            for ( int col = 0; col < mFs.mx; ++ col ) {
                int j0 = ( row * mFs.mx + col ) * 2;
                int j1 = j0 + 1;

                vx = mFs.vfield[ 0 ][ col + 1 ][ row + 1 ];
                vy = mFs.vfield[ 1 ][ col + 1 ][ row + 1 ];

                mVelVertices[ j1 * 2 ] = mVelVertices[ j0 * 2 ] + vx * dx * dispScale;
                mVelVertices[ j1 * 2 + 1 ] = mVelVertices[ j0 * 2 + 1 ] + vy * dy * dispScale;
            }
        }

        mFVelVertexBuffer.put ( mVelVertices );
        mFVelVertexBuffer.position ( 0 );
    }

    public void initDensityBuffers () {

        mDensVertices = new float[ mFs.dmx * mFs.dmy * 2 ];
        mDensColors = new byte[ mFs.dmx * mFs.dmy * 4 ];
        mDensIndices = new short[ ( mFs.dmx - 1 ) * ( mFs.dmy - 1 ) * 6 ];

        for ( int i = 0; i < mFs.dmx * mFs.dmy; i++ ) {

            mDensVertices[ i * 2 ] = 0.0f;
            mDensVertices[ i * 2 + 1 ] = 0.0f;

            mDensColors[ i * 4 ] = 0;
            mDensColors[ i * 4 + 1 ] = 0;
            mDensColors[ i * 4 + 2 ] = 0;
            mDensColors[ i * 4 + 3 ] = (byte) 255;
        }

        for ( int i = 0; i < mFs.dmx - 1; i++ ) {
            for ( int j = 0; j < mFs.dmy - 1; j++ ) {
                int idx = ( ( mFs.dmy - 1 ) * i + j ) * 6;

                // 1st triangle
                mDensIndices[ idx ] = (short) ( mFs.dmy * i + j );
                mDensIndices[ idx + 1 ] = (short) ( mFs.dmy * i + j + 1 );
                mDensIndices[ idx + 2 ] = (short) ( mFs.dmy * ( i + 1 ) + j + 1 );

                // 2nd triangle
                mDensIndices[ idx + 3 ] = (short) ( mFs.dmy * ( i + 1 ) + j + 1 );
                mDensIndices[ idx + 4 ] = (short) ( mFs.dmy * ( i + 1 ) + j );
                mDensIndices[ idx + 5 ] = (short) ( mFs.dmy * i + j );
            }
        }

        ByteBuffer vertBB = ByteBuffer.allocateDirect ( mDensVertices.length * 4 );
        vertBB.order ( ByteOrder.nativeOrder () );
        mFDensVertexBuffer = vertBB.asFloatBuffer ();
        mFDensVertexBuffer.put ( mDensVertices );
        mFDensVertexBuffer.position ( 0 );

        ByteBuffer idxBB = ByteBuffer.allocateDirect ( mDensIndices.length * 2 );
        idxBB.order ( ByteOrder.nativeOrder () );
        mUSDensIndexBuffer = idxBB.asShortBuffer ();
        mUSDensIndexBuffer.put ( mDensIndices );
        mUSDensIndexBuffer.position ( 0 );

        mBDensColorBuffer = ByteBuffer.allocateDirect ( mDensColors.length );
        mBDensColorBuffer.order ( ByteOrder.nativeOrder () );
        mBDensColorBuffer.put ( mDensColors );
        mBDensColorBuffer.position ( 0 );
    }

    public void resizeDensityGrid () {
        float x, y, dx, dy;

        dx = 1.0f / (float) ( mFs.dmx - 1 );
        dy = 1.0f / (float) ( mFs.dmy - 1 );

        for ( int i = 0; i < mFs.dmx; i++ ) {
            x = ( (float) i ) * dx;

            for ( int j = 0; j < mFs.dmy; j++ ) {
                y = ( (float) j ) * dy;

                int idx = ( i * mFs.dmy + j ) * 2;

                mDensVertices[ idx ] = x;
                mDensVertices[ idx + 1 ] = y;
            }
        }

        fillDensityBuffers ();
    }

    public void fillDensityBuffers () {
        byte rval = (byte) 255;
        byte gval = (byte) 255;
        byte bval = (byte) 0;


        for ( int i = 0; i < mFs.dmx; i++ ) {
            for ( int j = 0; j < mFs.dmy; j++ ) {

                int idx = ( i * mFs.dmy + j ) * 4;

//				float fdens = mFs.density_field[i][j];
//				byte bdens = (byte)(fdens * 255.0f);
//				mDensColors[idx] = bdens;
//				mDensColors[idx + 1] = bdens;
//				mDensColors[idx + 2] = bdens;

                if ( i / 8 + j / 8 % 2 == 0 ) {
                    mDensColors[ idx + 0 ] = rval;
                    mDensColors[ idx + 1 ] = gval;
                    mDensColors[ idx + 2 ] = bval;
                } else {
                    mDensColors[ idx + 0 ] = (byte) 0;
                    mDensColors[ idx + 1 ] = (byte) 0;
                    mDensColors[ idx + 2 ] = (byte) 0;
                }
                mDensColors[ idx + 3 ] = (byte) 255;
            }
        }

        mBDensColorBuffer.put ( mDensColors );
        mBDensColorBuffer.position ( 0 );
    }

    public void handleTouchEvent ( MotionEvent evt ) {

        switch ( evt.getAction () ) {
            case MotionEvent.ACTION_DOWN:
                handleActionDown ( evt );
                break;
            case MotionEvent.ACTION_MOVE:
                handleActionMove ( evt );
                break;
            case MotionEvent.ACTION_CANCEL:
                handleActionCancel ( evt );
                break;
            case MotionEvent.ACTION_UP:
                handleActionUp ( evt );
                break;
        }
    }

    public void handleActionDown ( MotionEvent evt ) {
        mIsPressed = true;
        mX = evt.getX ();
        mY = evt.getY ();

        if ( this.mForceMode == 1 ) {
            this.drag_path_x = new ArrayList<Float> ();
            this.drag_path_y = new ArrayList<Float> ();
            drag_path_x.add ( Float.valueOf ( mX ) );
            drag_path_y.add ( Float.valueOf ( mY ) );
        } else if ( this.mForceMode == 2 ) {
            this.p1[ 0 ] = evt.getX () / (float) mWinW;
            this.p1[ 1 ] = evt.getY () / (float) mWinH;
        }

    }

    public void handleActionMove ( MotionEvent evt ) {
        if ( ! mIsPressed ) {
            return;
        }

        mX = evt.getX ();
        mY = evt.getY ();

        if ( mDensityMode == 1 ) {
            int i = (int) ( ( mX * (float) mFs.dmx ) / (float) mWinW );
            int j = (int) ( ( mY * (float) mFs.dmy ) / (float) mWinH );

            if ( i >= 0 && i < this.mFs.dmx && j >= 0 && j < this.mFs.dmy ) {
                this.mFs.density_field[ i ][ j ] = mSource;
            }
        }

        if ( this.mForceMode == 1 ) {
            drag_path_x.add ( Float.valueOf ( mX ) );
            drag_path_y.add ( Float.valueOf ( mY ) );
        } else if ( this.mForceMode == 2 ) {
            this.p2[ 0 ] = mX / (float) mWinW;
            this.p2[ 1 ] = mY / (float) mWinH;

            float[][] force_path = new float[ 2 ][ 4 ];
            force_path[ 0 ][ 0 ] = p1[ 0 ];
            force_path[ 0 ][ 1 ] = p1[ 1 ];
            force_path[ 0 ][ 2 ] = ( p2[ 0 ] - p1[ 0 ] ) * this.mForce;
            force_path[ 0 ][ 3 ] = ( p2[ 1 ] - p1[ 1 ] ) * this.mForce;

            mFs.stir ( force_path );

            this.p1[ 0 ] = p2[ 0 ];
            this.p1[ 1 ] = p2[ 1 ];
        }
    }

    public void handleActionCancel ( MotionEvent evt ) {
        mIsPressed = false;
        return;
    }

    public void handleActionUp ( MotionEvent evt ) {
        if ( mForceMode == 1 ) {
            float[][] force_path = new float[ drag_path_x.size () ][ 4 ];

            Iterator<Float> itr_x = drag_path_x.iterator ();
            Iterator<Float> itr_y = drag_path_y.iterator ();

            int i = 0;
            while ( itr_x.hasNext () ) {
                Float x = itr_x.next ();
                Float y = itr_y.next ();
                force_path[ i ][ 0 ] = x.floatValue () / (float) ( mWinW );
                force_path[ i ][ 1 ] = y.floatValue () / (float) ( mWinH );
                i++;
            }

            for ( i = 0; i < force_path.length - 1; i++ ) {
                force_path[ i ][ 2 ] = ( force_path[ i + 1 ][ 0 ] - force_path[ i ][ 0 ] ) * this.mForce;
                force_path[ i ][ 3 ] = ( force_path[ i + 1 ][ 1 ] - force_path[ i ][ 1 ] ) * this.mForce;
            }

            mFs.stir ( force_path );
        }

        mIsPressed = false;
        mX = evt.getX ();
        mY = evt.getY ();
    }

    public LEFuncs mFs;

    public float[] p1;
    public float[] p2;

    public ArrayList<Float> drag_path_x;
    public ArrayList<Float> drag_path_y;

    public int mForceMode;
    public int mDensityMode;

    private boolean mIsPressed;
    private boolean mDispVel;
    private boolean mDispDens;
    private float mX;
    private float mY;
    private int mWinW;
    private int mWinH;
    private float mForce;
    private float mSource;

    private float[] mVelVertices;
    private byte[] mVelColors;

    private float[] mDensVertices;
    private short[] mDensIndices;
    private byte[] mDensColors;

    private FloatBuffer mFVelVertexBuffer;
    private ByteBuffer mBVelColorBuffer;

    private FloatBuffer mFDensVertexBuffer;
    private ShortBuffer mUSDensIndexBuffer;
    private ByteBuffer mBDensColorBuffer;
}
