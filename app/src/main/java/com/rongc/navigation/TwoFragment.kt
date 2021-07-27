package com.rongc.navigation

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.navigation.annotation.FragmentDestination
import com.navigation.navigation.ANIM_POP
import com.navigation.navigation.navigateBy
import com.rongc.navigator.NavigationAppNavigator

/**
 * @description 作用描述
 * @author rongc
 * @date 20-8-28$
 * @update
 */
@FragmentDestination("navigation/two", animStyle = ANIM_POP)
class TwoFragment: Fragment() {
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