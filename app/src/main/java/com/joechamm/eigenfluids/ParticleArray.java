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
import java.nio.ShortBuffer;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class ParticleArray {

    public ParticleArray(int numParticles, boolean randomLocations) {
        int idx = 0;

        mNumParticles = numParticles;

        mParticlesXY = new float[mNumParticles * 2];
        mBufferIndices = new short[mNumParticles];

        if(randomLocations) {
            for(int i = 0; i < mNumParticles; i++) {
                float rx = (float)Math.random();
                float ry = (float)Math.random();
                mParticlesXY[i * 2] = rx;
                mParticlesXY[i * 2 + 1] = ry;
            }
        } else {
            int sqrt = (int) Math.sqrt(mNumParticles);
            float x = 0.0f;
            float y = 0.0f;
            float dx = (float)1.0 / sqrt;
            float dy = (float)1.0 / sqrt;
            for(int i = 0; i < mNumParticles; i++) {
                mParticlesXY[i * 2] = x;
                mParticlesXY[i * 2 + 1] = y;

                x += dx;
                idx++;
                if (idx > sqrt) {
                    idx = 0;
                    x = 0.0f;
                    y += dy;
                }
            }
        }

        idx = 0;

        for (int i = 0; i < mNumParticles; i++) {
            mBufferIndices[idx] = (short) i;
            idx++;
        }

        byte maxColor = (byte) 255;
        byte[] colors = {maxColor, 0, 0, maxColor};

        ByteBuffer vbb = ByteBuffer.allocateDirect(mParticlesXY.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        mFVertexBuffer = vbb.asFloatBuffer();
        mFVertexBuffer.put(mParticlesXY);
        mFVertexBuffer.position(0);

        ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length);
        cbb.order(ByteOrder.nativeOrder());
        mColorBuffer = cbb;
        mColorBuffer.put(colors);
        mColorBuffer.position(0);

        ByteBuffer ibb = ByteBuffer.allocateDirect(mBufferIndices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        mIndexBuffer = ibb.asShortBuffer();
        mIndexBuffer.put(mBufferIndices);
        mIndexBuffer.position(0);
    }

    public void draw(GL10 gl) {
        gl.glVertexPointer(2, GL11.GL_FLOAT, 0, mFVertexBuffer);
        gl.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 0, mColorBuffer);
        gl.glDrawElements(GL11.GL_POINTS, mNumParticles, GL11.GL_UNSIGNED_SHORT, mIndexBuffer);
    }

    public void update() {
        ByteBuffer vbb = ByteBuffer.allocateDirect(mParticlesXY.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        mFVertexBuffer = vbb.asFloatBuffer();
        mFVertexBuffer.put(mParticlesXY);
        mFVertexBuffer.position(0);

        ByteBuffer ibb = ByteBuffer.allocateDirect(mBufferIndices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        mIndexBuffer = ibb.asShortBuffer();
        mIndexBuffer.put(mBufferIndices);
        mIndexBuffer.position(0);
    }

    public int[] mParticleIndex;
    public short[] mBufferIndices;
    public float[] mParticlesXY;
    public int mNumParticles = 0;

    private FloatBuffer mFVertexBuffer;
    private ShortBuffer mIndexBuffer;
    private final ByteBuffer mColorBuffer;
}
