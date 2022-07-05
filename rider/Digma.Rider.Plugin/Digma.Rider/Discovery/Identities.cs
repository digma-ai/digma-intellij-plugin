using System;
using System.Linq;
using Digma.Rider.Util;
using JetBrains.Annotations;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;

namespace Digma.Rider.Discovery
{
    public static class Identities
    {


        [NotNull]
        public static string ComputeFilePath([NotNull] IPsiSourceFile sourceFile)
        {
            return sourceFile.GetLocation().FullPath;
        }   
        
        [NotNull]
        public static string ComputeFileUri([NotNull] IPsiSourceFile sourceFile)
        {
            return sourceFile.GetLocation().ToUri().ToString();
        }

        public static string ComputeParametersPart(ICSharpFunctionDeclaration functionDeclaration)
        {
            var parametersOwner = functionDeclaration.GetParametersOwner();
            if (parametersOwner == null || parametersOwner.Parameters.Count <= 0)
            {
                return "";
            }

            var paramsPart = String.Join(",", parametersOwner.Parameters.Select(param =>
            {
                return param.Type.ToString() + "|" + param.ShortName + "|" + param.Kind;
            }));
            
            return "(" + paramsPart + ")";
        }
        
        public static string ComputeFqn(ICSharpFunctionDeclaration functionDeclaration,
            bool includeParameters = true)
        {
            var namespaceName = PsiUtils.GetNamespace(functionDeclaration);
            var className = PsiUtils.GetClassName(functionDeclaration);
            var methodName = PsiUtils.GetDeclaredName(functionDeclaration);
            var paramsPart = !includeParameters ? "" : ComputeParametersPart(functionDeclaration);
            var fqn = namespaceName + "." + className + "$_$" + methodName + paramsPart;
            return fqn;
        }

        public static string ComputeSpanFqn(string instLibrary, string spanName)
        {
            return instLibrary + "$_$" + spanName;
        }
    }
}