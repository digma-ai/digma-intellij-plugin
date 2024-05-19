tasks.register("printSemanticVersion") {
    doLast {
        println("${project.name} ${common.semanticversion.getSemanticVersion(project)}")
    }
}

tasks.register("incrementSemanticVersionPatch") {
    doLast {
        common.semanticversion.incrementSemanticVersionPatch(project)
    }
}
tasks.register("incrementSemanticVersionMinor") {
    doLast {
        common.semanticversion.incrementSemanticVersionMinor(project)
    }
}
tasks.register("incrementSemanticVersionMajor") {
    doLast {
        common.semanticversion.incrementSemanticVersionMajor(project)
    }
}