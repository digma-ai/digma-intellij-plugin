using JetBrains.Application.Parts;
using JetBrains.ProjectModel;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

using JetBrains.ReSharper.Feature.Services.Protocol;

namespace Digma.Rider.Protocol
{
#if (PROFILE_2024_3)
    [SolutionComponent(Instantiation.ContainerAsyncPrimaryThread)]
#else
    [SolutionComponent]
#endif    

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