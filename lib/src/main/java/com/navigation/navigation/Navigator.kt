package com.navigation.navigation

import android.os.Bundle
import androidx.navigation.AnimBuilder
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import com.rongc.navigation.R
import kotlin.math.abs

/**
 * @description 作用描述
 * @author rongc
 * @date 20-8-28$
 * @update
 */
object Navigator {

    fun NavController.navigate(url: String, args: Bundle? = null, options: NavOptions? = null) {
        navigate(url.destId(), args, options)
    }

    /**
     * @description 从当前页面弹出栈到目标页面
     * @param inclusive 弹出是否包含给的页面
     * @param url 页面url
     * @author rongc
     * @date 20-8-27
     */
    fun NavController.popBackTo(url: String, inclusive: Boolean = false) {
        popBackStack(url.destId(), inclusive)
    }


    fun NavOptionsBuilder.popUpTo(url: String, inclusive: Boolean = false) {
        popUpTo(url.destId()) {
            this.inclusive = inclusive
        }
    }

    fun String.destId() = abs(hashCode())

    val slideAnim: AnimBuilder.() -> Unit = {
        enter = R.anim.slide_enter
        exit = R.anim.slide_exit
        popEnter = R.anim.slide_pop_enter
        popExit = R.anim.slide_pop_exit
    }

    val popAnim: AnimBuilder.() -> Unit = {
        enter = R.anim.pop_enter
        exit = R.anim.pop_exit
        popEnter = R.anim.pop_pop_enter
        popExit = R.anim.pop_pop_exit
    }
}
