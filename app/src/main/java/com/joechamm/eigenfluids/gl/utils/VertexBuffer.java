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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.joechamm.eigenfluids.debugger.ProjectDebugger;

import android.opengl.GLES20;
import android.util.Log;

public abstract class VertexBuffer {
    private static final String TAG = "eigenfluids:VertexBuffer";

    private final int handle;

    public VertexBuffer ( float[] data ) {
        final int[] vbos = new int[ 1 ];
        GLES20.glGenBuffers ( vbos.length, vbos, 0 );

        if ( vbos[ 0 ] == 0 ) {
            Log.w ( TAG, "Failed to create vertex buffer." );
        }

        FloatBuffer dataBuffer = ByteBuffer.allocateDirect ( data.length * 4 )
                                           .order ( ByteOrder.nativeOrder () )
                                           .asFloatBuffer ()
                                           .put ( data );
        dataBuffer.position ( 0 );

        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, vbos[ 0 ] );
        GLES20.glBufferData ( GLES20.GL_ARRAY_BUFFER, data.length * 4, dataBuffer, GLES20.GL_STATIC_DRAW );
        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, 0 );

        handle = vbos[ 0 ];
    }

    public int getHandle () {
        return handle;
    }

    public void bind ( int attribLoc, int components, int stride, int offset ) {
        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, handle );
        GLES20.glEnableVertexAttribArray ( attribLoc );
        GLES20.glVertexAttribPointer ( attribLoc, components, GLES20.GL_FLOAT, false, stride, offset );
    }

    public void update ( float[] data, int offset ) {
        final int[] bufferSize = new int[ 1 ];
        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, handle );
        GLES20.glGetBufferParameteriv ( GLES20.GL_ARRAY_BUFFER, GLES20.GL_BUFFER_SIZE, bufferSize, 0 );
        if ( bufferSize[ 0 ] < data.length * 4 + offset ) {
            if ( ProjectDebugger.ON ) {
                Log.w ( TAG, "Invalid data size. Cannot update buffer." );
                GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, 0 );
                return;
            }
        }
        FloatBuffer dataBuffer = ByteBuffer.allocateDirect ( data.length * 4 )
                                           .order ( ByteOrder.nativeOrder () )
                                           .asFloatBuffer ()
                                           .put ( data );
        dataBuffer.position ( 0 );
        GLES20.glBufferSubData ( GLES20.GL_ARRAY_BUFFER, offset, data.length * 4, dataBuffer );
        GLES20.glBindBuffer ( GLES20.GL_ARRAY_BUFFER, 0 );
    }

}
