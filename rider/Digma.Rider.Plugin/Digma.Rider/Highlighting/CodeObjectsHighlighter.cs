using System;
using Digma.Rider.Discovery;
using Digma.Rider.Protocol;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Daemon.CodeInsights;
using JetBrains.ReSharper.Feature.Services.Daemon;
using JetBrains.ReSharper.Features.Internal.Resources;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Rider.Backend.Platform.Icons;
using JetBrains.UI.Icons;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Highlighting
{
    [ElementProblemAnalyzer(typeof(ICSharpFunctionDeclaration))]
    public class CodeObjectsHighlighter : ElementProblemAnalyzer<ICSharpFunctionDeclaration>
    {
        private readonly ILogger _logger;
        private readonly CodeObjectsHost _codeObjectsHost;
        private readonly ErrorHotspotMethodInsightsProvider _errorHotspotMethodInsightsProvider;
        private readonly UsageMethodInsightsProvider _usageMethodInsightsProvider;
        private readonly IconHost _iconHost;

        public CodeObjectsHighlighter(ILogger logger,
            CodeObjectsHost codeObjectsHost,
            ISolution solution,
            ErrorHotspotMethodInsightsProvider errorHotspotMethodInsightsProvider,
            UsageMethodInsightsProvider usageMethodInsightsProvider,IconHost iconHost)
        {
            _logger = logger;
            _codeObjectsHost = codeObjectsHost;
            _errorHotspotMethodInsightsProvider = errorHotspotMethodInsightsProvider;
            _usageMethodInsightsProvider = usageMethodInsightsProvider;
            _iconHost = iconHost;
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
                Log(_logger, "Found {0} code lens for method {1}", methodCodeLenses.Count,methodFqn);
                foreach (var riderCodeLensInfo in methodCodeLenses)
                {
                    Log(_logger, "Installing code lens for code method {0}: {1}", methodFqn,riderCodeLensInfo);

                    ICodeInsightsProvider codeInsightsProvider =
                        riderCodeLensInfo.Type.Equals(CodeLensType.ErrorHotspot)
                            ? _errorHotspotMethodInsightsProvider
                            : _usageMethodInsightsProvider;
                    
                    var icon =
                        riderCodeLensInfo.Type.Equals(CodeLensType.ErrorHotspot)
                            ? _iconHost.Transform(Icons.ExclMark)
                            : null;
                    
                    highlightingConsumer.AddHighlighting(
                        new CodeInsightsHighlighting(
                            functionDeclaration.GetNameDocumentRange(),
                            riderCodeLensInfo.LensText ?? throw new InvalidOperationException("LensText must not be null"),
                            riderCodeLensInfo.LensTooltip ?? string.Empty, //todo: can be null
                            riderCodeLensInfo.MoreText ?? string.Empty, //todo: can be null
                            codeInsightsProvider,
                            functionDeclaration.DeclaredElement,icon)
                    );
                }

            }
        }

    }
}