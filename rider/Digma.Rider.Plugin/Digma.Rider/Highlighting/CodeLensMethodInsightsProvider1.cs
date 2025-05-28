using Digma.Rider.Protocol;
using JetBrains.Application.Parts;
using JetBrains.ProjectModel;
using JetBrains.Util;

namespace Digma.Rider.Highlighting
{
#if (PROFILE_2024_3)
    [SolutionComponent(Instantiation.ContainerAsyncPrimaryThread)]
#else
    [SolutionComponent]
#endif

    public class CodeLensMethodInsightsProvider1 : BaseMethodInsightsProvider
    {
        public CodeLensMethodInsightsProvider1(
            ISolution solution,
            ILogger logger,
            ShowToolWindowHost showToolWindowHost,
            LanguageServiceHost languageServiceHost
        ) : base(solution, logger, showToolWindowHost, languageServiceHost)
        {
        }

        public override string ProviderId => "MethodInsightsProvider1";
        public override string DisplayName => "Method Insights Provider_1 Hints";
    }
}