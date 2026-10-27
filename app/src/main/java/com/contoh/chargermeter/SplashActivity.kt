
package com.contoh.chargermeter

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class SplashActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private val lanjut = Runnable {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        root.setBackgroundColor(Warna.hijauTua)

        // Cahaya yang berdenyut di belakang logo
        val cahaya = View(this)
        val radial = GradientDrawable()
        radial.setShape(GradientDrawable.OVAL)
        radial.setGradientType(GradientDrawable.RADIAL_GRADIENT)
        radial.setGradientRadius(dpF(200f))
        radial.setColors(intArrayOf(Color.parseColor("#66FFB300"), Color.parseColor("#00FFB300")))
        cahaya.background = radial

        // Logo dan tulisan di bawahnya
        val kolom = LinearLayout(this)
        kolom.orientation = LinearLayout.VERTICAL
        kolom.gravity = Gravity.CENTER_HORIZONTAL

        val logo = ImageView(this)
        logo.setImageResource(R.drawable.logo_charger)
        logo.scaleType = ImageView.ScaleType.FIT_CENTER
        logo.alpha = 0f
        logo.scaleX = 0.6f
        logo.scaleY = 0.6f
        kolom.addView(logo, LinearLayout.LayoutParams(dp(250), dp(250)))

        val tulisan = TextView(this)
        tulisan.text = "Charger Meter by Harinfood"
        tulisan.textSize = 17f
        tulisan.setTextColor(Color.WHITE)
        tulisan.typeface = android.graphics.Typeface.DEFAULT_BOLD
        tulisan.letterSpacing = 0.04f
        tulisan.gravity = Gravity.CENTER
        tulisan.alpha = 0f
        tulisan.translationY = dpF(12f)
        kolom.addView(tulisan, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(6)
        })

        root.addView(cahaya, FrameLayout.LayoutParams(dp(380), dp(380), Gravity.CENTER))
        root.addView(kolom, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        setContentView(root)

        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(900L)
            .setInterpolator(OvershootInterpolator())
            .start()

        // Tulisan muncul setelah logo, naik sambil memudar
        tulisan.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(700L)
            .setDuration(600L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        ObjectAnimator.ofFloat(cahaya, "scaleX", 0.9f, 1.1f).apply {
            setDuration(900L)
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }
        ObjectAnimator.ofFloat(cahaya, "scaleY", 0.9f, 1.1f).apply {
            setDuration(900L)
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }

        handler.postDelayed(lanjut, 2800L)
    }

    override fun onDestroy() {
        handler.removeCallbacks(lanjut)
        super.onDestroy()
    }
}
