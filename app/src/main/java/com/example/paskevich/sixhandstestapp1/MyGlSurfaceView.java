package com.example.paskevich.sixhandstestapp1;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by paskevich on 08.12.17.
 */

public class MyGlSurfaceView extends GLSurfaceView {

    MyRenderer renderer;

    float prevX;
    float prevY;

    public MyGlSurfaceView(Context context) {
        super(context);
    }

    public MyGlSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (renderer.getBitmapImage() == null) {
            return true;
        }

        float x = event.getX();
        float y = event.getY();

        Log.d("x and y", "onTouchEvent: " + x + ", " + y);

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:

                if (renderer.getFboRenderer().moveObject(x, y, prevX, prevY)) {
                    requestRender();
                }
                break;

            case MotionEvent.ACTION_DOWN:
                // TODO: 11.12.17 перевод координат точки касания
                renderer.getFboRenderer().setObject(x, y);
                break;

            // TODO: 11.12.17 поворот, масштаб
        }

        prevX = x;
        prevY = y;

        return true;
    }

    @Override
    public void setRenderer(Renderer renderer) {
        super.setRenderer(renderer);
        this.renderer = (MyRenderer) renderer;
    }
}
