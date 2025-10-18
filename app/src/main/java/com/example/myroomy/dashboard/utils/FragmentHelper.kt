package com.example.myroomy.dashboard.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.myroomy.R

object FragmentHelper {

    fun replaceWithSmoothSlide(activity: AppCompatActivity, fragment: Fragment) {
        activity.supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.contenedor_main, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun replaceWithFade(activity: AppCompatActivity, fragment: Fragment) {
        activity.supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.animator.fade_in,
                android.R.animator.fade_out,
                android.R.animator.fade_in,
                android.R.animator.fade_out
            )
            .replace(R.id.contenedor_main, fragment)
            .addToBackStack(null)
            .commit()
    }
}
