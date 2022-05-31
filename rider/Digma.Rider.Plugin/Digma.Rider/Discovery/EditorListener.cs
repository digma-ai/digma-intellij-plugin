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
using NuGet;
using static Digma.Rider.Logging.Logger;
using ILogger = JetBrains.Util.ILogger;

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


        //this method handles only the add event, when the text control is already fully loaded.
        //todo: consider adding DocumentChange listener , the code objects will be in CodeObjectsCache.
        // can also notify frontend about DocumentChange from CodeObjectsCache.
        // A new class will produce the event but will not be in the cache yet, so maybe 
        // its better to notify frontend about DocumentChanged from the cache, that will catch
        // changes on existing and new documents.
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
            var document = _codeObjectsCache.Map.TryGetValue(psiSourceFile);
            if (document == null)
            {
                Log(_logger,"Document for PsiSourceFile '{0}' was not found in cache. probably a new document or a document with no code objects.",
                    psiSourceFile);
                return;
            }

            Log(_logger,"Found cached Document for PsiSourceFile '{0}'", psiSourceFile);
            LogFoundMethodsForDocument(_logger, document);

            //no need to add or notify frontend if no methods found
            if (EnumerableExtensions.IsEmpty(document.Methods))
            {
                Log(_logger,"Document for PsiSourceFile '{0}' does not contain code objects. Not updating the protocol.", psiSourceFile);
                return;
            }

            _codeObjectsHost.AddOpenChangeDocument(psiSourceFile, document);
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
                    "DocumentChanged::" + _textControl.Document.Moniker, TimeSpan.FromSeconds(15), DocumentChanged);
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