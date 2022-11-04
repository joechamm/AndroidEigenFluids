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

import java.util.ArrayList;
import java.util.Iterator;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.joechamm.eigenfluids.gl_helpers.DensityBuffer;
import com.joechamm.eigenfluids.gl_helpers.VelocityBuffer;
import com.joechamm.eigenfluids.utils.LEFuncs;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;

public class EigenFluidsRenderer implements GLSurfaceView.Renderer {

    // private static final LEFuncs lapEFuncs = new LEFuncs();

    private static final String TAG = "eigenfluids:renderer";
    private final float[] mMVPMatrix = new float[ 16 ];
    private final float[] mProjectionMatrix = new float[ 16 ];
    private final float[] mModelMatrix = new float[ 16 ];

    private static final int VEL_RESOLUTION = 36;
    private static final int DEN_RESOLUTION = 36;
    private static final int DIMENSION = 36;

    public static final boolean RENDER_VELOCITY = true;
    public static final boolean RENDER_DENSITY = true;

    public static int FORCE_MODE = 2;
    public static int DENSITY_MODE = 1;
    public static int VELOCITY_MODE = 1;

    public static boolean IS_PRESSED = false;
    public static float PRESS_X = 0.5f;
    public static float PRESS_Y = 0.5f;

    private static int WIN_WIDTH;
    private static int WIN_HEIGHT;

    public static float[] POS_1 = new float[ 2 ];
    public static float[] POS_2 = new float[ 2 ];

    public static ArrayList<Float> DRAG_PATH_X;
    public static ArrayList<Float> DRAG_PATH_Y;

    public VelocityBuffer velocityBuffer = null;
    public DensityBuffer densityBuffer = null;

    @Override
    public void onSurfaceCreated ( GL10 unused, EGLConfig config ) {

        GLES20.glClearColor ( 0.0f, 0.0f, 0.0f, 1.0f );

        LEFuncs.init ( VEL_RESOLUTION, DEN_RESOLUTION, DIMENSION );
        LEFuncs.expand_basis ();

        checkGLError ( "LEFuncs.init(VEL_RESOLUTION, DEN_RESOLUTION, DIMENSION" );

        com.joechamm.eigenfluids.gl_helpers.SharedResources.initShaderPrograms ();

        velocityBuffer = new VelocityBuffer ();

        checkGLError ( "velocit creation" );

        velocityBuffer.initializeBuffers ();

        densityBuffer = new DensityBuffer ();

        checkGLError ( "density creation" );

        densityBuffer.initializeBuffers ();
        densityBuffer.initTextures ();

        velocityBuffer.updateBuffers ();

        checkGLError ( "velocity creation" );

        densityBuffer.updateBuffers ();

        checkGLError ( "density buffer update" );
    }

    @Override
    public void onSurfaceChanged ( GL10 unused, int width, int height ) {
        WIN_WIDTH = width;
        WIN_HEIGHT = height;

        Matrix.setIdentityM ( mModelMatrix, 0 );
        Matrix.orthoM ( mProjectionMatrix, 0, 0.0f, 1.0f, 0.0f, 1.0f, - 1.0f, 1.0f );
        Matrix.multiplyMM ( mMVPMatrix, 0, mProjectionMatrix, 0, mModelMatrix, 0 );

        GLES20.glClearColor ( 0.0f, 0.0f, 0.0f, 1.0f );
        GLES20.glLineWidth ( 1.0f );
        GLES20.glFrontFace ( GLES20.GL_CCW );

        GLES20.glViewport ( 0, 0, WIN_WIDTH, WIN_HEIGHT );

        checkGLError ( "onsurfaceChanged" );
    }

    @Override
    public void onDrawFrame ( GL10 unused ) {
        LEFuncs.step ();

        GLES20.glClear ( GLES20.GL_COLOR_BUFFER_BIT );

        if ( RENDER_DENSITY ) {
            densityBuffer.updateBuffers ();
            densityBuffer.render ( mMVPMatrix );

            checkGLError ( "render density" );
        }

        if ( RENDER_VELOCITY ) {
            velocityBuffer.updateBuffers ();
            velocityBuffer.render ( mMVPMatrix );

            checkGLError ( "render velocity" );
        }

    }

    public static int loadShader ( int type, String shaderCode ) {
        int shader = GLES20.glCreateShader ( type );

        GLES20.glShaderSource ( shader, shaderCode );
        GLES20.glCompileShader ( shader );

        checkGLError ( "create shader" );

        return shader;
    }

    public static void checkGLError ( String glOperation ) {
        int error;
        while ( ( error = GLES20.glGetError () ) != GLES20.GL_NO_ERROR ) {
            Log.e ( TAG, glOperation + ": glError " + error );
            throw new RuntimeException ( glOperation + ": glError " + error );
        }
    }

    public void renderVelocity ( GL10 gl ) {
        if ( velocityBuffer != null ) {
            velocityBuffer.render ( mMVPMatrix );
        }
    }

