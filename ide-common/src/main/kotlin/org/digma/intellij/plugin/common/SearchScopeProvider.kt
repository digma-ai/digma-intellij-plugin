package org.digma.intellij.plugin.common

import com.intellij.psi.search.SearchScope
import java.util.function.Supplier

/**
 * used for lazy creation of SearchScope.
 * SearchScope must be created in ReadAccess, but sometimes we send a search scope for a method before
 * opening read access. in these cases send a SearchScopeProvider and call get only in read access.
 */
fun interface SearchScopeProvider : Supplier<SearchScope>