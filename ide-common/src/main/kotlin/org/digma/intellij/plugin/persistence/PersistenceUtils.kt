package org.digma.intellij.plugin.persistence

import org.digma.intellij.plugin.common.IDEUtilsService


/**
 * Set global flag that this user has already passed the installation wizard
 */
fun updateInstallationWizardFlag() {
    if (IDEUtilsService.isIdeaIDE()) {
        if (!PersistenceService.getInstance().isAlreadyPassedTheInstallationWizardForIdeaIDE()) {
            PersistenceService.getInstance().setAlreadyPassedTheInstallationWizardForIdeaIDE()
        }
    } else if (IDEUtilsService.isRiderIDE()) {
        if (!PersistenceService.getInstance().isAlreadyPassedTheInstallationWizardForRiderIDE()) {
            PersistenceService.getInstance().setAlreadyPassedTheInstallationWizardForRiderIDE()
        }
    } else if (IDEUtilsService.isPyCharmIDE()) {
        if (!PersistenceService.getInstance().isAlreadyPassedTheInstallationWizardForPyCharmIDE()) {
            PersistenceService.getInstance().setAlreadyPassedTheInstallationWizardForPyCharmIDE()
        }
    }
}
