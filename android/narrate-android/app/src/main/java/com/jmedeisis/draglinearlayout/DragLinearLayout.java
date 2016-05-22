package com.jmedeisis.draglinearlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.datonicgroup.narrate.app.R;

/**
 * A LinearLayout that supports children Views that can be dragged and swapped around.
 * See {@link #addDragView(android.view.View, android.view.View)},
 * {@link #addDragView(android.view.View, android.view.View, int)},
 * {@link #setViewDraggable(android.view.View, android.view.View)}, and
 * {@link #removeDragView(android.view.View)}.
 * <p>
 * Currently, no error-checking is done on standard {@link #addView(android.view.View)} and
 * {@link #removeView(android.view.View)} calls, so avoid using these with children previously
 * declared as draggable to prevent memory leaks and/or subtle bugs.
 * <p>
 * Apologies - this class is neither clear nor readable. Maybe someday. But it works!
 *
 * https://github.com/justasm/DragLinearLayout/blob/master/library/src/main/java/com/jmedeisis/draglinearlayout/DragLinearLayout.java
 */
public class DragLinearLayout extends LinearLayout {
    private static final String LOG_TAG = "DragLinearLayout";
    private static final long NOMINAL_SWITCH_DURATION = 150;
    private static final long MIN_SWITCH_DURATION = NOMINAL_SWITCH_DURATION;
    private static final long MAX_SWITCH_DURATION = NOMINAL_SWITCH_DURATION * 2;
    private static final float NOMINAL_DISTANCE = 20;
    private final float nominalDistanceScaled;

    /**
     * Use with {@link com.jmedeisis.draglinearlayout.DragLinearLayout#setOnViewSwapListener(com.jmedeisis.draglinearlayout.DragLinearLayout.OnViewSwapListener)}
     * to listen for draggable view swaps.
     */
    public interface OnViewSwapListener {
        /**
         * Invoked right before the two items are swapped due to a drag event.
         * After the swap, the firstView will be in the secondPosition, and vice versa.
         * <p>
         * No guarantee is made as to which of the two has a lesser/greater position.
         */
        public void onSwap(View firstView, int firstPosition, View secondView, int secondPosition);

        public void onViewsSettled();

    }
    private OnViewSwapListener swapListener;

    /**
     * Mapping from child index to drag-related info container.
     * Presence of mapping implies the child can be dragged, and is considered for swaps with the
     * currently dragged item.
     */
    private final SparseArray<DraggableChild> draggableChildren;

    private class DraggableChild {
        /** If non-null, a reference to an on-going position animation. */
        private ValueAnimator swapAnimation;

        public void endExistingAnimation(){
            if(null != swapAnimation){
                swapAnimation.end();
            }
        }
    }

    /**
     * Holds state information about the currently dragged item.
     * <p>
     * Rough lifecycle:
     * <li>#setValidOnPossibleDrag - #valid == true</li>
     * <li>     if drag is recognised, #onDragStart - #dragging == true</li>
     * <li>     if drag ends, #onDragStop - #dragging == false, #settling == true</li>
     * <li>if gesture ends without drag, or settling finishes, #setInvalid - #valid == false</li>
     */
    private class DragItem {
        private View view;
        private int startVisibility;
        private BitmapDrawable viewDrawable;
        private int position;
        private int start;
        private int width;
        private int totalDragOffset;
        private int targetStartOffset;
        private ValueAnimator settleAnimation;

        private boolean valid;
        private boolean dragging;

        public DragItem(){
            setInvalid();
        }

        public void setValidOnPossibleDrag(final View view, final int position){
            this.view = view;
            this.startVisibility = view.getVisibility();
            this.viewDrawable = getDragDrawable(view);
            this.position = position;
            this.start = view.getLeft();
            this.width = view.getWidth();
            this.totalDragOffset = 0;
            this.targetStartOffset = 0;
            this.settleAnimation = null;

            this.valid = true;
        }

        public void onDragStart(){
            view.setVisibility(View.INVISIBLE);
            this.dragging = true;
        }

        public void setTotalOffset(int offset){
            totalDragOffset = offset;
            updateTargetLeft();
        }

        public void updateTargetLeft(){
            targetStartOffset = start - view.getLeft() + totalDragOffset;
        }

        public void onDragStop(){
            this.dragging = false;
        }

        public boolean settling(){
            return null != settleAnimation;
        }

