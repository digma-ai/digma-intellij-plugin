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

        public void OnClick(CodeInsightsHighlighting highlighting, ISolution solution)
        {
            Log(_logger, "OnClick invoked for {0}", highlighting.DeclaredElement.ShortName);
            NavigateToMethod(highlighting);
        }

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