package org.digma.intellij.plugin.posthog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SessionMetadataTests {

    //just test that SessionMetadata works, no need to test all keys


    @Test
    fun testInstallStatus() {

        val sessionMetadata = SessionMetadata()

        //null at startup
        val installStatus = sessionMetadata.getOrNull(getCurrentInstallStatusKey())
        assertNull(installStatus)

        //default from the key
        val defaultInstallStatus = sessionMetadata.get(getCurrentInstallStatusKey())
        assertEquals(defaultInstallStatus, InstallStatus.Active)

        //value from last put
        sessionMetadata.put(getCurrentInstallStatusKey(), InstallStatus.Disabled)
        val latestPutInstallStatus = sessionMetadata.get(getCurrentInstallStatusKey())
        assertEquals(latestPutInstallStatus, InstallStatus.Disabled)

        assertNotNull(sessionMetadata.getCreated(getCurrentInstallStatusKey()))
    }


    @Test
    fun testNull() {

        val sessionMetadata = SessionMetadata()

        //returns default value from key
        assertNotNull(sessionMetadata.get(SessionMetadataKey.create("nonExistent", false)))

        //no entry inserted, should be null
        assertNull(sessionMetadata.getOrNull(SessionMetadataKey.create("nonExistent", false)))
        assertNull(sessionMetadata.getCreated(SessionMetadataKey.create("nonExistent", false)))

    }

}