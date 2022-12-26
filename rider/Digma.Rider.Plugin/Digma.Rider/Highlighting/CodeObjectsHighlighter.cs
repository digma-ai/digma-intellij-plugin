using System;
using Digma.Rider.Discovery;
using Digma.Rider.Protocol;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Daemon.CodeInsights;
using JetBrains.ReSharper.Feature.Services.Daemon;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Rider.Backend.Platform.Icons;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Highlighting
{
    [ElementProblemAnalyzer(typeof(ICSharpFunctionDeclaration))]
    public class CodeObjectsHighlighter : ElementProblemAnalyzer<ICSharpFunctionDeclaration>
    {
        private readonly ILogger _logger;
        private readonly CodeObjectsHost _codeObjectsHost;
        private readonly CodeLensProviderFactory _codeLensProviderFactory;

        public CodeObjectsHighlighter(ILogger logger,
            CodeObjectsHost codeObjectsHost,
            ISolution solution,
            IconHost iconHost,
            ShowToolWindowHost showToolWindowHost,
            CodeLensProviderFactory codeLensProviderFactory
        )
        {
            _logger = logger;
            _codeObjectsHost = codeObjectsHost;
            _codeLensProviderFactory = codeLensProviderFactory;
        }

        protected override void Run(ICSharpFunctionDeclaration functionDeclaration,
            ElementProblemAnalyzerData data,
            IHighlightingConsumer highlightingConsumer)
        {
            Log(_logger, "CodeObjectsHighlighter.Run invoked for {0}", functionDeclaration);

            var methodFqn = Identities.ComputeFqn(functionDeclaration);
            var methodCodeLenses = _codeObjectsHost.GetRiderCodeLensInfo(methodFqn);
            if (methodCodeLenses is { Count: > 0 })
            {
                Log(_logger, "Found {0} code lens for method {1}", methodCodeLenses.Count, methodFqn);
                foreach (var riderCodeLensInfo in methodCodeLenses)
                {
                    Log(_logger, "Installing code lens for code method {0}: {1}", methodFqn, riderCodeLensInfo);
                    var codeLensMethodInsightsProvider =
                        _codeLensProviderFactory.GetFactory(riderCodeLensInfo.LensTitle);

                    highlightingConsumer.AddHighlighting(
                        new CodeInsightsHighlighting(
                            functionDeclaration.GetNameDocumentRange(),
                            riderCodeLensInfo.LensTitle ??
                            throw new InvalidOperationException("LensTitle must not be null"),
                            riderCodeLensInfo.LensDescription ?? string.Empty, //todo: can be null
                            riderCodeLensInfo.MoreText ?? string.Empty, //todo: can be null
                            codeLensMethodInsightsProvider,
                            functionDeclaration.DeclaredElement,
                            null // icon was set already on previous step inside CodeLensProvider.buildCodeLens()
                        )
                    );
                }
            }
        }
    }
}