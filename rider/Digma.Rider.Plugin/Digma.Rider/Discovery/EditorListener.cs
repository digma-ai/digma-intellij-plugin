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
    /// EditorListener is a TextControl listener that listens to document open and document change events
    /// that notifies frontend about code objects.
    /// when a document is opened or changed EditorListener will call CodeObjectsHost to do the actual work.
    /// </summary>
    [SolutionComponent]
    public class EditorListener
    {
        private readonly DocumentManager _documentManager;
        private readonly ILogger _logger;
        private readonly CodeObjectsHost _codeObjectsHost;
        private readonly IShellLocks _shellLocks;

        public EditorListener(
            Lifetime lifetime,
            TextControlManager textControlManager,
            DocumentManager documentManager,
            ILogger logger,
            CodeObjectsHost codeObjectsHost,
            IShellLocks shellLocks)
        {
            _documentManager = documentManager;
            _logger = logger;
            _codeObjectsHost = codeObjectsHost;
            _shellLocks = shellLocks;

            textControlManager.TextControls.AddRemove.Advise(lifetime, AddRemoveDocumentHandler);
        }


        private void AddRemoveDocumentHandler(AddRemoveEventArgs<ITextControl> addRemoveEvent)
        {
            Log(_logger, "Got AddRemoveEvent for TextControl {0}", addRemoveEvent.Value?.Document);
            if (addRemoveEvent.Value == null || addRemoveEvent.Value.Lifetime.IsNotAlive)
            {
                var reason = addRemoveEvent.Value == null ? "is null" : "is not alive";
                Log(_logger, "TextControl {0} {1}, aborting", reason, addRemoveEvent.Value?.Document);
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
                    
                    if (psiSourceFile == null)
                    {
                        Log(_logger, "PsiSourceFile for TextControl {0} is null", textControl.Document);
                        return;
                    }

                    if (!PsiUtils.IsPsiSourceFileApplicable(psiSourceFile))
                    {
                        Log(_logger, "PsiSourceFile '{0}' for TextControl {1} is not applicable for code objects discovery.", psiSourceFile,
                            textControl.Document);
                        return;
                    }
                }


                Log(_logger, "Found PsiSourceFile '{0}' for TextControl {1}", psiSourceFile, textControl.Document);

                var documentChangeTracker =
                    new DocumentChangeTracker(this, psiSourceFile, textControl, _logger, _shellLocks);
                textControl.Lifetime.AddDispose(documentChangeTracker);

                HandleDocumentOpenChange(psiSourceFile, textControl);
            }
        }


        private void HandleDocumentOpenChange([NotNull] IPsiSourceFile psiSourceFile, ITextControl textControl)
        {
            Log(_logger,"Calling NotifyDocumentOpenedOrChanged for {0}.",textControl.Document);
            _codeObjectsHost.NotifyDocumentOpenedOrChanged(psiSourceFile);
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
                _parent.HandleDocumentOpenChange(_psiSourceFile,_textControl);
            }

            private void DocumentChangedEventGrouper(object sender, EventArgs<DocumentChange> args)
            {
                Log(_logger, "Got DocumentChange event for {0}, OldModificationStamp={1}", _textControl.Document,args.Value.OldModificationStamp);
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