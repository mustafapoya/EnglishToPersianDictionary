package net.golbarg.engtoper.ui.flashcard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Container for the flashcard faces. It steals horizontal drags from any child (so the scrollable
 * back face can still be swiped) while leaving vertical scrolling and child buttons alone.
 */
public class SwipeCardLayout extends FrameLayout {

    public interface Listener {
        /** While true (e.g. during an animation) touches are swallowed. */
        boolean isBlocked();

        void onTap();

        void onDrag(float dx);

        void onRelease(float dx);

        void onDragCancelled();
    }

    private final int touchSlop;
    @Nullable
    private Listener listener;
    private float downX;
    private float downY;
    private boolean dragging;

    public SwipeCardLayout(@NonNull Context context) {
        this(context, null);
    }

    public SwipeCardLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (listener == null) return false;
        if (listener.isBlocked()) return true;
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                dragging = false;
                return false;
            case MotionEvent.ACTION_MOVE:
                return startDragIfHorizontal(ev);
            default:
                return false;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (listener == null) return false;
        if (listener.isBlocked()) return true;
        float dx = ev.getX() - downX;
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // No child wanted the touch (front face): track it ourselves for tap and drag
                downX = ev.getX();
                downY = ev.getY();
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                startDragIfHorizontal(ev);
                if (dragging) listener.onDrag(dx);
                return true;
            case MotionEvent.ACTION_UP:
                if (dragging) {
                    listener.onRelease(dx);
                } else if (Math.abs(dx) < touchSlop && Math.abs(ev.getY() - downY) < touchSlop) {
                    performClick();
                    listener.onTap();
                }
                dragging = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (dragging) listener.onDragCancelled();
                dragging = false;
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private boolean startDragIfHorizontal(MotionEvent ev) {
        if (dragging) return true;
        float dx = ev.getX() - downX;
        float dy = ev.getY() - downY;
        if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
            dragging = true;
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return dragging;
    }
}
