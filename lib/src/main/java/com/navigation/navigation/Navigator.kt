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
 * @description 从当前页面弹出栈到目标页面
 * @param url 页面url
 * @param inclusive 弹出是否包含url所在的页面
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

    val destination = NavGraphBuilder.findDestination("${uri.host}${uri.path}")
    val newOption = mapAnimOption(options, destination)
    navigate(destination?.id ?: url.destId(), bundle, newOption, navExtras)
}

private fun mapAnimOption(options: NavOptions?, destination: Destination?) = if (options == null) {
    navOptions {
        anim(
            when (destination?.animStyle) {
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
    val url = "${uri.host}${uri.path}"
    val destination = NavGraphBuilder.findDestination(url)
    val newOptions = mapAnimOption(options, destination)
    navigate(uri, newOptions, navExtras)
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