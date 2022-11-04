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

package com.joechamm.eigenfluids.gl_helpers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.joechamm.eigenfluids.utils.LEFuncs;

import android.opengl.GLES20;

public class VelocityBuffer implements BufferInterface {
    public int velRows = 0;
    public int velCols = 0;
    public float[] velArray = null;
    public int[] vbos = new int[ 1 ];

    public FloatBuffer velBuffer;

    public VelocityBuffer () {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.chamm.eigenfluids.gl_helpers.BufferInterface#render(float[])
     */
    @Override
    public void render ( float[] transform ) {
        // TODO Auto-generated method stub
        final float velocityColor[] = { 1.0f, 0.0f, 0.0f, 1.0f };

        int count = velArray.length / 2;

        GLES20.glUseProgram ( SharedResources.velProg );

        GLES20.glUniformMatrix4fv ( SharedResources.velUMVP, 1, false, transform, 0 );
        GLES20.glUniform4fv ( SharedResources.velUCol, 1, velocityColor, 0 );

        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, vbos[ 0 ] );
        GLES20.glEnableVertexAttribArray ( SharedResources.velAPos );
        GLES20.glVertexAttribPointer ( SharedResources.velAPos, 2, GLES20.GL_FLOAT, false, 0, 0 );

        GLES20.glDrawArrays ( GLES20.GL_LINES, 0, count );

        GLES20.glDisableVertexAttribArray ( SharedResources.velAPos );
        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, 0 );

        GLES20.glUseProgram ( 0 );

        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, 0 );

    }

    /*
     * (non-Javadoc)
     *
     * @see com.chamm.eigenfluids.gl_helpers.BufferInterface#initializeBuffers()
     */
    @Override
    public boolean initializeBuffers () {
        // TODO Auto-generated method stub

        float x0, x1, y0, y1, dx, dy;
        int j0, j1;

        velCols = LEFuncs.VEL_COLS;
        velRows = LEFuncs.VEL_ROWS;

        dx = 1.0f / LEFuncs.VEL_COLS;
        dy = 1.0f / LEFuncs.VEL_ROWS;

        velArray = new float[ velCols * velRows * 2 * 2 ];

        for ( int row = 0; row < velRows; ++ row ) {
            for ( int col = 0; col < velCols; ++ col ) {
                int i = row * velCols + col;

                j0 = i * 2;
                j1 = j0 + 1;

                x0 = ( col ) * dx;
                y0 = ( row ) * dy;

                x1 = x0;
                y1 = y0;

                velArray[ j0 * 2 ] = x0;
                velArray[ j0 * 2 + 1 ] = y0;

                velArray[ j1 * 2 ] = x1;
                velArray[ j1 * 2 + 1 ] = y1;
            }
        }

        ByteBuffer bbf = ByteBuffer.allocateDirect ( velArray.length * 4 );
        bbf.order ( ByteOrder.nativeOrder () );

        velBuffer = bbf.asFloatBuffer ();
        velBuffer.put ( velArray );
        velBuffer.position ( 0 );

        GLES20.glGenBuffers ( 1, vbos, 0 );
        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, vbos[ 0 ] );
        GLES20.glBufferData ( GLES20.GL_ARRAY_BUFFER, velArray.length * 4, velBuffer, GLES20.GL_STATIC_DRAW );
        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, 0 );

        return true;
    }

    @Override
    public void updateBuffers () {
        final float dispScale = 5.0f;

        float dx, dy, vx, vy;

        dx = 1.0f / LEFuncs.VEL_COLS;
        dy = 1.0f / LEFuncs.VEL_ROWS;

        for ( int row = 0; row < LEFuncs.VEL_ROWS; ++ row ) {
            for ( int col = 0; col < LEFuncs.VEL_COLS; ++ col ) {
                int j0 = ( row * LEFuncs.VEL_COLS + col ) * 2;
                int j1 = j0 + 1;

                vx = LEFuncs.VELOCITY_FIELD[ 0 ][ col + 1 ][ row + 1 ];
                vy = LEFuncs.VELOCITY_FIELD[ 1 ][ col + 1 ][ row + 1 ];

                velArray[ j1 * 2 ] = velArray[ j0 * 2 ] + vx * dx * dispScale;
                velArray[ j1 * 2 + 1 ] = velArray[ j0 * 2 + 1 ] + vy * dy * dispScale;
            }
        }

        velBuffer.put ( velArray );
        velBuffer.position ( 0 );

        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, vbos[ 0 ] );
        GLES20.glBufferSubData ( GLES20.GL_ARRAY_BUFFER, 0, velArray.length * 4, velBuffer );
        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, 0 );
    }
}
