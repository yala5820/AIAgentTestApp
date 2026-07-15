package com.hirain.aiagent.test.pet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import com.hirain.aiagent.test.R;

/**
 * 负责 Doge 静态 WebP 图集的裁切绘制和逐帧播放。
 *
 * <p>该 View 刻意不理解拖动、隐藏或业务请求状态。这样未来替换自制图集时，只需要调整
 * 动作帧表，悬浮交互控制器无需随素材一起重写。</p>
 */
public class PetSpriteView extends View {

    private static final int FRAME_WIDTH = 192;
    private static final int FRAME_HEIGHT = 208;

    public enum Animation {
        IDLE(0, 6, 160L, true),
        RUN_RIGHT(1, 8, 90L, true),
        RUN_LEFT(2, 8, 90L, true),
        WAVING(3, 4, 130L, false),
        JUMPING(4, 5, 120L, false),
        FAILED(5, 8, 140L, false),
        WAITING(6, 6, 140L, true),
        RUNNING(7, 6, 140L, true),
        REVIEW(8, 6, 140L, true);

        private final int row;
        private final int frameCount;
        private final long frameDurationMs;
        private final boolean loop;

        Animation(int row, int frameCount, long frameDurationMs, boolean loop) {
            this.row = row;
            this.frameCount = frameCount;
            this.frameDurationMs = frameDurationMs;
            this.loop = loop;
        }
    }

    public interface OnAnimationFinishedListener {
        void onAnimationFinished(Animation animation, int generation);
    }

    private final Paint bitmapPaint = new Paint(Paint.DITHER_FLAG);
    private final Rect sourceRect = new Rect();
    private final RectF destinationRect = new RectF();
    private final Runnable frameRunnable = this::advanceFrame;

    private Bitmap spriteSheet;
    private Animation currentAnimation = Animation.IDLE;
    private int frameIndex;
    private int generation;
    private boolean playing;
    private boolean callbackPosted;
    private long lastFrameTimeMs;
    private OnAnimationFinishedListener animationFinishedListener;

    public PetSpriteView(Context context) {
        super(context);
        init();
    }

    public PetSpriteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PetSpriteView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // drawable-nodpi 保证 Android 不会按设备密度缩放源图集坐标。
        spriteSheet = BitmapFactory.decodeResource(getResources(), R.drawable.pet_doge_spritesheet);
        bitmapPaint.setFilterBitmap(false);
        bitmapPaint.setAntiAlias(false);
        setClickable(true);
    }

    public void setOnAnimationFinishedListener(OnAnimationFinishedListener listener) {
        this.animationFinishedListener = listener;
    }

    /**
     * 切换动画并返回本次动画代次。Controller 使用代次忽略已经被新手势淘汰的完成回调。
     */
    public int setAnimation(Animation animation, boolean restart) {
        if (animation == null) {
            return generation;
        }
        if (animation == currentAnimation && playing && !restart) {
            return generation;
        }
        generation++;
        removeFrameCallback();
        currentAnimation = animation;
        if (restart || frameIndex >= animation.frameCount) {
            frameIndex = 0;
        }
        playing = true;
        lastFrameTimeMs = SystemClock.uptimeMillis();
        invalidate();
        scheduleFrameCallback();
        return generation;
    }

    public int showStaticFrame(Animation animation, int requestedFrameIndex) {
        generation++;
        removeFrameCallback();
        currentAnimation = animation == null ? Animation.IDLE : animation;
        frameIndex = Math.max(0, Math.min(requestedFrameIndex, currentAnimation.frameCount - 1));
        playing = false;
        invalidate();
        return generation;
    }

    public void start() {
        if (!playing) {
            playing = true;
            lastFrameTimeMs = SystemClock.uptimeMillis();
        }
        scheduleFrameCallback();
    }

    public void stop() {
        playing = false;
        removeFrameCallback();
    }

    public Animation getCurrentAnimation() {
        return currentAnimation;
    }

    private void advanceFrame() {
        callbackPosted = false;
        if (!playing) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        long elapsed = now - lastFrameTimeMs;
        if (elapsed >= currentAnimation.frameDurationMs) {
            int steps = Math.max(1, (int) (elapsed / currentAnimation.frameDurationMs));
            lastFrameTimeMs += steps * currentAnimation.frameDurationMs;
            int nextFrame = frameIndex + steps;
            if (currentAnimation.loop) {
                frameIndex = nextFrame % currentAnimation.frameCount;
            } else if (nextFrame >= currentAnimation.frameCount) {
                frameIndex = currentAnimation.frameCount - 1;
                playing = false;
                invalidate();
                OnAnimationFinishedListener listener = animationFinishedListener;
                if (listener != null) {
                    listener.onAnimationFinished(currentAnimation, generation);
                }
                return;
            } else {
                frameIndex = nextFrame;
            }
            invalidate();
        }
        scheduleFrameCallback();
    }

    private void scheduleFrameCallback() {
        if (!playing || callbackPosted || !isAttachedToWindow()
                || getWindowVisibility() != VISIBLE || getVisibility() != VISIBLE) {
            return;
        }
        callbackPosted = true;
        postOnAnimation(frameRunnable);
    }

    private void removeFrameCallback() {
        removeCallbacks(frameRunnable);
        callbackPosted = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (spriteSheet == null || spriteSheet.isRecycled() || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        int left = frameIndex * FRAME_WIDTH;
        int top = currentAnimation.row * FRAME_HEIGHT;
        sourceRect.set(left, top, left + FRAME_WIDTH, top + FRAME_HEIGHT);
        float scale = Math.min(getWidth() / (float) FRAME_WIDTH, getHeight() / (float) FRAME_HEIGHT);
        float drawWidth = FRAME_WIDTH * scale;
        float drawHeight = FRAME_HEIGHT * scale;
        float drawLeft = (getWidth() - drawWidth) / 2f;
        float drawTop = (getHeight() - drawHeight) / 2f;
        destinationRect.set(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawHeight);
        canvas.drawBitmap(spriteSheet, sourceRect, destinationRect, bitmapPaint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        lastFrameTimeMs = SystemClock.uptimeMillis();
        scheduleFrameCallback();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeFrameCallback();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            lastFrameTimeMs = SystemClock.uptimeMillis();
            scheduleFrameCallback();
        } else {
            removeFrameCallback();
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
