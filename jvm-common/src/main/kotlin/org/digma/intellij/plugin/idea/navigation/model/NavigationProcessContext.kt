package org.digma.intellij.plugin.idea.navigation.model

import com.intellij.openapi.progress.ProgressIndicator
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.progress.ProcessContext

class NavigationProcessContext(val searchScope: SearchScopeProvider, indicator: ProgressIndicator) : ProcessContext(indicator)