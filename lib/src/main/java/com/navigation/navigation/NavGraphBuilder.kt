package com.navigation.navigation

import android.content.ComponentName
import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.navigation.*
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

    fun build(
        context: FragmentActivity,
        controller: NavController,
        containerId: Int,
        intercept: ((NavGraph, Destination) -> Unit)? = null
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
        controller.graph = build(context, controller, containerId, mutableMapOf, intercept).apply {
            if (startDestination == 0) {
                startDestination = tabDestinations?.values?.firstOrNull()?.id
                    ?: otherDestinations?.values?.firstOrNull()?.id ?: 0
            }
        }
    }

    fun buildTab(
        context: FragmentActivity,
        controller: NavController,
        containerId: Int,
        intercept: ((NavGraph, Destination) -> Unit)? = null
    ) {
        if (tabDestinations == null) {
            tabDestinations = parseDestinationMap(context, "destination/tab")
        }
        controller.graph =
            build(context, controller, containerId, tabDestinations ?: return, intercept)
    }

    fun buildOther(
        context: FragmentActivity,
        controller: NavController,
        containerId: Int,
        intercept: ((NavGraph, Destination) -> Unit)? = null
    ) {
        if (otherDestinations == null) {
            otherDestinations = parseDestinationMap(context, "destination")
        }

        val graph = build(context, controller, containerId, otherDestinations ?: return, intercept)

        try {
            controller.graph
        } catch (e: IllegalStateException) {
            controller.graph = graph
        }
    }

    private fun build(
        context: FragmentActivity,
        controller: NavController,
        containerId: Int,
        destinationMap: Map<String, Destination>,
        intercept: ((NavGraph, Destination) -> Unit)?
    ): NavGraph {
        val provider = controller.navigatorProvider
        val activityNavigator = provider.getNavigator(ActivityNavigator::class.java)
        val fragmentNavigator =
            FixFragmentNavigator(context, context.supportFragmentManager, containerId)
        provider.addNavigator(fragmentNavigator)

        val navGraph = try {
            controller.graph
        } catch (e: IllegalStateException) {
            NavGraph(NavGraphNavigator(provider))
        }

        destinationMap.values.forEach {
            val destination: NavDestination = if (it.isFragment) {
                fragmentNavigator.createDestination().apply {
                    className = it.className
                }
            } else {
                activityNavigator.createDestination().apply {
                    setComponentName(ComponentName(context, it.className))
                }
            }
            if (it.popAnim) {
                destination.putAction(it.id, NavAction(it.id, navOptions {
                    anim(Navigator.popAnim)
                }))
            }
            destination.label = it.title
            destination.id = it.id
            destination.addDeepLink(it.url)
            if (it.isStarter) {
                navGraph.startDestination = destination.id
            }
            intercept?.invoke(navGraph, it)
            navGraph.addDestination(destination)
        }
        return navGraph
    }

    private fun parseDestinationMap(
        context: FragmentActivity,
        path: String
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
}