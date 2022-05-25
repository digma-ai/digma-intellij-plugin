package org.digma.intellij.plugin.model.rest.errors

import com.fasterxml.jackson.annotation.JsonCreator
import java.beans.ConstructorProperties
import java.sql.Timestamp

data class CodeObjectError
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "uid",
    "name",
    "scoreInfo",
    "sourceCodeObjectId",
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
    val characteristic: String,
    val startsHere: Boolean,
    val endsHere: Boolean,
    val firstOccurenceTime: Timestamp,
    val lastOccurenceTime: Timestamp,
) {
}