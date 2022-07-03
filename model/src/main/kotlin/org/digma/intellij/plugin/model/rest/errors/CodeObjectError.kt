package org.digma.intellij.plugin.model.rest.errors

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties
import java.sql.Timestamp

@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeObjectError
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "uid",
    "name",
    "scoreInfo",
    "sourceCodeObjectId",
    "codeObjectId",
    "characteristic",
    "startsHere",
    "endsHere",
    "firstOccurenceTime",
    "lastOccurenceTime",
)
constructor(
    val uid: String,
    val name: String,
    val scoreInfo: ScoreInfo,
    val sourceCodeObjectId: String,
    val codeObjectId: String,
    val characteristic: String,
    val startsHere: Boolean,
    val endsHere: Boolean,
    val firstOccurenceTime: Timestamp,
    val lastOccurenceTime: Timestamp,
) {
}