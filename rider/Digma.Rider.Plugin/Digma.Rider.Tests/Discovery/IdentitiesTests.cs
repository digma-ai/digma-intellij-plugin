using Digma.Rider.Discovery;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace Digma.Rider.Tests.Discovery;

[TestClass]
public class IdentitiesTests
{
    [TestMethod]
    // SimpleType
    [DataRow("SimpleType", "System.String", "String")]
    [DataRow("SimpleType", "System.Int32", "Int32")]
    [DataRow("SimpleType", "Hello.Great.World", "World")]
    [DataRow("SimpleType", "Abc", "Abc")]
    // Array
    [DataRow("Array", "System.String[]", "String[]")]
    [DataRow("Array", "System.Int32[]", "Int32[]")]
    [DataRow("Array", "Hello.Great.World[]", "World[]")]
    [DataRow("Array", "Abc[]", "Abc[]")]
    // MultiDimensionalArray
    [DataRow("MultiDimensionalArray", "System.String[,]", "String[,]")]
    [DataRow("MultiDimensionalArray", "System.Int32[,]", "Int32[,]")]
    [DataRow("MultiDimensionalArray", "Hello.Great.World[,]", "World[,]")]
    [DataRow("MultiDimensionalArray", "Abc[,]", "Abc[,]")]
    // JaggedArrays
    [DataRow("JaggedArrays", "System.String[][]", "String[][]")]
    [DataRow("JaggedArrays", "System.Int32[][][]", "Int32[][][]")]
    [DataRow("JaggedArrays", "Hello.Great.World[][][][]", "World[][][][]")]
    [DataRow("JaggedArrays", "Abc[][][][][]", "Abc[][][][][]")]
    // MixOfJaggedAndMultiDimensionalArrays
    [DataRow("MixOfJaggedAndMultiDimensionalArrays", "System.String[,,][]", "String[,,][]")]
    [DataRow("MixOfJaggedAndMultiDimensionalArrays", "System.Int32[][][,]", "Int32[][][,]")]
    [DataRow("MixOfJaggedAndMultiDimensionalArrays", "Hello.Great.World[][][,,,][]", "World[][][,,,][]")]
    [DataRow("MixOfJaggedAndMultiDimensionalArrays", "Abc[,,][][][,][]", "Abc[,,][][][,][]")]
    // WithGenerics , parameter defined as "IList<string> names"
    [DataRow("WithGenerics", "System.Collections.Generic.IList`1[T -> System.String]", "IList`1")]
    // GenericsListOfArray , parameter defined as "IList<string[]> listOfArray"
    [DataRow("GenericsListOfArray", "System.Collections.Generic.IList`1[T -> System.String[]]", "IList`1")]
    // GenericsArrayOfList , parameter defined as "IList<int>[] arrayOfList"
    [DataRow("GenericsArrayOfList", "System.Collections.Generic.IList`1[T -> System.Int32][]", "IList`1[]")]
    // RefTypes
    [DataRow("RefTypes", "System.String&", "String&")]
    [DataRow("RefTypes", "System.Int32&", "Int32&")]
    [DataRow("RefTypes", "Hello.Great.World&", "World&")]
    [DataRow("RefTypes", "Abc&", "Abc&")]
    // WithRefArrays
    [DataRow("WithRefArrays", "System.String[]&", "String[]&")]
    [DataRow("WithRefArrays", "System.Int32[]&", "Int32[]&")]
    [DataRow("WithRefArrays", "Hello.Great.World[]&", "World[]&")]
    [DataRow("WithRefArrays", "Abc[]&", "Abc[]&")]
    public void Test_ParameterShortType(string description, string input, string expected)
    {
        Assert.AreEqual(expected, Identities.ParameterShortType(input));
    }

    [TestMethod]
    [DataRow("System.String")]
    [DataRow("System.Collections.Generic.IList`1[T -> System.Int32][]")]
    public void Test_GetParameterTypeFqn_RefFalse(string typeFqn)
    {
        Assert.AreEqual(typeFqn, Identities.GetParameterTypeFqn(typeFqn, false, out bool managedToResolve));
        Assert.IsTrue(managedToResolve);
    }

    [TestMethod]
    [DataRow("System.String")]
    [DataRow("System.Collections.Generic.IList`1[T -> System.Int32][]")]
    public void Test_GetParameterTypeFqn_RefTrue(string typeFqn)
    {
        Assert.AreEqual(typeFqn + "&", Identities.GetParameterTypeFqn(typeFqn, true, out bool managedToResolve));
        Assert.IsTrue(managedToResolve);
    }
    
}