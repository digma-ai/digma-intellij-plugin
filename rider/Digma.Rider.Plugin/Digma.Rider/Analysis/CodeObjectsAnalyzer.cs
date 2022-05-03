using System;
using Digma.Rider.Protocol;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Daemon;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.Util;

namespace Digma.Rider.Analysis
{
    [ElementProblemAnalyzer(new Type[]{typeof(ICSharpFile)})]
    public class CodeObjectsAnalyzer : ElementProblemAnalyzer<ICSharpFile>
    {
        private readonly ILogger _logger;
        private readonly CodeObjectsAnalysisHost _codeObjectsAnalysisHost;
        private readonly ISolution _solution;
        private readonly MethodInsightsProvider _methodInsightsProvider;

        public CodeObjectsAnalyzer(ILogger logger, 
                        CodeObjectsAnalysisHost codeObjectsAnalysisHost,
                        ISolution solution,
                        MethodInsightsProvider methodInsightsProvider)
        {
            _logger = logger;
            _codeObjectsAnalysisHost = codeObjectsAnalysisHost;
            _solution = solution;
            _methodInsightsProvider = methodInsightsProvider;
        }

        protected override void Run(ICSharpFile element,
            ElementProblemAnalyzerData data,
            IHighlightingConsumer consumer)
        {
            //todo: differ between analyzing and highlighting
            //todo: analysis happens too many times, sometimes 2-3 times in a role. maybe keep a timestamp per file and don't analyze too often
            if (_codeObjectsAnalysisHost.FilesToReanalyze.Contains(element))
            {
                var elementProcessor = new CodeObjectsHighlightingProcessor(element,_codeObjectsAnalysisHost,consumer,_methodInsightsProvider);
                element.ProcessDescendants(elementProcessor);
                _codeObjectsAnalysisHost.FilesToReanalyze.Remove(element);
            }
            else
            {
                var elementProcessor = new CodeObjectsAnalysisProcessor(element,_codeObjectsAnalysisHost);
                element.ProcessDescendants(elementProcessor);
            
                var fileFqn = Identities.ComputeFilePath(element);
                _codeObjectsAnalysisHost.NotifyFileOpened(fileFqn);
            }
            
        }
    }
}