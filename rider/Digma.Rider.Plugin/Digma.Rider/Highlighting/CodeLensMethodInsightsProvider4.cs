using System.Collections.Generic;
using Digma.Rider.Protocol;
using JetBrains.ProjectModel;
using JetBrains.Rider.Model;
using JetBrains.Util;

namespace Digma.Rider.Highlighting
{
    [SolutionComponent]
    public class CodeLensMethodInsightsProvider4 : BaseMethodInsightsProvider
    {
        public CodeLensMethodInsightsProvider4(
            ISolution solution,
            ILogger logger,
            ShowToolWindowHost showToolWindowHost
        ) : base(solution, logger, showToolWindowHost)
        { }
        
        public override string ProviderId => "MethodInsightsProvider4";
        public override string DisplayName => "Method Insights Provider_4 Hints";

        public override ICollection<CodeLensRelativeOrdering> RelativeOrderings => new CodeLensRelativeOrdering[]
            { new CodeLensRelativeOrderingFirst() };
    }
}