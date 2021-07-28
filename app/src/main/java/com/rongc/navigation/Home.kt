package com.rongc.navigation

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.navigation.annotation.FragmentDestination
import com.navigation.navigation.navigateBy
import com.rongc.navigator.NavigationAppNavigator

/**
 * <p>
 * describe:
 *
 * </p>
 * @author qiurong
 * @date 2021/7/28
 * @since 2.1.4
 */
@FragmentDestination("home/home", isStarter = true)
class Home: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return TextView(inflater.context).apply {
            text = "two Fragment"
            textSize = 30f
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(-1, -1)
            setBackgroundColor(Color.BLUE)
        }.apply {
            setOnClickListener {
                findNavController().navigateBy(NavigationAppNavigator.NAVIGATION_ONE)
            }
        }
    }
}