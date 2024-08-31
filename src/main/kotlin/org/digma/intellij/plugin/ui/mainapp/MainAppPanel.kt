package org.digma.intellij.plugin.ui.mainapp

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.ex.fastCompareLines
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.reload.ReloadService
import org.digma.intellij.plugin.ui.insights.InsightsService
import org.digma.intellij.plugin.ui.jcef.DownloadHandlerAdapter
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.navigation.CodeButtonCaretContextService
import org.digma.intellij.plugin.ui.navigation.NavigationService
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import org.digma.intellij.plugin.ui.panels.ReloadablePanel
import org.digma.intellij.plugin.ui.tests.TestsUpdater
import java.awt.BorderLayout
import java.awt.GraphicsEnvironment
import java.awt.Insets
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.Timer


class MainAppPanel(private val project: Project) : DisposablePanel(), ReloadablePanel {

    private var jCefComponent: JCefComponent? = null

    private var parentDisposable = Disposer.newDisposable()

    private var graphicsChanged =false;

    private var screensNumber = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.size;


    init {
        jCefComponent = build()

        service<ReloadService>().register(this, MainAppService.getInstance(project))
        Disposer.register(MainAppService.getInstance(project)) {
            dispose()
        }

//        val timer = Timer(1000) {
//
//
//            if (graphicsChanged){
//                graphicsChanged= false;
//                System.gc();
//
//                val screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
//                GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
//                var newScreenNumber = screenDevices.size;
//                System.out.println("screens:"+ newScreenNumber);
//
//                if (screensNumber!=newScreenNumber) {
//                    screensNumber= newScreenNumber;
//
//                    try {
//                        System.out.println("reloading ui");
//                        service<ReloadService>().reload()
//                    } catch (e: Throwable) {
//                        ErrorReporter.getInstance().reportError("ReloadAction.actionPerformed", e)
//                    }
//                }
//
//
//
//            }
//        }
//        timer.start()
    }


    private fun build(): JCefComponent? {

        val jCefComponent = createJcefComponent()

        val jcefUiComponent: JComponent = jCefComponent?.getComponent() ?: JLabel("JCEF not supported")

        jcefUiComponent.addPropertyChangeListener("graphicsConfiguration",{ evt: PropertyChangeEvent ->

            System.out.println("Property changed:" + evt.propertyName + " old: " +  evt.oldValue + " new: " + evt.newValue)

            if (evt.propertyName=="graphicsConfiguration"  && evt.oldValue!=null && evt.newValue!=null) {

                System.out.println("graphicsConfiguration changed:" + " old: " + evt.oldValue + " new: " + evt.newValue)

                CoroutineScope(Dispatchers.Main).launch {
                    if (graphicsChanged) {
                        graphicsChanged = false;
                        System.gc();
                        Thread.sleep(1500);


                        val screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
                        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
                        var newScreenNumber = screenDevices.size;
                        System.out.println("screens:" + newScreenNumber);

                        if (screensNumber != newScreenNumber) {
                            screensNumber = newScreenNumber;

                            try {
                                System.out.println("reloading ui");
                                service<ReloadService>().reload()
                            } catch (e: Throwable) {
                                ErrorReporter.getInstance().reportError("ReloadAction.actionPerformed", e)
                            }
                        }

                    }

                    graphicsChanged = true;

                }
            }});
        //jcefUiComponent.addPropertyChangeListener("painting", PropertyChangeListener )
        layout = BorderLayout()
        border = JBUI.Borders.empty()
        add(jcefUiComponent, BorderLayout.CENTER)
        background = listBackground()

        jCefComponent?.let {
            MainAppService.getInstance(project).setJCefComponent(it)
            TestsUpdater.getInstance(project).setJCefComponent(it)
            InsightsService.getInstance(project).setJCefComponent(it)
            NavigationService.getInstance(project).setJCefComponent(it)
            CodeButtonCaretContextService.getInstance(project).setJCefComponent(it)
        }

        return jCefComponent
    }


    override fun reload() {
        dispose()
        removeAll()
        parentDisposable = Disposer.newDisposable()
        jCefComponent = build()
    }


    private fun createJcefComponent(): JCefComponent? {
        return if (JBCefApp.isSupported()) {
            JCefComponent.JCefComponentBuilder(
                project, "Main", parentDisposable,
                MAIN_APP_URL,
                MainAppMessageRouterHandler(project)
            )
                .withDownloadAdapter(DownloadHandlerAdapter())
                .build()
        } else {
            null
        }
    }


    override fun getInsets(): Insets {
        return JBUI.emptyInsets()
    }

    override fun dispose() {
        Disposer.dispose(parentDisposable)
        jCefComponent?.let {
            Disposer.dispose(it)
        }
    }
}