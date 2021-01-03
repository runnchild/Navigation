package com.navigation.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DialogDestination(
    val url: String,
    /**
     * 页面标题，可以使用在toolBar上
     */
    val title: String = "",
    val needLogin: Boolean = true,
    /**
     * url 注释， 如果不设置默认使用title
     */
    val doc: String = ""
)