package org.digma.intellij.plugin.test;

import org.digma.intellij.plugin.common.CodeObjectsUtil;
import org.junit.jupiter.api.*;

public class CodeObjectsUtilsTest {

    @Test
    void testSplit() {
        var codeObjectId = "my.package.MyClass$_$myMethod";
        var methodName = CodeObjectsUtil.extractMethodName(codeObjectId);
        Assertions.assertEquals("myMethod", methodName);

        var className = CodeObjectsUtil.extractFqnClassName(codeObjectId);
        Assertions.assertEquals("my.package.MyClass", className);
    }

}
