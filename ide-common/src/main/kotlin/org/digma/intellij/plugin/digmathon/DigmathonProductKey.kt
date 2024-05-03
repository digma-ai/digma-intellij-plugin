package org.digma.intellij.plugin.digmathon

import com.google.common.hash.Hashing
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import org.digma.intellij.plugin.common.UniqueGeneratedUserId
import java.nio.charset.StandardCharsets


private const val MY_SERVICE = "org.digma.digmathon.productKey"

//change MY_KEY for every new dismathon
private const val MY_KEY = "product-key-2024.5"

class DigmathonProductKey {

    private val myHash = "89e0f23f9a0a670b2bb7393a1280aa59eaf7c59c22b77d869b5c9c7af021785f"

    @Throws(InvalidProductKeyException::class)
    fun validateAndSave(productKey: String) {
        validate(productKey)
        val credentialAttributes = createCredentialAttributes()
        PasswordSafe.instance.setPassword(credentialAttributes, productKey)
    }

    @Throws(InvalidProductKeyException::class)
    fun validateAndGet(): String? {
        val credentialAttributes = createCredentialAttributes()
        val productKey = PasswordSafe.instance.getPassword(credentialAttributes)
        return productKey?.let {
            validate(it)
            it
        }
    }


    @Throws(InvalidProductKeyException::class)
    private fun validate(productKey: String) {
        if (productKey.isBlank()) {
            throw InvalidProductKeyException(productKey, "product key is empty")
        }
        //the sha256 was produces for upper case so uppercase before validation
        val hash = Hashing.sha256().hashString(productKey.uppercase(), StandardCharsets.UTF_8).toString()
        if (hash != myHash) {
            throw InvalidProductKeyException(productKey, "product key invalid,hash don't match")
        }
    }


    private fun createCredentialAttributes(): CredentialAttributes {
        return CredentialAttributes(
            generateServiceName(MY_SERVICE, MY_KEY),
            UniqueGeneratedUserId.userId,
            this::class.java
        )
    }


    fun clear() {
        val credentialAttributes = createCredentialAttributes()
        PasswordSafe.instance.setPassword(credentialAttributes, null)
    }

}

class InvalidProductKeyException(val productKey: String, message: String) : RuntimeException(message)