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

public class DensityBuffer {

    private final String vertexShaderCode =
            "attribute vec4 aPos;\n" +
                    "attribute vec2 aTex;\n" +
                    "varying vec2 vTex;\n" +
                    "uniform mat4 uMVP;\n" +
                    "void main() {\n" +
                    "   vTex = aTex;\n" +
                    "	gl_Position = uMVP * aPos;\n" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;\n" +
                    "varying vec2 vTex;\n" +
                    "uniform sampler2D uDensity;\n" +
                    "uniform vec4 uColor;\n" +
                    "void main() {\n" +
                    "   float d = texture2D(uDensity, vTex).r;\n" +
                    "	vec3 color = uColor.rgb * d;\n" +
                    "	gl_FragColor = vec4(color, 1.0);\n" +
                    "}\n";

    private final float densityColor[] = { 1.0f, 1.0f, 0.0f, 1.0f };

    private final float squarePos[] = {
            0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
    };

    private final float squareTex[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
    };

    private final ByteBuffer mDensBuffer;

    private final FloatBuffer mPosBuffer;
    private final FloatBuffer mTexBuffer;
    private final int mProgram;
    private int m_aPos;
    private int m_aTex;
    private int m_uCol;
    private int m_uDens;
    private int m_uMVP;
    private int[] m_texDens = new int[ 1 ];

    public byte[] mDensArray;
    public int mNumRows;
    public int mNumCols;

    public DensityBuffer () {
        int vs, fs;
        float x, y, dx, dy;

        ByteBuffer bbf = ByteBuffer.allocateDirect ( squarePos.length * 4 );
        bbf.order ( ByteOrder.nativeOrder () );

        mPosBuffer = bbf.asFloatBuffer ();
        mPosBuffer.put ( squarePos );
        mPosBuffer.position ( 0 );

        bbf = ByteBuffer.allocateDirect ( squareTex.length * 4 );
        bbf.order ( ByteOrder.nativeOrder () );

        mTexBuffer = bbf.asFloatBuffer ();
        mTexBuffer.put ( squareTex );
        mTexBuffer.position ( 0 );

        vs = EigenFluidsRenderer.loadShader ( GLES20.GL_VERTEX_SHADER, vertexShaderCode );
        fs = EigenFluidsRenderer.loadShader ( GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode );

        mProgram = GLES20.glCreateProgram ();
        GLES20.glAttachShader ( mProgram, vs );
        GLES20.glAttachShader ( mProgram, fs );
        GLES20.glLinkProgram ( mProgram );

        m_aPos = GLES20.glGetAttribLocation ( mProgram, "aPos" );
        m_aTex = GLES20.glGetAttribLocation ( mProgram, "aTex" );
        m_uMVP = GLES20.glGetUniformLocation ( mProgram, "uMVP" );
        m_uDens = GLES20.glGetUniformLocation ( mProgram, "uDensity" );
        m_uCol = GLES20.glGetUniformLocation ( mProgram, "uColor" );

        mNumCols = npt ( LEFuncs.DEN_COLS );
        mNumRows = npt ( LEFuncs.DEN_ROWS );

        mDensArray = new byte[ mNumCols * mNumRows ];

        dx = 1.0f / (float) ( mNumCols - 1 );
        dy = 1.0f / (float) ( mNumRows - 1 );

        for ( int row = 0; row < mNumRows; ++ row ) {
            y = ( (float) row ) * dy;
            for ( int col = 0; col < mNumCols; ++ col ) {
                x = ( (float) col ) * dx;

                float fdens = LEFuncs.density_at ( x, y );
                mDensArray[ row * mNumCols + col ] = (byte) ( fdens * 255.0f );
            }
        }

        mDensBuffer = ByteBuffer.allocateDirect ( mDensArray.length );
        mDensBuffer.order ( ByteOrder.nativeOrder () );
        mDensBuffer.put ( mDensArray );
        mDensBuffer.position ( 0 );

        initTextures ();
    }

    public void initTextures () {

        GLES20.glGenTextures ( 1, m_texDens, 0 );

        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, m_texDens[ 0 ] );

        GLES20.glTexParameterf ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST );
        GLES20.glTexParameterf ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR );
        GLES20.glTexParameterf ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE );
        GLES20.glTexParameterf ( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE );

        GLES20.glTexImage2D ( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mNumCols, mNumRows, 0,
                              GLES20.GL_LUMINANCE, GLES20.GL_BYTE, mDensBuffer );

        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, 0 );
    }

    public int npt ( int n ) {
        int p = 1;
        while ( p < n ) {
            p <<= 1;
        }
        return p;
    }


    public void draw ( float[] mvpMatrix ) {

        GLES20.glActiveTexture ( GLES20.GL_TEXTURE0 );
        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, m_texDens[ 0 ] );

        GLES20.glUseProgram ( mProgram );

        GLES20.glUniformMatrix4fv ( m_uMVP, 1, false, mvpMatrix, 0 );
        GLES20.glUniform4fv ( m_uCol, 1, densityColor, 0 );
        GLES20.glUniform1i ( m_uDens, 0 );

        GLES20.glEnableVertexAttribArray ( m_aPos );
        GLES20.glVertexAttribPointer ( m_aPos, 3, GLES20.GL_FLOAT, false, 0, mPosBuffer );

        GLES20.glEnableVertexAttribArray ( m_aTex );
        GLES20.glVertexAttribPointer ( m_aTex, 2, GLES20.GL_FLOAT, false, 0, mTexBuffer );

        GLES20.glDrawArrays ( GLES20.GL_TRIANGLE_STRIP, 0, 4 );

        GLES20.glDisableVertexAttribArray ( m_aPos );
        GLES20.glDisableVertexAttribArray ( m_aTex );

        GLES20.glUseProgram ( 0 );

        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, 0 );
    }


    public void updateBuffers () {
        float x, y, dx, dy;
        dx = 1.0f / (float) ( mNumCols - 1 );
        dy = 1.0f / (float) ( mNumRows - 1 );

        for ( int row = 0; row < mNumRows; ++ row ) {
            y = ( (float) row ) * dy;
            for ( int col = 0; col < mNumCols; ++ col ) {
                x = ( (float) col ) * dx;

                float fdens = LEFuncs.density_at ( x, y );
                mDensArray[ row * mNumCols + col ] = (byte) ( fdens * 255.0f );
            }
        }

        mDensBuffer.put ( mDensArray );
        mDensBuffer.position ( 0 );

        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, m_texDens[ 0 ] );
        GLES20.glTexSubImage2D ( GLES20.GL_TEXTURE_2D, 0, 0, 0, mNumCols, mNumRows, GLES20.GL_LUMINANCE, GLES20.GL_BYTE, mDensBuffer );
        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, 0 );
    }

}
