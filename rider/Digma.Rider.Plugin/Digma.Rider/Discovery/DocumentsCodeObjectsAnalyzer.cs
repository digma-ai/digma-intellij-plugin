using System;
using System.Diagnostics.CodeAnalysis;
using Digma.Rider.Protocol;
using JetBrains.Application.Threading;
using JetBrains.DataFlow;
using JetBrains.DocumentManagers;
using JetBrains.DocumentModel;
using JetBrains.Lifetimes;
using JetBrains.ReSharper.Feature.Services.CSharp.CompleteStatement;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.TextControl;
using JetBrains.TextControl.TextControlsManagement;
using JetBrains.Threading;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Discovery
{
    /// <summary>
    /// a TextControl and DocumentChanged listener that discovers code objects on the fly every time
    /// a document is opened or changed and notifies frontend.
    /// its a lot of waste comparing to CodeObjectsCache because documents are analyzed every time they are
    /// opened. and it will happen again and again when the solution is closed/opened. 
    /// </summary>
    [Obsolete(message: "not used, we use CodeObjectsCache. its here as example and is fully working")]
    // [SolutionComponent]
    public class DocumentsCodeObjectsAnalyzer
    {
        private readonly DocumentManager _documentManager;
        private readonly ILogger _logger;
        private readonly IShellLocks _shellLocks;
        private readonly CodeObjectsHost _codeObjectsHost;

        public DocumentsCodeObjectsAnalyzer(
            Lifetime lifetime,
            TextControlManager textControlManager,
            DocumentManager documentManager,
            ILogger logger,
            IShellLocks shellLocks,
            CodeObjectsHost codeObjectsHost)
        {
            _documentManager = documentManager;
            _logger = logger;
            _shellLocks = shellLocks;
            _codeObjectsHost = codeObjectsHost;

            textControlManager.TextControls.BeforeAddRemove.Advise(lifetime, BeforeAddRemoveDocumentHandler);
            textControlManager.TextControls.AddRemove.Advise(lifetime, AddRemoveDocumentHandler);
        }

        //this method handles only the add event, when the text control is already fully loaded.
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
                DiscoverCodeObjects(textControl);
            }
        }


        //this method handles only the remove event before the text control is fully removed.
        //in case we want to do something while the psi source file can still be found.
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


        [SuppressMessage("ReSharper", "PossibleMultipleEnumeration")]
        private void DiscoverCodeObjects(ITextControl textControl)
        {
            Log(_logger, "Starting code objects discovery for TextControl {0}", textControl.Document);
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
                var psiFiles = psiSourceFile.GetPsiFiles<CSharpLanguage>();
                var docId = Identities.ComputeFileUri(psiSourceFile);
                var document = new Document(docId);
                foreach (var psiFile in psiFiles)
                {
                    var cSharpFile = psiFile.Is<ICSharpFile>();
                    if (cSharpFile == null)
                        continue;

                    var discoveryProcessor = new CodeObjectsDiscoveryProcessor();
                    cSharpFile.ProcessDescendants(discoveryProcessor);
                    var methodInfos = discoveryProcessor.MethodInfos;
                    foreach (var riderMethodInfo in methodInfos)
                    {
                        document.Methods.Add(riderMethodInfo.Id, riderMethodInfo);
                    }
                }

                LogFoundMethodsForDocument(_logger, document);

                _codeObjectsHost.AddOpenChangeDocument(psiSourceFile, document);
            }
        }

        
        
        
        
        
        [SuppressMessage("ReSharper", "PossibleMultipleEnumeration")]
        private class DocumentChangeTracker : IDisposable
        {
            private readonly DocumentsCodeObjectsAnalyzer _parent;
            private readonly ITextControl _textControl;
            private readonly ILogger _logger;
            private readonly GroupingEvent _groupingEvent;

            public DocumentChangeTracker(DocumentsCodeObjectsAnalyzer parent,
                ITextControl textControl, ILogger logger,
                IThreading shellLocks)
            {
                _parent = parent;
                _textControl = textControl;
                _logger = logger;

                _groupingEvent = shellLocks.CreateGroupingEvent(textControl.Lifetime,
                    "DocumentChanged::" + _textControl.Document.Moniker, TimeSpan.FromSeconds(5), DocumentChanged);
                textControl.Lifetime.Bracket(() => _textControl.Document.DocumentChanged += DocumentChangedEventGrouper,
                    () => _textControl.Document.DocumentChanged -= DocumentChangedEventGrouper);
            }

            private void DocumentChanged()
            {
                Log(_logger, "DocumentChanged for doc {0}", _textControl.Document);
                _parent.DiscoverCodeObjects(_textControl);
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