using System.Collections.Generic;
using Digma.Rider.Protocol;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Daemon.CodeInsights;
using JetBrains.ReSharper.Feature.Services.Navigation;
using JetBrains.ReSharper.Psi;
using JetBrains.Rider.Model;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Highlighting
{
    public abstract class BaseMethodInsightsProvider : ICodeInsightsProvider
    {
        private readonly ISolution _solution;
        private readonly ILogger _logger;
        private readonly ShowToolWindowHost _showToolWindowHost;

        protected BaseMethodInsightsProvider(ISolution solution,ILogger logger,ShowToolWindowHost showToolWindowHost)
        {
            _solution = solution;
            _logger = logger;
            _showToolWindowHost = showToolWindowHost;
        }

        public bool IsAvailableIn(ISolution solution)
        {
            return _solution == solution;
        }

        public void OnClick(CodeInsightHighlightInfo highlightInfo, ISolution solution)
        {
            Log(_logger, "OnClick invoked for {0}", highlightInfo.CodeInsightsHighlighting.DeclaredElement.ShortName);
            NavigateToMethod(highlightInfo.CodeInsightsHighlighting);
        }

        public void OnExtraActionClick(CodeInsightHighlightInfo highlightInfo, string actionId, ISolution solution)
        {
            Log(_logger, "OnExtraActionClick invoked for {0}", highlightInfo.CodeInsightsHighlighting.DeclaredElement);
            NavigateToMethod(highlightInfo.CodeInsightsHighlighting);
        }

        //todo: this method implements the interface of 2022.2, can be removed after dropping support for 2022.2
        public void OnClick(CodeInsightsHighlighting highlighting, ISolution solution)
        {
            Log(_logger, "OnClick invoked for {0}", highlighting.DeclaredElement.ShortName);
            NavigateToMethod(highlighting);
        }

        //todo: this method implements the interface of 2022.2, can be removed after dropping support for 2022.2
        public void OnExtraActionClick(CodeInsightsHighlighting highlighting, string actionId, ISolution solution)
        {
            Log(_logger, "OnExtraActionClick invoked for {0}", highlighting.DeclaredElement);
            NavigateToMethod(highlighting);
        }

        public abstract string ProviderId { get; }
        public abstract string DisplayName { get; }

        private void NavigateToMethod(CodeInsightsHighlighting highlighting)
        {
            using (CompilationContextCookie.GetExplicitUniversalContextIfNotSet())
            {
                highlighting.DeclaredElement.Navigate(true);
            }
            _showToolWindowHost.ShowToolWindow();
        }

        
        public CodeLensAnchorKind DefaultAnchor => CodeLensAnchorKind.Top;
        public abstract ICollection<CodeLensRelativeOrdering> RelativeOrderings { get; }
    }
}