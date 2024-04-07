package org.digma.intellij.plugin.digmathon

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import org.digma.intellij.plugin.common.UserId


private const val MY_SERVICE = "org.digma.digmathon.productKey"
private const val MY_KEY = "product-key"

class DigmathonProductKey {

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
        //todo:
        //validate
        //send posthog event
        //throw InvalidProductKeyException
        if (productKey.isBlank()) {
            throw InvalidProductKeyException("product key is empty")
        }
    }


    private fun createCredentialAttributes(): CredentialAttributes {
        return CredentialAttributes(
            generateServiceName(MY_SERVICE, MY_KEY),
            UserId.userId,
            this::class.java
        )
    }

    @Throws(InvalidProductKeyException::class)
    fun exists(): Boolean {
        return validateAndGet() != null
    }

    fun clear() {
        val credentialAttributes = createCredentialAttributes()
        PasswordSafe.instance.setPassword(credentialAttributes, null)
    }

}

class InvalidProductKeyException(message: String) : RuntimeException(message)