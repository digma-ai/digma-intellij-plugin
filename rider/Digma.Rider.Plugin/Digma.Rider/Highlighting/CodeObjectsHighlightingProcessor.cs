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
                //visiting only IClassDeclaration effectively ignores interfaces
                case ICSharpNamespaceDeclaration cSharpNamespaceDeclaration:
                case INamespaceBody namespaceBody:
                case IClassDeclaration typeDeclaration:
                case IClassBody classBody:
                    return true;
            }

            return false;
        }

        public void ProcessBeforeInterior(ITreeNode element)
        {
            switch (element)
            {
                case ICSharpFunctionDeclaration functionDeclaration:
                {
                    var methodFqn = Identities.ComputeFqn(functionDeclaration);
                    var methodCodeLenses = _codeObjectsHost.GetRiderCodeLensInfo(methodFqn);
                    if (methodCodeLenses is { Count: > 0 })
                    {
                        Log(_logger, "Found {0} code lens for method {1}", methodCodeLenses.Count,methodFqn);
                        foreach (var riderCodeLensInfo in methodCodeLenses)
                        {
                            Log(_logger, "Installing code lens for code method {0}: {1}", methodFqn,riderCodeLensInfo);
                            _highlightingConsumer.AddHighlighting(
                                new CodeInsightsHighlighting(
                                    functionDeclaration.GetNameDocumentRange(),
                                    riderCodeLensInfo.LensText ?? throw new InvalidOperationException("LensText must not be null"),
                                    riderCodeLensInfo.LensTooltip ?? string.Empty, //todo: can be null
                                    riderCodeLensInfo.MoreText ?? string.Empty, //todo: can be null
                                    _methodInsightsProvider,
                                    functionDeclaration.DeclaredElement,null)
                            );
                        }
                        
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