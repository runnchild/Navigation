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
import com.navigation.navigation.Navigator.navigate
import com.rongc.navigator.NavigationNavigator

/**
 * @description 作用描述
 * @author rongc
 * @date 20-8-28$
 * @update
 */
@FragmentDestination("navigation/one", isStarter = true)
class OneFragment : Fragment() {

//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return TextView(inflater.context).apply {
//            text = "one Fragment"
//            textSize = 30f
//            gravity = Gravity.CENTER
//            layoutParams = ViewGroup.LayoutParams(-1, -1)
//            setBackgroundColor(Color.GRAY)
//        }
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.login_phone_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.tv_btn).setOnClickListener {
            findNavController().navigate(NavigationNavigator.NAVIGATION_TWO)
        }
    }
}