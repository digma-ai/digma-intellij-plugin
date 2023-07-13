using Digma.Rider.Protocol;
using JetBrains.ProjectModel;
using JetBrains.Util;

namespace Digma.Rider.Highlighting
{
    [SolutionComponent]
    public class CodeLensMethodInsightsProvider5 : BaseMethodInsightsProvider
    {
        public CodeLensMethodInsightsProvider5(
            ISolution solution,
            ILogger logger,
            ShowToolWindowHost showToolWindowHost
        ) : base(solution, logger, showToolWindowHost)
        { }
        
        public override string ProviderId => "MethodInsightsProvider5";
        public override string DisplayName => "Method Insights Provider_5 Hints";
        
    }
}