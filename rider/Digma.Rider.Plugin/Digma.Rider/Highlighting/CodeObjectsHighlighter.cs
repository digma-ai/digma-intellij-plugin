using Digma.Rider.Protocol;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Daemon;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Highlighting
{
    [ElementProblemAnalyzer(typeof(ICSharpFile))]
    public class CodeObjectsHighlighter : ElementProblemAnalyzer<ICSharpFile>
    {
        private readonly ILogger _logger;
        private readonly CodeObjectsHost _codeObjectsHost;
        private readonly MethodInsightsProvider _methodInsightsProvider;

        public CodeObjectsHighlighter(ILogger logger,
            CodeObjectsHost codeObjectsHost,
            ISolution solution,
            MethodInsightsProvider methodInsightsProvider)
        {
            _logger = logger;
            _codeObjectsHost = codeObjectsHost;
            _methodInsightsProvider = methodInsightsProvider;
        }

        protected override void Run(ICSharpFile element,
            ElementProblemAnalyzerData data,
            IHighlightingConsumer consumer)
        {
            Log(_logger, "CodeObjectsHighlighter.Run invoked for {0}", element.GetSourceFile());
            var elementProcessor =
                new CodeObjectsHighlightingProcessor(_codeObjectsHost, consumer, _methodInsightsProvider,_logger);
            element.ProcessDescendants(elementProcessor);
        }
    }
}