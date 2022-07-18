using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Psi.Caches;
using JetBrains.Util;

namespace Digma.Rider.Protocol
{
    
    //wait for psei caches and refresh incomplete documents.
    
    // [SolutionComponent] : disabled , just an example, WaitForCaches throws exception.
    public class WaitForPsiCache
    {
        public WaitForPsiCache(Lifetime lifetime,ILogger logger,ISolution solution,IPsiCaches caches,CodeObjectsHost codeObjectsHost)
        {

            //WaitForCaches always throws exception
            // SingleThreadScheduler.RunOnSeparateThread(lifetime, "wait for psi caches", scheduler =>
            // {
            //     Log(logger, "Waiting for psi caches");
            //     new PsiCachesAwaiter(solution,caches).WaitForCaches("waiting for psi caches");
            // }).Queue(codeObjectsHost.RefreshAllIncompleteDocuments);
        }
    }
}