using System.Collections.Generic;
using System.Linq;
using Digma.Rider.Discovery;
using Digma.Rider.Protocol;
using JetBrains.Annotations;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Daemon.CodeInsights;
using JetBrains.ReSharper.Feature.Services.Navigation;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
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
        private readonly LanguageServiceHost _languageServiceHost;

        protected BaseMethodInsightsProvider(ISolution solution, ILogger logger, ShowToolWindowHost showToolWindowHost,
            LanguageServiceHost languageServiceHost)
        {
            _solution = solution;
            _logger = logger;
            _showToolWindowHost = showToolWindowHost;
            _languageServiceHost = languageServiceHost;
        }

        public bool IsAvailableIn(ISolution solution)
        {
            return _solution == solution;
        }

#if (PROFILE_2025_2)
        public void OnClick(CodeInsightHighlightInfo highlightInfo, ISolution solution, CodeInsightsClickInfo clickInfo)
        {
            Log(_logger, "OnClick invoked for {0}", highlightInfo.CodeInsightsHighlighting.DeclaredElement.ShortName);
            NavigateToMethod(highlightInfo.CodeInsightsHighlighting);
        }
#endif
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


        public abstract string ProviderId { get; }
        public abstract string DisplayName { get; }

        private void NavigateToMethod(CodeInsightsHighlighting highlighting)
        {
            Log(_logger, "NavigateToMethod called with highlighting for {0}", highlighting.DeclaredElement);

            using (CompilationContextCookie.GetExplicitUniversalContextIfNotSet())
            {
                Log(_logger, "Navigating to method {0}", highlighting.DeclaredElement);
                highlighting.DeclaredElement.Navigate(true);
            }

            var codeLens = FindCodelensInHighlighting(highlighting);
            if (codeLens == null)
            {
                Log(_logger, "No code lens found for method {0}, showinf tool window with no codelens", highlighting.DeclaredElement);
                _showToolWindowHost.ShowToolWindow(null);
            }
            else
            {
                Log(_logger, "Showing tool window with code lens for method {0}, [{1}]", highlighting.DeclaredElement,
                    codeLens);
                _showToolWindowHost.ShowToolWindow(codeLens);

            }
        }

        [CanBeNull]
        private RiderCodeLensInfo FindCodelensInHighlighting(CodeInsightsHighlighting highlighting)
        {
            Log(_logger, "Checking if highlighting.Entry is TextCodeVisionEntry {0}", highlighting.Entry);
            if (highlighting.Entry is TextCodeVisionEntry textCodeVisionEntry)
            {
                Log(_logger, "Searching for method in highlighting for {0}", highlighting.DeclaredElement);

                var functionDeclaration = highlighting.DeclaredElement?
                    .GetDeclarations()
                    .OfType<ICSharpFunctionDeclaration>()
                    .FirstOrDefault();

                if (functionDeclaration == null)
                {
                    Log(_logger, "No method found in highlighting for {0}", highlighting.DeclaredElement);
                    return null;
                }

                Log(_logger, "Found method to in highlighting {0}", functionDeclaration);

                var methodFqn = Identities.ComputeFqn(functionDeclaration);
                var lensTitle = textCodeVisionEntry.Text;
                var methodCodeLenses = _languageServiceHost.GetRiderCodeLensInfo(methodFqn);
                if (methodCodeLenses == null)
                {
                    Log(_logger, "No code lens found for method {0}", functionDeclaration);
                    return null;
                }

                Log(_logger, "Searching for code lens [{0}] for method {1} , code lens list size [{2}]", lensTitle,
                    functionDeclaration, methodCodeLenses.Count);
                var codeLens = methodCodeLenses.FirstOrDefault(info => info.LensTitle == lensTitle);
                if (codeLens != null)
                {
                    Log(_logger, "Found code lens [{0}] for method {1}, [{2}]", lensTitle, functionDeclaration,
                        codeLens);
                    return codeLens;
                }
                else
                {
                    Log(_logger, "No code lens [{0}] found for method {1}", lensTitle, functionDeclaration);
                    return null;
                }
            }
            else
            {
                Log(_logger, "highlighting.Entry is NOT TextCodeVisionEntry {0}", highlighting.Entry);
                return null;
            }
        }

        public virtual CodeVisionAnchorKind DefaultAnchor => CodeVisionAnchorKind.Top;

        public virtual ICollection<CodeVisionRelativeOrdering> RelativeOrderings => new CodeVisionRelativeOrdering[]
            { new CodeVisionRelativeOrderingAfter(nameof(LiveMethodInsightsProvider)) };
    }
}