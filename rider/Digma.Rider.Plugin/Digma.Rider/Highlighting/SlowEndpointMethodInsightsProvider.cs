using System.Collections.Generic;
using Digma.Rider.Protocol;
using JetBrains.ProjectModel;
using JetBrains.Rider.Model;
using JetBrains.Util;

namespace Digma.Rider.Highlighting
{
    [SolutionComponent]
    public class SlowEndpointMethodInsightsProvider : BaseMethodInsightsProvider
    {
        public SlowEndpointMethodInsightsProvider(ISolution solution,ILogger logger,ShowToolWindowHost showToolWindowHost) 
            : base(solution,logger,showToolWindowHost)
        {}
        

        public override string ProviderId => nameof(SlowEndpointMethodInsightsProvider);
        public override string DisplayName => "Slow Endpoint Method Hints";
        
        public override ICollection<CodeLensRelativeOrdering> RelativeOrderings => new CodeLensRelativeOrdering[]
            { new CodeLensRelativeOrderingFirst() };
        
    }
}