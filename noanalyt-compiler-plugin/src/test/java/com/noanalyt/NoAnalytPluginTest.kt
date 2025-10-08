package com.noanalyt

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStreamReader
import java.io.PrintStream

@OptIn(ExperimentalCompilerApi::class)
class NoAnalytPluginTest {

    @Rule
    @JvmField
    var temporaryFolder: TemporaryFolder = TemporaryFolder()

    val analyticsClass = SourceFile.kotlin(
        "Analytics.kt",
        """
            package com.noanalyt.runtime
            fun log(key: String) {
                println(key)
            }
        """.trimIndent()
    )

    val configFile: File = temporaryFolder.apply {
        create()
    }.newFile("config-file").apply {
        writeText(
            """
            """.trimIndent()
        )
    }

    @Test
    fun `generate simple result`() {
        val result = compile(
            SourceFile.kotlin(
                "SimpleClass.kt",
                """package com.example.analytics.compiler.test
                   fun main(args: Array<String>?) {
                       println("test called")
                   }
                   """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val output =
            runFiles(result.classLoader, "com.example.analytics.compiler.test.SimpleClassKt")
        assertThat(output.trimIndent()).isEqualTo(
            """
            0.0.test;<main>;com.example.analytics.compiler.test;;main;(kotlin.Array)->kotlin.Unit
            test called
            """.trimIndent()
        )
    }

    @Test
    fun `generate simple bytecode`() {
        val result = compile(
            SourceFile.kotlin(
                "SimpleClass.kt",
                """package com.example.analytics.compiler.test
                   fun main(args: Array<String>?) {
                       println("test called")
                   }
                   """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val bytecode = fileBytecode(
            result.generatedFiles
                .first { it.exists() && it.isFile && it.name == "SimpleClassKt.class" }
        )

        assertThat(bytecode.trimIndent()).isEqualTo(
            """
            Compiled from "SimpleClass.kt"
            public final class com.example.analytics.compiler.test.SimpleClassKt {
              public static final void main(java.lang.String[]);
                Code:
                   0: ldc           #9                  // String 0.0.test;<main>;com.example.analytics.compiler.test;;main;(kotlin.Array)->kotlin.Unit
                   2: invokestatic  #15                 // Method com/noanalyt/runtime/AnalyticsKt.log:(Ljava/lang/String;)V
                   5: ldc           #17                 // String test called
                   7: getstatic     #23                 // Field java/lang/System.out:Ljava/io/PrintStream;
                  10: swap
                  11: invokevirtual #29                 // Method java/io/PrintStream.println:(Ljava/lang/Object;)V
                  14: return
            }
            """.trimIndent()
        )
    }

    @Test
    fun `generate another private function result`() {
        val result = compile(
            SourceFile.kotlin(
                "SimpleClass.kt",
                """package com.example.analytics.compiler.test
                   fun main(args: Array<String>?) {
                       checkThis()
                   }

                   private fun checkThis() {
                       println("test called")
                   }
                   """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val output =
            runFiles(result.classLoader, "com.example.analytics.compiler.test.SimpleClassKt")
        assertThat(output.trimIndent()).isEqualTo(
            """
            0.0.test;<main>;com.example.analytics.compiler.test;;main;(kotlin.Array)->kotlin.Unit
            0.0.test;<main>;com.example.analytics.compiler.test;;checkThis;()->kotlin.Unit
            test called
            """.trimIndent()
        )
    }

    @Test
    fun `generate another private function bytecode`() {
        val result = compile(
            SourceFile.kotlin(
                "SimpleClass.kt",
                """package com.example.analytics.compiler.test
                   fun main(args: Array<String>?) {
                       checkThis()
                   }

                   private fun checkThis() {
                       println("test called")
                   }
                   """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val bytecode = fileBytecode(
            result.generatedFiles
                .first { it.exists() && it.isFile && it.name == "SimpleClassKt.class" }
        )

        assertThat(bytecode.trimIndent()).isEqualTo(
            """
            Compiled from "SimpleClass.kt"
            public final class com.example.analytics.compiler.test.SimpleClassKt {
              public static final void main(java.lang.String[]);
                Code:
                   0: ldc           #9                  // String 0.0.test;<main>;com.example.analytics.compiler.test;;main;(kotlin.Array)->kotlin.Unit
                   2: invokestatic  #15                 // Method com/noanalyt/runtime/AnalyticsKt.log:(Ljava/lang/String;)V
                   5: invokestatic  #19                 // Method checkThis:()V
                   8: return
            }
            """.trimIndent()
        )
    }

    @Test
    fun `generate method in a class result`() {
        val result = compile(
            SourceFile.kotlin(
                "SimpleClass.kt",
                """package com.example.analytics.compiler.test
                   fun main(args: Array<String>?) {
                       val e = Example()
                       e.checkThis()
                   }

                   class Example {
                       fun checkThis() {
                           println("test called")
                       }
                   }
                   """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val output =
            runFiles(result.classLoader, "com.example.analytics.compiler.test.SimpleClassKt")
        assertThat(output.trimIndent()).isEqualTo(
            """
            0.0.test;<main>;com.example.analytics.compiler.test;;main;(kotlin.Array)->kotlin.Unit
            0.0.test;<main>;com.example.analytics.compiler.test;Example;<init0;()->com.example.analytics.compiler.test.Example
            0.0.test;<main>;com.example.analytics.compiler.test;Example;checkThis;()->kotlin.Unit
            test called
            """.trimIndent()
        )
    }

    @Test
    fun `generate method in a class bytecode`() {
        val result = compile(
            SourceFile.kotlin(
                "SimpleClass.kt",
                """
                   package com.example.analytics.compiler.test
                   fun main(args: Array<String>?) {
                       val e = Example()
                       e.checkThis()
                   }

                   class Example {
                       fun checkThis() {
                           println("test called")
                       }
                   }
               """.trimIndent()
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val bytecode = fileBytecode(
            result.generatedFiles
                .first { it.exists() && it.isFile && it.name == "SimpleClassKt.class" }
        )

        assertThat(bytecode.trimIndent()).isEqualTo(
            """
            Compiled from "SimpleClass.kt"
            public final class com.example.analytics.compiler.test.SimpleClassKt {
              public static final void main(java.lang.String[]);
                Code:
                   0: ldc           #9                  // String 0.0.test;<main>;com.example.analytics.compiler.test;;main;(kotlin.Array)->kotlin.Unit
                   2: invokestatic  #15                 // Method com/noanalyt/runtime/AnalyticsKt.log:(Ljava/lang/String;)V
                   5: new           #17                 // class com/example/analytics/compiler/test/Example
                   8: dup
                   9: invokespecial #21                 // Method com/example/analytics/compiler/test/Example."<init>":()V
                  12: astore_1
                  13: aload_1
                  14: invokevirtual #24                 // Method com/example/analytics/compiler/test/Example.checkThis:()V
                  17: return
            }
            """.trimIndent()
        )
    }


    @Test
    fun `generate method in a class with multiple constructor result`() {
        val result = compile(
            SourceFile.kotlin(
                "SimpleClass.kt",
                """package com.example.analytics.compiler.test
                   fun main(args: Array<String>?) {
                       val e = Example()
                       e.checkThis()
                   }

                   class Example(val needed: String) {

                       constructor(): this("not needed")

                       fun checkThis() {
                           println("test called")
                       }
                   }
                   """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val output =
            runFiles(result.classLoader, "com.example.analytics.compiler.test.SimpleClassKt")
        assertThat(output.trimIndent()).isEqualTo(
            """
            0.0.test;<main>;com.example.analytics.compiler.test;;main;(kotlin.Array)->kotlin.Unit
            0.0.test;<main>;com.example.analytics.compiler.test;Example;<init1;()->com.example.analytics.compiler.test.Example
            0.0.test;<main>;com.example.analytics.compiler.test;Example;<init0;(kotlin.String)->com.example.analytics.compiler.test.Example
            0.0.test;<main>;com.example.analytics.compiler.test;Example;checkThis;()->kotlin.Unit
            test called
            """.trimIndent()
        )
    }

    @Test
    fun `generate method in a class with multiple constructor bytecode`() {
        val result = compile(
            SourceFile.kotlin(
                "SimpleClass.kt",
                """
                   package com.example.analytics.compiler.test
                   fun main(args: Array<String>?) {
                       val e = Example()
                       e.checkThis()
                   }

                   class Example(val needed: String) {

                       constructor(): this("not needed")

                       fun checkThis() {
                           println("test called")
                       }
                   }
               """.trimIndent()
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val bytecode = fileBytecode(
            result.generatedFiles
                .first { it.exists() && it.isFile && it.name == "SimpleClassKt.class" }
        )

        assertThat(bytecode.trimIndent()).isEqualTo(
            """
            Compiled from "SimpleClass.kt"
            public final class com.example.analytics.compiler.test.SimpleClassKt {
              public static final void main(java.lang.String[]);
                Code:
                   0: ldc           #9                  // String 0.0.test;<main>;com.example.analytics.compiler.test;;main;(kotlin.Array)->kotlin.Unit
                   2: invokestatic  #15                 // Method com/noanalyt/runtime/AnalyticsKt.log:(Ljava/lang/String;)V
                   5: new           #17                 // class com/example/analytics/compiler/test/Example
                   8: dup
                   9: invokespecial #21                 // Method com/example/analytics/compiler/test/Example."<init>":()V
                  12: astore_1
                  13: aload_1
                  14: invokevirtual #24                 // Method com/example/analytics/compiler/test/Example.checkThis:()V
                  17: return
            }
            """.trimIndent()
        )
    }

    @Test
    fun `generate method in an interface result`() {
        val result = compile(
            SourceFile.kotlin(
                "SimpleClass.kt",
                """package com.example.analytics.compiler.test
                   fun main(args: Array<String>?) {
                       val e: Example = ExampleImpl()
                       e.checkThis()
                   }

                   interface Example {
                       fun checkThis()
                   }

                   class ExampleImpl : Example {
                       override fun checkThis() {
                           println("test called")
                       }
                   }
                   """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val output =
            runFiles(result.classLoader, "com.example.analytics.compiler.test.SimpleClassKt")
        assertThat(output.trimIndent()).isEqualTo(
            """
            0.0.test;<main>;com.example.analytics.compiler.test;;main;(kotlin.Array)->kotlin.Unit
            0.0.test;<main>;com.example.analytics.compiler.test;ExampleImpl;<init0;()->com.example.analytics.compiler.test.ExampleImpl
            0.0.test;<main>;com.example.analytics.compiler.test;ExampleImpl;checkThis;()->kotlin.Unit
            test called
            """.trimIndent()
        )
    }

    @Test
    fun `generate method in an interface bytecode`() {
        val result = compile(
            SourceFile.kotlin(
                "SimpleClass.kt",
                """
                   package com.example.analytics.compiler.test
                   fun main(args: Array<String>?) {
                       val e: Example = ExampleImpl()
                       e.checkThis()
                   }

                   interface Example {
                       fun checkThis()
                   }

                   class ExampleImpl : Example {
                       override fun checkThis() {
                           println("test called")
                       }
                   }
               """.trimIndent()
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val bytecode = fileBytecode(
            result.generatedFiles
                .first { it.exists() && it.isFile && it.name == "SimpleClassKt.class" }
        )

        assertThat(bytecode.trimIndent()).isEqualTo(
            """
            Compiled from "SimpleClass.kt"
            public final class com.example.analytics.compiler.test.SimpleClassKt {
              public static final void main(java.lang.String[]);
                Code:
                   0: ldc           #9                  // String 0.0.test;<main>;com.example.analytics.compiler.test;;main;(kotlin.Array)->kotlin.Unit
                   2: invokestatic  #15                 // Method com/noanalyt/runtime/AnalyticsKt.log:(Ljava/lang/String;)V
                   5: new           #17                 // class com/example/analytics/compiler/test/ExampleImpl
                   8: dup
                   9: invokespecial #21                 // Method com/example/analytics/compiler/test/ExampleImpl."<init>":()V
                  12: checkcast     #23                 // class com/example/analytics/compiler/test/Example
                  15: astore_1
                  16: aload_1
                  17: invokeinterface #26,  1           // InterfaceMethod com/example/analytics/compiler/test/Example.checkThis:()V
                  22: return
            }
            """.trimIndent()
        )
    }

    @Test
    fun `generate anonymous method result`() {
        val result = compile(
            SourceFile.kotlin(
                "SimpleClass.kt",
                """package com.example.analytics.compiler.test
                   fun main(args: Array<String>?) {
                       val e = { input: String -> println(input) }
                       e("test called")
                   }
                   """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val output =
            runFiles(result.classLoader, "com.example.analytics.compiler.test.SimpleClassKt")
        assertThat(output.trimIndent()).isEqualTo(
            """
            0.0.test;<main>;com.example.analytics.compiler.test;;main;(kotlin.Array)->kotlin.Unit
            0.0.test;<main>;com.example.analytics.compiler.test;;<anonymous0;(kotlin.String)->kotlin.Unit
            test called
            """.trimIndent()
        )
    }

    @Test
    fun `generate anonymous method bytecode`() {
        val result = compile(
            SourceFile.kotlin(
                "SimpleClass.kt",
                """
                   package com.example.analytics.compiler.test
                   fun main(args: Array<String>?) {
                       val e = { input: String -> println(input) }
                       e("test called")
                   }
               """.trimIndent()
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val bytecode = fileBytecode(
            result.generatedFiles
                .first { it.exists() && it.isFile && it.name == "SimpleClassKt.class" }
        )

        assertThat(bytecode.trimIndent()).isEqualTo(
            """
            Compiled from "SimpleClass.kt"
            public final class com.example.analytics.compiler.test.SimpleClassKt {
              public static final void main(java.lang.String[]);
                Code:
                   0: ldc           #9                  // String 0.0.test;<main>;com.example.analytics.compiler.test;;main;(kotlin.Array)->kotlin.Unit
                   2: invokestatic  #15                 // Method com/noanalyt/runtime/AnalyticsKt.log:(Ljava/lang/String;)V
                   5: invokedynamic #34,  0             // InvokeDynamic #0:invoke:()Lkotlin/jvm/functions/Function1;
                  10: astore_1
                  11: aload_1
                  12: ldc           #36                 // String test called
                  14: invokeinterface #40,  2           // InterfaceMethod kotlin/jvm/functions/Function1.invoke:(Ljava/lang/Object;)Ljava/lang/Object;
                  19: pop
                  20: return
            }
            """.trimIndent()
        )
    }

    @Test
    fun `generate multiple anonymous methods result`() {
        val result = compile(
            SourceFile.kotlin(
                "SimpleClass.kt",
                """package com.example.analytics.compiler.test
                   fun main(args: Array<String>?) {
                       val e = { input: String -> println(input) }
                       val v = { input: String -> input }

                       e(v("test called"))
                   }
                   """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val output =
            runFiles(result.classLoader, "com.example.analytics.compiler.test.SimpleClassKt")
        assertThat(output.trimIndent()).isEqualTo(
            """
            0.0.test;<main>;com.example.analytics.compiler.test;;main;(kotlin.Array)->kotlin.Unit
            0.0.test;<main>;com.example.analytics.compiler.test;;<anonymous1;(kotlin.String)->kotlin.String
            0.0.test;<main>;com.example.analytics.compiler.test;;<anonymous0;(kotlin.String)->kotlin.Unit
            test called
            """.trimIndent()
        )
    }

    @Test
    fun `generate multiple anonymous methods bytecode`() {
        val result = compile(
            SourceFile.kotlin(
                "SimpleClass.kt",
                """
                   package com.example.analytics.compiler.test
                   fun main(args: Array<String>?) {
                       val e = { input: String -> println(input) }
                       val v = { input: String -> input }

                       e(v("test called"))
                   }
               """.trimIndent()
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val bytecode = fileBytecode(
            result.generatedFiles
                .first { it.exists() && it.isFile && it.name == "SimpleClassKt.class" }
        )

        assertThat(bytecode.trimIndent()).isEqualTo(
            """
            Compiled from "SimpleClass.kt"
            public final class com.example.analytics.compiler.test.SimpleClassKt {
              public static final void main(java.lang.String[]);
                Code:
                   0: ldc           #9                  // String 0.0.test;<main>;com.example.analytics.compiler.test;;main;(kotlin.Array)->kotlin.Unit
                   2: invokestatic  #15                 // Method com/noanalyt/runtime/AnalyticsKt.log:(Ljava/lang/String;)V
                   5: invokedynamic #34,  0             // InvokeDynamic #0:invoke:()Lkotlin/jvm/functions/Function1;
                  10: astore_1
                  11: invokedynamic #41,  0             // InvokeDynamic #1:invoke:()Lkotlin/jvm/functions/Function1;
                  16: astore_2
                  17: aload_1
                  18: aload_2
                  19: ldc           #43                 // String test called
                  21: invokeinterface #47,  2           // InterfaceMethod kotlin/jvm/functions/Function1.invoke:(Ljava/lang/Object;)Ljava/lang/Object;
                  26: invokeinterface #47,  2           // InterfaceMethod kotlin/jvm/functions/Function1.invoke:(Ljava/lang/Object;)Ljava/lang/Object;
                  31: pop
                  32: return
            }
            """.trimIndent()
        )
    }

    private fun prepareCompilation(
        configFile: File,
        vararg sourceFiles: SourceFile
    ): KotlinCompilation {
        return KotlinCompilation()
            .apply {
                workingDir = temporaryFolder.root
                compilerPluginRegistrars = listOf(
                    NoAnalytPluginRegistrar(
                        "0.0.test", "<main>",
                        BuildConfig.pilot,
                        configFile,
                    )
                )
                inheritClassPath = true
                sources = sourceFiles.asList() + listOf(analyticsClass)
                verbose = false
                jvmTarget = JvmTarget.JVM_11.description
                includeRuntime = true
            }
    }

    private fun compile(vararg sourceFiles: SourceFile): JvmCompilationResult {
        return prepareCompilation(configFile, *sourceFiles).compile()
    }

    private fun runFiles(classLoader: ClassLoader, mainClass: String): String {
        val clazz = classLoader.loadClass(mainClass)
        val m = clazz.methods.first { it.name == "main" }

        val original = System.out
        val stream = ByteArrayOutputStream()
        val current = PrintStream(stream)
        System.setOut(current)
        // Run command
        m.invoke(null, arrayOf<String>())
        System.setOut(original)
        return stream.toString()
    }

    private fun fileBytecode(file: File) = runCommand("javap -c $file")

    private fun runCommand(command: String): String {
        val p: Process = Runtime.getRuntime().exec(command)
        val input = BufferedReader(InputStreamReader(p.inputStream))
        var line: String?
        val sb = StringBuilder()
        while (run { line = input.readLine();line } != null) {
            sb.appendLine(line)
        }
        input.close()
        val error = BufferedReader(InputStreamReader(p.errorStream))
        while (run { line = error.readLine();line } != null) {
            println("Error of running: $command -> $line")
        }
        error.close()
        return sb.toString()
    }
}
