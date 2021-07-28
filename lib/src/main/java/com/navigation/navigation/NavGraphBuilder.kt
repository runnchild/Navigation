package com.navigation.navigation

import android.content.ComponentName
import android.content.Context
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.navigation.*
import androidx.navigation.fragment.DialogFragmentNavigator
import androidx.navigation.fragment.FragmentNavigator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * <p>
 * describe:
 *
 * </p>
 * @author qiurong
 * @date 2020/8/15
 */
object NavGraphBuilder {
    var tabDestinations: HashMap<String, Destination>? = null
    var otherDestinations: HashMap<String, Destination>? = null
    var startDestinationId = 0

    private val modulesDestination = hashMapOf<String, HashMap<String, Destination>>()

    fun build(
        context: FragmentActivity, controller: NavController, containerId: Int,
        intercept: ((NavGraph, NavDestination) -> Unit)? = null
    ) {
        val mutableMapOf = mutableMapOf<String, Destination>()
        if (tabDestinations == null) {
            tabDestinations = parseDestinationMap(context, "destination/tab")
        }
        if (otherDestinations == null) {
            otherDestinations = parseDestinationMap(context, "destination")
        }
        tabDestinations?.let {
            mutableMapOf.putAll(it)
        }
        otherDestinations?.let {
            mutableMapOf.putAll(it)
        }

        mutableMapOf.keys.forEach {
           val map = modulesDestination.getOrPut(it.toUri().pathSegments[0] ?: "") {
                hashMapOf(it to mutableMapOf[it]!!)
            }
            map[it] = mutableMapOf[it]!!
        }

        val rootGraph = NavGraph(NavGraphNavigator(controller.navigatorProvider))
        modulesDestination.keys.forEach {
            rootGraph.addDestination(
                build(context, controller, containerId, modulesDestination[it]!!, intercept).apply {
                    if (startDestinationId == 0) {
                        setStartDestination(
                            tabDestinations?.values?.firstOrNull()?.id
                                ?: otherDestinations?.values?.firstOrNull()?.id ?: 0
                        )
                    }
                }
            )
        }
        rootGraph.setStartDestination("http://navigation/one".destId())
        controller.graph = rootGraph
//        controller.graph = build(context, controller, containerId, mutableMapOf, intercept).apply {
//            if (startDestinationId == 0) {
//                setStartDestination(
//                    tabDestinations?.values?.firstOrNull()?.id
//                        ?: otherDestinations?.values?.firstOrNull()?.id ?: 0
//                )
//            }
//        }
    }

    private fun build(
        context: FragmentActivity, controller: NavController, containerId: Int,
        destinationMap: Map<String, Destination>, intercept: ((NavGraph, NavDestination) -> Unit)?
    ): NavGraph {
        val provider = controller.navigatorProvider
        val activityNavigator = provider.getNavigator(ActivityNavigator::class.java)
        val dialogNavigator = provider.getNavigator(DialogFragmentNavigator::class.java)
        val tabNavigator = provider.getNavigator(FragmentNavigator::class.java)

        val navGraph = NavGraph(NavGraphNavigator(provider))

        destinationMap.values.forEach {
            val destination = when (it.pageType) {
                PAGE_TYPE_FRAGMENT -> {
                    tabNavigator.createDestination().apply {
                        setClassName(it.className)

                        val anim = when (it.animStyle) {
                            ANIM_DEFAULT -> {
                                slideAnim
                            }
                            ANIM_POP -> {
                                popAnim
                            }
                            else -> {//ANIM_NON
                                nonAnim
                            }
                        }
                        putAction(it.id, NavAction(it.id, navOptions {
                            anim(anim)
                            if (it.isHomeTab) {
                                launchSingleTop = true
                            }
                        }))
                    }
                }
                PAGE_TYPE_DIALOG -> {
                    dialogNavigator.createDestination().apply {
                        setClassName(it.className)
                    }
                }
                else -> {
                    activityNavigator.createDestination().apply {
                        setComponentName(ComponentName(context, it.className))
                    }
                }
            }

            destination.label = it.title
            destination.id = it.id
            destination.addDeepLink(it.url)
            if (it.isStarter) {
                navGraph.setStartDestination(destination.id)
            }
            intercept?.invoke(navGraph, destination)
            startDestinationId = navGraph.startDestinationId
            navGraph.addDestination(destination)
        }
        navGraph.id = navGraph.startDestinationId
        return navGraph
    }

    private fun parseDestinationMap(
        context: FragmentActivity, path: String
    ): HashMap<String, Destination>? {
        val stringBuilder = StringBuilder()
        val list = context.assets.list(path)?.filter { it.endsWith(".json") }
        list?.forEachIndexed { index, it ->
            var parse = parse(context, "$path/$it")
            if (index > 0) {
                parse = parse.replaceFirst(Regex("[{]"), ",")
            }
            stringBuilder.append(parse)
            if (index < list.size - 1) {
                stringBuilder.delete(stringBuilder.lastIndexOf("}"), stringBuilder.length)
            }
        }
        return Gson().fromJson<HashMap<String, Destination>>(
            stringBuilder.toString(),
            object : TypeToken<HashMap<String, Destination>>() {}.type
        )
    }

    private fun parse(context: Context, assets: String): String {
        var inputStream: InputStream? = null
        var reader: BufferedReader? = null
        var sb: StringBuilder? = null
        try {
            inputStream = context.assets.open(assets)
            reader = BufferedReader(InputStreamReader(inputStream))

            sb = StringBuilder()
            var line: String
            while (reader.readLine().apply {
                    line = this
                } != null) {
                sb.append(line)
            }
        } catch (e: Exception) {
        } finally {
            inputStream?.close()
            reader?.close()
        }

        return sb.toString()
    }

    fun isTab(id: Int): Boolean {
        return tabDestinations?.values?.firstOrNull { it.id == id }?.isHomeTab ?: false
    }

    fun findCustomDestination(id: Int): Destination? {
        return tabDestinations?.values?.firstOrNull { id == it.id }
            ?: otherDestinations?.values?.firstOrNull { id == it.id }
    }

    fun findDestination(url: String): Destination? {
        return tabDestinations?.keys?.firstOrNull {
            it.startsWith(url)
        }?.let { tabDestinations!![it] } ?: otherDestinations?.keys?.firstOrNull {
            it.startsWith(url)
        }?.let { otherDestinations!![it] }
    }
}