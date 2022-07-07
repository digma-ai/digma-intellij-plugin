using System;
using System.Linq;
using Digma.Rider.Util;
using System.Text.RegularExpressions;
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

        static readonly Regex arrayRegex = new Regex("\\[,*]$");

        static string GetArraysPart(string typeFqn)
        {
            var arraysValue = "";
            var trimmableValue = typeFqn;

            var arrayMatchResult = arrayRegex.Match(trimmableValue, 0);
            while (arrayMatchResult != null && arrayMatchResult.Success)
            {
                arraysValue = arrayMatchResult.Value + arraysValue;
                trimmableValue = trimmableValue.Substring(0, arrayMatchResult.Index);

                arrayMatchResult = arrayRegex.Match(trimmableValue, 0);
            }

            return arraysValue;
        }
        
        public static string ParameterShortType(string typeFqn)
        {
            string refSignToAppend = "";
            string localTypeFqn = typeFqn;
            if (typeFqn.EndsWith("&"))
            {
                refSignToAppend = "&";
                localTypeFqn = typeFqn.TrimEnd('&');
            }

            var arraysValuePart = GetArraysPart(localTypeFqn);

            var firstIndexOfSquaredParenthesisOpening = localTypeFqn.IndexOf('[');
            var relevantTypeFqn = localTypeFqn;
            if (firstIndexOfSquaredParenthesisOpening >= 0) 
            {
                // has SquaredParenthesisOpening - generic or array, never mind just get the first part
                relevantTypeFqn = localTypeFqn.Substring(0, firstIndexOfSquaredParenthesisOpening);
            }

            var shortNameBeforeArray = relevantTypeFqn.Split('.').Last();

            return $"{shortNameBeforeArray}{arraysValuePart}{refSignToAppend}";            
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
            var paramsPart = "";
            if (includeParameters)
            {
                using (CompilationContextCookie.GetOrCreate(functionDeclaration.GetSourceFile().ResolveContext))
                {
                    paramsPart = ComputeParametersPart(functionDeclaration);
                }
            }

            var fqn = namespaceName + "." + className + "$_$" + methodName + paramsPart;
            return fqn;
        }

        public static string ComputeSpanFqn(string instLibrary, string spanName)
        {
            return instLibrary + "$_$" + spanName;
        }
    }
}