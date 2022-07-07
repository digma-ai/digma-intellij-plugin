using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
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

        public static readonly string PsiValueOfUnresolved = "???";

        public static string GetParameterTypeFqn(IParameter param, out bool managedToResolve)
        {
            var partialFqn = param.Type.ToString();

            var isRef = (param.Kind == ParameterKind.OUTPUT // parameter for example: "out string msg"
                         || param.Kind == ParameterKind.REFERENCE); // parameter for example: "ref string msg"

            return GetParameterTypeFqn(partialFqn, isRef, out managedToResolve);
        }

        public static string GetParameterTypeFqn(string typePartialFqn, bool isRef, out bool managedToResolve)
        {
            managedToResolve = !typePartialFqn.Contains(PsiValueOfUnresolved);
            var refSign = isRef ? "&" : "";
            return $"{typePartialFqn}{refSign}";
        }

        private static string ComputeParametersPart(ICSharpFunctionDeclaration functionDeclaration,
            out bool managedToResolveParams)
        {
            managedToResolveParams = true;
            var parametersOwner = functionDeclaration.GetParametersOwner();
            if (parametersOwner == null || parametersOwner.Parameters.Count <= 0)
            {
                return "";
            }

            var paramsPart = new StringBuilder();
            paramsPart.Append('(');
            bool firstIteration = true;
            foreach (var param in parametersOwner.Parameters)
            {
                if (!firstIteration)
                {
                    paramsPart.Append(',');
                }

                firstIteration = false;

                var paramTypeFqn = GetParameterTypeFqn(param, out bool managedToResolveCurrent);
                managedToResolveParams &= managedToResolveCurrent;

                var parameterShortType = ParameterShortType(paramTypeFqn);
                paramsPart.Append(parameterShortType);
            }

            paramsPart.Append(')');

            return paramsPart.ToString();
        }

        // computes MethodFqn supposed to equal CodeObjectId
        public static string ComputeFqn(ICSharpFunctionDeclaration functionDeclaration,
            out bool managedToResolve)
        {
            managedToResolve = true;
            var namespaceName = PsiUtils.GetNamespace(functionDeclaration);
            var className = PsiUtils.GetClassName(functionDeclaration);
            var methodName = PsiUtils.GetDeclaredName(functionDeclaration);
            var paramsPart = "";
            using (CompilationContextCookie.GetOrCreate(functionDeclaration.GetSourceFile().ResolveContext))
            {
                paramsPart = ComputeParametersPart(functionDeclaration, out managedToResolve);
            }

            var fqn = namespaceName + "." + className + "$_$" + methodName + paramsPart;
            return fqn;
        }

        public static string ComputeFqn(ICSharpFunctionDeclaration functionDeclaration)
        {
            return ComputeFqn(functionDeclaration, out bool b);
        }

        public static string ComputeSpanFqn(string instLibrary, string spanName)
        {
            return instLibrary + "$_$" + spanName;
        }
    }
}