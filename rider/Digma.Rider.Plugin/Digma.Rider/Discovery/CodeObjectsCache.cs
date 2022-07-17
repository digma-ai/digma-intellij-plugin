using Digma.Rider.Protocol;
using Digma.Rider.Util;
using JetBrains.Application.Progress;
using JetBrains.Application.Threading;
using JetBrains.Collections;
using JetBrains.DocumentManagers.impl;
using JetBrains.Lifetimes;
using JetBrains.ReSharper.Feature.Services.CSharp.CompleteStatement;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Caches;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Discovery
{
    /// <summary>
    /// An IPsiSourceFileCache for code objects.
    /// the keys are IPsiSourceFile and the values are Documents containing code objects.
    ///
    ///
    /// A note about incomplete documents.
    /// when building the cache sometimes reference resolving is necessary. if resharper caches are not ready yet
    /// then not all reference resolving will succeed. it usually happens in startup and after invalidating caches.
    /// If that happens we mark the documents as isComplete=False.
    /// later when the document is necessary we try to fix it by rebuilding the cache again in a later stage when all
    /// resharper caches are ready. the rebuild happens in CodeObjectsHost.NotifyDocumentOpenedOrChanged that is called
    /// on various events.
    /// 
    /// </summary>
    [PsiComponent]
    public class CodeObjectsCache: SimpleICache<Document>
    {
        private readonly ILogger _logger;

        /// <summary>
        /// Increment the version when the cache structure changes.
        /// it will set ClearOnLoad to true and the cache will be invalidated and rebuild.
        /// </summary>
        public override string Version => "2";

        public CodeObjectsCache(ILogger logger,Lifetime lifetime, IShellLocks locks, IPersistentIndexManager persistentIndexManager) 
            : base(lifetime, locks, persistentIndexManager, new DocumentMarshaller())
        {
            _logger = logger;
            
            //todo: check PsiCachesAwaiter and PsiCacheNotifier, maybe wait for caches before starting load
        }


        public override object Build(IPsiSourceFile sourceFile, bool isStartup)
        {
            Log(_logger,"Building cache for IPsiSourceFile '{0}'",sourceFile);
            var isCommitted = sourceFile.GetPsiServices().Files.IsCommitted(sourceFile);
            if (!isCommitted)
            {
                Log(_logger,"IPsiSourceFile '{0}' is not committed, Can not build cache",sourceFile);
                return null;
            }

            var psiFiles = sourceFile.GetPsiFiles<CSharpLanguage>();
            var fileUri = Identities.ComputeFileUri(sourceFile);
            var discoveryContext = new DocumentDiscoveryContext(sourceFile, isStartup, fileUri);
            
            foreach (var psiFile in psiFiles)
            {
                var cSharpFile = psiFile.Is<ICSharpFile>();
                if (cSharpFile == null) 
                    continue;
                Log(_logger,"Processing ICSharpFile '{0}' in '{1}'",cSharpFile,sourceFile);
                var discoveryProcessor = new CodeObjectsDiscoveryFileProcessor(cSharpFile,discoveryContext);
                cSharpFile.ProcessDescendants(discoveryProcessor);
            }

            Log(_logger,"Discovery for '{0}' completed",sourceFile);
            //don't keep cache for documents with no code objects, it could also be an interface which we ignore
            if (discoveryContext.Methods.IsEmpty())
            {
                Log(_logger,"IPsiSourceFile '{0}' does not contain any code objects",sourceFile);
                return null;
            }
            else
            {
                Log(_logger,"Found code objects for IPsiSourceFile '{0}':",sourceFile);

                var isComplete = !discoveryContext.HasReferenceResolvingErrors;
                var document = new Document(isComplete, fileUri);
                foreach (var (key, value) in discoveryContext.Methods)
                {
                    document.Methods.Add(key,value);
                }
                Log(_logger,"Built code objects Document '{0}' for IPsiSourceFile '{1}':",document,sourceFile);
                return document;
            }
        }

        protected override bool IsApplicable(IPsiSourceFile sf)
        {
            var properties = sf.Properties;
            var isApplicable = PsiUtils.IsPsiSourceFileApplicable(sf) && 
                               properties.IsICacheParticipant;
            Log(_logger,"IsApplicable '{0}' for file '{1}'",isApplicable,sf);
            
            return isApplicable;
        }


        public override object Load(IProgressIndicator progress, bool enablePersistence)
        {
            Log(_logger,"Load, enablePersistence = {0}",enablePersistence);
            return base.Load(progress, enablePersistence);
        }

        public override void OnPsiChange(ITreeNode elementContainingChanges, PsiChangedElementType type)
        {
            if (elementContainingChanges != null)
            {
                Log(_logger,"OnPsiChange elementContainingChanges = {0},type = {1}",elementContainingChanges,type);
                base.OnPsiChange(elementContainingChanges, type);
            }
        }

        public override void OnDocumentChange(IPsiSourceFile sourceFile, ProjectFileDocumentCopyChange change)
        {
            Log(_logger,"OnDocumentChange sf = {0}",sourceFile);
            base.OnDocumentChange(sourceFile, change);
        }

        public override void MarkAsDirty(IPsiSourceFile sourceFile)
        {
            Log(_logger,"MarkAsDirty sf = {0}",sourceFile);
            base.MarkAsDirty(sourceFile);
        }

        protected override void ProcessDirty()
        {
            Log(_logger,"ProcessDirty");
            base.ProcessDirty();
        }

        protected override void ProcessDirtyFile(IPsiSourceFile psiSourceFile)
        {
            Log(_logger,"ProcessDirtyFile sf = {0}",psiSourceFile);
            base.ProcessDirtyFile(psiSourceFile);
        }


        protected override void RemoveFromDirty(IPsiSourceFile sf)
        {
            Log(_logger,"RemoveFromDirty sf = {0}",sf);
            base.RemoveFromDirty(sf);
        }

        public override void MergeLoaded(object data)
        {
            Log(_logger,"MergeLoaded data = {0}",data);
            base.MergeLoaded(data);
        }

        public override void Save(IProgressIndicator progress, bool enablePersistence)
        {
            Log(_logger,"Save enablePersistence = {0}",enablePersistence);
            base.Save(progress, enablePersistence);
        }

        public override void Merge(IPsiSourceFile sourceFile, object builtPart)
        {
            Log(_logger,"Merge sf = {0}, builtPart = {1}",sourceFile,builtPart);
            base.Merge(sourceFile, builtPart);
        }

        public override void Drop(IPsiSourceFile sourceFile)
        {
            Log(_logger,"Drop sf = {0}",sourceFile);
            base.Drop(sourceFile);
        }

        public override void SyncUpdate(bool underTransaction)
        {
            Log(_logger,"SyncUpdate underTransaction = {0}",underTransaction);
            base.SyncUpdate(underTransaction);
        }

        public override bool UpToDate(IPsiSourceFile sourceFile)
        {
            bool upToDate = base.UpToDate(sourceFile);
            Log(_logger,"UpToDate {0} for sourceFile = {1}",upToDate,sourceFile);
            return upToDate;
        }
    }
}