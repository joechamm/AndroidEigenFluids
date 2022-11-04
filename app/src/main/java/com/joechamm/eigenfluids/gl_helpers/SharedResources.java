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

public class SharedResources {

    public static int velProg = 0;
    public static int velAPos = - 1;
    public static int velUMVP = - 1;
    public static int velUCol = - 1;

    public static int densProg = 0;
    public static int densAPos = - 1;
    public static int densATex = - 1;
    public static int densUMVP = - 1;
    public static int densUCol = - 1;
    public static int densUDens = - 1;

    public static void initShaderPrograms () {

        densProg = CommonGL.loadShaderProgram ( densVertShaderCode, densFragmentShaderCode );
        densAPos = GLES20.glGetAttribLocation ( densProg, "aPos" );
        densATex = GLES20.glGetAttribLocation ( densProg, "aTex" );
        densUMVP = GLES20.glGetUniformLocation ( densProg, "uMVP" );
        densUCol = GLES20.glGetUniformLocation ( densProg, "uColor" );
        densUDens = GLES20.glGetUniformLocation ( densProg, "uDensity" );

        velProg = CommonGL.loadShaderProgram ( velVertexShaderCode, velFragmentShaderCode );
        velAPos = GLES20.glGetAttribLocation ( velProg, "aPos" );
        velUMVP = GLES20.glGetUniformLocation ( velProg, "uMVP" );
        velUCol = GLES20.glGetUniformLocation ( velProg, "uColor" );
    }

}