        public void setInvalid(){
            this.valid = false;
            if(null != view) view.setVisibility(startVisibility);
            view = null;
            startVisibility = -1;
            viewDrawable = null;
            position = -1;
            start = -1;
            width = -1;
            totalDragOffset = 0;
            targetStartOffset = 0;
            if(null != settleAnimation) settleAnimation.end();
            settleAnimation = null;
        }
    }
    /** The currently dragged item, if {@link com.jmedeisis.draglinearlayout.DragLinearLayout.DragItem#valid}. */
    private final DragItem draggedItem;
    private final int slop;

    private static final int INVALID_POINTER_ID = -1;
    private int downX = -1;
    private int activePointerId = INVALID_POINTER_ID;

    /** See {@link #setContainerScrollView(android.widget.ScrollView)}. */
    private ScrollView containerScrollView;
    private int scrollSensitiveAreaWidth;
    private static final int DEFAULT_SCROLL_SENSITIVE_AREA_WIDTH_DP = 48;
    private static final int MAX_DRAG_SCROLL_SPEED = 16;

    /*private int placeholderViewResId;
    private View placeholderView;*/

    public DragLinearLayout(Context context){
        this(context, null);
    }

    public DragLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragLinearLayout(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);

        setOrientation(LinearLayout.HORIZONTAL);

        draggableChildren = new SparseArray<DraggableChild>();

        draggedItem = new DragItem();
        ViewConfiguration vc = ViewConfiguration.get(context);
        slop = vc.getScaledTouchSlop();

