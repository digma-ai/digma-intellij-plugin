using Digma.Rider.Protocol;
using JetBrains.ProjectModel;
using JetBrains.Util;

namespace Digma.Rider.Highlighting
{
    [SolutionComponent]
    public class ScaleFactorMethodInsightsProvider : BaseMethodInsightsProvider
    {
        public ScaleFactorMethodInsightsProvider(ISolution solution,ILogger logger,ShowToolWindowHost showToolWindowHost) 
            : base(solution,logger,showToolWindowHost)
        {}


        public override string ProviderId => nameof(ScaleFactorMethodInsightsProvider);
        public override string DisplayName => "Scale Factor Method Hints";
        
    }
}