@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.digma.rider.protocol

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.reflect.KClass
import kotlin.jvm.JvmStatic



/**
 * #### Generated from [ElementUnderCaretModel.kt:8]
 */
class ElementUnderCaretModel private constructor(
    private val _elementUnderCaret: RdOptionalProperty<ElementUnderCaret>,
    private val _notifyElementUnderCaret: RdSignal<Unit>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(ElementUnderCaret)
        }
        
        
        
        
        const val serializationHash = -7498972151373417626L
        
    }
    override val serializersOwner: ISerializersOwner get() = ElementUnderCaretModel
    override val serializationHash: Long get() = ElementUnderCaretModel.serializationHash
    
    //fields
    val elementUnderCaret: IOptProperty<ElementUnderCaret> get() = _elementUnderCaret
    val notifyElementUnderCaret: ISignal<Unit> get() = _notifyElementUnderCaret
    //methods
    //initializer
    init {
        _elementUnderCaret.optimizeNested = true
    }
    
    init {
        bindableChildren.add("elementUnderCaret" to _elementUnderCaret)
        bindableChildren.add("notifyElementUnderCaret" to _notifyElementUnderCaret)
    }
    
    //secondary constructor
    internal constructor(
    ) : this(
        RdOptionalProperty<ElementUnderCaret>(ElementUnderCaret),
        RdSignal<Unit>(FrameworkMarshallers.Void)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ElementUnderCaretModel (")
        printer.indent {
            print("elementUnderCaret = "); _elementUnderCaret.print(printer); println()
            print("notifyElementUnderCaret = "); _notifyElementUnderCaret.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): ElementUnderCaretModel   {
        return ElementUnderCaretModel(
            _elementUnderCaret.deepClonePolymorphic(),
            _notifyElementUnderCaret.deepClonePolymorphic()
        )
    }
    //contexts
}
val com.jetbrains.rd.ide.model.Solution.elementUnderCaretModel get() = getOrCreateExtension("elementUnderCaretModel", ::ElementUnderCaretModel)



/**
 * #### Generated from [ElementUnderCaretModel.kt:10]
 */
data class ElementUnderCaret (
    val fqn: String,
    val name: String,
    val className: String,
    val fileUri: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<ElementUnderCaret> {
        override val _type: KClass<ElementUnderCaret> = ElementUnderCaret::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ElementUnderCaret  {
            val fqn = buffer.readString()
            val name = buffer.readString()
            val className = buffer.readString()
            val fileUri = buffer.readString()
            return ElementUnderCaret(fqn, name, className, fileUri)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ElementUnderCaret)  {
            buffer.writeString(value.fqn)
            buffer.writeString(value.name)
            buffer.writeString(value.className)
            buffer.writeString(value.fileUri)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as ElementUnderCaret
        
        if (fqn != other.fqn) return false
        if (name != other.name) return false
        if (className != other.className) return false
        if (fileUri != other.fileUri) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + fqn.hashCode()
        __r = __r*31 + name.hashCode()
        __r = __r*31 + className.hashCode()
        __r = __r*31 + fileUri.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ElementUnderCaret (")
        printer.indent {
            print("fqn = "); fqn.print(printer); println()
            print("name = "); name.print(printer); println()
            print("className = "); className.print(printer); println()
            print("fileUri = "); fileUri.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