        final Resources resources = getResources();

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DragLinearLayout, 0, 0);
        try{
            scrollSensitiveAreaWidth = a.getDimensionPixelSize(R.styleable.DragLinearLayout_scrollSensitiveWidth,
                    (int) (DEFAULT_SCROLL_SENSITIVE_AREA_WIDTH_DP * resources.getDisplayMetrics().density + 0.5f));
            // placeholderViewResId = a.getResourceId(R.styleable.DragLinearLayout_placeholderView, 0);
        } finally {
            a.recycle();
        }
        /*if(0 != placeholderViewResId){
            placeholderView = View.inflate(context, placeholderViewResId, null);
        }*/

        nominalDistanceScaled = (int)(NOMINAL_DISTANCE * resources.getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void setOrientation(int orientation){
        if(LinearLayout.VERTICAL == orientation){
            throw new IllegalArgumentException("DragLinearLayout must be HORIZONTAL.");
        }
        super.setOrientation(orientation);
    }

    /** Calls {@link #addView(android.view.View)} followed by {@link #setViewDraggable(android.view.View, android.view.View)}. */
    public void addDragView(View child, View dragHandle){
        addView(child);
        setViewDraggable(child, dragHandle);
    }

    /**
     * Calls {@link #addView(android.view.View, int)} followed by
     * {@link #setViewDraggable(android.view.View, android.view.View)} and correctly updates the
     * drag-ability state of all existing views.
     */
    public void addDragView(View child, View dragHandle, int index){
        addView(child, index);

        // update drag-able children mappings
        final int numMappings = draggableChildren.size();
        for(int i = numMappings - 1; i >= 0; i--){
            final int key = draggableChildren.keyAt(i);
            if(key >= index){
                draggableChildren.put(key + 1, draggableChildren.get(key));
            }
        }

        setViewDraggable(child, dragHandle);
    }

    /** Makes the child a candidate for dragging. Must be an existing child of this layout. */
    public void setViewDraggable(View child, View dragHandle){
        if(this == child.getParent()){
            dragHandle.setOnTouchListener(new DragHandleOnTouchListener(child));
            draggableChildren.put(indexOfChild(child), new DraggableChild());
        } else {
            Log.e(LOG_TAG, child + " is not a child, cannot make draggable.");
        }
    }

    /**
     * Calls {@link #removeView(android.view.View)} and correctly updates the drag-ability state of
     * all remaining views.
     */
    public void removeDragView(View child){
        if(this == child.getParent()){
            final int index = indexOfChild(child);
            removeView(child);

            // update drag-able children mappings
            final int mappings = draggableChildren.size();
            for(int i = 0; i < mappings; i++){
                final int key = draggableChildren.keyAt(i);
                if(key >= index){
                    DraggableChild next = draggableChildren.get(key + 1);
                    if(null == next){
                        draggableChildren.delete(key);
                    } else {
                        draggableChildren.put(key, next);
                    }
                }
            }
        }
    }

    /**
     * If this layout is within a {@link android.widget.ScrollView}, register it here so that it
     * can be scrolled during item drags.
     */
    public void setContainerScrollView(ScrollView scrollView){
        this.containerScrollView = scrollView;
    }

    /**
     * Sets the height from upper / lower edge at which a container {@link android.widget.ScrollView},
     * if one is registered via {@link #setContainerScrollView(android.widget.ScrollView)},
     * is scrolled.
     */
    public void setScrollSensitiveHeight(int height){
        this.scrollSensitiveAreaWidth = height;
    }
    public int getScrollSensitiveHeight(){
        return scrollSensitiveAreaWidth;
    }

    /** See {@link com.jmedeisis.draglinearlayout.DragLinearLayout.OnViewSwapListener}. */
    public void setOnViewSwapListener(OnViewSwapListener swapListener){
        this.swapListener = swapListener;
    }

    /** A linear relationship b/w distance and duration, bounded. */
    private long getTranslateAnimationDuration(int distance){
        return Math.min(MAX_SWITCH_DURATION, Math.max(MIN_SWITCH_DURATION,
                (long)(NOMINAL_SWITCH_DURATION * Math.abs(distance) / nominalDistanceScaled)));
    }

    /*private void layoutPlaceholder(View view){
        int ws = MeasureSpec.makeMeasureSpec(view.getWidth(), MeasureSpec.EXACTLY);
        int hs = MeasureSpec.makeMeasureSpec(view.getHeight(), MeasureSpec.EXACTLY);
        placeholderView.measure(ws, hs);
        placeholderView.layout(0, 0,
                placeholderView.getMeasuredWidth(), placeholderView.getMeasuredHeight());
    }*/

    /**
     * Initiates a new {@link #draggedItem} unless the current one is still
     * {@link com.jmedeisis.draglinearlayout.DragLinearLayout.DragItem#valid}.
     */
    private void startDetectingDrag(View child){
        if(draggedItem.valid) return; // existing drag in process, only one at a time is allowed

        final int position = indexOfChild(child);

        // complete any existing animations, both for the newly selected child and the previous dragged one
        draggableChildren.get(position).endExistingAnimation();

        draggedItem.setValidOnPossibleDrag(child, position);
    }

    private void startDrag(){
        draggedItem.onDragStart();
        requestDisallowInterceptTouchEvent(true);
    }

    /** Animates the dragged item to its final resting position. */
    private void stopDrag(){
        draggedItem.settleAnimation = ValueAnimator.ofFloat(draggedItem.totalDragOffset,
                draggedItem.totalDragOffset - draggedItem.targetStartOffset)
                .setDuration(getTranslateAnimationDuration(draggedItem.targetStartOffset));
        draggedItem.settleAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if(!draggedItem.valid) return; // already stopped

                draggedItem.setTotalOffset(((Float) animation.getAnimatedValue()).intValue());

                invalidate();
            }
        });
        draggedItem.settleAnimation.addListener(new AnimatorListenerAdapter(){
            @Override
            public void onAnimationStart(Animator animation) {
                draggedItem.onDragStop();
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                if ( swapListener != null ) swapListener.onViewsSettled();
                if(!draggedItem.valid){
                    return; // already stopped
                }

                draggedItem.settleAnimation = null;
                draggedItem.setInvalid();
            }
        });
        draggedItem.settleAnimation.start();
    }

    /**
     * Updates the dragged item with the given total offset from its starting position.
     * Evaluates and executes draggable view swaps.
     */
    private void onDrag(final int offset){
        draggedItem.setTotalOffset(offset);
        invalidate();

        int currentLeft = draggedItem.start + draggedItem.totalDragOffset;

        handleContainerScroll(currentLeft);

        int nextPosition = nextDraggablePosition(draggedItem.position);
        int prevPosition = previousDraggablePosition(draggedItem.position);

        View nextView = getChildAt(nextPosition);
        View prevView = getChildAt(prevPosition);

        final boolean isAfter = (nextView != null) &&
                (currentLeft + draggedItem.width > nextView.getLeft() + nextView.getWidth() / 2);
        final boolean isBefore = (prevView != null) &&
                (currentLeft < prevView.getLeft() + prevView.getWidth() / 2);

        if(isAfter || isBefore){
            final View switchView = isAfter ? nextView : prevView;

            if(null == switchView){
                Log.e(LOG_TAG, "Switching with null");
                return;
            }

            // swap elements
            final int originalPosition = draggedItem.position;
            final int switchPosition = isAfter ? nextPosition : prevPosition;
            final int switchViewStartLeft = switchView.getLeft();

            if(null != swapListener){
                swapListener.onSwap(draggedItem.view, draggedItem.position, switchView, switchPosition);
            }

            if(isAfter){
                removeViewAt(originalPosition);
                removeViewAt(switchPosition - 1);

                addView(nextView, originalPosition);
                addView(draggedItem.view, switchPosition);
            } else {
                removeViewAt(switchPosition);
                removeViewAt(originalPosition - 1);

                addView(draggedItem.view, switchPosition);
                addView(prevView, originalPosition);
            }
            draggedItem.position = switchPosition;

            final ViewTreeObserver switchViewObserver = switchView.getViewTreeObserver();
            switchViewObserver.addOnPreDrawListener(new OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    switchViewObserver.removeOnPreDrawListener(this);

                    final ObjectAnimator switchAnimator = ObjectAnimator.ofFloat(switchView, "x",
                            switchViewStartLeft, switchView.getLeft())
                            .setDuration(getTranslateAnimationDuration(switchView.getLeft() - switchViewStartLeft));
                    switchAnimator.addListener(new AnimatorListenerAdapter(){
                        @Override
                        public void onAnimationStart(Animator animation) {
                            draggableChildren.get(originalPosition).swapAnimation = switchAnimator;
                        }
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            draggableChildren.get(originalPosition).swapAnimation = null;
                        }
                    });
                    switchAnimator.start();

                    return true;
                }
            });

            final ViewTreeObserver observer = draggedItem.view.getViewTreeObserver();
            observer.addOnPreDrawListener(new OnPreDrawListener(){
                @Override
                public boolean onPreDraw() {
                    observer.removeOnPreDrawListener(this);
                    draggedItem.updateTargetLeft();

                    // TODO test if still necessary..
                    // because draggedItem#view#getTop() is only up-to-date NOW
                    // (and not right after the #addView() swaps above)
                    // we may need to update an ongoing settle animation
                    if(draggedItem.settling()){
                        Log.d(LOG_TAG, "Updating settle animation");
                        draggedItem.settleAnimation.removeAllListeners();
                        draggedItem.settleAnimation.cancel();
                        stopDrag();
                    }
                    return true;
                }
            });
        }
    }

    private int previousDraggablePosition(int position){
        int startIndex = draggableChildren.indexOfKey(position);
        if(startIndex < 1 || startIndex > draggableChildren.size()) return -1;
        return draggableChildren.keyAt(startIndex - 1);
    }

    private int nextDraggablePosition(int position){
        int startIndex = draggableChildren.indexOfKey(position);
        if(startIndex < -1 || startIndex > draggableChildren.size() - 2) return -1;
        return draggableChildren.keyAt(startIndex + 1);
    }

    private Runnable dragUpdater;
    private void handleContainerScroll(final int currentLeft){
        if(null != containerScrollView){
            final int startScrollX = containerScrollView.getScrollX();
            final int absLeft = getLeft() - startScrollX + currentLeft;
            final int height = containerScrollView.getHeight();

            final int delta;

            if(absLeft < scrollSensitiveAreaWidth){
                delta = (int)(-MAX_DRAG_SCROLL_SPEED * smootherStep(scrollSensitiveAreaWidth, 0, absLeft));
            } else if(absLeft > height - scrollSensitiveAreaWidth){
                delta = (int)(MAX_DRAG_SCROLL_SPEED * smootherStep(height - scrollSensitiveAreaWidth, height, absLeft));
            } else {
                delta = 0;
            }

            containerScrollView.removeCallbacks(dragUpdater);
            containerScrollView.smoothScrollBy(delta, 0);
            dragUpdater = new Runnable(){
                @Override
                public void run() {
                    if(draggedItem.dragging && startScrollX != containerScrollView.getScrollX()){
                        onDrag(draggedItem.totalDragOffset + delta);
                    }
                }
            };
            containerScrollView.post(dragUpdater);
        }
    }

    /** By Ken Perlin. See <a href="http://en.wikipedia.org/wiki/Smoothstep">Smoothstep - Wikipedia</a>. */
    private static float smootherStep(float e1, float e2, float x){
        x = Math.max(0, Math.min((x - e1) / (e2 - e1), 1));
        return x * x * x * (x * (x * 6 - 15) + 10);
    }

    @Override
    protected void dispatchDraw(Canvas canvas){
        super.dispatchDraw(canvas);

        if(draggedItem.valid && (draggedItem.dragging || draggedItem.settling())){
            canvas.save();
            canvas.translate(draggedItem.totalDragOffset, 0);
            draggedItem.viewDrawable.draw(canvas);

//            final int left = draggedItem.viewDrawable.getBounds().left;
//            final int right = draggedItem.viewDrawable.getBounds().right;
//            final int top = draggedItem.viewDrawable.getBounds().top;
//            final int bottom = draggedItem.viewDrawable.getBounds().bottom;
//
//            dragRightShadowDrawable.setBounds(left, top, right + dragShadowWidth, bottom);
//            dragRightShadowDrawable.draw(canvas);
//
//            if(null != dragLeftShadowDrawable){
//                dragLeftShadowDrawable.setBounds(left - dragShadowWidth, top, right, bottom);
//                dragLeftShadowDrawable.draw(canvas);
//            }

            canvas.restore();
        }
    }

    /*
     * Note regarding touch handling:
     * In general, we have three cases -
     * 1) User taps outside any children.
     *      #onInterceptTouchEvent receives DOWN
     *      #onTouchEvent receives DOWN
     *          draggedItem.valid == false, we return false and no further events are received
     * 2) User taps on non-interactive drag handle / child, e.g. TextView or ImageView.
     *      #onInterceptTouchEvent receives DOWN
     *      DragHandleOnTouchListener (attached to each draggable child) #onTouch receives DOWN
     *      #startDetectingDrag is called, draggedItem is now valid
     *      view does not handle touch, so our #onTouchEvent receives DOWN
     *          draggedItem.valid == true, we #startDrag() and proceed to handle the drag
     * 3) User taps on interactive drag handle / child, e.g. Button.
     *      #onInterceptTouchEvent receives DOWN
     *      DragHandleOnTouchListener (attached to each draggable child) #onTouch receives DOWN
     *      #startDetectingDrag is called, draggedItem is now valid
     *      view handles touch, so our #onTouchEvent is not called yet
     *      #onInterceptTouchEvent receives ACTION_MOVE
     *      if dy > touch slop, we assume user wants to drag and intercept the event
     *      #onTouchEvent receives further ACTION_MOVE events, proceed to handle the drag
     *
     * For cases 2) and 3), lifting the active pointer at any point in the sequence of events
     * triggers #onTouchEnded and the draggedItem, if valid, is #setInvalid.
     */

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event){
        switch(MotionEventCompat.getActionMasked(event)){
            case MotionEvent.ACTION_DOWN: {
                if(draggedItem.valid) return false; // an existing item is (likely) settling
                downX = (int) MotionEventCompat.getX(event, 0);
                activePointerId = MotionEventCompat.getPointerId(event, 0);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if(!draggedItem.valid) return false;
                if(INVALID_POINTER_ID == activePointerId) break;
                final int pointerIndex = event.findPointerIndex(activePointerId);
                final float x = MotionEventCompat.getX(event, pointerIndex);
                final float dx = x - downX;
                if(Math.abs(dx) > slop){
                    startDrag();
                    return true;
                }
                return false;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = MotionEventCompat.getActionIndex(event);
                final int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);

                if(pointerId != activePointerId) break; // if active pointer, fall through and cancel!
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                onTouchEnded();

                if(draggedItem.valid) draggedItem.setInvalid();
                break;
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        switch(MotionEventCompat.getActionMasked(event)){
            case MotionEvent.ACTION_DOWN: {
                if(!draggedItem.valid || draggedItem.settling()) return false;
                startDrag();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if(!draggedItem.dragging) break;
                if(INVALID_POINTER_ID == activePointerId) break;

                int pointerIndex = event.findPointerIndex(activePointerId);
                int lastEventX = (int) MotionEventCompat.getX(event, pointerIndex);
                int deltaX = lastEventX - downX;

                onDrag(deltaX);
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = MotionEventCompat.getActionIndex(event);
                final int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);

                if(pointerId != activePointerId) break; // if active pointer, fall through and cancel!
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                onTouchEnded();

                if(draggedItem.dragging) stopDrag(); // TODO test whether check necessary
                return true;
            }
        }
        return false;
    }

    private void onTouchEnded(){
        downX = -1;
        activePointerId = INVALID_POINTER_ID;
    }

    private class DragHandleOnTouchListener implements OnTouchListener {
        private final View view;

        public DragHandleOnTouchListener(final View view){
            this.view = view;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(MotionEvent.ACTION_DOWN == MotionEventCompat.getActionMasked(event)){
                startDetectingDrag(view);
            }
            return false;
        }
    }

    private BitmapDrawable getDragDrawable(View view) {
        int top = view.getTop();
        int left = view.getLeft();

        Bitmap bitmap = getBitmapFromView(view);

        BitmapDrawable drawable = new BitmapDrawable(getResources(), bitmap);

        drawable.setBounds(new Rect(left, top, left + view.getWidth(), top + view.getHeight()));

        return drawable;
    }

    /** @return a bitmap showing a screenshot of the view passed in. */
    private static Bitmap getBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }
}