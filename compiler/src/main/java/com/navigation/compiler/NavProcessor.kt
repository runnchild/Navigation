package com.navigation.compiler

import com.google.auto.service.AutoService
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.navigation.annotation.ActivityDestination
import com.navigation.annotation.DialogDestination
import com.navigation.annotation.FragmentDestination
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.FileObject
import javax.tools.StandardLocation
import kotlin.math.abs

const val PAGE_TYPE_FRAGMENT = "fragment"
const val PAGE_TYPE_ACTIVITY = "activity"
const val PAGE_TYPE_DIALOG = "dialog"

@Suppress("unused")
@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class NavProcessor : AbstractProcessor() {
    private var messager: Messager? = null
    private var filer: Filer? = null
    private var moduleName: String? = null
    private var projectName: String? = null

    private val outPutName by lazy {
        "destination_${moduleName}_$projectName.json"
    }

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        messager = processingEnv?.messager
        filer = processingEnv?.filer
        moduleName = processingEnv?.options?.get("MODULE_NAME")
        projectName = processingEnv?.options?.get("PROJECT_NAME")
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            ActivityDestination::class.java.canonicalName,
            DialogDestination::class.java.canonicalName,
            FragmentDestination::class.java.canonicalName
        )
    }

    override fun process(set: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        messager?.printMessage(Diagnostic.Kind.NOTE, "start process")
        val activityElements = roundEnv?.getElementsAnnotatedWith(ActivityDestination::class.java)
        val fragmentElements = roundEnv?.getElementsAnnotatedWith(FragmentDestination::class.java)
        val dialogElements = roundEnv?.getElementsAnnotatedWith(DialogDestination::class.java)

        val destMap = mutableMapOf<String, JsonObject>()
        val tabDestMap = mutableMapOf<String, JsonObject>()
        handleActivityElements(activityElements, destMap)
        handleDialogElements(dialogElements, destMap)
        handleFragmentElements(fragmentElements, destMap, tabDestMap)
        val resource: FileObject?
        try {
            resource = filer?.createResource(
                StandardLocation.CLASS_OUTPUT, "",
                outPutName
            )
        } catch (e: Exception) {
            return false
        }
        val resourcePath = resource?.toUri()?.path
        //resourcePath=/D:/StudioProjects/Sign/Home/lib/build/tmp/kapt3/classes/debug/destination
        val appPath = resourcePath?.substring(0, resourcePath.indexOf("build"))
        val assetsPath = appPath.plus("src/main/assets/destination/")
        messager?.printMessage(Diagnostic.Kind.NOTE, "resourcePath=$resourcePath")

        if (destMap.isNotEmpty()) {
            writeToFile(assetsPath, destMap)
        }
        if (tabDestMap.isNotEmpty()) {
            writeToFile("${assetsPath}/tab", tabDestMap)
        }
        makeType(destMap, tabDestMap)
        return false
    }

    private fun makeType(
        destMap: MutableMap<String, JsonObject>,
        tabDestMap: MutableMap<String, JsonObject>
    ) {
        var moduleName = moduleName
        moduleName =
            moduleName!!.first().toUpperCase().plus(moduleName.substring(1, moduleName.length))
        println("module Name == $moduleName")
        val clazName = if (projectName == "app") {
            "${moduleName}AppNavigator"
        } else {
            "${moduleName}Navigator"
        }
        val helloWorld = TypeSpec.interfaceBuilder(clazName)
            .addModifiers(Modifier.PUBLIC)
//            .addMethod(main)
            .apply {
                generateType(destMap)
                generateType(tabDestMap)
            }
            .build()

        val javaFile = JavaFile.builder("com.rongc.navigator", helloWorld)
            .build()

        val path = filer?.createResource(
            StandardLocation.SOURCE_OUTPUT, "",
            "java"
        )?.toUri()?.path!!
//        println("path === ${path}")
        javaFile.writeTo(File(path.substring(0, path.indexOf("java"))))
    }

    private fun TypeSpec.Builder.generateType(destMap: MutableMap<String, JsonObject>) {
        destMap.keys.forEach {
            var toPath = ""
            val split = it.split("/")//community/user_argue_list -> COMMUNITY_USER_ARGUE_LIST?USER_ID=
            val size = split.size
            split.forEachIndexed { index, path ->
                toPath += path.toUpperCase(Locale.getDefault()) + if (size > 1 && index < size - 1) {
                    "_"
                } else ""
            }
            // 如果没有设置doc，去title作为注释
            val doc =
                destMap[it]?.getAsJsonPrimitive("doc")?.asString ?: destMap[it]?.getAsJsonPrimitive(
                    "title"
                )?.asString
            if (toPath.contains("?")) {
                toPath = toPath.substring(0, toPath.indexOf("?"))
            }
            println("toPath=$toPath")

            val field = FieldSpec.builder(
                String::class.java,
                toPath,
                Modifier.PUBLIC,
                Modifier.STATIC,
                Modifier.FINAL
            ).initializer("\"http://$it\"")
                .addJavadoc(doc)
                .build()

            try {
                addField(field)
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    private fun writeToFile(assetsPath: String, destMap: MutableMap<String, JsonObject>) {
        val outputDir = File(assetsPath)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outFile = File(
            outputDir,
            outPutName
        )
        if (outFile.exists()) {
            outFile.delete()
        }
        outFile.createNewFile()

        var fos: FileOutputStream? = null
        var outWriter: OutputStreamWriter? = null
        try {
            fos = FileOutputStream(outFile)
            outWriter = OutputStreamWriter(fos)
            outWriter.write(Gson().toJson(destMap))
            outWriter.flush()
        } catch (e: Exception) {
        } finally {
            try {
                fos?.close()
            } catch (e: Exception) {
            } finally {
                try {
                    outWriter?.close()
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun handleFragmentElements(
        fragmentElements: MutableSet<out Element>?,
        destMap: MutableMap<String, JsonObject>,
        tabDestMap: MutableMap<String, JsonObject>
    ) {
        fragmentElements?.forEach {
            it as TypeElement
            val annotation = it.getAnnotation(FragmentDestination::class.java)

            addObj(
                destMap,
                it.qualifiedName.toString(),
                annotation.url,
                annotation.needLogin,
                annotation.isStarter,
                PAGE_TYPE_FRAGMENT,
                annotation.isHomeTab,
                annotation.doc,
                annotation.title,
                annotation.animStyle
            )?.let { jsonObj ->
                if (annotation.isHomeTab) {
                    tabDestMap[annotation.url] = jsonObj
                    destMap.remove(annotation.url)
                }
            }
        }
    }

    private fun handleActivityElements(
        activityElements: MutableSet<out Element>?, destMap: MutableMap<String, JsonObject>
    ) {
        activityElements?.forEach {
            it as TypeElement
            val annotation = it.getAnnotation(ActivityDestination::class.java)

            addObj(
                destMap,
                it.qualifiedName.toString(),
                annotation.url,
                annotation.needLogin,
                annotation.isStarter,
                PAGE_TYPE_ACTIVITY,
                annotation.isHomeTab,
                annotation.doc,
                annotation.title,
                annotation.animStyle
            )
        }
    }

    private fun handleDialogElements(
        activityElements: MutableSet<out Element>?, destMap: MutableMap<String, JsonObject>
    ) {
        activityElements?.forEach {
            it as TypeElement
            val annotation = it.getAnnotation(DialogDestination::class.java)

            addObj(
                destMap,
                it.qualifiedName.toString(),
                annotation.url,
                annotation.needLogin,
                false,
                PAGE_TYPE_DIALOG,
                false,
                annotation.doc,
                annotation.title,
                0
            )
        }
    }

    private fun addObj(
        destMap: MutableMap<String, JsonObject>,
        clzName: String,
        url: String,
        needLogin: Boolean,
        isStarter: Boolean,
        pageType: String,
        isHomeTab: Boolean,
        doc: String,
        title: String,
        animStyle: Int
    ): JsonObject? {
        return if (destMap.containsKey(url)) {
            messager?.printMessage(Diagnostic.Kind.ERROR, "不同的页面不允许添加相同的url: $url")
            null
        } else {
            val jsonObject = JsonObject()
            val uri = "http://$url"
            jsonObject.addProperty("id", abs(uri.hashCode()))
            jsonObject.addProperty("className", clzName)
            jsonObject.addProperty("url", uri)
            jsonObject.addProperty("title", title)
            jsonObject.addProperty("needLogin", needLogin)
            jsonObject.addProperty("isStarter", isStarter)
            jsonObject.addProperty("pageType", pageType)
            jsonObject.addProperty("isHomeTab", isHomeTab)
            jsonObject.addProperty("doc", doc)
            jsonObject.addProperty("animStyle", animStyle)
            destMap[url] = jsonObject
            jsonObject
        }
    }
}