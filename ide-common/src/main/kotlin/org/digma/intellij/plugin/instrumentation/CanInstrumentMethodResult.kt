package org.digma.intellij.plugin.instrumentation


open class CanInstrumentMethodResult(val failureCause: IFailureCause? = null) {


    companion object {
        @JvmStatic
        fun failure(): CanInstrumentMethodResult {
            return CanInstrumentMethodResult(GenericFailureCause())
        }
    }


    fun wasSucceeded(): Boolean {
        return failureCause == null
    }

//    fun getFailureCause(): IFailureCause? {
//        return failureCause
//    }


}


interface IFailureCause

class MissingDependencyCause(val dependency: String) : IFailureCause

class GenericFailureCause : IFailureCause