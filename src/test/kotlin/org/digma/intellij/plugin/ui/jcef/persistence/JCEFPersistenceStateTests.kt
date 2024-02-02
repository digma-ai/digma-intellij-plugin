package org.digma.intellij.plugin.ui.jcef.persistence

import com.intellij.util.xmlb.XmlSerializer
import org.digma.intellij.plugin.ui.jcef.createObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class JCEFPersistenceStateTests {


    @Test
    fun testSerialize() {

        val objectMapper = createObjectMapper()
        val json = objectMapper.createObjectNode()
        json.put("myFilter", "myValue")

        val state = JCEFPersistenceState()
        state.state[PersistenceKey("mykey")] = PersistenceValue(json)

        val serialized = XmlSerializer.serialize(state)

        val deserialized = XmlSerializer.deserialize(serialized, JCEFPersistenceState::class.java)

        assertEquals(state, deserialized)


    }

}