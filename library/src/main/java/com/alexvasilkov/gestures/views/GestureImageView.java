package com.alexvasilkov.gestures.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.Settings;
import com.alexvasilkov.gestures.State;
import com.alexvasilkov.gestures.utils.ClipHelper;
import com.alexvasilkov.gestures.views.interfaces.ClipView;
import com.alexvasilkov.gestures.views.interfaces.GestureView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GestureImageView extends ImageView implements GestureView, ClipView {

    private GestureController controller;
    private final ClipHelper clipViewHelper = new ClipHelper(this);
    private final ClipHelper clipBoundsHelper = new ClipHelper(this);
    private final Matrix imageMatrix = new Matrix();

    public GestureImageView(Context context) {
        this(context, null, 0);
    }

    public GestureImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        ensureControllerCreated();
        controller.getSettings().initFromAttributes(context, attrs);
        controller.addOnStateChangeListener(new GestureController.OnStateChangeListener() {
            @Override
            public void onStateChanged(State state) {
                applyState(state);
            }

            @Override
            public void onStateReset(State oldState, State newState) {
                applyState(newState);
            }
        });

        setScaleType(ImageView.ScaleType.MATRIX);
    }

    private void ensureControllerCreated() {
        if (controller == null) {
            controller = new GestureController(this);
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        clipBoundsHelper.onPreDraw(canvas);
        clipViewHelper.onPreDraw(canvas);
        super.draw(canvas);
        clipViewHelper.onPostDraw(canvas);
        clipBoundsHelper.onPostDraw(canvas);
    }

    @Override
    public GestureController getController() {
        return controller;
    }

    @Override
    public void clipView(@Nullable RectF rect, float rotation) {
        clipViewHelper.clipView(rect, rotation);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return controller.onTouch(this, event);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        controller.getSettings().setViewport(width - getPaddingLeft() - getPaddingRight(), height - getPaddingTop() - getPaddingBottom());
        controller.resetState();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);

        ensureControllerCreated();

        Settings settings = controller.getSettings();

        float oldWidth = settings.getImageWidth();
        float oldHeight = settings.getImageHeight();

        if (drawable == null) {
            settings.setImage(0, 0);
        } else if (drawable.getIntrinsicWidth() == -1 || drawable.getIntrinsicHeight() == -1) {
            settings.setImage(settings.getViewportWidth(), settings.getViewportHeight());
        } else {
            settings.setImage(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }

        float newWidth = settings.getImageWidth();
        float newHeight = settings.getImageHeight();

        if (newWidth > 0f && newHeight > 0f && oldWidth > 0f && oldHeight > 0f) {
            float scaleFactor = Math.min(oldWidth / newWidth, oldHeight / newHeight);
            controller.getStateController().setTempZoomPatch(scaleFactor);
            controller.updateState();
            controller.getStateController().setTempZoomPatch(0f);
        } else {
            controller.resetState();
        }
    }

    protected void applyState(State state) {
        state.get(imageMatrix);
        setImageMatrix(imageMatrix);
    }
}
