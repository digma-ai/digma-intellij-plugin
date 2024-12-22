package org.digma.intellij.plugin.ui.wizard

import org.cef.browser.CefBrowser
import org.cef.handler.CefResourceHandler
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory

class InstallationWizardSchemeHandlerFactory : BaseSchemeHandlerFactory() {

    override fun createResourceHandler(browser: CefBrowser, resourcePath: String): CefResourceHandler {
        return InstallationWizardResourceHandler(browser, resourcePath)
    }

    override fun getSchema(): String {
        return INSTALLATION_WIZARD_SCHEMA
    }

    override fun getDomain(): String {
        return INSTALLATION_WIZARD_DOMAIN_NAME
    }
}