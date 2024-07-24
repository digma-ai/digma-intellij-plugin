package org.digma.intellij.plugin.auth

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.datetime.Clock
import org.digma.intellij.plugin.auth.credentials.DigmaCredentials
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals

class CredentialsTests {

    @Test
    fun testSerialize() {

        val objectManager = ObjectMapper()

        val digmaCredentials = DigmaCredentials(
            "userid",
            "myaccesstoken",
            "myrefreshtoken",
            "http://localhost5051",
            TokenType.Bearer.name,
            Date().time + 10000,
            Clock.System.now().toEpochMilliseconds()
        )

        val asJson = objectManager.writeValueAsString(digmaCredentials)

        val deserialized = objectManager.treeToValue(objectManager.readTree(asJson), DigmaCredentials::class.java)

        assertEquals(digmaCredentials.url, deserialized.url)
        assertEquals(digmaCredentials.tokenType, deserialized.tokenType)
        assertEquals(digmaCredentials.expirationTime, deserialized.expirationTime)
        assertEquals(digmaCredentials.accessToken, deserialized.accessToken)
        assertEquals(digmaCredentials.refreshToken, deserialized.refreshToken)
//        assertEquals(digmaCredentials.expiresIn, max(0, digmaCredentials.expirationTime - Clock.System.now().toEpochMilliseconds()))
    }

}