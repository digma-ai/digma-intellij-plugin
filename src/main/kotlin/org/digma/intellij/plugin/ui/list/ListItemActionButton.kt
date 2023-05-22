package org.digma.intellij.plugin.ui.list

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.scaled
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractButton
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.plaf.basic.BasicButtonUI
import kotlin.math.max


class ListItemActionButton(text: String) : AbstractListItemActionButton(text)

class ListItemActionIconButton(text: String,icon: Icon) : AbstractListItemActionButton(text,icon)

abstract class AbstractListItemActionButton: JButton {
		constructor(text: String): super(text)
		constructor(text: String,icon: Icon): super(text,icon)


	init {
		isOpaque = false
		background = getDefaultBgColor()
		isContentAreaFilled = false
		isBorderPainted = false
		border = JBUI.Borders.empty()

		addMouseListener(object : MouseAdapter() {
			override fun mouseEntered(e: MouseEvent?) {
				background = getEnteredBgColor()
			}

			override fun mouseExited(e: MouseEvent?) {
				background = Laf.Colors.PLUGIN_BACKGROUND
			}

			override fun mousePressed(e: MouseEvent?) {
				background = getPressedBgColor()
			}
			override fun mouseReleased(e: MouseEvent?) {
				background = Laf.Colors.PLUGIN_BACKGROUND
			}
		})
	}

	private fun getDefaultBgColor() = Laf.Colors.PLUGIN_BACKGROUND

	private fun getEnteredBgColor():Color
	{
		return if (JBColor.isBright())
			getDefaultBgColor().darker()
		else
			getDefaultBgColor().brighter()
	}

	private fun getPressedBgColor():Color
	{
		return if (JBColor.isBright())
			getDefaultBgColor().darker().darker()
		else
			getDefaultBgColor().darker()
	}

	override fun getPreferredSize(): Dimension {
		val original = super.getPreferredSize()
		var widthToSubtract = 15.scaled()
		var heightToSubtract = 5.scaled()
		return if (isBorderPainted && border != null) {
			val borderWidth = border.getBorderInsets(this).left + border.getBorderInsets(this).right
			val borderHeight = border.getBorderInsets(this).top + border.getBorderInsets(this).bottom
			widthToSubtract = widthToSubtract.plus(borderWidth)
			heightToSubtract = heightToSubtract.plus(borderHeight)
			var width = original.width - widthToSubtract
			var height = original.height - heightToSubtract

			icon?.let {
				width = max(width,(icon.iconWidth + borderWidth))
				height = max(height,(icon.iconHeight + borderHeight))
			}
			Dimension(width + 2.scaled(), height + 2.scaled())
		}else{
			Dimension(original.width - widthToSubtract, original.height - heightToSubtract)
		}
	}

	override fun paintComponent(g: Graphics) {
		val graphics = g as Graphics2D
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

		graphics.color = super.getBackground()
		val border = (super.getBorder()?: JBUI.Borders.empty()).getBorderInsets(this)
		graphics.fillRoundRect(border.left, border.top, width-border.right-border.left, height-border.bottom-border.top, 5, 5)
		super.paintComponent(g)
	}
}

internal class ListItemActionButton2 constructor(text: String): JButton(text) {
	init {
		isOpaque = false
		isBorderPainted = false
		border = JBUI.Borders.empty(1, 7,3,7)
	    setUI(ListItemActionButtonUI())
	}
}

class ListItemActionButtonUI : BasicButtonUI() {
	override fun paint(g: Graphics, c: JComponent) {
		val button = c as AbstractButton
		val model = button.model
		val size = button.size
		if(model.isRollover)
		{
			paintButtonRollover(g, size)
		}
		else if (!model.isPressed || !model.isArmed) {
			paintButtonUnpressed(g, size)
		}
		super.paint(g, button)
	}

	override fun paintButtonPressed(g: Graphics, b: AbstractButton) {
		paintButton(g, b.size, PRESSED_BACKGROUND())
	}

	private fun paintButtonUnpressed(g: Graphics, size: Dimension) {
		paintButton(g, size, REGULAR_BACKGROUND())
	}

	private fun paintButtonRollover(g: Graphics, size: Dimension) {
		paintButton(g, size, OVER_BACKGROUND())
	}

	private fun paintButton(g: Graphics, size: Dimension, bgColor: Color) {
		val g2d = createGraphics(g)
		try {
			g2d.color = bgColor
			g2d.fillRoundRect(0, 0, size.width - 1, size.height - 1, 5, 5)
		} finally {
			g2d.dispose()
		}
	}

	private fun createGraphics(g: Graphics): Graphics2D {
		val g2d = g.create() as Graphics2D
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
		return g2d
	}

	companion object {
		private fun REGULAR_BACKGROUND(): Color = Laf.Colors.PLUGIN_BACKGROUND
		private fun OVER_BACKGROUND(): Color = REGULAR_BACKGROUND().brighter()
		private fun PRESSED_BACKGROUND(): Color = REGULAR_BACKGROUND().darker()
	}
}