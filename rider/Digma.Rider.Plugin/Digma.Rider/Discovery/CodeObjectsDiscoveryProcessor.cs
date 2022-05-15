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
        //fileUri goes in every method info
        private readonly string _fileUri;
        public bool ProcessingIsFinished => false;
        private readonly IList<RiderMethodInfo> _methodInfos = new List<RiderMethodInfo>();

        public CodeObjectsDiscoveryProcessor(string fileUri)
        {
            _fileUri = fileUri;
        }

        [NotNull]
        public IEnumerable<RiderMethodInfo> MethodInfos => _methodInfos;

        [SuppressMessage("ReSharper", "UnusedVariable")]
        public bool InteriorShouldBeProcessed(ITreeNode element)
        {
            switch (element)
            {
                //visiting only IClassDeclaration effectively ignores interfaces
                case ICSharpNamespaceDeclaration cSharpNamespaceDeclaration:
                case INamespaceBody namespaceBody:
                case IClassDeclaration classDeclaration:
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
                case ICSharpFunctionDeclaration functionDeclaration:
                {
                    var methodFqn = Identities.ComputeFqn(functionDeclaration);
                    var declaredName = PsiUtils.GetDeclaredName(functionDeclaration);
                    var containingClassName = PsiUtils.GetClassName(functionDeclaration);
                    var containingNamespace = PsiUtils.GetNamespace(functionDeclaration);
                    var containingFileUri = _fileUri;
                    
                    var methodInfo = new RiderMethodInfo(methodFqn,declaredName,containingClassName,containingNamespace,containingFileUri);
                    _methodInfos.Add(methodInfo);
                    break;
                }
            }
        }
    }

}