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

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class EigenFluidsSurfaceView extends GLSurfaceView {

    /**
     * @param context
     */

    private final EigenFluidsRenderer mRenderer;

    public EigenFluidsSurfaceView ( Context context ) {
        super ( context );
        // TODO Auto-generated constructor stub

        setEGLContextClientVersion ( 2 );

        mRenderer = new EigenFluidsRenderer ();

        setRenderer ( mRenderer );

        //	setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

//	private final float TOUCH_SCALE_FACTOR = 180.0f / 320.0f;
//	private float mPreviousX;
//	private float mPreviousY;

    @Override
    public boolean onTouchEvent ( MotionEvent e ) {

        mRenderer.handleTouchEvent ( e );
        requestRender ();

	/*	float x = e.getX();
		float y = e.getY();

		switch(e.getAction()) {
		case MotionEvent.ACTION_MOVE:
			float dx = x - mPreviousX;
			float dy = y - mPreviousY;

			if(y > getHeight() / 2) {
				dx = - dx;
			}

			if(x < getWidth() / 2) {
				dy = - dy;
			}
			break;
		case MotionEvent.ACTION_MOVE:
			break;
		case MotionEvent.ACTION_MOVE:
			break;
		case MotionEvent.ACTION_MOVE:
			break;
		}*/

        return true;
    }

}
