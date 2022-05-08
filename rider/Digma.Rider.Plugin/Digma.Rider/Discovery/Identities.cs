using JetBrains.Annotations;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.RiderTutorials.Utils;

namespace Digma.Rider.Discovery
{
    public static class Identities
    {

        [NotNull]
        public static string ComputeFqn([NotNull] IMethodDeclaration methodDeclaration)
        {
            var namespaceName = PsiUtils.GetNamespace(methodDeclaration);
            var className = PsiUtils.GetClassName(methodDeclaration);
            var methodName = methodDeclaration.GetDeclaredShortName();
            var fqn = namespaceName + "." + className + "$_$" + methodName;
            return fqn;
        }



        [NotNull]
        public static string ComputeFilePath([NotNull] IPsiSourceFile sourceFile)
        {
            return sourceFile.GetLocation().ToUri().ToString();
        }
    }
}