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
 * #### Generated from [CodeObjectsModel.kt:9]
 */
class CodeObjectsModel private constructor(
    private val _reanalyze: RdSignal<String>,
    private val _fileAnalyzed: RdSignal<String>,
    private val _documents: RdMap<String, Document>,
    private val _codeLens: RdMap<String, RiderCodeLensInfo>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(RiderMethodInfo)
            serializers.register(RiderCodeLensInfo)
            serializers.register(Document)
        }
        
        
        
        
        const val serializationHash = -3687301284060761471L
        
    }
    override val serializersOwner: ISerializersOwner get() = CodeObjectsModel
    override val serializationHash: Long get() = CodeObjectsModel.serializationHash
    
    //fields
    val reanalyze: ISignal<String> get() = _reanalyze
    val fileAnalyzed: ISignal<String> get() = _fileAnalyzed
    val documents: IMutableViewableMap<String, Document> get() = _documents
    val codeLens: IMutableViewableMap<String, RiderCodeLensInfo> get() = _codeLens
    //methods
    //initializer
    init {
        _codeLens.optimizeNested = true
    }
    
    init {
        bindableChildren.add("reanalyze" to _reanalyze)
        bindableChildren.add("fileAnalyzed" to _fileAnalyzed)
        bindableChildren.add("documents" to _documents)
        bindableChildren.add("codeLens" to _codeLens)
    }
    
    //secondary constructor
    internal constructor(
    ) : this(
        RdSignal<String>(FrameworkMarshallers.String),
        RdSignal<String>(FrameworkMarshallers.String),
        RdMap<String, Document>(FrameworkMarshallers.String, Document),
        RdMap<String, RiderCodeLensInfo>(FrameworkMarshallers.String, RiderCodeLensInfo)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("CodeObjectsModel (")
        printer.indent {
            print("reanalyze = "); _reanalyze.print(printer); println()
            print("fileAnalyzed = "); _fileAnalyzed.print(printer); println()
            print("documents = "); _documents.print(printer); println()
            print("codeLens = "); _codeLens.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): CodeObjectsModel   {
        return CodeObjectsModel(
            _reanalyze.deepClonePolymorphic(),
            _fileAnalyzed.deepClonePolymorphic(),
            _documents.deepClonePolymorphic(),
            _codeLens.deepClonePolymorphic()
        )
    }
    //contexts
}
val com.jetbrains.rd.ide.model.Solution.codeObjectsModel get() = getOrCreateExtension("codeObjectsModel", ::CodeObjectsModel)



/**
 * #### Generated from [CodeObjectsModel.kt:43]
 */
class Document private constructor(
    private val _methods: RdMap<String, RiderMethodInfo>
) : RdBindableBase() {
    //companion
    
    companion object : IMarshaller<Document> {
        override val _type: KClass<Document> = Document::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): Document  {
            val _id = RdId.read(buffer)
            val _methods = RdMap.read(ctx, buffer, FrameworkMarshallers.String, RiderMethodInfo)
            return Document(_methods).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: Document)  {
            value.rdid.write(buffer)
            RdMap.write(ctx, buffer, value._methods)
        }
        
        
    }
    //fields
    val methods: IMutableViewableMap<String, RiderMethodInfo> get() = _methods
    //methods
    //initializer
    init {
        _methods.optimizeNested = true
    }
    
    init {
        bindableChildren.add("methods" to _methods)
    }
    
    //secondary constructor
    constructor(
    ) : this(
        RdMap<String, RiderMethodInfo>(FrameworkMarshallers.String, RiderMethodInfo)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("Document (")
        printer.indent {
            print("methods = "); _methods.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): Document   {
        return Document(
            _methods.deepClonePolymorphic()
        )
    }
    //contexts
}


/**
 * #### Generated from [CodeObjectsModel.kt:23]
 */
