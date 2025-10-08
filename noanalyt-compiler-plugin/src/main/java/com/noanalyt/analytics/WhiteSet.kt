package com.noanalyt.analytics

import java.io.File

interface WhiteSet {
    fun isWhite(
        projectPath: String,
        packageFqName: String,
        className: String,
        methodName: String,
        methodType: String,
    ): Boolean
}

class PilotWhiteSet : WhiteSet {
    override fun isWhite(
        projectPath: String,
        packageFqName: String,
        className: String,
        methodName: String,
        methodType: String
    ): Boolean {
        return true
    }
}

class ProductionWhiteSet : WhiteSet {
    class ProjectPathMap : HashMap<String, PackageFqNameMap>()
    class PackageFqNameMap : HashMap<String, ClassNameMap>()
    class ClassNameMap : HashMap<String, MethodNameMap>()
    class MethodNameMap : HashMap<String, MethodTypeMap>()
    class MethodTypeMap : HashMap<String, Boolean>()

    private val projectPathMap: ProjectPathMap = ProjectPathMap()

    fun prepare(configFile: File): WhiteSet {
        val lines = configFile.bufferedReader().lineSequence()
        lines.forEach { line ->
            val keys = line.split(";")
            if (keys.size != 5) {
                return@forEach
            }

            val (
                projectPath: String,
                packageFqName: String,
                className: String,
                methodName: String,
                methodType: String
            ) = keys

            projectPathMap[projectPath]?.let { packageFqNameMap ->
                packageFqNameMap[packageFqName]?.let { classNameMap ->
                    classNameMap[className]?.let { methodNameMap ->
                        methodNameMap[methodName]?.let { methodTypeMap ->
                            methodTypeMap[methodType] = true
                        } ?: run {
                            methodNameMap[methodName] = MethodTypeMap().apply {
                                put(methodType, true)
                            }
                        }
                    } ?: run {
                        classNameMap[className] = MethodNameMap().apply {
                            put(
                                methodName,
                                MethodTypeMap().apply {
                                    put(methodType, true)
                                }
                            )
                        }
                    }
                } ?: run {
                    packageFqNameMap[packageFqName] = ClassNameMap().apply {
                        put(
                            className,
                            MethodNameMap().apply {
                                put(
                                    methodName,
                                    MethodTypeMap().apply {
                                        put(methodType, true)
                                    }
                                )
                            }
                        )
                    }
                }
            } ?: run {
                projectPathMap[projectPath] = PackageFqNameMap().apply {
                    put(
                        packageFqName,
                        ClassNameMap().apply {
                            put(className, MethodNameMap().apply {
                                put(methodName, MethodTypeMap().apply {
                                    put(methodType, true)
                                })
                            })
                        }
                    )
                }
            }
        }

        return this
    }

    override fun isWhite(
        projectPath: String,
        packageFqName: String,
        className: String,
        methodName: String,
        methodType: String
    ): Boolean {
        return projectPathMap[projectPath]?.let { packageFqNameMap ->
            packageFqNameMap[packageFqName]?.let { classNameMap ->
                classNameMap[className]?.let { methodNameMap ->
                    methodNameMap[methodName]?.let { methodTypeMap ->
                        methodTypeMap[methodType]
                    }
                }
            }
        } ?: false
    }
}
