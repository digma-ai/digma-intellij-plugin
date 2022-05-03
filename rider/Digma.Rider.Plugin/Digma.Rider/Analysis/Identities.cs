using System.Collections.Generic;
using System.Diagnostics;
using JetBrains.Annotations;
using JetBrains.DocumentManagers;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.Util;

namespace Digma.Rider.Analysis
{
    public static class Identities
    {
        
        static Dictionary < string, ICSharpFile > _myPathTpSharpFile = new Dictionary < string, ICSharpFile > ();
        
        public static string ComputeFqn([NotNull] ICSharpFunctionDeclaration declaration)
        {
            var namespaceName = declaration.DeclaredElement?.ContainingType?.GetContainingNamespace().QualifiedName;
            var className = declaration.DeclaredElement?.ContainingType?.ShortName;
            var methodName = declaration.DeclaredElement?.ShortName;
            var fqn = namespaceName + "." + className + "$_$" + methodName;
            return fqn;
        }
        
        public static string ComputeFilePath([NotNull] ICSharpFile cSharpFile)
        {
            string path = cSharpFile.GetSourceFile()?.Document.TryGetFilePath().ToUri().ToString();
            Debug.Assert(path != null, nameof(path) + " != null");
            if (!_myPathTpSharpFile.ContainsKey(path))
            {
                _myPathTpSharpFile.Add(path,cSharpFile);    
            }
            
            return path;
        }

        public static ICSharpFile FilePathToCSharpFile(string file)
        {
            return _myPathTpSharpFile.TryGetValue(file);
        }
    }
}