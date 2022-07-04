package org.digma.intellij.plugin.model.discovery

data class MethodParameter(
    val typeFqn: String,
    val name: String
) {

    companion object {
        private val arrayRegex = Regex("\\[,*]$")
    }

    // should meet the name of parameter as used in stack trace
    fun typeShortName(): String {
        val arrayMatchResult = arrayRegex.find(typeFqn, 0)

        val arrayPart: String
        if (arrayMatchResult != null) {
            arrayPart = arrayMatchResult.value
        } else {
            arrayPart = ""
        }

        val firstIndexOfSquaredParenthesisOpening = typeFqn.indexOf('[')
        val relevantTypeFqn =
            if (firstIndexOfSquaredParenthesisOpening < 0) {
                typeFqn
            } else {
                // has SquaredParenthesisOpening - generic or array, never mind just get the first part
                typeFqn.substring(0, firstIndexOfSquaredParenthesisOpening)
            }
        val shortNameBeforeArray = relevantTypeFqn.split('.').last()

        return "$shortNameBeforeArray$arrayPart"
    }

}
