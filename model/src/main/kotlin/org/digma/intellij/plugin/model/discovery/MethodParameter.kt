package org.digma.intellij.plugin.model.discovery

data class MethodParameter(
    val typeFqn: String,
    val name: String
) {

    companion object {
        private val arrayRegex = Regex("\\[,*]$")

        private fun getArraysPart(typeFqn: String): String {
            var arraysValue = ""
            var trimmableValue = typeFqn

            var arrayMatchResult = arrayRegex.find(trimmableValue, 0)
            while (arrayMatchResult != null) {
                arraysValue = arrayMatchResult.value + arraysValue
                trimmableValue = trimmableValue.substring(0, arrayMatchResult.range.start);

                arrayMatchResult = arrayRegex.find(trimmableValue, 0)
            }
            return arraysValue
        }
    }

    // should meet the name of parameter as used in stack trace
    fun typeShortName(): String {
        val arraysValuePart = getArraysPart(typeFqn);

        val firstIndexOfSquaredParenthesisOpening = typeFqn.indexOf('[')
        val relevantTypeFqn =
            if (firstIndexOfSquaredParenthesisOpening < 0) {
                typeFqn
            } else {
                // has SquaredParenthesisOpening - generic or array, never mind just get the first part
                typeFqn.substring(0, firstIndexOfSquaredParenthesisOpening)
            }
        val shortNameBeforeArray = relevantTypeFqn.split('.').last()

        return "$shortNameBeforeArray$arraysValuePart"
    }

}
