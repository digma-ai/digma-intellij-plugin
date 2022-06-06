using JetBrains.Core;
using JetBrains.ProjectModel;
using JetBrains.RdBackend.Common.Features;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Protocol
{
    [SolutionComponent]
    public class ShowToolWindowHost
    {
        private readonly ILogger _logger;
        private readonly ShowToolWindowModel _showToolWindowModel;

        public ShowToolWindowHost(
            ISolution solution,
            ILogger logger)
        {
            _logger = logger;
            _showToolWindowModel = solution.GetProtocolSolution().GetShowToolWindowModel();
        }

        public void ShowToolWindow()
        {
            Log(_logger, "ShowToolWindow called");
            _showToolWindowModel.ShowToolWindow.Fire(Unit.Instance);
        }
       
        
    }
    
}