using System;
using System.Diagnostics.CodeAnalysis;
using Digma.Rider.Protocol;
using JetBrains.ReSharper.Daemon.CodeInsights;
using JetBrains.ReSharper.Feature.Services.Daemon;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;

namespace Digma.Rider.Analysis
{
    internal class CodeObjectsHighlightingProcessor : IRecursiveElementProcessor
    {
        private readonly ICSharpFile _cSharpFile;
        private readonly CodeObjectsAnalysisHost _codeObjectsAnalysisHost;
        private readonly IHighlightingConsumer _highlightingConsumer;
        private readonly MethodInsightsProvider _methodInsightsProvider;

        public bool ProcessingIsFinished => false;

        public CodeObjectsHighlightingProcessor(ICSharpFile element, CodeObjectsAnalysisHost codeObjectsAnalysisHost,
            IHighlightingConsumer highlightingConsumer, MethodInsightsProvider methodInsightsProvider)
        {
            _cSharpFile = element;
            _codeObjectsAnalysisHost = codeObjectsAnalysisHost;
            _highlightingConsumer = highlightingConsumer;
            _methodInsightsProvider = methodInsightsProvider;
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
            switch (element)
            {
                case IMethodDeclaration methodDeclaration:
                {
                    var fqn = Identities.ComputeFqn(methodDeclaration);
                    RiderCodeLensInfo riderCodeLensInfo = _codeObjectsAnalysisHost.GetRiderCodeLensInfo(fqn);
                    if (riderCodeLensInfo != null)
                    {
                        _highlightingConsumer.AddHighlighting(
                            new CodeInsightsHighlighting(
                                methodDeclaration.GetNameDocumentRange(),
                                riderCodeLensInfo.LensText ?? throw new InvalidOperationException(),
                                riderCodeLensInfo.LensTooltip ?? string.Empty, //todo: can be null
                                riderCodeLensInfo.MoreText ?? string.Empty, //todo: can be null
                                _methodInsightsProvider,
                                methodDeclaration.DeclaredElement,null)
                        );
                    }
                    
                    break;
                }
            }
        }

        public void ProcessAfterInterior(ITreeNode element)
        {
            
        }
    }
}