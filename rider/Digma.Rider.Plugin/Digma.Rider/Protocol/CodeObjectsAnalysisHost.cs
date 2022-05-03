using System.Collections.Generic;
using Digma.Rider.Analysis;
using JetBrains.DocumentManagers;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.RdBackend.Common.Features;
using JetBrains.RdBackend.Common.Features.Components;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.ReSharper.TestRunner.Abstractions.Extensions;
using JetBrains.Rider.Backend.Features.SolutionAnalysis;

namespace Digma.Rider.Protocol
{
    [SolutionComponent]
    public class CodeObjectsAnalysisHost
    {
        public HashSet<ICSharpFile> FilesToReanalyze = new HashSet<ICSharpFile>();
        private readonly ISolution _solution;
        private readonly ShellRdDispatcher _shellRdDispatcher;
        private readonly DocumentManager _documentManager;
        private readonly Lifetime _lifetime;
        private readonly CodeObjectsModel _codeObjectsModel;

        public CodeObjectsAnalysisHost(Lifetime lifetime, ISolution solution,
            RiderSolutionAnalysisServiceImpl riderSolutionAnalysisService,
            ShellRdDispatcher shellRdDispatcher,DocumentManager documentManager)
        {
            _solution = solution;
            _shellRdDispatcher = shellRdDispatcher;
            _documentManager = documentManager;
            _lifetime = lifetime;
            _codeObjectsModel = solution.GetProtocolSolution().GetCodeObjectsModel();
            
            _codeObjectsModel.Reanalyze.Advise(lifetime, file =>
            {
                using (ReadLockCookie.Create())
                {
                    var cSharpFile = Identities.FilePathToCSharpFile(file);
                    if (!FilesToReanalyze.Contains(cSharpFile))
                    {
                        FilesToReanalyze.Add(cSharpFile);
                        riderSolutionAnalysisService.ReanalyzeFile(cSharpFile.GetSourceFile());
                    }
                    
                    //todo: this code doesn't work, can't find the Document from path.
                    // currently keeping a mapping in Identities class
                    // var virtualPath = VirtualFileSystemPath.Parse(file, InteractionContext.Local);
                    // var document = _documentManager.Mapping.TryGetDocumentByPath(virtualPath);
                    // if (document != null)
                    // {
                    //     var sourceFile = document.GetPsiSourceFile(_solution);    
                    //     riderSolutionAnalysisService.ReanalyzeFile(sourceFile);
                    // }
                    // else
                    // {
                    //     riderSolutionAnalysisService.ReanalyzeAll();
                    // }
                }
            });
        }


        public void NotifyFileOpened(string file)
        {
            _shellRdDispatcher.UnguardedScheduler.Queue(() =>
            {
                //maybe there where no findings for this document, don't change the value in that case
                if (_codeObjectsModel.Documents.ContainsKey(file))
                {
                    _codeObjectsModel.FileAnalyzed.Fire(file);
                }
            });
        }


      
        public void AddOrUpdateMethodInfo(string filePath,string methodFqn, RiderMethodInfo methodInfo)
        {
            _shellRdDispatcher.UnguardedScheduler.Queue(() =>
            {
                var document = _codeObjectsModel.Documents.GetOrCreate(filePath, () => new Document());
                document.Methods.Remove(methodFqn);
                document.Methods.Add(methodFqn, methodInfo);
            });
        }

        public RiderCodeLensInfo GetRiderCodeLensInfo(string fqn)
        {
            using (ReadLockCookie.Create())
            {
                _codeObjectsModel.CodeLens.TryGetValue(fqn, out var lensInfo);
                return lensInfo;
            }
        }
    }
}