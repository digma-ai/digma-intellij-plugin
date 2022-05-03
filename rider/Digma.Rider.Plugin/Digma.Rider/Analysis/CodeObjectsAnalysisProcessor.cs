using System;
using System.Diagnostics.CodeAnalysis;
using Digma.Rider.Protocol;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;

namespace Digma.Rider.Analysis
{
    internal class CodeObjectsAnalysisProcessor : IRecursiveElementProcessor
    {
        private readonly ICSharpFile _cSharpFile;
        private readonly CodeObjectsAnalysisHost _codeObjectsAnalysisHost;

        public bool ProcessingIsFinished => false;

        public CodeObjectsAnalysisProcessor(ICSharpFile element, CodeObjectsAnalysisHost codeObjectsAnalysisHost)
        {
            _cSharpFile = element;
            _codeObjectsAnalysisHost = codeObjectsAnalysisHost;
        }

        [SuppressMessage("ReSharper", "UnusedVariable")]
        public bool InteriorShouldBeProcessed(ITreeNode element)
        {
            switch (element)
            {
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
                case IMethodDeclaration methodDeclaration:
                {
                    var filePath = Identities.ComputeFilePath(_cSharpFile);
                    var methodFqn = Identities.ComputeFqn(methodDeclaration);
            
                    
                    var methodInfo = new RiderMethodInfo(methodFqn,
                        methodDeclaration.DeclaredElement?.ShortName ?? 
                        throw new NullReferenceException(),
                        methodDeclaration.DeclaredName,
                        methodDeclaration.DeclaredElement?.ContainingType?.ShortName ??
                        throw new NullReferenceException(),
                        methodDeclaration.DeclaredElement?.ContainingType?.GetContainingNamespace().QualifiedName ??
                        throw new NullReferenceException(),
                        methodDeclaration.GetSourceFile().GetLocation().FullPath,
                        methodDeclaration.GetSourceFile()?.DisplayName ?? 
                        throw new NullReferenceException());
                    
                    _codeObjectsAnalysisHost.AddOrUpdateMethodInfo(filePath, methodFqn, methodInfo);
            
                    break;
                }
            }
        }
    }
}