package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefMessageRouter
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil
import org.digma.intellij.plugin.ui.settings.ApplicationUISettingsChangeNotifier
import org.digma.intellij.plugin.ui.settings.SettingsChangeListener
import org.digma.intellij.plugin.ui.settings.Theme
import java.util.Objects
import javax.swing.JComponent

class JCefComponent(
    val project: Project,
    val jbCefBrowser: JBCefBrowser,
    val cefMessageRouter: CefMessageRouter,
    val messageRouterHandler: BaseMessageRouterHandler?,
    val schemeHandlerFactory: BaseSchemeHandlerFactory?,
    val lifeSpanHandler: CefLifeSpanHandlerAdapter,
) : Disposable {


    init {
        ApplicationUISettingsChangeNotifier.getInstance(project).addSettingsChangeListener(object : SettingsChangeListener {
            override fun systemFontChange(fontName: String) {
                JCefBrowserUtil.sendRequestToChangeFont(fontName, jbCefBrowser)
            }

            override fun systemThemeChange(theme: Theme) {
                JCefBrowserUtil.sendRequestToChangeUiTheme(theme, jbCefBrowser)
            }

            override fun editorFontChange(fontName: String) {
                JCefBrowserUtil.sendRequestToChangeCodeFont(fontName, jbCefBrowser)
            }
        })
    }


    override fun dispose() {
        jbCefBrowser.dispose()
        cefMessageRouter.dispose()
        jbCefBrowser.jbCefClient.removeLifeSpanHandler(lifeSpanHandler, jbCefBrowser.cefBrowser)
    }

    fun getComponent(): JComponent {
        return jbCefBrowser.component
    }
}


class JCefComponentBuilder(val project: Project) {

    private var url: String? = null
    private var messageRouterHandler: BaseMessageRouterHandler? = null
    private var schemeHandlerFactory: BaseSchemeHandlerFactory? = null
    private var parentDisposable: Disposable? = null


    fun build(): JCefComponent {

        Objects.requireNonNull(url, "url must not be null")
        Objects.requireNonNull(messageRouterHandler, "messageRouterHandler must not be null")
        Objects.requireNonNull(schemeHandlerFactory, "schemeHandlerFactory must not be null")

        //todo: move the code from JBCefBrowserBuilderCreator to here when all apps use the same infrastructure
        val jbCefBrowser = JBCefBrowserBuilderCreator.create()
            .setUrl(url)
            .build()

        val jbCefClient = jbCefBrowser.jbCefClient
        val cefMessageRouter = CefMessageRouter.create()
        cefMessageRouter.addHandler(messageRouterHandler, true)
        jbCefClient.cefClient.addMessageRouter(cefMessageRouter)

        val lifeSpanHandler: CefLifeSpanHandlerAdapter = object : CefLifeSpanHandlerAdapter() {
            override fun onAfterCreated(browser: CefBrowser) {
                registerAppSchemeHandler(schemeHandlerFactory)
            }
        }

        jbCefBrowser.jbCefClient.addLifeSpanHandler(lifeSpanHandler, jbCefBrowser.cefBrowser)

        parentDisposable?.let {
            Disposer.register(it) {
                jbCefBrowser.jbCefClient.removeLifeSpanHandler(lifeSpanHandler, jbCefBrowser.cefBrowser)
            }
        }

        return JCefComponent(project, jbCefBrowser, cefMessageRouter, messageRouterHandler, schemeHandlerFactory, lifeSpanHandler)
    }


    private fun registerAppSchemeHandler(schemeHandlerFactory: BaseSchemeHandlerFactory?) {

        Objects.requireNonNull(schemeHandlerFactory, "schemeHandlerFactory must not be null")

        CefApp.getInstance().registerSchemeHandlerFactory(
//            "http", DOMAIN_NAME, AssetsSchemeHandlerFactory(project)
            schemeHandlerFactory!!.getSchema(), schemeHandlerFactory.getDomain(), schemeHandlerFactory
        )
    }


    fun url(url: String): JCefComponentBuilder {
        this.url = url
        return this
    }

    fun messageRouterHandler(messageRouterHandler: BaseMessageRouterHandler): JCefComponentBuilder {
        this.messageRouterHandler = messageRouterHandler
        return this
    }

    fun schemeHandlerFactory(schemeHandlerFactory: BaseSchemeHandlerFactory): JCefComponentBuilder {
        this.schemeHandlerFactory = schemeHandlerFactory
        return this
    }

    fun withParentDisposable(parentDisposable: Disposable): JCefComponentBuilder {
        this.parentDisposable = parentDisposable
        return this
    }

}