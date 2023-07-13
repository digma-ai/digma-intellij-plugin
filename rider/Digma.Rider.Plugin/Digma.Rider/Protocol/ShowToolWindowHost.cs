using JetBrains.ProjectModel;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

#if (PROFILE_2023_2) // FIX_WHEN_MIN_IS_232
using JetBrains.ReSharper.Feature.Services.Protocol;
#else
using JetBrains.RdBackend.Common.Features;
#endif

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
            _showToolWindowModel.ShowToolWindow();
        }
       
        
    }
    
}