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

    public class SlowEndpointMethodInsightsProvider : BaseMethodInsightsProvider
    {
        public SlowEndpointMethodInsightsProvider(ISolution solution,ILogger logger,ShowToolWindowHost showToolWindowHost) 
            : base(solution,logger,showToolWindowHost)
        {}
        

        public override string ProviderId => nameof(SlowEndpointMethodInsightsProvider);
        public override string DisplayName => "Slow Endpoint Method Hints";
        
    }
}