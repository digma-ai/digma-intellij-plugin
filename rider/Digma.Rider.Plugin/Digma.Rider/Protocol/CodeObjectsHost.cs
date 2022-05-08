using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.RdBackend.Common.Features;
using JetBrains.RdBackend.Common.Features.Components;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Rider.Backend.Features.SolutionAnalysis;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Protocol
{
    [SolutionComponent]
    public class CodeObjectsHost
    {
        // A dictionary to help us call reanalyze for specific PsiSourceFile when code lens are available.
        private readonly Dictionary<string, IPsiSourceFile> _docToPsiSourceFile =
            new Dictionary<string, IPsiSourceFile>();

        private readonly ShellRdDispatcher _shellRdDispatcher;
        private readonly ILogger _logger;
        private readonly CodeObjectsModel _codeObjectsModel;

        public CodeObjectsHost(Lifetime lifetime, ISolution solution,
            RiderSolutionAnalysisServiceImpl riderSolutionAnalysisService,
            ShellRdDispatcher shellRdDispatcher,
            ILogger logger)
        {
            _shellRdDispatcher = shellRdDispatcher;
            _logger = logger;
            _codeObjectsModel = solution.GetProtocolSolution().GetCodeObjectsModel();

            _codeObjectsModel.Reanalyze.Advise(lifetime, filePath =>
            {
                using (ReadLockCookie.Create())
                {
                    //usually we should have the PsiSourceFile so we can call ReanalyzeFile.
                    //if not found fallback to ReanalyzeAll 
                    var psiSourceFile = _docToPsiSourceFile.TryGetValue(filePath);
                    if (psiSourceFile != null && psiSourceFile.IsValid())
                    {
                        Log(_logger,"Found PsiSourceFile for {0}, Calling ReanalyzeFile {0}",filePath,psiSourceFile);
                        riderSolutionAnalysisService.ReanalyzeFile(psiSourceFile);
                    }
                    else
                    {
                        Log(_logger,"Could not find PsiSourceFile for {0}, Calling ReanalyzeAll",filePath);
                        riderSolutionAnalysisService.ReanalyzeAll();
                    }
                }
            });
        }


        [CanBeNull]
        public RiderCodeLensInfo GetRiderCodeLensInfo([NotNull] string fqn)
        {
            using (ReadLockCookie.Create())
            {
                _codeObjectsModel.CodeLens.TryGetValue(fqn, out var lensInfo);
                return lensInfo;
            }
        }


        public void AddOpenChangeDocument([NotNull] IPsiSourceFile psiSourceFile, [NotNull] Document document)
        {
            if (document.Methods.IsEmpty())
                return;

            var documentPath = document.Path;

            if (document.Methods.IsEmpty())
            {
                return;
            }
            
            //todo: consider removing the document from the protocol when the frontend consumes it.
            // then avoid notifying the frontend of changes if the code objects didn't change by keeping
            // and checking last update timestamp, the cache keeps a timestamp of last update.
            
            //relying on Equals method.
            //if the document is still in the protocol and hasn't change then do nothing.
            //if the new document is different then remove it.
            if (document.CheckEquals(_codeObjectsModel.Documents.TryGetValue(documentPath)))
            {
                Log(_logger,"Document {0} already exists and is equals to the new one",documentPath);
                //if the document exists in the protocol then update the PsiSourceFile mapping, it can be a new instance of PsiSourceFile
                //remove before add because it may be a new IPsiSourceFile instance
                _docToPsiSourceFile.Remove(documentPath);
                _docToPsiSourceFile.Add(documentPath, psiSourceFile);
                return;
            }

            
            //remove before add because it may be a new IPsiSourceFile instance
            _docToPsiSourceFile.Remove(documentPath);
            _docToPsiSourceFile.Add(documentPath, psiSourceFile);
            //remove before add in case there is a non equals document already
            Log(_logger,"Pushing Document {0} to the protocol",documentPath);
            _shellRdDispatcher.UnguardedScheduler.Queue(() =>
            {
                _codeObjectsModel.Documents.Remove(documentPath);
                _codeObjectsModel.Documents.Add(documentPath, document);
                NotifyOnDocumentCodeObjects(documentPath);
            });
        }


        private void NotifyOnDocumentCodeObjects([NotNull] string documentPath)
        {
            _shellRdDispatcher.UnguardedScheduler.Queue(() =>
            {
                //maybe there where no findings for this document, don't change the value in that case
                if (_codeObjectsModel.Documents.ContainsKey(documentPath))
                {
                    Log(_logger,"NotifyDocumentOpened for {0}",documentPath);
                    _codeObjectsModel.DocumentAnalyzed.Fire(documentPath);
                }
            });
        }
    }
}