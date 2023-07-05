package common.semanticversion

import com.glovoapp.versioning.PersistedProperties
import com.glovoapp.versioning.PersistedVersion
import com.glovoapp.versioning.SemanticVersion
import org.gradle.api.Project


/**
 * the plugin semantic-versioning.gradle.plugin doesn't work anymore with gradle 8.
 * com.glovoapp.semantic-versioning:com.glovoapp.semantic-versioning.gradle.plugin:1.1.10
 *
 * but it has a simple-to-use API for managing the version in version.properties.
 * the library is com.glovoapp.gradle:versioning:1.1.10
 *
 * these methods are a simple semantic version plugin.
 * currently not supporting tags and snapshots but can easily be extended.
 */

const val VERSION_PROPERTIES_FILE_NAME = "version.properties"
const val VERSION_PROPERTY_NAME = "version"

fun getSemanticVersion(project: Project): String {
    val persistedProperties = PersistedProperties(project.rootProject.file(VERSION_PROPERTIES_FILE_NAME))
    val persistedVersion = PersistedVersion(persistedProperties, VERSION_PROPERTY_NAME, SemanticVersion::parse)
    return persistedVersion.value.toString()
}

fun incrementSemanticVersionPatch(project: Project) {

    val persistedProperties = PersistedProperties(project.rootProject.file(VERSION_PROPERTIES_FILE_NAME))
    val persistedVersion = PersistedVersion(persistedProperties, VERSION_PROPERTY_NAME, SemanticVersion::parse)
    val incrementedValue = persistedVersion.value.plus(SemanticVersion.Increment.PATCH)
    persistedVersion.value = incrementedValue
}

fun incrementSemanticVersionMinor(project: Project) {

    val persistedProperties = PersistedProperties(project.rootProject.file(VERSION_PROPERTIES_FILE_NAME))
    val persistedVersion = PersistedVersion(persistedProperties, VERSION_PROPERTY_NAME, SemanticVersion::parse)
    val incrementedValue = persistedVersion.value.plus(SemanticVersion.Increment.MINOR)
    persistedVersion.value = incrementedValue
}

fun incrementSemanticVersionMajor(project: Project) {
    val persistedProperties = PersistedProperties(project.rootProject.file(VERSION_PROPERTIES_FILE_NAME))
    val persistedVersion = PersistedVersion(persistedProperties, VERSION_PROPERTY_NAME, SemanticVersion::parse)
    val incrementedValue = persistedVersion.value.plus(SemanticVersion.Increment.MAJOR)
    persistedVersion.value = incrementedValue
}