data class RiderCodeLensInfo (
    val codeObjectId: String,
    val lensText: String?,
    val lensTooltip: String?,
    val moreText: String?,
    val anchor: String?
) : IPrintable {
    //companion
    
    companion object : IMarshaller<RiderCodeLensInfo> {
        override val _type: KClass<RiderCodeLensInfo> = RiderCodeLensInfo::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RiderCodeLensInfo  {
            val codeObjectId = buffer.readString()
            val lensText = buffer.readNullable { buffer.readString() }
            val lensTooltip = buffer.readNullable { buffer.readString() }
            val moreText = buffer.readNullable { buffer.readString() }
            val anchor = buffer.readNullable { buffer.readString() }
            return RiderCodeLensInfo(codeObjectId, lensText, lensTooltip, moreText, anchor)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RiderCodeLensInfo)  {
            buffer.writeString(value.codeObjectId)
            buffer.writeNullable(value.lensText) { buffer.writeString(it) }
            buffer.writeNullable(value.lensTooltip) { buffer.writeString(it) }
            buffer.writeNullable(value.moreText) { buffer.writeString(it) }
            buffer.writeNullable(value.anchor) { buffer.writeString(it) }
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
        
        other as RiderCodeLensInfo
        
        if (codeObjectId != other.codeObjectId) return false
        if (lensText != other.lensText) return false
        if (lensTooltip != other.lensTooltip) return false
        if (moreText != other.moreText) return false
        if (anchor != other.anchor) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + codeObjectId.hashCode()
        __r = __r*31 + if (lensText != null) lensText.hashCode() else 0
        __r = __r*31 + if (lensTooltip != null) lensTooltip.hashCode() else 0
        __r = __r*31 + if (moreText != null) moreText.hashCode() else 0
        __r = __r*31 + if (anchor != null) anchor.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("RiderCodeLensInfo (")
        printer.indent {
            print("codeObjectId = "); codeObjectId.print(printer); println()
            print("lensText = "); lensText.print(printer); println()
            print("lensTooltip = "); lensTooltip.print(printer); println()
            print("moreText = "); moreText.print(printer); println()
            print("anchor = "); anchor.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [CodeObjectsModel.kt:12]
 */
data class RiderMethodInfo (
    val id: String,
    val name: String,
    val displayName: String,
    val containingClass: String,
    val containingNamespace: String,
    val containingFile: String,
    val containingFileDisplayName: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<RiderMethodInfo> {
        override val _type: KClass<RiderMethodInfo> = RiderMethodInfo::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RiderMethodInfo  {
            val id = buffer.readString()
            val name = buffer.readString()
            val displayName = buffer.readString()
            val containingClass = buffer.readString()
            val containingNamespace = buffer.readString()
            val containingFile = buffer.readString()
            val containingFileDisplayName = buffer.readString()
            return RiderMethodInfo(id, name, displayName, containingClass, containingNamespace, containingFile, containingFileDisplayName)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RiderMethodInfo)  {
            buffer.writeString(value.id)
            buffer.writeString(value.name)
            buffer.writeString(value.displayName)
            buffer.writeString(value.containingClass)
            buffer.writeString(value.containingNamespace)
            buffer.writeString(value.containingFile)
            buffer.writeString(value.containingFileDisplayName)
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
        
        other as RiderMethodInfo
        
        if (id != other.id) return false
        if (name != other.name) return false
        if (displayName != other.displayName) return false
        if (containingClass != other.containingClass) return false
        if (containingNamespace != other.containingNamespace) return false
        if (containingFile != other.containingFile) return false
        if (containingFileDisplayName != other.containingFileDisplayName) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + id.hashCode()
        __r = __r*31 + name.hashCode()
        __r = __r*31 + displayName.hashCode()
        __r = __r*31 + containingClass.hashCode()
        __r = __r*31 + containingNamespace.hashCode()
        __r = __r*31 + containingFile.hashCode()
        __r = __r*31 + containingFileDisplayName.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("RiderMethodInfo (")
        printer.indent {
            print("id = "); id.print(printer); println()
            print("name = "); name.print(printer); println()
            print("displayName = "); displayName.print(printer); println()
            print("containingClass = "); containingClass.print(printer); println()
            print("containingNamespace = "); containingNamespace.print(printer); println()
            print("containingFile = "); containingFile.print(printer); println()
            print("containingFileDisplayName = "); containingFileDisplayName.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
