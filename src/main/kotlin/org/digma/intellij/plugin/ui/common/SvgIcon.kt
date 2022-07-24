package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.SVGLoader
import org.digma.intellij.plugin.log.Log
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import javax.swing.Icon
import javax.swing.ImageIcon

typealias ColorGetter = () -> Color

class SvgIcon constructor(val path: String, val getColor : ColorGetter) : Icon {

    companion object {
        private val cache: MutableMap<String, Icon> = HashMap()
        private val LOGGER = Logger.getInstance(SvgIcon::class.java)

        fun withColor(path: String, color: Color): SvgIcon{
            return SvgIcon(path) { color }
        }
    }


    override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
        getIcon().paintIcon(c,g,x,y)
    }

    override fun getIconWidth(): Int {
        return getIcon().iconWidth
    }

    override fun getIconHeight(): Int {
        return getIcon().iconHeight
    }

    private fun getIcon(): Icon {
        val color = getColor()
        val scale = JBUIScale.sysScale()
        val key = "$path:x$scale:${color.getHex()}"
        var icon = cache[key]
        if(icon == null){
            icon = loadSvg(path, color, scale)
            cache[key] = icon
        }
        return icon
    }

    private fun loadSvg(path: String, color: Color, scale: Float): Icon {
        try {
            Laf::class.java.getResourceAsStream(path).use { inputStream ->
                Objects.requireNonNull(inputStream)
                var text = String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                text = text.replace("currentColor".toRegex(), color.getHex())
                val coloredStream: InputStream = text.byteInputStream()
                return ImageIcon(SVGLoader.load(coloredStream, scale))
            }
        } catch (e: Exception) {
            Log.error(LOGGER, e, "Could not load svg", path)
            return IconLoader.getIcon(path, Laf::class.java.classLoader)
        }
    }
}