using Digma.Rider.Protocol;
using JetBrains.ProjectModel;
using JetBrains.Util;

namespace Digma.Rider.Highlighting
{
    [SolutionComponent]
    public class ErrorHotspotMethodInsightsProvider : BaseMethodInsightsProvider
    {
        public ErrorHotspotMethodInsightsProvider(ISolution solution,ILogger logger,ShowToolWindowHost showToolWindowHost) 
            : base(solution,logger,showToolWindowHost)
        {}


        public override string ProviderId => nameof(ErrorHotspotMethodInsightsProvider);
        public override string DisplayName => "Error Hotspot Method Hints";
        
    }
}