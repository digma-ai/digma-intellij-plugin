using Digma.Rider.Protocol;
using Digma.Rider.Util;
using JetBrains.Lifetimes;
using JetBrains.Platform.RdFramework.Impl;
using JetBrains.ProjectModel;
using JetBrains.RdBackend.Common.Features;
using JetBrains.ReSharper.Feature.Services.Daemon;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Highlighting
{
    [SolutionComponent]
    public class DaemonReanalyze
    {
        public DaemonReanalyze(Lifetime lifetime, ISolution solution,
            ShellRdDispatcher shellRdDispatcher,
            ILogger logger, IDaemon daemon)
        {
            var codeObjectsModel = solution.GetProtocolSolution().GetCodeObjectsModel();

            codeObjectsModel.Reanalyze.Advise(lifetime, psiFileId =>
            {
                Log(logger, "Reanalyze called for {0}", psiFileId);
                shellRdDispatcher.Queue(() =>
                {
                    using (ReadLockCookie.Create())
                    {
                        //if PsiSourceFile not found fallback to Invalidate() for all daemons 
                        var psiSourceFile = PsiUtils.FindPsiSourceFile(psiFileId,solution);
                        if (psiSourceFile != null && psiSourceFile.IsValid())
                        {
                            Log(logger, "Found PsiSourceFile for {0}, Calling Invalidate(psiSourceFile.Document) {1}",psiFileId, psiSourceFile);
                            daemon.Invalidate(psiSourceFile.Document);
                        }
                        else
                        {
                            Log(logger, "Could not find PsiSourceFile {0}. calling Calling Invalidate() for all files.", psiFileId);
                            daemon.Invalidate();
                        }
                    }
                });
            });

        }
    }
}