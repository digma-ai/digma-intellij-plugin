using System.Collections.Generic;
using Digma.Rider.Protocol;
using JetBrains.Application.Parts;
using JetBrains.ProjectModel;
using JetBrains.Rider.Model;
using JetBrains.Util;

namespace Digma.Rider.Highlighting
{
#if (PROFILE_2024_3)
    [SolutionComponent(Instantiation.ContainerAsyncPrimaryThread)]
#else
    [SolutionComponent]
#endif

    public class UsageMethodInsightsProvider : BaseMethodInsightsProvider
    {
        public UsageMethodInsightsProvider(ISolution solution, ILogger logger, ShowToolWindowHost showToolWindowHost,
            LanguageServiceHost languageServiceHost)
            : base(solution, logger, showToolWindowHost, languageServiceHost)
        {
        }

        public override string ProviderId => nameof(UsageMethodInsightsProvider);
        public override string DisplayName => "Usage Method Hints";

        public new ICollection<CodeVisionRelativeOrdering> RelativeOrderings => new CodeVisionRelativeOrdering[]
            { new CodeVisionRelativeOrderingAfter(nameof(ErrorHotspotMethodInsightsProvider)) };
    }
}