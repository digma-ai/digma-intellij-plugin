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
 * #### Generated from [MethodInfoModel.kt:10]
 */
class MethodInfoModel private constructor(
    private val _getMethodUnderCaret: RdCall<Unit, MethodInfo>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(MethodInfo)
        }
        
        
        
        
        const val serializationHash = 4524857110976482394L
        
    }
    override val serializersOwner: ISerializersOwner get() = MethodInfoModel
    override val serializationHash: Long get() = MethodInfoModel.serializationHash
    
    //fields
    val getMethodUnderCaret: IRdCall<Unit, MethodInfo> get() = _getMethodUnderCaret
    //methods
    //initializer
    init {
        bindableChildren.add("getMethodUnderCaret" to _getMethodUnderCaret)
    }
    
    //secondary constructor
    internal constructor(
    ) : this(
        RdCall<Unit, MethodInfo>(FrameworkMarshallers.Void, MethodInfo)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("MethodInfoModel (")
        printer.indent {
            print("getMethodUnderCaret = "); _getMethodUnderCaret.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): MethodInfoModel   {
        return MethodInfoModel(
            _getMethodUnderCaret.deepClonePolymorphic()
        )
    }
    //contexts
}
val com.jetbrains.rd.ide.model.Solution.methodInfoModel get() = getOrCreateExtension("methodInfoModel", ::MethodInfoModel)



/**
 * #### Generated from [MethodInfoModel.kt:12]
 */
data class MethodInfo (
    val fqn: String,
    val filePath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<MethodInfo> {
        override val _type: KClass<MethodInfo> = MethodInfo::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): MethodInfo  {
            val fqn = buffer.readString()
            val filePath = buffer.readString()
            return MethodInfo(fqn, filePath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: MethodInfo)  {
            buffer.writeString(value.fqn)
            buffer.writeString(value.filePath)
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
        
        other as MethodInfo
        
        if (fqn != other.fqn) return false
        if (filePath != other.filePath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + fqn.hashCode()
        __r = __r*31 + filePath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("MethodInfo (")
        printer.indent {
            print("fqn = "); fqn.print(printer); println()
            print("filePath = "); filePath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
