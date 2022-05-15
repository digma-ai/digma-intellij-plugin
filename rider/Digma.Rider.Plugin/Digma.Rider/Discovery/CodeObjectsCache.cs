using Digma.Rider.Protocol;
using JetBrains.Application.Progress;
using JetBrains.Application.Threading;
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
    [PsiComponent]
    public class CodeObjectsCache: SimpleICache<Document>
    {
        private readonly ILogger _logger;

        public CodeObjectsCache(ILogger logger,Lifetime lifetime, IShellLocks locks, IPersistentIndexManager persistentIndexManager) 
            : base(lifetime, locks, persistentIndexManager, new DocumentMarshaller())
        {
            _logger = logger;
        }


        public override object Build(IPsiSourceFile sourceFile, bool isStartup)
        {
            Log(_logger,"Building cache for sourceFile = {0}",sourceFile);
            var isCommitted = sourceFile.GetPsiServices().Files.IsCommitted(sourceFile);
            if (!isCommitted)
                return null;

            var psiFiles = sourceFile.GetPsiFiles<CSharpLanguage>();
            var fileUri = Identities.ComputeFileUri(sourceFile);
            var document = new Document(fileUri);
            foreach (var psiFile in psiFiles)
            {
                var cSharpFile = psiFile.Is<ICSharpFile>();
                if (cSharpFile == null) 
                    continue;
                
                var discoveryProcessor = new CodeObjectsDiscoveryProcessor(fileUri);
                cSharpFile.ProcessDescendants(discoveryProcessor);
                var methodInfos = discoveryProcessor.MethodInfos;
                foreach (var riderMethodInfo in methodInfos)
                {
                    document.Methods.Add(riderMethodInfo.Id,riderMethodInfo);
                }
            }

            //don't keep cache for documents with no code objects, it could also be an interface which we ignore
            return document.HasCodeObjects() ? document : null;
        }

        protected override bool IsApplicable(IPsiSourceFile sf)
        {
            var properties = sf.Properties;
            var primaryPsiLanguage = sf.PrimaryPsiLanguage;
            var isApplicable = !primaryPsiLanguage.IsNullOrUnknown() &&
                               !properties.IsGeneratedFile &&
                               primaryPsiLanguage.Is<CSharpLanguage>() && 
                               properties.ShouldBuildPsi && 
                               properties.ProvidesCodeModel && 
                               properties.IsICacheParticipant;
            Log(_logger,"IsApplicable sf = {0}, applicable = {1}",sf,isApplicable);
            return isApplicable;
        }


        public override object Load(IProgressIndicator progress, bool enablePersistence)
        {
            Log(_logger,"Load enablePersistence = {0}",enablePersistence);
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
    }
}