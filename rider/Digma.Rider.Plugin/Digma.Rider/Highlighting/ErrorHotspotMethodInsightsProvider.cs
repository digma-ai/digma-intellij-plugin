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

    public class ErrorHotspotMethodInsightsProvider : BaseMethodInsightsProvider
    {
        public ErrorHotspotMethodInsightsProvider(ISolution solution, ILogger logger,
            ShowToolWindowHost showToolWindowHost,
            LanguageServiceHost languageServiceHost)
            : base(solution, logger, showToolWindowHost, languageServiceHost)
        {
        }


        public override string ProviderId => nameof(ErrorHotspotMethodInsightsProvider);
        public override string DisplayName => "Error Hotspot Method Hints";
    }
}