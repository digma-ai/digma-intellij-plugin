using System.Collections.Generic;
using Digma.Rider.Protocol;
using JetBrains.ProjectModel;
using JetBrains.Rider.Model;
using JetBrains.Util;

namespace Digma.Rider.Highlighting
{
    [SolutionComponent]
    public class LiveMethodInsightsProvider : BaseMethodInsightsProvider
    {
        public LiveMethodInsightsProvider(ISolution solution,ILogger logger,ShowToolWindowHost showToolWindowHost) 
            : base(solution,logger,showToolWindowHost)
        {}


        public override string ProviderId => nameof(LiveMethodInsightsProvider);
        public override string DisplayName => "Live Method Hints";
        
        public override CodeVisionAnchorKind DefaultAnchor => CodeVisionAnchorKind.Top;
        public override ICollection<CodeVisionRelativeOrdering> RelativeOrderings => new CodeVisionRelativeOrdering[]
            { new CodeVisionRelativeOrderingFirst() };
        
    }
}