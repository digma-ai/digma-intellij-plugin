using System;
using System.Diagnostics.CodeAnalysis;
using Digma.Rider.Discovery;
using Digma.Rider.Protocol;
using JetBrains.ReSharper.Daemon.CodeInsights;
using JetBrains.ReSharper.Feature.Services.Daemon;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Highlighting
{
    internal class CodeObjectsHighlightingProcessor : IRecursiveElementProcessor
    {
        private readonly CodeObjectsHost _codeObjectsHost;
        private readonly IHighlightingConsumer _highlightingConsumer;
        private readonly MethodInsightsProvider _methodInsightsProvider;
        private readonly ILogger _logger;

        public bool ProcessingIsFinished => false;

        public CodeObjectsHighlightingProcessor(CodeObjectsHost codeObjectsHost,
            IHighlightingConsumer highlightingConsumer, MethodInsightsProvider methodInsightsProvider, ILogger logger)
        {
            _codeObjectsHost = codeObjectsHost;
            _highlightingConsumer = highlightingConsumer;
            _methodInsightsProvider = methodInsightsProvider;
            _logger = logger;
        }

        [SuppressMessage("ReSharper", "UnusedVariable")]
        public bool InteriorShouldBeProcessed(ITreeNode element)
        {
            switch (element)
            {
                case ICSharpNamespaceDeclaration cSharpNamespaceDeclaration:
                case INamespaceBody namespaceBody:
                case ICSharpTypeDeclaration typeDeclaration:
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
                    var methodFqn = Identities.ComputeFqn(methodDeclaration);
                    var riderCodeLensInfo = _codeObjectsHost.GetRiderCodeLensInfo(methodFqn);
                    if (riderCodeLensInfo != null)
                    {
                        Log(_logger, "Installing code lens for code object {0}", methodFqn);
                        _highlightingConsumer.AddHighlighting(
                            new CodeInsightsHighlighting(
                                methodDeclaration.GetNameDocumentRange(),
                                riderCodeLensInfo.LensText ?? throw new InvalidOperationException("LensText must not be null"),
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