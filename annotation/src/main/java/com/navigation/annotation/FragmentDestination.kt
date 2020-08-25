package com.navigation.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FragmentDestination(
    val url: String,
    val needLogin: Boolean = false,
    val isStarter: Boolean = false,
    val isHomeTab: Boolean = false,
    val doc: String = ""
)