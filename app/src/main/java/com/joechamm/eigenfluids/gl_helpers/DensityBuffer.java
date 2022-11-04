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

public class DensityBuffer implements BufferInterface {

    public int densRows = 0;
    public int densCols = 0;
    public int[] texDens = new int[ 1 ];
    public ByteBuffer densBuffer;
    public byte[] densArray = null;

    public int[] vbos = new int[ 2 ];

    public DensityBuffer () {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.chamm.eigenfluids.gl_helpers.BufferInterface#render(float[])
     */
    @Override
    public void render ( float[] transform ) {
        // TODO Auto-generated method stub

        final float densityColor[] = { 1.0f, 1.0f, 0.0f, 1.0f };

        GLES20.glActiveTexture ( GLES20.GL_TEXTURE0 );
        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, texDens[ 0 ] );

        GLES20.glUseProgram ( SharedResources.densProg );

        GLES20.glUniformMatrix4fv ( SharedResources.densUMVP, 1, false,
                                    transform, 0 );
        GLES20.glUniform4fv ( SharedResources.densUCol, 1, densityColor, 0 );
        GLES20.glUniform1i ( SharedResources.densUDens, 0 );

        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, vbos[ 0 ] );
        GLES20.glEnableVertexAttribArray ( SharedResources.densAPos );
        GLES20.glVertexAttribPointer ( SharedResources.densAPos, 3,
                                       GLES20.GL_FLOAT, false, 0, 0 );

        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, vbos[ 1 ] );
        GLES20.glEnableVertexAttribArray ( SharedResources.densATex );
        GLES20.glVertexAttribPointer ( SharedResources.densATex, 2,
                                       GLES20.GL_FLOAT, false, 0, 0 );

        GLES20.glDrawArrays ( GLES20.GL_TRIANGLE_STRIP, 0, 4 );

        GLES20.glDisableVertexAttribArray ( SharedResources.densATex );
        GLES20.glDisableVertexAttribArray ( SharedResources.densAPos );

        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, 0 );

        GLES20.glUseProgram ( 0 );

        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, 0 );
    }

    @Override
    public boolean initializeBuffers () {
        // TODO Auto-generated method stub

        final float squarePos[] = { 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f, 0.0f, };

        final float squareTex[] = { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
                1.0f };

        GLES20.glGenBuffers ( 2, vbos, 0 );

        ByteBuffer bbf = ByteBuffer.allocateDirect ( squarePos.length * 4 );
        bbf.order ( ByteOrder.nativeOrder () );

        FloatBuffer fb = bbf.asFloatBuffer ();
        fb.put ( squarePos );
        fb.position ( 0 );

        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, vbos[ 0 ] );
        GLES20.glBufferData ( GLES20.GL_ARRAY_BUFFER, squarePos.length * 4, fb,
                              GLES20.GL_STATIC_DRAW );
        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, 0 );

        bbf = ByteBuffer.allocateDirect ( squareTex.length * 4 );
        bbf.order ( ByteOrder.nativeOrder () );

        fb = bbf.asFloatBuffer ();
        fb.put ( squareTex );
        fb.position ( 0 );

        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, vbos[ 1 ] );
        GLES20.glBufferData ( GLES20.GL_ARRAY_BUFFER, squareTex.length * 4, fb,
                              GLES20.GL_STATIC_DRAW );
        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, 0 );

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.chamm.eigenfluids.gl_helpers.BufferInterface#updateBuffers()
     */
    @Override
    public void updateBuffers () {
        // TODO Auto-generated method stub

        float x, y, dx, dy;
        dx = 1.0f / ( densCols - 1 );
        dy = 1.0f / ( densRows - 1 );

        for ( int row = 0; row < densRows; ++ row ) {
            y = ( row ) * dy;
            for ( int col = 0; col < densCols; ++ col ) {
                x = ( col ) * dx;

                float fdens = LEFuncs.density_at ( x, y );
                densArray[ row * densCols + col ] = (byte) ( fdens * 255.0f );
            }
        }

        densBuffer.put ( densArray );
        densBuffer.position ( 0 );

        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, texDens[ 0 ] );
        GLES20.glTexSubImage2D ( GLES20.GL_TEXTURE_2D, 0, 0, 0, densCols,
                                 densRows, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                                 densBuffer );
        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, 0 );

    }

    public void initTextures () {

        float x, y, dx, dy;
        densCols = LEFuncs.DEN_COLS;
        densRows = LEFuncs.DEN_COLS;

        dx = 1.0f / ( densCols - 1 );
        dy = 1.0f / ( densRows - 1 );

        densArray = new byte[ densRows * densCols ];

        for ( int row = 0; row < densRows; ++ row ) {
            y = ( row ) * dy;
            for ( int col = 0; col < densCols; ++ col ) {
                x = ( col ) * dx;

                float fdens = LEFuncs.density_at ( x, y );
                densArray[ row * densCols + col ] = (byte) ( fdens * 255.0f );
            }
        }

        densBuffer = ByteBuffer.allocateDirect ( densArray.length );
        densBuffer.order ( ByteOrder.nativeOrder () );
        densBuffer.put ( densArray );
        densBuffer.position ( 0 );

        densBuffer = ByteBuffer.allocateDirect ( densArray.length );
        densBuffer.order ( ByteOrder.nativeOrder () );
        densBuffer.put ( densArray );
        densBuffer.position ( 0 );

        GLES20.glGenTextures ( 1, texDens, 0 );

        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, texDens[ 0 ] );

        GLES20.glTexParameterf ( GLES20.GL_TEXTURE_2D,
                                 GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST );
        GLES20.glTexParameterf ( GLES20.GL_TEXTURE_2D,
                                 GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR );
        GLES20.glTexParameterf ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                                 GLES20.GL_CLAMP_TO_EDGE );
        GLES20.glTexParameterf ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                                 GLES20.GL_CLAMP_TO_EDGE );

        GLES20.glTexImage2D ( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                              densRows, densCols, 0, GLES20.GL_LUMINANCE,
                              GLES20.GL_UNSIGNED_BYTE, densBuffer );

        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, 0 );

    }

}
