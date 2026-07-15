package com.hirain.aiagent.test.pet;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.appcompat.widget.PopupMenu;
import androidx.drawerlayout.widget.DrawerLayout;

import com.hirain.aiagent.test.R;

/**
 * 管理应用内 Doge 的手势、贴边、隐藏恢复和位置持久化。
 *
 * <p>坐标始终相对聊天区域内的 petOverlay 计算。Overlay 本身不消费触摸，因此宠物之外
 * 的消息滚动、侧边栏手势和输入操作保持原有行为。</p>
 */
public final class FloatingPetController {

    private static final String PREFS_NAME = "floating_pet_state";
    private static final String KEY_HIDDEN = "pet_hidden";
    private static final String KEY_EDGE = "pet_edge";
    private static final String KEY_VERTICAL_FRACTION = "pet_vertical_fraction";
    private static final float DEFAULT_VERTICAL_FRACTION = 0.82f;
    private static final long SNAP_DURATION_MS = 180L;

    private enum State {
        IDLE,
        JUMPING,
        WAVING,
        DRAGGING,
        HIDDEN,
        RELEASED
    }

    private enum Edge {
        LEFT,
        RIGHT
    }

    private final Context context;
    private final DrawerLayout drawerLayout;
    private final FrameLayout petOverlay;
    private final PetSpriteView petView;
    private final SharedPreferences preferences;
    private final GestureDetector gestureDetector;
    private final int touchSlop;
    private final int edgeMarginPx;
    private final int normalWidthPx;
    private final int normalHeightPx;
    private final int hiddenWidthPx;
    private final int hiddenHeightPx;

    private State state;
    private Edge edge;
    private float verticalFraction;
    private boolean dragging;
    private boolean hiddenGestureMoved;
    private float downRawX;
    private float downRawY;
    private float startViewX;
    private float startViewY;
    private float lastRawX;
    private int activeAnimationGeneration = -1;
    private PopupMenu activePopupMenu;

    private final View.OnLayoutChangeListener overlayLayoutChangeListener =
            this::onOverlayLayoutChanged;

