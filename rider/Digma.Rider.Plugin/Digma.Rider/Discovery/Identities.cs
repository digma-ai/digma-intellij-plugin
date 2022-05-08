using JetBrains.Annotations;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.RiderTutorials.Utils;

namespace Digma.Rider.Discovery
{
    public static class Identities
    {


        [NotNull]
        public static string ComputeFileUri([NotNull] IPsiSourceFile sourceFile)
        {
            return sourceFile.GetLocation().ToUri().ToString();
        }

        public static string ComputeFqn(ICSharpFunctionDeclaration functionDeclaration)
        {
            var namespaceName = PsiUtils.GetNamespace(functionDeclaration);
            var className = PsiUtils.GetClassName(functionDeclaration);
            var methodName = PsiUtils.GetDeclaredName(functionDeclaration);
            var fqn = namespaceName + "." + className + "$_$" + methodName;
            return fqn;
        }
    }
}