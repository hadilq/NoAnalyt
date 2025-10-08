package com.noanalyt.analytics

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private const val root = "com.noanalyt.runtime"
private const val ANALYTICS_LOG = "log"
internal val rootFqName = FqName(root)

private fun classIdFor(cname: String) =
    ClassId(rootFqName, Name.identifier(cname))

private fun topLevelCallableId(name: String) =
    CallableId(rootFqName, Name.identifier(name))

internal val log = topLevelCallableId(ANALYTICS_LOG)
