package org.digma.intellij.plugin.common

import com.intellij.openapi.Disposable

/**
 * an adaptor for Disposable interface. can be inherited by classes that need to be Disposables
 * but have nothing to do in the dispose method
 */
interface DisposableAdaptor : Disposable {

    override fun dispose() {
        //nothing to do.
    }
}