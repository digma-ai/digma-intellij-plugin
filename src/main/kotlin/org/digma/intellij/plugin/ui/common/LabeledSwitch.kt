import org.digma.intellij.plugin.ui.common.Laf
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel

class LabeledSwitch(private val state1: String,
                    private val state2: String,
                    private val stateChangedListener: (Boolean) -> Unit
) : JPanel() {
    private var isOn: Boolean = true
    private val buttonActiveColor = Color.decode("#3538CD")
    private val buttonInactiveColor = Color.decode("#49494D")
    private val buttonBorderColor = Color.decode("#7a7b7e")
    private val buttonTextColor = Color.decode("#bbbbbb")
    private val buttonWidth = 88
    private val buttonHeight = 24
    private val cornerRadius = 4
    private val padding = Insets(2, 4, 2, 4)
    private val gapSize = 2
    private val componentPadding = 2

    init {
        background = Laf.Colors.TRANSPARENT
        isOpaque = false

        val totalWidth = (buttonWidth + gapSize) * 2 + gapSize * 2
        preferredSize = Dimension(totalWidth, buttonHeight + gapSize * 2 * componentPadding)

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                isOn = !isOn
                stateChangedListener(isOn)
                repaint()
            }
        })
    }

    override fun paintComponent(g: Graphics) {
        val g2d = g.create() as Graphics2D

        // Draw border for the toggle component
        g2d.color = buttonBorderColor
        g2d.drawRoundRect(componentPadding, componentPadding, width - 4, buttonHeight + 4, cornerRadius, cornerRadius)

        // Paint button 1
        g2d.color = if (isOn) buttonActiveColor else buttonInactiveColor
        g2d.fillRoundRect(padding.left + gapSize, componentPadding + padding.top + gapSize, buttonWidth - gapSize * 2, buttonHeight - gapSize * 2, cornerRadius, cornerRadius)

        // Paint button 2
        g2d.color = if (isOn) buttonInactiveColor else buttonActiveColor
        g2d.fillRoundRect( padding.left + buttonWidth + gapSize, componentPadding + padding.top + gapSize, buttonWidth - gapSize * 2, buttonHeight - gapSize * 2, cornerRadius, cornerRadius)

        // Paint labels
        //g2d.font = font
        g2d.color = buttonTextColor
        val fontMetrics = g2d.fontMetrics
        val labelOnWidth = fontMetrics.stringWidth(state1)
        val labelOffWidth = fontMetrics.stringWidth(state2)
        val labelHeight = fontMetrics.height
        val labelXOn = padding.left + (buttonWidth - labelOnWidth) / 2
        val labelXOff = padding.left + buttonWidth + gapSize + (buttonWidth - labelOffWidth) / 2
        val labelY = componentPadding - gapSize / 2 + (buttonHeight + labelHeight) / 2
        g2d.drawString(state1, labelXOn, labelY)
        g2d.drawString(state2, labelXOff, labelY)

        g2d.dispose()
    }
}