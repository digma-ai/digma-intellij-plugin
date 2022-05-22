package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.SlowSpanInfo
import org.digma.intellij.plugin.model.rest.insights.SlowestSpansInsight

fun slowestSpansPanel(insight: SlowestSpansInsight): DialogPanel {
    val result = panel {
        twoColumnsRow(
            column1 = {
                panel {
                    row {
                        label("Span Bottleneck").bold()
                    }
                    row {
                        label("The following spans are slowing request handling")
                    }
                }
            },
            column2 = {
                panel {
                    row {
                        icon(Icons.Insight.BOTTLENECK)
                    }
                    row {
                        label("Slow Spans")
                    }
                }
            }
        )

        insight.spans.forEach {
            row {
                label(it.spanInfo.displayName).bold()
            }
            row {
                label(descriptionOf(it))
            }
        }
    }

    return result
}


fun descriptionOf(span: SlowSpanInfo): String {
    if (span.p50.fraction > 0.4)
        return "50% of the users by up to ${span.p50.maxDuration.value}${span.p50.maxDuration.unit}";

    if (span.p95.fraction > 0.4)
        return "5% of the users by up to ${span.p95.maxDuration.value}${span.p95.maxDuration.unit}";

    return "1% of the users by up to ${span.p99.maxDuration.value}${span.p99.maxDuration.unit}";
}
