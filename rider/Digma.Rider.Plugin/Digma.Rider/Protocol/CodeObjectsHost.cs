using System;
using System.Collections.Generic;
using Digma.Rider.Discovery;
using JetBrains.Annotations;
using JetBrains.Application.Threading;
using JetBrains.Collections;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.Rd;
using JetBrains.Rd.Tasks;
using JetBrains.RdBackend.Common.Features;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Util;
using JetBrains.Util.Extension;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Protocol
{
    [SolutionComponent]
    public class CodeObjectsHost
    {
        // A dictionary of documentUri to PsiSourceFile to help us in various tasks.
        internal readonly Dictionary<string, IPsiSourceFile> DocPathToPsiSourceFile = new();

        private readonly Lifetime _lifetime;
        private readonly ILogger _logger;
        private readonly CodeObjectsModel _codeObjectsModel;
        private readonly CodeObjectsCache _codeObjectsCache;
        private readonly IShellLocks _shellLocks;

        public CodeObjectsHost(Lifetime lifetime, ISolution solution,
            CodeObjectsCache codeObjectsCache,
            IShellLocks shellLocks,
            ILogger logger)
        {
            _lifetime = lifetime;
            _codeObjectsCache = codeObjectsCache;
            _shellLocks = shellLocks;
            _logger = logger;
            _codeObjectsModel = solution.GetProtocolSolution().GetCodeObjectsModel();

            

            //some documents in the protocol may be incomplete on startup, it usually happens after 
            //invalidating caches and our cache executes before reference resolving is available.
            //when the solution is fully loaded the frontend will call RefreshIncompleteDocuments 
            //and hopefully will fix all incomplete documents and notify the frontend again about
            //new code objects if necessary..
            _codeObjectsModel.RefreshIncompleteDocuments.Advise(lifetime, _ => { RefreshAllIncompleteDocuments(); });


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
                                    uris.Add(new CodeObjectIdUriPair(codeObjectId, document.FileUri));
                                }
                                else
                                {
                                    var className = codeObjectId.SubstringBefore("$_$").SubstringAfterLast(".");
                                    foreach (var riderMethodInfo in document.Methods.Values)
                                    {
                                        if (riderMethodInfo.ContainingClass.Equals(className))
                                        {
                                            //need to find the first method and break
                                            uris.Add(new CodeObjectIdUriPair(codeObjectId, document.FileUri));
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
                        //todo: maybe throw an error to notify the frontend ?
                        _logger.Error(e, "Error searching documents uris");
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
                        foreach (var psiSourceFile in _codeObjectsCache.Map.Keys)
                        {
                            if (psiSourceFile == null)
                            {
                                Log(_logger, "psiSourceFile is null. Aborting operation.");
                                continue;
                            }
                            
                            var document = _codeObjectsCache.Map.TryGetValue(psiSourceFile);
                            if (document == null)
                            {
                                Log(_logger, "Got null Document for psiSourceFile '{0}'. Aborting operation.",
                                    psiSourceFile);
                                continue;
                            }

                            //it may be that the document is in the cache but reference resolving was not complete
                            //when the cache was built. this is a compensation and the document will be updated..
                            if (!document.IsComplete)
                            {
                                Log(_logger, "Document '{0}' is not complete. Trying to build it on-demand.",
                                    document.FileUri);
                                NotifyDocumentOpenedOrChanged(psiSourceFile);
                                document = _codeObjectsCache.Map.TryGetValue(psiSourceFile);
                            }

                            if (document == null)
                            {
                                Log(_logger,
                                    "Still could not find Document for psiSourceFile '{0}' in the cache. Aborting operation.",
                                    psiSourceFile);
                                continue;
                            }

                            foreach (var riderMethodInfo in document.Methods.Values)
                            {
                                foreach (var riderSpanInfo in riderMethodInfo.Spans)
                                {
                                    if (list.Contains(riderSpanInfo.Id))
                                    {
                                        uris.Add(new CodeObjectIdUriOffsetTrouple(riderSpanInfo.Id, document.FileUri,
                                            riderSpanInfo.Offset));
                                    }
                                }
                            }
                        }

                        result.Set(uris);
                    }
                    catch (Exception e)
                    {
                        //todo: maybe throw an error to notify the fronend ?
                        _logger.Error(e, "Error searching documents uris");
                        result.Set(new List<CodeObjectIdUriOffsetTrouple>());
                    }
                }

                return result;
            });
        }


        [CanBeNull]
        public IList<RiderCodeLensInfo> GetRiderCodeLensInfo([NotNull] string codeObjectId)
        {
            using (ReadLockCookie.Create())
            {
                return _codeObjectsModel.CodeLens.TryGetValue(codeObjectId)?.Lens;
            }
        }


        private void RefreshAllIncompleteDocuments()
        {
            Log(_logger, "Refreshing all incomplete documents in the protocol");
            var incompleteDocuments = new List<string>();
            foreach (var (key, value) in _codeObjectsModel.Documents)
            {
                if (!value.IsComplete)
                {
                    Log(_logger, "Found incomplete document {0}", key);
                    incompleteDocuments.Add(key);
                }
            }

            if (incompleteDocuments.IsEmpty())
            {
                Log(_logger, "No incomplete documents found");
            }

            foreach (var incompleteDocument in incompleteDocuments)
            {
                Log(_logger, "Refreshing incomplete document {0}", incompleteDocument);
                var psiSourceFile = DocPathToPsiSourceFile.TryGetValue(incompleteDocument);

                if (psiSourceFile != null)
                {
                    Log(_logger, "Found psiSourceFile for incomplete document {0}", psiSourceFile);
                    _shellLocks.ExecuteOrQueue(_lifetime, "refresh document", () =>
                    {
                        Log(_logger, "Starting refresh incomplete document {0}", psiSourceFile);
                        //call NotifyDocumentOpenedOrChanged, if the document produces any code objects then it
                        //will be in cache. if it doesn't produce code objects it will not be in the cache.
                        NotifyDocumentOpenedOrChanged(psiSourceFile);
                        var document = _codeObjectsCache.Map.TryGetValue(psiSourceFile);

                        if (document is { IsComplete: true })
                        {
                            Log(_logger, "Document is complete after refresh {0} {1}", psiSourceFile, document);
                        }
                        else if (document is { IsComplete: false })
                        {
                            Log(_logger, "Document is still not complete after refresh {0} {1}", psiSourceFile,
                                document);
                        }
                        else if (document == null)
                        {
                            Log(_logger, "Document is null after refresh, probably no code objects found {0}",
                                psiSourceFile);
                        }

                        Log(_logger, "Document refresh complete for {0}, {1}", psiSourceFile, document);
                    });
                }
                else
                {
                    Log(_logger, "Could not find psiSourceFile for incomplete document {0}", incompleteDocument);
                }
            }
        }


        /// <summary>
        /// This is a central method in code objects discovery.
        /// when called it will try to load psiSourceFile from the CodeObjectsCache and if necessary notify the frontend
        /// about the code object for this file. if the cache Document for this file is not complete it will be rebuilt.
        /// It is called on few events when we need to notify the frontend. usually when a document is opened or changed.
        /// This method will eventually notify the frontend about the code objects. 
        /// </summary>
        /// <param name="psiSourceFile"></param>
        public void NotifyDocumentOpenedOrChanged([NotNull] IPsiSourceFile psiSourceFile)
        {
            Log(_logger, "NotifyDocumentOpenedOrChanged started for {0}", psiSourceFile);

            if (!psiSourceFile.GetPsiServices().Files.IsCommitted(psiSourceFile))
            {
                Log(_logger,
                    "PsiSourceFile {0} is not committed, will execute after all documents are committed.",
                    psiSourceFile);
            }


            using (ReadLockCookie.Create())
            {
                psiSourceFile.GetPsiServices().Files.DoOnCommitedPsi(_lifetime, () =>
                {
                    Log(_logger, "Task DoOnCommitedPsi started for {0}", psiSourceFile);
                    FindDocumentCodeObjectsAndNotifyFrontend(psiSourceFile);
                });
            }
        }


        //this method must execute under ReadLockCookie and only when psiSourceFile is committed.
        //the central logic of this method is to rebuild incomplete documents before notifying the frontend
        private void FindDocumentCodeObjectsAndNotifyFrontend([NotNull] IPsiSourceFile psiSourceFile)
        {
            Log(_logger, "NotifyDocumentCodeObjectsToFrontend for PsiSourceFile '{0}'", psiSourceFile);
            Log(_logger, "Trying to find PsiSourceFile '{0}' in cache", psiSourceFile);
            var document = _codeObjectsCache.Map.TryGetValue(psiSourceFile);

            //todo: in case of DocumentChange event
            // the document from the cache should already be updated after DocumentChange event because we have 2 seconds delay,
            // that gives time for the cache to update.
            // without this delay we may need to build the document on demand for DocumentChange event, this method is executed
            // also on document open so we may need two separate flows, one for document opened and one for DocumentChange.

            //document may be null if:
            //the cache is not ready yet, it can happen if a document is re-opened on startup by the IDE and the cache is
            //not ready yet.
            //the document has no code objects, the cache doesn't save empty documents.
            //document.IsComplete will be false if the cache couldn't resolve references on startup, it happens on startup
            //if resharper's caches are not ready yet.
            //in all cases we try to build the Document on demand, in this stage references should resolve.
            if (document is not { IsComplete: true })
            {
                var reason = document == null ? "was not found in cache" : "was found in cache but is not complete";
                Log(_logger, "Document for PsiSourceFile '{0}' {1}. Trying to build it on-demand.",
                    psiSourceFile, reason);

                Log(_logger, "Trying build on demand for PsiSourceFile '{0}'", psiSourceFile);

                document = BuildDocumentOnDemand(psiSourceFile);
                if (document != null)
                {
                    Log(_logger, "Document for PsiSourceFile '{0}' did produce code objects after build on demand",
                        psiSourceFile);
                }


                if (document == null)
                {
                    Log(_logger,
                        "Document for PsiSourceFile '{0}' was not found in cache after ProcessOnDemand. Probably a document with no code objects. using an empty document to update the protocol",
                        psiSourceFile);
                    //the document may be null if this file didn't produce code objects, but it may be a file that had code objects
                    //and now it doesn't, for example a method deleted. in that case we want to update the protocol and the fronend.
                    //AddOpenChangeDocument will decide if this document should be updated in the protocol or ignored.
                    var fileUri = Identities.ComputeFileUri(psiSourceFile);
                    document = new Document(true,fileUri);
                }
            }

            Log(_logger, "Found Document for PsiSourceFile '{0}' {1}", psiSourceFile, document);
            if (document.HasCodeObjects())
            {
                LogFoundMethodsForDocument(_logger, document);    
            }
            

            AddOpenChangeDocument(psiSourceFile, document);
        }


        private Document BuildDocumentOnDemand([NotNull] IPsiSourceFile psiSourceFile)
        {
            using (CompilationContextCookie.GetOrCreate(psiSourceFile.ResolveContext))
            {
                Log(_logger, "Building document on demand for PsiSourceFile '{0}'", psiSourceFile);
                var document = (Document)_codeObjectsCache.Build(psiSourceFile, false);
                Log(_logger, "Document on demand for PsiSourceFile '{0}' is {1}", psiSourceFile, document);
                //only add the document to the cache if it has code objects
                if (document != null && document.HasCodeObjects())
                {
                    //todo: instead of calling merge we can just call MarkAsDirty if this is a cached file.
                    // then fix code that expects an immediate merge like GetSpansWorkspaceUris
                    Log(_logger, "Merging Document to cache for PsiSourceFile '{0}'", psiSourceFile);
                    _codeObjectsCache.Merge(psiSourceFile, document);
                }

                document = _codeObjectsCache.Map.TryGetValue(psiSourceFile);
                Log(_logger, "Document from cache after building on demand for PsiSourceFile '{0}'  is {1}",
                    psiSourceFile, document);
                return document;
            }
        }

        //This method adds the Document to the protocol if necessary and nofifies frontend
        private void AddOpenChangeDocument([NotNull] IPsiSourceFile psiSourceFile, [NotNull] Document document)
        {
            var documentKey = Identities.ComputeFilePath(psiSourceFile);

            Log(_logger, "Got AddOpenChangeDocument request for {0}, document:{1} key:{2}", psiSourceFile, document,
                documentKey);

            //no need to add or notify frontend if no code objects found found.
            //unless the document is already in the protocol because it had code object and now it doesn't , that can happen after document change.
            if (!document.HasCodeObjects() && !_codeObjectsModel.Documents.ContainsKey(documentKey))
            {
                Log(_logger,
                    "Document for PsiSourceFile '{0}' does not contain code objects. Not updating the protocol.",
                    psiSourceFile);
                return;
            }


            //relying on Equals method.
            //if the document is still in the protocol and hasn't change then don't notify frontend.
            if (_codeObjectsModel.Documents.ContainsKey(documentKey) &&
                document.CheckEquals(_codeObjectsModel.Documents.TryGetValue(documentKey)))
            {
                Log(_logger, "Document {0} already exists and is equals to the new one, not pushing to the protocol",
                    documentKey);
                //if the document exists in the protocol then update the PsiSourceFile mapping, it can be a new instance of PsiSourceFile
                //remove before add because it may be a new IPsiSourceFile instance
                DocPathToPsiSourceFile.Remove(documentKey);
                DocPathToPsiSourceFile.Add(documentKey, psiSourceFile);
                return;
            }


            //remove before add because it may be a new IPsiSourceFile instance
            DocPathToPsiSourceFile.Remove(documentKey);
            DocPathToPsiSourceFile.Add(documentKey, psiSourceFile);
            //remove before add in case there is a non equals document already
            Log(_logger, "Pushing Document {0} to the protocol: {1}", documentKey, document);
            _codeObjectsModel.Proto.Scheduler.InvokeOrQueue(() =>
            {
                using (WriteLockCookie.Create())
                {
                    //sometimes a document already has RdId, if it was already added to the protocol before ?
                    if (!document.RdId.IsNil)
                    {
                        _logger.Warn("Document already has RdId, setting RdId to null for {0}", documentKey);
                        document.RdId = RdId.Nil;
                    }

                    var removed = _codeObjectsModel.Documents.Remove(documentKey);
                    if (!removed)
                    {
                        _logger.Warn("Document was not removed, _codeObjectsModel.Documents.Remove returned false {0}",
                            documentKey);
                    }

                    if (_codeObjectsModel.Documents.ContainsKey(documentKey))
                    {
                        _logger.Warn("Document is still in the protocol after calling remove {0}", documentKey);
                    }

                    _codeObjectsModel.Documents.Add(documentKey, document);
                    Log(_logger, "Notifying DocumentAnalyzed for {0}", documentKey);
                    _codeObjectsModel.DocumentAnalyzed(documentKey);
                }
            });
        }
    }
}