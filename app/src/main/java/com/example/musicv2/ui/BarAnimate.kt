package com.example.musicv2.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import com.example.musicv2.R
import kotlinx.android.synthetic.main.view_equalizer.view.*

class BarAnimate : LinearLayout {
    var playingSet: AnimatorSet? = null
    var stopSet: AnimatorSet? = null
    var isAnimating = false
    var duration = 0

    constructor(context: Context?) : super(context) { initViews() }

    constructor(context: Context, attrs: AttributeSet?): super(context, attrs) {
        setAttrs(context, attrs)
        initViews()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        setAttrs(context, attrs)
        initViews()
    }

    private fun setAttrs(context: Context, attrs: AttributeSet?) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.EqualizerView,0,0)
        try {
            duration = a.getInt(R.styleable.EqualizerView_animDuration, 3000)
        } finally {
            a.recycle()
        }
    }

    private fun initViews() {
        LayoutInflater.from(context).inflate(R.layout.view_equalizer, this, true)
        setPivots()
    }

    private fun setPivots() {
        music_bar1!!.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (music_bar1!!.height > 0) {
                    music_bar1!!.pivotY = music_bar1!!.height.toFloat()
                    if (Build.VERSION.SDK_INT >= 16) {
                        music_bar1!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            }
        })
        music_bar2!!.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (music_bar2!!.height > 0) {
                    music_bar2!!.pivotY = music_bar2!!.height.toFloat()
                    if (Build.VERSION.SDK_INT >= 16) {
                        music_bar2!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            }
        })
        music_bar3!!.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (music_bar3!!.height > 0) {
                    music_bar3!!.pivotY = music_bar3!!.height.toFloat()
                    if (Build.VERSION.SDK_INT >= 16) {
                        music_bar3!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            }
        })
    }

    fun animateBars() {
        isAnimating = true
        if (playingSet == null) {
            val scaleYbar1 = ObjectAnimator.ofFloat(music_bar1, "scaleY", 0.2f, 0.8f, 0.1f, 0.1f, 0.3f, 0.1f, 0.2f, 0.8f, 0.7f, 0.2f, 0.4f, 0.9f, 0.7f, 0.6f, 0.1f, 0.3f, 0.1f, 0.4f, 0.1f, 0.8f, 0.7f, 0.9f, 0.5f, 0.6f, 0.3f, 0.1f)
            scaleYbar1.repeatCount = ValueAnimator.INFINITE
            val scaleYbar2 = ObjectAnimator.ofFloat(music_bar2,"scaleY", 0.2f, 0.5f, 1.0f, 0.5f, 0.3f, 0.1f, 0.2f, 0.3f, 0.5f, 0.1f, 0.6f, 0.5f, 0.3f, 0.7f, 0.8f, 0.9f, 0.3f, 0.1f, 0.5f, 0.3f, 0.6f, 1.0f, 0.6f, 0.7f, 0.4f, 0.1f)
            scaleYbar2.repeatCount = ValueAnimator.INFINITE
            val scaleYbar3 = ObjectAnimator.ofFloat(music_bar3, "scaleY", 0.6f, 0.5f, 1.0f, 0.6f, 0.5f, 1.0f, 0.6f, 0.5f, 1.0f, 0.5f, 0.6f, 0.7f, 0.2f, 0.3f, 0.1f, 0.5f, 0.4f, 0.6f, 0.7f, 0.1f, 0.4f, 0.3f, 0.1f, 0.4f, 0.3f, 0.7f)
            scaleYbar3.repeatCount = ValueAnimator.INFINITE
            playingSet = AnimatorSet()
            playingSet!!.playTogether(scaleYbar2, scaleYbar3, scaleYbar1)
            playingSet!!.duration = duration.toLong()
            playingSet!!.interpolator = LinearInterpolator()
            playingSet!!.start()
        } else if (Build.VERSION.SDK_INT < 19) {
            if (!playingSet!!.isStarted) {
                playingSet!!.start()
            }
        } else {
            if (playingSet!!.isPaused) {
                playingSet!!.resume()
            }
        }
    }

    fun stopBars() {
        isAnimating = false
        if (playingSet != null && playingSet!!.isRunning && playingSet!!.isStarted) {
            if (Build.VERSION.SDK_INT < 19) { playingSet!!.end() }
            else { playingSet!!.pause() }
        }
        if (stopSet == null) {
            // Animate stopping bars
            val scaleY1 = ObjectAnimator.ofFloat(music_bar1, "scaleY", 0.1f)
            val scaleY2 = ObjectAnimator.ofFloat(music_bar2, "scaleY", 0.1f)
            val scaleY3 = ObjectAnimator.ofFloat(music_bar3, "scaleY", 0.1f)
            stopSet = AnimatorSet()
            stopSet!!.playTogether(scaleY3, scaleY2, scaleY1)
            stopSet!!.duration = 200
            stopSet!!.start()
        } else if (!stopSet!!.isStarted) stopSet!!.start()
    }

}