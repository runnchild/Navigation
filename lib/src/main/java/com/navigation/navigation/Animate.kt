package com.navigation.navigation

import androidx.navigation.AnimBuilder
import com.rongc.navigation.R

const val ANIM_NON = -1
const val ANIM_DEFAULT = 0
const val ANIM_POP = 1

const val PAGE_TYPE_FRAGMENT = "fragment"
const val PAGE_TYPE_ACTIVITY = "activity"
const val PAGE_TYPE_DIALOG = "dialog"

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

val nonAnim: AnimBuilder.() -> Unit = {
    enter = 0
    exit = 0
    popEnter = 0
    popExit = 0
}
