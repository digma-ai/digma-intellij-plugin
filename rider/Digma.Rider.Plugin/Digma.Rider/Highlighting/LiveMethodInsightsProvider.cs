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

    public class LiveMethodInsightsProvider : BaseMethodInsightsProvider
    {
        public LiveMethodInsightsProvider(ISolution solution, ILogger logger, ShowToolWindowHost showToolWindowHost,
            LanguageServiceHost languageServiceHost)
            : base(solution, logger, showToolWindowHost, languageServiceHost)
        {
        }


        public override string ProviderId => nameof(LiveMethodInsightsProvider);
        public override string DisplayName => "Live Method Hints";

        public override CodeVisionAnchorKind DefaultAnchor => CodeVisionAnchorKind.Top;

        public override ICollection<CodeVisionRelativeOrdering> RelativeOrderings => new CodeVisionRelativeOrdering[]
            { new CodeVisionRelativeOrderingFirst() };
    }
}