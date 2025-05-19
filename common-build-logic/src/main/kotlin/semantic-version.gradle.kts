tasks.register("printSemanticVersion") {
    notCompatibleWithConfigurationCache("printSemanticVersion is not yet compatible with configuration cache")
    doLast {
        println("${project.name} ${common.semanticversion.getSemanticVersion(project)}")
    }
}

tasks.register("incrementSemanticVersionPatch") {
    notCompatibleWithConfigurationCache("incrementSemanticVersionPatch is not yet compatible with configuration cache")
    doLast {
        common.semanticversion.incrementSemanticVersionPatch(project)
    }
}
tasks.register("incrementSemanticVersionMinor") {
    notCompatibleWithConfigurationCache("incrementSemanticVersionMinor is not yet compatible with configuration cache")
    doLast {
        common.semanticversion.incrementSemanticVersionMinor(project)
    }
}
tasks.register("incrementSemanticVersionMajor") {
    notCompatibleWithConfigurationCache("incrementSemanticVersionMajor is not yet compatible with configuration cache")
    doLast {
        common.semanticversion.incrementSemanticVersionMajor(project)
    }
}