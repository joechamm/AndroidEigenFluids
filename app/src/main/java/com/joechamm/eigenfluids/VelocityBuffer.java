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

import android.opengl.GLES20;

public class VelocityBuffer {

    private final String vertexShaderCode =
            "attribute vec4 aPos;\n" +
                    "uniform mat4 uMVP;\n" +
                    "void main() {\n" +
                    "	gl_Position = uMVP * aPos;\n" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;\n" +
                    "uniform vec4 uColor;\n" +
                    "void main() {\n" +
                    "	gl_FragColor = uColor;\n" +
                    "}\n";

    private final float velocityColor[] = { 1.0f, 0.0f, 0.0f, 1.0f };

    private final FloatBuffer mPosBuffer;
    private final int mProgram;
    private int m_aPos;
    private int m_uCol;
    private int m_uMVP;

    public float[] mPosArray;

    public VelocityBuffer () {
        float x0, x1, y0, y1, dx, dy;
        int j0, j1, vs, fs;

        dx = 1.0f / (float) LEFuncs.VEL_COLS;
        dy = 1.0f / (float) LEFuncs.VEL_ROWS;

        mPosArray = new float[ LEFuncs.VEL_COLS * LEFuncs.VEL_ROWS * 2 * 2 ];

        for ( int row = 0; row < LEFuncs.VEL_ROWS; ++ row ) {
            for ( int col = 0; col < LEFuncs.VEL_COLS; ++ col ) {
                int i = row * LEFuncs.VEL_COLS + col;

                j0 = i * 2;
                j1 = j0 + 1;

                x0 = ( (float) col ) * dx;
                y0 = ( (float) row ) * dy;

                x1 = x0;
                y1 = y0;

                mPosArray[ j0 * 2 ] = x0;
                mPosArray[ j0 * 2 + 1 ] = y0;

                mPosArray[ j1 * 2 ] = x1;
                mPosArray[ j1 * 2 + 1 ] = y1;
            }
        }

        ByteBuffer bbf = ByteBuffer.allocateDirect ( mPosArray.length * 4 );
        bbf.order ( ByteOrder.nativeOrder () );

        mPosBuffer = bbf.asFloatBuffer ();
        mPosBuffer.put ( mPosArray );
        mPosBuffer.position ( 0 );

        vs = EigenFluidsRenderer.loadShader ( GLES20.GL_VERTEX_SHADER, vertexShaderCode );
        fs = EigenFluidsRenderer.loadShader ( GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode );

        mProgram = GLES20.glCreateProgram ();
        GLES20.glAttachShader ( mProgram, vs );
        GLES20.glAttachShader ( mProgram, fs );
        GLES20.glLinkProgram ( mProgram );

        m_aPos = GLES20.glGetAttribLocation ( mProgram, "aPos" );
        m_uMVP = GLES20.glGetUniformLocation ( mProgram, "uMVP" );
        m_uCol = GLES20.glGetUniformLocation ( mProgram, "uColor" );
    }

    public void draw ( float[] mvpMatrix ) {
        int count = mPosArray.length / 2;

        GLES20.glUseProgram ( mProgram );

        GLES20.glUniformMatrix4fv ( m_uMVP, 1, false, mvpMatrix, 0 );
        GLES20.glUniform4fv ( m_uCol, 1, velocityColor, 0 );

        GLES20.glEnableVertexAttribArray ( m_aPos );
        GLES20.glVertexAttribPointer ( m_aPos, 2, GLES20.GL_FLOAT, false, 0, mPosBuffer );

        GLES20.glDrawArrays ( GLES20.GL_LINES, 0, count );

        GLES20.glDisableVertexAttribArray ( m_aPos );

        GLES20.glUseProgram ( 0 );
    }


    public void updateBuffers () {
        final float dispScale = 5.0f;

        float dx, dy, vx, vy;

        dx = 1.0f / (float) LEFuncs.VEL_COLS;
        dy = 1.0f / (float) LEFuncs.VEL_ROWS;

        for ( int row = 0; row < LEFuncs.VEL_ROWS; ++ row ) {
            for ( int col = 0; col < LEFuncs.VEL_COLS; ++ col ) {
                int j0 = ( row * LEFuncs.VEL_COLS + col ) * 2;
                int j1 = j0 + 1;

                vx = LEFuncs.VELOCITY_FIELD[ 0 ][ col + 1 ][ row + 1 ];
                vy = LEFuncs.VELOCITY_FIELD[ 1 ][ col + 1 ][ row + 1 ];

                mPosArray[ j1 * 2 ] = mPosArray[ j0 * 2 ] + vx * dx * dispScale;
                mPosArray[ j1 * 2 + 1 ] = mPosArray[ j0 * 2 + 1 ] + vy * dy * dispScale;
            }
        }

        mPosBuffer.put ( mPosArray );
        mPosBuffer.position ( 0 );
    }

}
