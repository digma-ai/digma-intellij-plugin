package org.digma.intellij.plugin.idea.navigation.model

import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.process.ProcessContext

class NavigationProcessContext(val searchScope: SearchScopeProvider, processName: String) :
    ProcessContext(processName)