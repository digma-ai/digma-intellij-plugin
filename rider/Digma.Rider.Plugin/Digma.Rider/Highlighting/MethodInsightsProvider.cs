using System.Collections.Generic;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Daemon.CodeInsights;
using JetBrains.ReSharper.Feature.Services.Navigation;
using JetBrains.ReSharper.Psi;
using JetBrains.Rider.Model;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Highlighting
{
    [SolutionComponent]
    public class MethodInsightsProvider : ICodeInsightsProvider
    {
        private readonly ILogger _logger;

        public MethodInsightsProvider(ILogger logger)
        {
            _logger = logger;
        }

        public bool IsAvailableIn(ISolution solution)
        {
            return true;
        }

        public void OnClick(CodeInsightsHighlighting highlighting, ISolution solution)
        {
            Log(_logger, "OnClick invoked for {0}", highlighting.DeclaredElement.ShortName);
            using (CompilationContextCookie.GetExplicitUniversalContextIfNotSet())
            {
                highlighting.DeclaredElement.Navigate(true);
            }
        }

        public void OnExtraActionClick(CodeInsightsHighlighting highlighting, string actionId, ISolution solution)
        {
            Log(_logger, "OnExtraActionClick invoked for {0}", highlighting.DeclaredElement);
            using (CompilationContextCookie.GetExplicitUniversalContextIfNotSet())
            {
                highlighting.DeclaredElement.Navigate(true);
            }
        }

        public string ProviderId => nameof(MethodInsightsProvider);
        public string DisplayName => "Method Hints";
        public CodeLensAnchorKind DefaultAnchor => CodeLensAnchorKind.Top;

        public ICollection<CodeLensRelativeOrdering> RelativeOrderings => new CodeLensRelativeOrdering[]
            { new CodeLensRelativeOrderingFirst() };
    }
}