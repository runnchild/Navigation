package com.navigation.navigation

import android.net.Uri
import android.os.Bundle
import androidx.annotation.Keep
import androidx.core.net.toUri
import androidx.navigation.*
import androidx.navigation.Navigator
import com.rongc.navigation.R
import java.util.regex.Pattern
import kotlin.math.abs

/**
 * @description 作用描述
 * @author rongc
 * @date 20-8-28$
 * @update
 */
@Keep

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
    exit = 0//R.anim.pop_exit
    popEnter = 0//R.anim.pop_pop_enter
    popExit = R.anim.pop_pop_exit
}
val nonAnim: AnimBuilder.() -> Unit = {
    enter = 0
    exit = 0
    popEnter = 0
    popExit = 0
}

fun String.deepLink(vararg params: String?): Uri {
    val uri = this.toUri()
    return if (uri.queryParameterNames.size > 0) {
        val fillInPattern = Pattern.compile("\\{(.+?)\\}")
        val match = fillInPattern.matcher(this)
        val uriBuilder = uri.buildUpon().clearQuery()
        var index = 0
        while (match.find()) {
            uriBuilder.appendQueryParameter(match.group(1), params.getOrNull(index++))
        }
        uriBuilder.build()
    } else {
        uri
    }
}

fun NavController.navigateBy(
    url: String, args: Bundle? = null,
    options: NavOptions? = null, navExtras: Navigator.Extras? = null
) {
    val uri = url.toUri()
    var bundle = args
    if (uri.queryParameterNames.size > 0) {
        bundle = args ?: Bundle()
        uri.queryParameterNames.forEach {
            bundle.putString(it, uri.getQueryParameter(it))
        }
    }

    val destId = url.destId()
//    val newOption = mapAnimOption(options, destId)
    navigate(destId, bundle, options, navExtras)
}

private fun mapAnimOption(options: NavOptions?, destId: Int) = if (options == null) {
    navOptions {
        anim(
            when (NavGraphBuilder.findCustomDestination(destId)?.animStyle) {
                ANIM_DEFAULT -> slideAnim
                ANIM_POP -> popAnim
                else -> nonAnim
            }
        )
    }
} else {
    val anim = options.enterAnim + options.exitAnim + options.popEnterAnim + options.popExitAnim
    if (anim == 0) {
        navOptions {
            anim(slideAnim)
            launchSingleTop = options.shouldLaunchSingleTop()
            popUpTo = options.popUpTo
            popUpTo(options.popUpTo) {
                inclusive = options.isPopUpToInclusive
            }
        }
    } else {
        options
    }
}

fun NavController.navigateBy(
    uri: Uri, options: NavOptions? = null, navExtras: Navigator.Extras? = null
) {
    navigate(uri, options, navExtras)
}

fun <T> NavController?.observeCurrent(key: String, call: (T) -> Unit) {
    this?.currentBackStackEntry?.run {
        savedStateHandle.getLiveData<T>(key).observe(this) {
            call(it)
        }
    }
}

fun <T> NavController?.notifyPreBack(key: String, value: T) {
    this?.previousBackStackEntry?.run {
        savedStateHandle.set(key, value)
    }
}
