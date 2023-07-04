using Digma.Rider.Protocol;
using JetBrains.ProjectModel;
using JetBrains.Util;

namespace Digma.Rider.Highlighting
{
    [SolutionComponent]
    public class CodeLensMethodInsightsProvider2 : BaseMethodInsightsProvider
    {
        public CodeLensMethodInsightsProvider2(
            ISolution solution,
            ILogger logger,
            ShowToolWindowHost showToolWindowHost
        ) : base(solution, logger, showToolWindowHost)
        {
        }
        
        public override string ProviderId => "MethodInsightsProvider2";
        public override string DisplayName => "Method Insights Provider_2 Hints";

    }
}