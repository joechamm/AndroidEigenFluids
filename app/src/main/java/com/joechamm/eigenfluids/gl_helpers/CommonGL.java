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

import android.opengl.GLES20;
import android.util.Log;

public class CommonGL {

    public static int prevPowTwo ( int n ) {
        int p = 1;
        while ( p < n ) {
            p <<= 1;
        }
        return p;
    }

    public static int loadShader ( int type, String shaderCode ) {
        int shader = GLES20.glCreateShader ( type );
        GLES20.glShaderSource ( shader, shaderCode );
        GLES20.glCompileShader ( shader );

        return shader;
    }

    public static int loadShaderProgram ( String vsCode, String fsCode ) {
        int vs, fs, prog;
        vs = GLES20.glCreateShader ( GLES20.GL_VERTEX_SHADER );
        fs = GLES20.glCreateShader ( GLES20.GL_FRAGMENT_SHADER );
        prog = GLES20.glCreateProgram ();

        GLES20.glShaderSource ( vs, vsCode );
        GLES20.glCompileShader ( vs );

        GLES20.glShaderSource ( fs, fsCode );
        GLES20.glCompileShader ( fs );

        GLES20.glAttachShader ( prog, vs );
        GLES20.glAttachShader ( prog, fs );
        GLES20.glLinkProgram ( prog );

        GLES20.glDeleteShader ( vs );
        GLES20.glDeleteShader ( fs );

        return prog;
    }

    public static int loadShader ( int type, String shaderCode, String debugTag ) {
        final int[] shadiv = new int[ 1 ];

        int shader = GLES20.glCreateShader ( type );
        GLES20.glShaderSource ( shader, shaderCode );
        GLES20.glCompileShader ( shader );

        GLES20.glGetShaderiv ( shader, GLES20.GL_COMPILE_STATUS, shadiv, 0 );
        if ( shadiv[ 0 ] != GLES20.GL_TRUE ) {
            String infoLog = GLES20.glGetShaderInfoLog ( shader );
            Log.e ( debugTag, "FAILED TO COMPILE SHADER: " + infoLog );
            return - 1;
        }

        return shader;
    }

    public static int loadShaderProgram (
            String vsCode, String fsCode, String debugTag
    ) {
        int vs, fs, prog;
        final int[] var = new int[ 1 ];
        vs = GLES20.glCreateShader ( GLES20.GL_VERTEX_SHADER );
        GLES20.glShaderSource ( vs, vsCode );
        GLES20.glCompileShader ( vs );
        GLES20.glGetShaderiv ( vs, GLES20.GL_COMPILE_STATUS, var, 0 );
        if ( var[ 0 ] != GLES20.GL_TRUE ) {
            String infoLog = GLES20.glGetShaderInfoLog ( vs );
            Log.e ( debugTag, "FAILED TO COMPILE SHADER: " + infoLog );
            return - 1;
        }

        fs = GLES20.glCreateShader ( GLES20.GL_FRAGMENT_SHADER );
        GLES20.glShaderSource ( fs, fsCode );
        GLES20.glCompileShader ( fs );
        GLES20.glGetShaderiv ( fs, GLES20.GL_COMPILE_STATUS, var, 0 );
        if ( var[ 0 ] != GLES20.GL_TRUE ) {
            String infoLog = GLES20.glGetShaderInfoLog ( fs );
            Log.e ( debugTag, "FAILED TO COMPILE SHADER: " + infoLog );
            GLES20.glDeleteShader ( vs );
            return - 1;
        }

        prog = GLES20.glCreateProgram ();

        GLES20.glAttachShader ( prog, vs );
        GLES20.glAttachShader ( prog, fs );
        GLES20.glLinkProgram ( prog );

        GLES20.glGetProgramiv ( prog, GLES20.GL_LINK_STATUS, var, 0 );
        if ( var[ 0 ] != GLES20.GL_TRUE ) {
            String infoLog = GLES20.glGetProgramInfoLog ( prog );
            Log.e ( debugTag, "FAILED TO LINK PROGRAM: " + infoLog );
            GLES20.glDeleteShader ( vs );
            GLES20.glDeleteShader ( fs );
            GLES20.glDeleteProgram ( prog );
            return - 1;
        }

        GLES20.glDeleteShader ( vs );
        GLES20.glDeleteShader ( fs );

        return prog;
    }

    public static void checkGLError ( String tag, String desc ) {
        int err;
        while ( ( err = GLES20.glGetError () ) != GLES20.GL_NO_ERROR ) {
            Log.e ( tag, desc + ": glError " + err );
            throw new RuntimeException ( desc + ": glError " + err );
        }
    }

}
