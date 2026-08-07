package com.credenceid.sdkapp

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.net.URLDecoder
import java.util.jar.JarFile

/**
 * This module declares "minSdk 24", but the "java.time" package was only added to Android in API
 * 26. A reference to it from application code compiles and passes Lint, yet resolves to nothing on
 * an API 24/25 device, so the first instruction which touches it throws NoClassDefFoundError.
 *
 * Annotating a method with "@RequiresApi" does not help when that method is a listener the
 * CredenceSDK invokes, since nothing checks Build.VERSION.SDK_INT before the callback runs.
 *
 * This test scans the compiled application classes so a re-introduction is caught by the build
 * instead of by a device in the field.
 */
class MinSdkApiLevelTest {

    @Test
    fun applicationClassesDoNotReferenceJavaTime() {
        val offenders = compiledApplicationClasses()
            .filter { (_, bytecode) -> bytecode.referencesType(JAVA_TIME) }
            .keys
            .sorted()

        assertEquals(
            "Classes referencing \"$JAVA_TIME\", which requires API 26, above a minSdk of 24",
            emptyList<String>(),
            offenders
        )
    }

    /**
     * Reads every compiled class of this application, keyed by class file path. Depending on the
     * variant, Gradle hands unit tests either a directory of classes or a jar holding them.
     *
     * @return Map of class file path to that class file's bytecode.
     */
    private fun compiledApplicationClasses(): Map<String, ByteArray> {
        val marker = javaClass.classLoader?.getResource(MARKER_CLASS)
            ?: error("Unable to locate compiled application classes, looked for $MARKER_CLASS.")

        if ("jar" != marker.protocol) {
            val packageDirectory = File(marker.toURI()).parentFile
                ?: error("Unable to locate compiled application classes, looked for $MARKER_CLASS.")
            return packageDirectory.walkTopDown()
                .filter { it.isFile && CLASS_EXTENSION == it.extension }
                .associate { PACKAGE_PATH + it.name to it.readBytes() }
        }

        val jarPath = URLDecoder.decode(marker.path.substringBefore("!").removePrefix("file:"), "UTF-8")
        JarFile(File(jarPath)).use { jar ->
            return jar.entries().asSequence()
                .filter { it.name.startsWith(PACKAGE_PATH) && it.name.endsWith(".$CLASS_EXTENSION") }
                .associate { it.name to jar.getInputStream(it).readBytes() }
        }
    }

    /**
     * Class file constant pools store type names as UTF-8, so a byte-preserving decode is enough to
     * find a reference without pulling in a bytecode parsing library.
     *
     * @return True if this bytecode names the given type, false otherwise.
     */
    private fun ByteArray.referencesType(type: String): Boolean =
        String(this, Charsets.ISO_8859_1).contains(type)

    companion object {
        private const val PACKAGE_PATH = "com/credenceid/sdkapp/"
        private const val MARKER_CLASS = PACKAGE_PATH + "App.class"
        private const val CLASS_EXTENSION = "class"
        private const val JAVA_TIME = "java/time/"
    }
}
