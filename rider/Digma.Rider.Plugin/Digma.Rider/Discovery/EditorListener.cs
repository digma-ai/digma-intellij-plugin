using System;
using System.Diagnostics.CodeAnalysis;
using Digma.Rider.Protocol;
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

            textControlManager.TextControls.BeforeAddRemove.Advise(lifetime, BeforeAddRemoveDocumentHandler);
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
            Log(_logger, "Got AddRemoveEvent for TextControl {0}", addRemoveEvent);
            if (addRemoveEvent.Value == null || addRemoveEvent.Value.Lifetime.IsNotAlive)
                return;

            if (addRemoveEvent.IsAdding)
            {
                var textControl = addRemoveEvent.Value;
                Log(_logger, "TextControl Added {0}", textControl.Document);
                var documentChangeTracker =
                    new DocumentChangeTracker(this, textControl, _logger, _shellLocks);
                textControl.Lifetime.AddDispose(documentChangeTracker);
                HandleDocumentOpenChange(textControl);
            }
        }


        //this method handles only the remove event before the text control is fully removed.
        //in case we want to do something while the psi source file can still be found.
        //todo: currently this method only logs a message, maybe we need to do something here when TextControl is removed?
        private void BeforeAddRemoveDocumentHandler(BeforeAddRemoveEventArgs<ITextControl> addRemoveEvent)
        {
            Log(_logger, "Got BeforeAddRemoveEvent for TextControl {0}", addRemoveEvent);
            if (addRemoveEvent.Value == null)
                return;
            if (addRemoveEvent.IsRemoving)
            {
                var textControl = addRemoveEvent.Value;
                Log(_logger, "TextControl Removed {0}", textControl.Document);
                var psiSourceFile = _documentManager.GetProjectFile(textControl.Document).ToSourceFile();
                if (psiSourceFile != null)
                {
                    Log(_logger, "Found PsiSourceFile {0} for removed TextControl {1}", psiSourceFile,
                        textControl.Document);
                    //todo: maybe remove document from CodeObjectsHost for the textControl
                }
                else
                {
                    Log(_logger, "Could not find PsiSourceFile for removed TextControl {1}", textControl.Document);
                }
            }
        }



        private void HandleDocumentOpenChange(ITextControl textControl)
        {
            using (ReadLockCookie.Create())
            {
                var psiSourceFile = _documentManager.GetProjectFile(textControl.Document).ToSourceFile();
                if (psiSourceFile == null || !psiSourceFile.GetPsiServices().Files.AllDocumentsAreCommitted)
                {
                    Log(_logger, "PsiSourceFile {0} for TextControl {1} is null or not committed", psiSourceFile,
                        textControl.Document);
                    return;
                }

                Log(_logger, "Found PsiSourceFile {0} for TextControl {1}", psiSourceFile, textControl.Document);
                var document = _codeObjectsCache.Map.TryGetValue(psiSourceFile);
                if (document == null)
                {
                    Log(_logger, "Document for PsiSourceFile {0} was not found in cache, probably a new document",
                        psiSourceFile);
                    return;
                }

                LogFoundMethodsForDocument(_logger, document);

                //no need to add or notify frontend if no methods found
                if (EnumerableExtensions.IsEmpty(document.Methods))
                    return;

                _codeObjectsHost.AddOpenChangeDocument(psiSourceFile, document);
            }

        }
        

        
        
        
        
        [SuppressMessage("ReSharper", "PossibleMultipleEnumeration")]
        private class DocumentChangeTracker : IDisposable
        {
            private readonly EditorListener _parent;
            private readonly ITextControl _textControl;
            private readonly ILogger _logger;
            private readonly GroupingEvent _groupingEvent;

            public DocumentChangeTracker(EditorListener parent,
                ITextControl textControl, ILogger logger,
                IThreading shellLocks)
            {
                _parent = parent;
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
                _parent.HandleDocumentOpenChange(_textControl);
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