using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using Digma.Rider.Protocol;
using JetBrains.Annotations;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;

namespace Digma.Rider.Discovery
{
    internal class CodeObjectsDiscoveryProcessor : IRecursiveElementProcessor
    {
        public bool ProcessingIsFinished => false;
        private readonly IList<RiderMethodInfo> _methodInfos = new List<RiderMethodInfo>();

        [NotNull]
        public IEnumerable<RiderMethodInfo> MethodInfos => _methodInfos;

        [SuppressMessage("ReSharper", "UnusedVariable")]
        public bool InteriorShouldBeProcessed(ITreeNode element)
        {
            switch (element)
            {
                case ICSharpNamespaceDeclaration cSharpNamespaceDeclaration:
                case INamespaceBody namespaceBody:
                case ICSharpTypeDeclaration classDeclaration:
                case IClassBody classBody:
                    return true;
            }

            return false;
        }

        public void ProcessBeforeInterior(ITreeNode element)
        {

        }

        public void ProcessAfterInterior(ITreeNode element)
        {
            switch (element)
            {
                case IMethodDeclaration methodDeclaration:
                {
                    var methodFqn = Identities.ComputeFqn(methodDeclaration);
                    var shortName = methodDeclaration.GetDeclaredShortName();
                    var declaredName = methodDeclaration.DeclaredName;
                    var containingClassName = PsiUtils.GetClassName(methodDeclaration);
                    var containingNamespace = PsiUtils.GetNamespace(methodDeclaration);
                    var containingFile = methodDeclaration.GetSourceFile().GetLocation().FullPath;
                    
                    var methodInfo = new RiderMethodInfo(methodFqn,
                        shortName,declaredName,containingClassName,containingNamespace,containingFile);

                    _methodInfos.Add(methodInfo);
                    break;
                }
            }
        }
    }

}