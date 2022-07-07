using System;
using System.Diagnostics.CodeAnalysis;
using Digma.Rider.Protocol;
using Digma.Rider.Util;
using JetBrains.Annotations;
using JetBrains.Application.Threading;
using JetBrains.DataFlow;
using JetBrains.DocumentManagers;
using JetBrains.DocumentModel;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.TextControl;
using JetBrains.TextControl.TextControlsManagement;
using JetBrains.Threading;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Discovery
{
    /// <summary>
    /// A TextControl listener that notifies frontend about code objects in opened or changed documents.
    /// the code objects are in CodeObjectsCache.
    /// </summary>
    [SolutionComponent]
    public class EditorListener
    {
        private readonly DocumentManager _documentManager;
        private readonly ILogger _logger;
        private readonly CodeObjectsCache _codeObjectsCache;
        private readonly CodeObjectsHost _codeObjectsHost;
        private readonly IShellLocks _shellLocks;

        public EditorListener(
            Lifetime lifetime,
            TextControlManager textControlManager,
            DocumentManager documentManager,
            ILogger logger,
            CodeObjectsCache codeObjectsCache,
            CodeObjectsHost codeObjectsHost,
            IShellLocks shellLocks)
        {
            _documentManager = documentManager;
            _logger = logger;
            _codeObjectsCache = codeObjectsCache;
            _codeObjectsHost = codeObjectsHost;
            _shellLocks = shellLocks;

            textControlManager.TextControls.AddRemove.Advise(lifetime, AddRemoveDocumentHandler);
        }


        private void AddRemoveDocumentHandler(AddRemoveEventArgs<ITextControl> addRemoveEvent)
        {
            Log(_logger, "Got AddRemoveEvent for TextControl {0}", addRemoveEvent.Value?.Document);
            if (addRemoveEvent.Value == null || addRemoveEvent.Value.Lifetime.IsNotAlive)
            {
                Log(_logger, "TextControl is null or not alive {0}", addRemoveEvent.Value?.Document);
                return;
            }

            if (addRemoveEvent.IsAdding)
            {
                var textControl = addRemoveEvent.Value;
                Log(_logger, "TextControl Added {0}", textControl.Document);
                IPsiSourceFile psiSourceFile;
                using (ReadLockCookie.Create())
                {
                    psiSourceFile = _documentManager.GetProjectFile(textControl.Document).ToSourceFile();
                    //psiSourceFile can be a non project file, for example libraries classes
                    if (psiSourceFile == null || !psiSourceFile.GetPsiServices().Files.IsCommitted(psiSourceFile))
                    {
                        Log(_logger, "PsiSourceFile '{0}' for TextControl {1} is null or not committed", psiSourceFile,
                            textControl.Document);
                        return;
                    }
                }

                if (!PsiUtils.IsPsiSourceFileApplicable(psiSourceFile))
                {
                    Log(_logger, "PsiSourceFile '{0}' for TextControl {1} is not applicable", psiSourceFile,
                        textControl.Document);
                    return;
                }

                Log(_logger, "Found PsiSourceFile '{0}' for TextControl {1}", psiSourceFile, textControl.Document);

                var documentChangeTracker =
                    new DocumentChangeTracker(this, psiSourceFile, textControl, _logger, _shellLocks);
                textControl.Lifetime.AddDispose(documentChangeTracker);

                HandleDocumentOpenChange(psiSourceFile);
            }
        }




        private void HandleDocumentOpenChange([NotNull] IPsiSourceFile psiSourceFile)
        {
            Log(_logger,"HandleDocumentOpenChange for PsiSourceFile '{0}'", psiSourceFile);
            Log(_logger,"Trying to find PsiSourceFile '{0}' in cache", psiSourceFile);
            var document = _codeObjectsCache.Map.TryGetValue(psiSourceFile);
            //document may be null if:
            //the cache is not ready yet, it can happen if a document is opened on startup because it was opened before shutdown.
            //the document has no code objects, the cache doesn't save empty documents.
            //document.IsComplete will be false if the cache couldn't resolve references on startup, it happens on startup
            //if resharper's caches are not ready yet.
            //in all cases we try to build the Document on demand, in this stage references should resolve.
            if (document is not { IsComplete: true })
            {
                var reason = document == null ? "was not found in cache" : "was found in cache but is not complete";
                Log(_logger,"Document for PsiSourceFile '{0}' {1}. Trying to build it on-demand.",
                    psiSourceFile,reason);

                //it may be a document that is not in the cache,
                //if it wasn't process yet , that may happen on startup.
                //if this document did not produce code objects when it was processed by the cache.
                if (_codeObjectsCache.Map.ContainsKey(psiSourceFile))
                {
                    Log(_logger, "Document for PsiSourceFile '{0}' in in cache , trying ProcessOnDemand",
                        psiSourceFile);
                    _codeObjectsCache.ProcessOnDemand(psiSourceFile);
                    document = _codeObjectsCache.Map.TryGetValue(psiSourceFile);
                }
                else
                {
                    //if the document is not in the cache, try to build code objects.
                    //if this is a document that does not produce code objects then its not necessary 
                    //but we have no knowledge of that, and we don't know if it was already processed before.
                    //hopefully that doesn't happen a lot and usually f=very fast.
                    Log(_logger, "Document for PsiSourceFile '{0}' in Not in cache , trying to build code objects on the fly",
                        psiSourceFile);

                    document = BuildDocumentOnDemand(psiSourceFile);
                    if (document != null)
                    {
                        Log(_logger, "Document for PsiSourceFile '{0}' did produce code objects",psiSourceFile);
                    }
                }
                
                
                
                if (document == null)
                {
                    Log(_logger,"Document for PsiSourceFile '{0}' was not found in cache after ProcessOnDemand. Probably a document with no code objects.",
                        psiSourceFile);
                    return;    
                }
            }

            Log(_logger,"Found cached Document for PsiSourceFile '{0}'", psiSourceFile);
            LogFoundMethodsForDocument(_logger, document);

            //no need to add or notify frontend if no methods found
            if (!document.HasCodeObjects())
            {
                Log(_logger,"Document for PsiSourceFile '{0}' does not contain code objects. Not updating the protocol.", psiSourceFile);
                return;
            }

            _codeObjectsHost.AddOpenChangeDocument(psiSourceFile, document);
        }



        private Document BuildDocumentOnDemand([NotNull] IPsiSourceFile psiSourceFile)
        {
            using (ReadLockCookie.Create())
            {
                var document = (Document)_codeObjectsCache.Build(psiSourceFile, false);
                //only add the document to the cache if it has code objects
                if (document != null && document.HasCodeObjects())
                {
                    _codeObjectsCache.Merge(psiSourceFile, document);
                }
                
                return _codeObjectsCache.Map.TryGetValue(psiSourceFile);
            }
        }
        
        
        
        

        [SuppressMessage("ReSharper", "PossibleMultipleEnumeration")]
        private class DocumentChangeTracker : IDisposable
        {
            private readonly EditorListener _parent;
            private readonly IPsiSourceFile _psiSourceFile;
            private readonly ITextControl _textControl;
            private readonly ILogger _logger;
            private readonly GroupingEvent _groupingEvent;

            public DocumentChangeTracker([NotNull] EditorListener parent,
                [NotNull] IPsiSourceFile psiSourceFile,
                [NotNull] ITextControl textControl, 
                [NotNull] ILogger logger,
                [NotNull] IThreading shellLocks)
            {
                _parent = parent;
                _psiSourceFile = psiSourceFile;
                _textControl = textControl;
                _logger = logger;

                _groupingEvent = shellLocks.CreateGroupingEvent(textControl.Lifetime,
                    "DocumentChanged::" + _textControl.Document.Moniker, TimeSpan.FromSeconds(2), DocumentChanged);
                
                textControl.Lifetime.Bracket(() => _textControl.Document.DocumentChanged += DocumentChangedEventGrouper,
                    () => _textControl.Document.DocumentChanged -= DocumentChangedEventGrouper);
            }

            private void DocumentChanged()
            {
                Log(_logger, "DocumentChanged for doc {0}", _textControl.Document);
                _parent.HandleDocumentOpenChange(_psiSourceFile);
            }

            private void DocumentChangedEventGrouper(object sender, EventArgs<DocumentChange> args)
            {
                _groupingEvent.FireIncoming();
            }

            public void Dispose()
            {
                Log(_logger, "Canceling grouping event  for doc {0}", _textControl.Document);
                _groupingEvent.CancelIncoming();
                _groupingEvent.Outgoing.Dispose();
                _groupingEvent.Incoming.Dispose();
            }
        }
    }
}