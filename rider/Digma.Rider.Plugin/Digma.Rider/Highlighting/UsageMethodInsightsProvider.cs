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
        
        public override ICollection<CodeLensRelativeOrdering> RelativeOrderings => new CodeLensRelativeOrdering[]
            { new CodeLensRelativeOrderingAfter(nameof(ErrorHotspotMethodInsightsProvider)) };
    }
}