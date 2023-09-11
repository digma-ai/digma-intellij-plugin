package org.digma.intellij.plugin.test.system.framework

import java.lang.reflect.Method

fun invokeMethod(toInvokeOn: Any, methodRef: Method, vararg args: Any): Any? {
    
    return methodRef.invoke(toInvokeOn, *args)
}

fun getMethodReference(obj: Any, methodName: String, vararg argsTypes: Class<*>): Method 
{
    val method = obj.javaClass.getDeclaredMethod(methodName, *argsTypes)
    method.isAccessible = true
    return method
}