    private final DrawerLayout.DrawerListener drawerListener = new DrawerLayout.SimpleDrawerListener() {
        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            dismissActionMenu();
        }
    };

    public FloatingPetController(Context context, DrawerLayout drawerLayout,
                                 FrameLayout petOverlay, PetSpriteView petView) {
        this.context = context;
        this.drawerLayout = drawerLayout;
        this.petOverlay = petOverlay;
        this.petView = petView;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.edgeMarginPx = context.getResources().getDimensionPixelSize(R.dimen.pet_edge_margin);
        this.normalWidthPx = context.getResources().getDimensionPixelSize(R.dimen.pet_width);
        this.normalHeightPx = context.getResources().getDimensionPixelSize(R.dimen.pet_height);
        this.hiddenWidthPx = context.getResources().getDimensionPixelSize(R.dimen.pet_hidden_width);
        this.hiddenHeightPx = context.getResources().getDimensionPixelSize(R.dimen.pet_hidden_height);

        edge = "left".equals(preferences.getString(KEY_EDGE, "right")) ? Edge.LEFT : Edge.RIGHT;
        verticalFraction = sanitizeFraction(
                preferences.getFloat(KEY_VERTICAL_FRACTION, DEFAULT_VERTICAL_FRACTION));
        boolean hidden = preferences.getBoolean(KEY_HIDDEN, false);
        state = hidden ? State.HIDDEN : State.IDLE;

        gestureDetector = new GestureDetector(context, new PetGestureListener());
        petView.setOnAnimationFinishedListener(this::onAnimationFinished);
        petView.setOnTouchListener(this::onPetTouch);
        petOverlay.addOnLayoutChangeListener(overlayLayoutChangeListener);
        drawerLayout.addDrawerListener(drawerListener);

        if (hidden) {
            applyPetSize(true);
            petView.showStaticFrame(PetSpriteView.Animation.IDLE, 0);
            petView.setContentDescription(context.getString(R.string.pet_hidden_content_description));
        } else {
            applyPetSize(false);
            setIdle();
        }
        petView.post(this::restorePositionFromState);
    }

    private void onOverlayLayoutChanged(View view, int left, int top, int right, int bottom,
                                        int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (state == State.RELEASED || (right - left == oldRight - oldLeft
                && bottom - top == oldBottom - oldTop)) {
            return;
        }
        petView.animate().cancel();
        dragging = false;
        if (state == State.DRAGGING) {
            setIdle();
        }
        petView.post(this::restorePositionFromState);
    }

    private boolean onPetTouch(View view, MotionEvent event) {
        if (state == State.RELEASED) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                petView.animate().cancel();
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                lastRawX = downRawX;
                startViewX = petView.getX();
                startViewY = petView.getY();
                dragging = false;
                hiddenGestureMoved = false;
                drawerLayout.requestDisallowInterceptTouchEvent(true);
                gestureDetector.onTouchEvent(event);
                return true;

            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - downRawX;
                float deltaY = event.getRawY() - downRawY;
                float distance = (float) Math.hypot(deltaX, deltaY);
                if (state == State.HIDDEN) {
                    hiddenGestureMoved |= distance > touchSlop;
                    gestureDetector.onTouchEvent(event);
                    return true;
                }
                if (!dragging && distance > touchSlop) {
                    dragging = true;
                    state = State.DRAGGING;
                    dismissActionMenu();
                    activeAnimationGeneration = -1;
                }
                if (dragging) {
                    float targetX = clampX(startViewX + deltaX);
                    float targetY = clampY(startViewY + deltaY);
                    petView.setX(targetX);
                    petView.setY(targetY);
                    float horizontalStep = event.getRawX() - lastRawX;
                    PetSpriteView.Animation runAnimation = petView.getCurrentAnimation();
                    if (Math.abs(horizontalStep) >= 1f
                            || (runAnimation != PetSpriteView.Animation.RUN_LEFT
                            && runAnimation != PetSpriteView.Animation.RUN_RIGHT)) {
                        runAnimation = horizontalStep >= 0f
                                ? PetSpriteView.Animation.RUN_RIGHT
                                : PetSpriteView.Animation.RUN_LEFT;
                    }
                    activeAnimationGeneration = petView.setAnimation(runAnimation, false);
                    lastRawX = event.getRawX();
                } else {
                    gestureDetector.onTouchEvent(event);
                }
                return true;

            case MotionEvent.ACTION_UP:
                try {
                    if (state == State.HIDDEN && hiddenGestureMoved) {
                        restorePet();
                    } else if (dragging) {
                        finishDragAndSnap();
                    } else {
                        resumeEdgeSnap();
                        gestureDetector.onTouchEvent(event);
                    }
                    return true;
                } finally {
                    dragging = false;
                    hiddenGestureMoved = false;
                    drawerLayout.requestDisallowInterceptTouchEvent(false);
                }

            case MotionEvent.ACTION_CANCEL:
                try {
                    if (dragging) {
                        finishDragAndSnap();
                    } else if (state != State.HIDDEN) {
                        petView.setX(clampX(petView.getX()));
                        petView.setY(clampY(petView.getY()));
                        setIdle();
                    }
                    return true;
                } finally {
                    dragging = false;
                    hiddenGestureMoved = false;
                    drawerLayout.requestDisallowInterceptTouchEvent(false);
                }

            default:
                gestureDetector.onTouchEvent(event);
                return true;
        }
    }

    private void finishDragAndSnap() {
        float centerX = petView.getX() + petView.getWidth() / 2f;
        edge = centerX < petOverlay.getWidth() / 2f ? Edge.LEFT : Edge.RIGHT;
        petView.setY(clampY(petView.getY()));
        verticalFraction = calculateVerticalFraction(petView.getY());
        persistState(false);
        setIdle();
        petView.animate()
                .x(edge == Edge.LEFT ? minX() : maxX())
                .setDuration(SNAP_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * 用户可能在贴边动画尚未结束时点击宠物。ACTION_DOWN 会取消旧动画以保证手势稳定，
     * 如果最终只是点击而非拖动，则继续回到原边缘，避免宠物永久停在聊天区中央。
     */
    private void resumeEdgeSnap() {
        if (state == State.HIDDEN || state == State.RELEASED) {
            return;
        }
        petView.animate()
                .x(edge == Edge.LEFT ? minX() : maxX())
                .setDuration(SNAP_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void showActionMenu() {
        if (state == State.HIDDEN || state == State.RELEASED) {
            return;
        }
        dismissActionMenu();
        PopupMenu popupMenu = new PopupMenu(context, petView);
        popupMenu.inflate(R.menu.menu_pet_actions);
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_hide_pet) {
                hidePet();
                return true;
            }
            return false;
        });
        popupMenu.setOnDismissListener(menu -> {
            if (activePopupMenu == menu) {
                activePopupMenu = null;
            }
        });
        activePopupMenu = popupMenu;
        popupMenu.show();
    }

    private void dismissActionMenu() {
        PopupMenu popupMenu = activePopupMenu;
        activePopupMenu = null;
        if (popupMenu != null) {
            popupMenu.dismiss();
        }
    }

    private void hidePet() {
        if (state == State.HIDDEN || state == State.RELEASED) {
            return;
        }
        dismissActionMenu();
        petView.animate().cancel();
        edge = petView.getX() + petView.getWidth() / 2f < petOverlay.getWidth() / 2f
                ? Edge.LEFT : Edge.RIGHT;
        verticalFraction = calculateVerticalFraction(petView.getY());
        state = State.HIDDEN;
        activeAnimationGeneration = petView.showStaticFrame(PetSpriteView.Animation.IDLE, 0);
        applyPetSize(true);
        petView.setContentDescription(context.getString(R.string.pet_hidden_content_description));
        persistState(true);
        petView.post(this::restorePositionFromState);
    }

    private void restorePet() {
        if (state != State.HIDDEN) {
            return;
        }
        state = State.WAVING;
        applyPetSize(false);
        petView.setContentDescription(context.getString(R.string.pet_content_description));
        persistState(false);
        petView.post(() -> {
            restorePositionFromState();
            activeAnimationGeneration = petView.setAnimation(PetSpriteView.Animation.WAVING, true);
        });
    }

    private void playJumping() {
        if (state == State.HIDDEN) {
            restorePet();
            return;
        }
        if (state == State.RELEASED || state == State.DRAGGING) {
            return;
        }
        dismissActionMenu();
        state = State.JUMPING;
        activeAnimationGeneration = petView.setAnimation(PetSpriteView.Animation.JUMPING, true);
    }

    private void onAnimationFinished(PetSpriteView.Animation animation, int generation) {
        if (state == State.RELEASED || generation != activeAnimationGeneration) {
            return;
        }
        boolean jumpingFinished = state == State.JUMPING
                && animation == PetSpriteView.Animation.JUMPING;
        boolean wavingFinished = state == State.WAVING
                && animation == PetSpriteView.Animation.WAVING;
        if (jumpingFinished || wavingFinished) {
            setIdle();
        }
    }

    private void setIdle() {
        if (state == State.RELEASED || state == State.HIDDEN) {
            return;
        }
        state = State.IDLE;
        activeAnimationGeneration = petView.setAnimation(PetSpriteView.Animation.IDLE, true);
    }

    private void restorePositionFromState() {
        if (state == State.RELEASED || petOverlay.getWidth() <= 0 || petOverlay.getHeight() <= 0
                || petView.getWidth() <= 0 || petView.getHeight() <= 0) {
            return;
        }
        petView.setX(edge == Edge.LEFT ? minX() : maxX());
        float availableHeight = Math.max(0f, maxY() - minY());
        petView.setY(clampY(minY() + availableHeight * verticalFraction));
    }

    private void applyPetSize(boolean hidden) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) petView.getLayoutParams();
        params.width = hidden ? hiddenWidthPx : normalWidthPx;
        params.height = hidden ? hiddenHeightPx : normalHeightPx;
        petView.setLayoutParams(params);
    }

    private float minX() {
        return Math.min(edgeMarginPx, Math.max(0f, petOverlay.getWidth() - petView.getWidth()));
    }

    private float maxX() {
        return Math.max(minX(), petOverlay.getWidth() - petView.getWidth() - edgeMarginPx);
    }

    private float minY() {
        return Math.min(edgeMarginPx, Math.max(0f, petOverlay.getHeight() - petView.getHeight()));
    }

    private float maxY() {
        return Math.max(minY(), petOverlay.getHeight() - petView.getHeight() - edgeMarginPx);
    }

    private float clampX(float value) {
        return Math.max(minX(), Math.min(value, maxX()));
    }

    private float clampY(float value) {
        return Math.max(minY(), Math.min(value, maxY()));
    }

    private float calculateVerticalFraction(float y) {
        float range = maxY() - minY();
        if (range <= 0f) {
            return DEFAULT_VERTICAL_FRACTION;
        }
        return sanitizeFraction((y - minY()) / range);
    }

    private float sanitizeFraction(float fraction) {
        if (Float.isNaN(fraction) || Float.isInfinite(fraction)) {
            return DEFAULT_VERTICAL_FRACTION;
        }
        return Math.max(0f, Math.min(fraction, 1f));
    }

    private void persistState(boolean hidden) {
        preferences.edit()
                .putBoolean(KEY_HIDDEN, hidden)
                .putString(KEY_EDGE, edge == Edge.LEFT ? "left" : "right")
                .putFloat(KEY_VERTICAL_FRACTION, verticalFraction)
                .apply();
    }

    /**
     * 对称释放所有监听和动画。方法可重复调用，避免 Activity 异常销毁路径留下回调引用。
     */
    public void destroy() {
        if (state == State.RELEASED) {
            return;
        }
        state = State.RELEASED;
        dismissActionMenu();
        petView.animate().cancel();
        petView.stop();
        petView.setOnAnimationFinishedListener(null);
        petView.setOnTouchListener(null);
        petOverlay.removeOnLayoutChangeListener(overlayLayoutChangeListener);
        drawerLayout.removeDrawerListener(drawerListener);
        drawerLayout.requestDisallowInterceptTouchEvent(false);
    }

    private final class PetGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            petView.performClick();
            if (state == State.HIDDEN) {
                restorePet();
            } else if (state != State.DRAGGING && state != State.RELEASED) {
                showActionMenu();
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            petView.performClick();
            if (state == State.HIDDEN) {
                restorePet();
            } else {
                playJumping();
            }
            return true;
        }
    }
}