    public void renderDensity ( GL10 gl ) {
        if ( densityBuffer != null ) {
            densityBuffer.render ( mMVPMatrix );
        }
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
        IS_PRESSED = true;
        PRESS_X = evt.getX ();
        PRESS_Y = evt.getY ();

        if ( FORCE_MODE == 1 ) {
            DRAG_PATH_X = new ArrayList<Float> ();
            DRAG_PATH_Y = new ArrayList<Float> ();
            DRAG_PATH_X.add ( Float.valueOf ( PRESS_X ) );
            DRAG_PATH_Y.add ( Float.valueOf ( PRESS_Y ) );
        } else if ( FORCE_MODE == 2 ) {
            POS_1[ 0 ] = evt.getX () / WIN_WIDTH;
            POS_1[ 1 ] = evt.getY () / WIN_HEIGHT;

        }

    }

    public void handleActionMove ( MotionEvent evt ) {
        if ( ! IS_PRESSED ) {
            return;
        }

        PRESS_X = evt.getX ();
        PRESS_Y = evt.getY ();

        if ( DENSITY_MODE == 1 ) {
            int i = (int) ( ( PRESS_X * DEN_RESOLUTION ) / WIN_WIDTH );
            int j = (int) ( ( PRESS_Y * DEN_RESOLUTION ) / WIN_HEIGHT );

            if ( i >= 2 && i < DEN_RESOLUTION - 2 && j >= 2
                    && j < DEN_RESOLUTION - 2 ) {
                for ( int i0 = i - 2; i0 < i + 2; ++ i0 ) {
                    for ( int j0 = j - 2; j0 < j + 2; ++ j0 ) {
                        LEFuncs.DENSITY_FIELD[ i0 ][ j0 ] = 1.0f;
                    }
                }
            }

            float[][] force_path = new float[ 2 ][ 4 ];
            force_path[ 0 ][ 0 ] = 0.25f;
            force_path[ 0 ][ 1 ] = 0.35f;
            force_path[ 0 ][ 2 ] = - 0.015f * LEFuncs.FORCE_MAG;
            force_path[ 0 ][ 3 ] = 0.02f * LEFuncs.FORCE_MAG;

            LEFuncs.stir ( force_path );

        } else if ( FORCE_MODE == 1 ) {
            DRAG_PATH_X.add ( Float.valueOf ( PRESS_X ) );
            DRAG_PATH_Y.add ( Float.valueOf ( PRESS_Y ) );
        } else if ( FORCE_MODE == 2 ) {
            POS_2[ 0 ] = PRESS_X / WIN_WIDTH;
            POS_2[ 1 ] = PRESS_Y / WIN_HEIGHT;

            float[][] force_path = new float[ 2 ][ 4 ];
            force_path[ 0 ][ 0 ] = POS_1[ 0 ];
            force_path[ 0 ][ 1 ] = POS_1[ 1 ];
            force_path[ 0 ][ 2 ] = ( POS_2[ 0 ] - POS_1[ 0 ] ) * LEFuncs.FORCE_MAG;
            force_path[ 0 ][ 3 ] = ( POS_2[ 1 ] - POS_1[ 1 ] ) * LEFuncs.FORCE_MAG;

            LEFuncs.stir ( force_path );

            POS_1[ 0 ] = POS_2[ 0 ];
            POS_1[ 1 ] = POS_2[ 1 ];
        }

    }

    public void handleActionCancel ( MotionEvent evt ) {
        IS_PRESSED = false;
        return;
    }

    public void handleActionUp ( MotionEvent evt ) {
        if ( FORCE_MODE == 1 ) {
            float[][] force_path = new float[ DRAG_PATH_X.size () ][ 4 ];

            Iterator<Float> itr_x = DRAG_PATH_X.iterator ();
            Iterator<Float> itr_y = DRAG_PATH_Y.iterator ();

            int i = 0;
            while ( itr_x.hasNext () ) {
                Float x = itr_x.next ();
                Float y = itr_y.next ();
                force_path[ i ][ 0 ] = x.floatValue () / ( WIN_WIDTH );
                force_path[ i ][ 1 ] = y.floatValue () / ( WIN_HEIGHT );
                i++;
            }

            for ( i = 0; i < force_path.length - 1; i++ ) {
                force_path[ i ][ 2 ] = ( force_path[ i + 1 ][ 0 ] - force_path[ i ][ 0 ] )
                        * LEFuncs.FORCE_MAG;
                force_path[ i ][ 3 ] = ( force_path[ i + 1 ][ 1 ] - force_path[ i ][ 1 ] )
                        * LEFuncs.FORCE_MAG;
            }

            LEFuncs.stir ( force_path );
        }

        IS_PRESSED = false;
        PRESS_X = evt.getX ();
        PRESS_Y = evt.getY ();
    }

}
