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

    public class ScaleFactorMethodInsightsProvider : BaseMethodInsightsProvider
    {
        public ScaleFactorMethodInsightsProvider(ISolution solution, ILogger logger,
            ShowToolWindowHost showToolWindowHost,
            LanguageServiceHost languageServiceHost)
            : base(solution, logger, showToolWindowHost, languageServiceHost)
        {
        }


        public override string ProviderId => nameof(ScaleFactorMethodInsightsProvider);
        public override string DisplayName => "Scale Factor Method Hints";
    }
}