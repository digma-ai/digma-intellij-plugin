using System.Collections.Generic;
using Digma.Rider.Protocol;
using JetBrains.ProjectModel;
using JetBrains.Rider.Model;
using JetBrains.Util;

namespace Digma.Rider.Highlighting
{
    [SolutionComponent]
    public class UsageMethodInsightsProvider : BaseMethodInsightsProvider
    {
        public UsageMethodInsightsProvider(ISolution solution,ILogger logger,ShowToolWindowHost showToolWindowHost)
            : base(solution,logger,showToolWindowHost)
        {}

        public override string ProviderId => nameof(UsageMethodInsightsProvider);
        public override string DisplayName => "Usage Method Hints";
        
    #if (PROFILE_2022_3) // FIX_WHEN_MIN_IS_232
        public new ICollection<CodeLensRelativeOrdering> RelativeOrderings => new CodeLensRelativeOrdering[]
            { new CodeLensRelativeOrderingAfter(nameof(ErrorHotspotMethodInsightsProvider)) };
    #else
       public new ICollection<CodeVisionRelativeOrdering> RelativeOrderings => new CodeVisionRelativeOrdering[]
            { new CodeVisionRelativeOrderingAfter(nameof(ErrorHotspotMethodInsightsProvider)) };
    #endif
        
    }
}