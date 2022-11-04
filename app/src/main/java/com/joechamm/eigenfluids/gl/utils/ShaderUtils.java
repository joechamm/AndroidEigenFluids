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

package com.joechamm.eigenfluids.gl.utils;

import com.joechamm.eigenfluids.debugger.ProjectDebugger;

import android.opengl.GLES20;
import android.util.Log;

public class ShaderUtils {
    private static final String TAG = "eigenfluids:ShaderUtils";

    public static int loadShaderProgram ( String vsCode, String fsCode ) {
        int vs, fs, prog;
        vs = ShaderUtils.loadShader ( GLES20.GL_VERTEX_SHADER, vsCode );
        if ( vs == 0 ) {
            return 0;
        }
        fs = ShaderUtils.loadShader ( GLES20.GL_FRAGMENT_SHADER, fsCode );
        if ( fs == 0 ) {
            return 0;
        }

        prog = GLES20.glCreateProgram ();

        if ( prog == 0 ) {
            if ( ProjectDebugger.ON ) {
                Log.w ( TAG, "Failed to create shader program." );
            }

            GLES20.glDeleteShader ( vs );
            GLES20.glDeleteShader ( fs );
            return 0;

        }

        if ( ! ShaderUtils.linkShaderProgram ( vs, fs, prog ) ) {
            return 0;
        }

        if ( ProjectDebugger.ON ) {
            ShaderUtils.validateShaderProgram ( prog );
        }

        return prog;
    }

    public static int loadShader ( int type, String shaderCode ) {
        final int shader = GLES20.glCreateShader ( type );

        if ( shader == 0 ) {
            if ( ProjectDebugger.ON ) {
                Log.w ( TAG, "Failed to create shader." );
            }

            return 0;
        }

        GLES20.glShaderSource ( shader, shaderCode );
        GLES20.glCompileShader ( shader );

        final int[] compiled = new int[ 1 ];
        GLES20.glGetShaderiv ( shader, GLES20.GL_COMPILE_STATUS, compiled, 0 );

        if ( ProjectDebugger.ON ) {
            Log.v ( TAG, "Shader Info Log:" + "\n" + shaderCode + "\n:" + GLES20.glGetShaderInfoLog ( shader ) );
        }

        if ( compiled[ 0 ] == 0 ) {
            GLES20.glDeleteShader ( shader );

            if ( ProjectDebugger.ON ) {
                Log.w ( TAG, "Failed to compile shader." );
            }

            return 0;
        }

        return shader;
    }

    public static boolean linkShaderProgram ( int vs, int fs, int prog ) {
        if ( vs == 0 ) {
            if ( ProjectDebugger.ON ) {
                Log.w ( TAG, "No vertex shader handle passed. Cannot link." );
            }

            return false;
        }

        if ( fs == 0 ) {
            if ( ProjectDebugger.ON ) {
                Log.w ( TAG, "No fragment shader handle passed. Cannot link." );
            }

            return false;
        }

        if ( prog == 0 ) {
            if ( ProjectDebugger.ON ) {
                Log.w ( TAG, "No program handle passed. Cannot link." );
            }

            return false;
        }

        GLES20.glAttachShader ( prog, vs );
        GLES20.glAttachShader ( prog, fs );
        GLES20.glLinkProgram ( prog );

        final int[] linked = new int[ 1 ];
        GLES20.glGetProgramiv ( prog, GLES20.GL_LINK_STATUS, linked, 0 );
        if ( ProjectDebugger.ON ) {
            Log.v ( TAG, "Program Info Log:\n " + GLES20.glGetProgramInfoLog ( prog ) );
        }

        if ( linked[ 0 ] == 0 ) {
            GLES20.glDeleteProgram ( prog );

            if ( ProjectDebugger.ON ) {
                Log.w ( TAG, "Failed to link shader program." );
            }

            GLES20.glDeleteShader ( vs );
            GLES20.glDeleteShader ( fs );

            return false;
        }

        GLES20.glDeleteShader ( vs );
        GLES20.glDeleteShader ( fs );

        return true;
    }

    public static boolean validateShaderProgram ( int prog ) {
        GLES20.glValidateProgram ( prog );
        final int[] validated = new int[ 1 ];
        GLES20.glGetProgramiv ( prog, GLES20.GL_VALIDATE_STATUS, validated, 0 );
        Log.v ( TAG, "Shader Program Validate Status: " + validated[ 0 ] + "\nInfo Log: " + GLES20.glGetProgramInfoLog ( prog ) );
        return validated[ 0 ] != 0;
    }

}
