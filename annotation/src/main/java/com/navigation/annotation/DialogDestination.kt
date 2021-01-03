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
    val isStarter: Boolean = false,
    val isHomeTab: Boolean = false,
    /**
     * url 注释， 如果不设置默认使用title
     */
    val doc: String = "",
    /**
     * 页面打开动画，默认slide， true 为pop
     */
    val popAnim:Boolean = false,
    /**
     * ANIMATE_NON, ANIMATE_DEFAULT, ANIMATE_POP
     */
    val animStyle:Int = 2
)