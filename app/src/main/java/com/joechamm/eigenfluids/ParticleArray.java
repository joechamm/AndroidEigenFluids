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
            int sqrt = (int)Math.sqrt((double)mNumParticles);
            float x = 0.0f;
            float y = 0.0f;
            float dx = (float)1.0 / sqrt;
            float dy = (float)1.0 / sqrt;
            for(int i = 0; i < mNumParticles; i++) {
                mParticlesXY[i * 2] = x;
                mParticlesXY[i * 2 + 1] = y;

                x += dx;
                if(idx > sqrt) {
                    idx = 0;
                    x = 0.0f;
                    y += dy;
                }
            }
        }

        idx = 0;

        for(int i = 0; i < mNumParticles; i++) {
            mBufferIndices[idx] = (short)i;
            idx++;
        }

        byte maxColor = (byte)255;
        byte colors[] = { maxColor, 0, 0, maxColor};

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
    private ByteBuffer mColorBuffer;
}
