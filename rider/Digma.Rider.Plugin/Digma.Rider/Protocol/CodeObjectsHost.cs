using System;
using System.Collections.Generic;
using Digma.Rider.Discovery;
using JetBrains.Annotations;
using JetBrains.Lifetimes;
using JetBrains.Platform.RdFramework.Impl;
using JetBrains.ProjectModel;
using JetBrains.Rd.Tasks;
using JetBrains.RdBackend.Common.Features;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Rider.Backend.Features.SolutionAnalysis;
using JetBrains.Util;
using JetBrains.Util.Extension;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Protocol
{
    [SolutionComponent]
    public class CodeObjectsHost
    {
        // A dictionary to help us call reanalyze for specific PsiSourceFile when code lens are available.
        private readonly Dictionary<string, IPsiSourceFile> _docPathToPsiSourceFile =
            new Dictionary<string, IPsiSourceFile>();

        private readonly ShellRdDispatcher _shellRdDispatcher;
        private readonly ILogger _logger;
        private readonly CodeObjectsModel _codeObjectsModel;
        private readonly CodeObjectsCache _codeObjectsCache;

        public CodeObjectsHost(Lifetime lifetime, ISolution solution,
            RiderSolutionAnalysisServiceImpl riderSolutionAnalysisService,
            ShellRdDispatcher shellRdDispatcher,
            CodeObjectsCache codeObjectsCache,
            ILogger logger)
        {
            _shellRdDispatcher = shellRdDispatcher;
            _codeObjectsCache = codeObjectsCache;
            _logger = logger;
            _codeObjectsModel = solution.GetProtocolSolution().GetCodeObjectsModel();


            _codeObjectsModel.ReanalyzeAll.Advise(lifetime, _ =>
            {
                using (ReadLockCookie.Create())
                {
                    Log(_logger, "Calling ReanalyzeAll");
                    riderSolutionAnalysisService.ReanalyzeAll();
                }
            });
            
            _codeObjectsModel.Reanalyze.Advise(lifetime, documentKey =>
            {
                Log(_logger, "Reanalyze called for {0}",documentKey);
                using (ReadLockCookie.Create())
                {
                    //usually we should have the PsiSourceFile so we can call ReanalyzeFile.
                    //if not found fallback to ReanalyzeAll 
                    var psiSourceFile = _docPathToPsiSourceFile.TryGetValue(documentKey);
                    if (psiSourceFile != null && psiSourceFile.IsValid())
                    {
                        Log(_logger,"Found PsiSourceFile in local map for {0}, Calling ReanalyzeFile {0}",documentKey,psiSourceFile);
                        riderSolutionAnalysisService.ReanalyzeFile(psiSourceFile);
                    }
                    else
                    {
                        Log(_logger,"Could not find PsiSourceFile  in local map for {0}, Calling ReanalyzeAll",documentKey);
                        riderSolutionAnalysisService.ReanalyzeAll();
                    }
                }
            });


            _codeObjectsModel.GetWorkspaceUris.Set((_, list) =>
            {
                RdTask<List<CodeObjectIdUriPair>> result = new RdTask<List<CodeObjectIdUriPair>>();
                using (ReadLockCookie.Create())
                {
                    try
                    {
                        var uris = new List<CodeObjectIdUriPair>();
                        foreach (var document in _codeObjectsCache.Map.Values)
                        {
                            foreach (var codeObjectId in list)
                            {
                                //if a method found add its document's file uri
                                //else try to search by class name
                                if (document.Methods.Keys.Contains(codeObjectId))
                                {
                                    uris.Add(new CodeObjectIdUriPair(codeObjectId,document.FileUri));
                                }
                                else
                                {
                                    var className = codeObjectId.SubstringBefore("$_$").SubstringAfterLast(".");
                                    foreach (var riderMethodInfo in document.Methods.Values)
                                    {
                                        if (riderMethodInfo.ContainingClass.Equals(className))
                                        {
                                            //need to find the first method and break
                                            uris.Add(new CodeObjectIdUriPair(codeObjectId,document.FileUri));
                                            break;
                                        }
                                    }
                                }    
                            }
                        }
                        
                        result.Set(uris);
                    }
                    catch (Exception e)
                    {
                        //todo: maybe throw an error to notify the fronend ?
                        _logger.Error(e,"Error searching documents uris");
                        result.Set(new List<CodeObjectIdUriPair>());
                    }
                }

                return result;
            });
            
            
            _codeObjectsModel.GetSpansWorkspaceUris.Set((_, list) =>
            {
                RdTask<List<CodeObjectIdUriOffsetTrouple>> result = new RdTask<List<CodeObjectIdUriOffsetTrouple>>();
                using (ReadLockCookie.Create())
                {
                    try
                    {
                        var uris = new List<CodeObjectIdUriOffsetTrouple>();
                        foreach (var document in _codeObjectsCache.Map.Values)
                        {
                            foreach (var riderMethodInfo in document.Methods.Values)
                            {
                                foreach (var riderSpanInfo in riderMethodInfo.Spans)
                                {
                                    if (list.Contains(riderSpanInfo.Id))
                                    {
                                        uris.Add(new CodeObjectIdUriOffsetTrouple(riderSpanInfo.Id,document.FileUri,riderSpanInfo.Offset));
                                    }
                                }
                            }
                        }
                        
                        result.Set(uris);
                    }
                    catch (Exception e)
                    {
                        //todo: maybe throw an error to notify the fronend ?
                        _logger.Error(e,"Error searching documents uris");
                        result.Set(new List<CodeObjectIdUriOffsetTrouple>());
                    }
                }

                return result;
            });
        }


        [CanBeNull]
        public RiderCodeLensInfo GetRiderCodeLensInfo([NotNull] string codeObjectId)
        {
            using (ReadLockCookie.Create())
            {
                _codeObjectsModel.CodeLens.TryGetValue(codeObjectId, out var lensInfo);
                return lensInfo;
            }
        }


        public void AddOpenChangeDocument([NotNull] IPsiSourceFile psiSourceFile, [NotNull] Document document)
        {
            if (document.Methods.IsEmpty())
                return;

            var documentKey = Identities.ComputeFilePath(psiSourceFile);

            //todo: consider removing the document from the protocol when the frontend consumes it.
            // then avoid notifying the frontend of changes if the code objects didn't change by keeping
            // and checking last update timestamp, the cache keeps a timestamp of last update.
            
            //relying on Equals method.
            //if the document is still in the protocol and hasn't change then do nothing.
            if (document.CheckEquals(_codeObjectsModel.Documents.TryGetValue(documentKey)))
            {
                Log(_logger,"Document {0} already exists and is equals to the new one",documentKey);
                //if the document exists in the protocol then update the PsiSourceFile mapping, it can be a new instance of PsiSourceFile
                //remove before add because it may be a new IPsiSourceFile instance
                _docPathToPsiSourceFile.Remove(documentKey);
                _docPathToPsiSourceFile.Add(documentKey, psiSourceFile);
                return;
            }

            
            //remove before add because it may be a new IPsiSourceFile instance
            _docPathToPsiSourceFile.Remove(documentKey);
            _docPathToPsiSourceFile.Add(documentKey, psiSourceFile);
            //remove before add in case there is a non equals document already
            Log(_logger,"Pushing Document {0} to the protocol",documentKey);
            _shellRdDispatcher.UnguardedScheduler.Queue(() =>
            {
                _codeObjectsModel.Documents.Remove(documentKey);
                if (_codeObjectsModel.Documents.ContainsKey(documentKey))
                {
                    _logger.Error("Document is still in the protocol after calling remove {0}",documentKey);
                }
                _codeObjectsModel.Documents.Add(documentKey, document);
                NotifyOnDocumentCodeObjects(documentKey);
            });
        }


        private void NotifyOnDocumentCodeObjects([NotNull] string documentKey)
        {
            _shellRdDispatcher.UnguardedScheduler.Queue(() =>
            {
                //maybe there where no findings for this document, don't change the value in that case
                if (_codeObjectsModel.Documents.ContainsKey(documentKey))
                {
                    Log(_logger,"NotifyDocumentOpened for {0}",documentKey);
                    _codeObjectsModel.DocumentAnalyzed.Fire(documentKey);
                }
            });
        }
    }
}