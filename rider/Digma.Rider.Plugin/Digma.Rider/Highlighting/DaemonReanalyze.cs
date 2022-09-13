using Digma.Rider.Protocol;
using JetBrains.Lifetimes;
using JetBrains.Platform.RdFramework.Impl;
using JetBrains.ProjectModel;
using JetBrains.RdBackend.Common.Features;
using JetBrains.ReSharper.Feature.Services.Daemon;
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
            CodeObjectsHost codeObjectsHost,
            ILogger logger, IDaemon daemon)
        {
            var codeObjectsModel = solution.GetProtocolSolution().GetCodeObjectsModel();

            codeObjectsModel.Reanalyze.Advise(lifetime, documentKey =>
            {
                Log(logger, "Reanalyze called for {0}", documentKey);
                shellRdDispatcher.Queue(() =>
                {
                    using (ReadLockCookie.Create())
                    {
                        //usually we should have the PsiSourceFile so we can call Invalidate(psiSourceFile.Document).
                        //if PsiSourceFile not found fallback to Invalidate() 
                        var psiSourceFile = codeObjectsHost.DocPathToPsiSourceFile.TryGetValue(documentKey);
                        if (psiSourceFile != null && psiSourceFile.IsValid())
                        {
                            Log(logger, "Found PsiSourceFile in local map for {0}, Calling Invalidate(psiSourceFile.Document) {0}",documentKey, psiSourceFile);
                            daemon.Invalidate(psiSourceFile.Document);
                        }
                        else
                        {
                            Log(logger, "Could not find PsiSourceFile {0} in local map. calling Calling Invalidate()", documentKey);
                            daemon.Invalidate();
                        }
                    }
                });
            });

        }
    }
}