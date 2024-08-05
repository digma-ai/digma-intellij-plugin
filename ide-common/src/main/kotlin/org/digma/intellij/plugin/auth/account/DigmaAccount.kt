@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.auth.account

import com.intellij.collaboration.api.ServerPath
import com.intellij.collaboration.auth.ServerAccount
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag
import java.net.URI


const val DEFAULT_ACCOUNT_NAME = "default"
const val DEFAULT_LOGIN_ID = "unknown"

@Tag("account")
class DigmaAccount(
    @NlsSafe
    @Attribute("id")
    override val id: String = generateId(),
    @NlsSafe
    @Attribute("name")
    override val name: String = DEFAULT_ACCOUNT_NAME,
    @Property(style = Property.Style.ATTRIBUTE, surroundWithTag = false)
    override val server: MyServerPath = MyServerPath(),
    @Attribute("userId")
    val userId: String = DEFAULT_LOGIN_ID,
    //createdBy used for information and debugging, never used for comparison or things like that. nullable for backward compatibility.
    @Attribute("createdBy")
    val createdBy: String? = null,
    //creationDate used for information and debugging, never used for comparison or things like that. nullable for backward compatibility.
    @Attribute("creationDate")
    val creationDate: String? = null
) : ServerAccount() {

    override fun toString(): String {
        return "DigmaAccount(id=$id, name=$name, url=${server.url}), user id=$userId)"
    }
}


@Tag("server")
class MyServerPath(@NlsSafe @Attribute("url") var url: String = "") : ServerPath {

    override fun toString(): String {
        return url
    }

    override fun toURI(): URI {
        return URI(url)
    }

}