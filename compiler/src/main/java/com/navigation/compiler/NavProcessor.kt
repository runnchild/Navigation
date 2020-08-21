package com.navigation.compiler

import com.google.auto.service.AutoService
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.navigation.annotation.ActivityDestination
import com.navigation.annotation.FragmentDestination
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
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
            FragmentDestination::class.java.canonicalName
        )
    }

    override fun process(set: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        messager?.printMessage(Diagnostic.Kind.NOTE, "start process")
        val activityElements = roundEnv?.getElementsAnnotatedWith(ActivityDestination::class.java)
        val fragmentElements = roundEnv?.getElementsAnnotatedWith(FragmentDestination::class.java)

        val destMap = mutableMapOf<String, JsonObject>()
        val tabDestMap = mutableMapOf<String, JsonObject>()
        handleActivityElements(activityElements, destMap)
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
        messager?.printMessage(Diagnostic.Kind.NOTE, "resourcePath=$assetsPath")

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
        val main = MethodSpec.methodBuilder("main")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(Void.TYPE)
            .addParameter(Array<String>::class.java, "args")
            .addStatement("\$T.out.println(\$S)", System::class.java, "Hello, JavaPoet!")
            .build()

        val helloWorld = TypeSpec.interfaceBuilder("${moduleName}Navigator")
            .addModifiers(Modifier.PUBLIC)
//            .addMethod(main)
            .apply {
                destMap.keys.forEach {
                    var toPath = ""
                    val split = it.substring(1, it.length).split("/")
                    val size = split.size
                    split.forEachIndexed { index, path ->
                        toPath += path.toUpperCase(Locale.getDefault()) + if (size > 1 && index < size - 1) {
                            "_"
                        } else ""
                    }
                    val field = FieldSpec.builder(
                        String::class.java,
                        toPath,
                        Modifier.PUBLIC,
                        Modifier.STATIC,
                        Modifier.FINAL
                    ).initializer(""""$it"""").build()

                    try {
                        addField(field)
                    } catch (e: Exception) {
                        println(e.message)
                    }
                }
            }
            .build()

        val javaFile = JavaFile.builder("com.psnlove.navigator", helloWorld)
            .build()
        javaFile.writeTo(System.out)
    }

    private fun writeToFile(
        assetsPath: String,
        destMap: MutableMap<String, JsonObject>
    ) {
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
                true,
                annotation.isHomeTab
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
                false,
                annotation.isHomeTab
            )
        }
    }

    private fun addObj(
        destMap: MutableMap<String, JsonObject>,
        clzName: String,
        url: String,
        needLogin: Boolean,
        isStarter: Boolean,
        isFragment: Boolean,
        isHomeTab: Boolean
    ): JsonObject? {
        return if (destMap.containsKey(url)) {
            messager?.printMessage(Diagnostic.Kind.ERROR, "不同的页面不允许添加相同的url: $url")
            null
        } else {
            val jsonObject = JsonObject()
            jsonObject.addProperty("id", abs(clzName.hashCode()))
            jsonObject.addProperty("className", clzName)
            jsonObject.addProperty("url", "http://$url")
            jsonObject.addProperty("needLogin", needLogin)
            jsonObject.addProperty("isStarter", isStarter)
            jsonObject.addProperty("isFragment", isFragment)
            jsonObject.addProperty("isHomeTab", isHomeTab)
            destMap[url] = jsonObject
            jsonObject
        }
    }
}