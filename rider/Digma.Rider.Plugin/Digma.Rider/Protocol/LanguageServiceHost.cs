using System;
using System.Collections.Generic;
using Digma.Rider.Discovery;
using Digma.Rider.Util;
using JetBrains.Annotations;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.Rd.Tasks;
using JetBrains.RdBackend.Common.Features;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Caches;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Util;
using JetBrains.Util.Extension;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Protocol
{
    [SolutionComponent]
    public class LanguageServiceHost
    {
        private readonly Lifetime _lifetime;
        private readonly ISolution _solution;
        private readonly ILogger _logger;
        private readonly IPsiServices _psiServices;
        private readonly MethodUnderCaretDetector _methodUnderCaretDetector;
        private readonly CodeObjectsModel _codeObjectsModel;
        private readonly CodeObjectsCache _codeObjectsCache;
        private readonly MethodNavigator _methodNavigator;

        public LanguageServiceHost(Lifetime lifetime, ISolution solution,
            CodeObjectsCache codeObjectsCache,
            MethodNavigator methodNavigator,
            ILogger logger,
            IPsiServices psiServices,
            MethodUnderCaretDetector methodUnderCaretDetector)
        {
            _lifetime = lifetime;
            _solution = solution;
            _codeObjectsCache = codeObjectsCache;
            _methodNavigator = methodNavigator;
            _logger = logger;
            _psiServices = psiServices;
            _methodUnderCaretDetector = methodUnderCaretDetector;
            _codeObjectsModel = solution.GetProtocolSolution().GetCodeObjectsModel();
            var languageServiceModel = solution.GetProtocolSolution().GetLanguageServiceModel();


            languageServiceModel.GetDocumentInfo.Set(GetDocumentInfoHandler);
            languageServiceModel.DetectMethodUnderCaret.Set(DetectMethodUnderCaret);
            languageServiceModel.GetWorkspaceUris.Set(GetWorkspaceUris);
            languageServiceModel.GetSpansWorkspaceUris.Set(GetSpansWorkspaceUris);
            languageServiceModel.IsCsharpMethod.Set(IsCsharpMethod);
            languageServiceModel.NavigateToMethod.Advise(lifetime, NavigateToMethod);
        }


        [CanBeNull]
        public IList<RiderCodeLensInfo> GetRiderCodeLensInfo([NotNull] string codeObjectId)
        {
            using (ReadLockCookie.Create())
            {
                return _codeObjectsModel.CodeLens.TryGetValue(codeObjectId)?.Lens;
            }
        }
        
        
        //The RdTask may return null, or the method may throw exception. the method itself never returns null it returns RDTask.
        // will return null if there is no document info for the file, for example for interfaces or classes with no methods.
        // will throw exception if the psi file not found which is bad, it means some problem with the client because the
        // client sent the psi uri. 
        [NotNull]
        private RdTask<RiderDocumentInfo> GetDocumentInfoHandler(Lifetime _, PsiFileID psiFileId)
        {
            Log(_logger, "Got request for GetDocumentInfo for '{0}'", psiFileId.PsiUri);
            
            var result = new RdTask<RiderDocumentInfo>();

            RiderDocumentInfo document = null;
            using (ReadLockCookie.Create())
            {
                var psiSourceFile = PsiUtils.FindPsiSourceFile(psiFileId, _solution);
                
                    if (psiSourceFile != null)
                    {
                        psiSourceFile.GetPsiServices().Files.DoOnCommitedPsi(_lifetime, () =>
                        {
                            Log(_logger, "Found IPsiSourceFile for '{0}'", psiFileId.PsiUri);
                            document = _codeObjectsCache.Map.TryGetValue(psiSourceFile);

                            if (document is { IsComplete: false })
                            {
                                document = BuildDocumentOnDemand(psiSourceFile);
                            }
                        });
                    }
                    else
                    {
                        Log(_logger, "Could not find IPsiSourceFile in GetDocumentInfo for '{0}', Aborting request",
                            psiFileId.PsiUri);
                    }
            }

            if (document == null)
            {
                result.Set((RiderDocumentInfo)null);    
            }
            else
            {
                result.Set(document);
            }
            
            return result;
        }


        [NotNull]
        private RdTask<RiderMethodUnderCaret> DetectMethodUnderCaret(Lifetime _,
            MethodUnderCaretRequest methodUnderCaretRequest)
        {
            Log(_logger, "Got request for GetDocumentInfo for '{0}'", methodUnderCaretRequest.PsiId.PsiUri);
            var result = new RdTask<RiderMethodUnderCaret>();
            RiderMethodUnderCaret methodUnderCaret;
            using (ReadLockCookie.Create())
            {
                methodUnderCaret = _methodUnderCaretDetector.Detect(methodUnderCaretRequest);
            }

            //never return null
            if (methodUnderCaret == null)
            {
                methodUnderCaret = new RiderMethodUnderCaret("", "", "", "", false);
            }

            result.Set(methodUnderCaret);
            return result;
        }


        [NotNull]
        private RdTask<List<CodeObjectIdUriPair>> GetWorkspaceUris(Lifetime _, List<string> methodsCodeObjectIds)
        {
            var result = new RdTask<List<CodeObjectIdUriPair>>();
            var uris = new List<CodeObjectIdUriPair>();

            using (CompilationContextCookie.GetExplicitUniversalContextIfNotSet())
            {
                foreach (var methodId in methodsCodeObjectIds)
                {
                    var className = methodId.SubstringBefore("$_$");
                    var methodName = methodId.SubstringAfter("$_$").SubstringBefore("(");
                    if (className != null && className.IsNotEmpty() && methodName != null && methodName.IsNotEmpty())
                    {
                        //LibrarySymbolScope.NONE means we'll find only workspace classes
                        var symbolScope = _psiServices.Symbols.GetSymbolScope(LibrarySymbolScope.NONE, false);
                        var elements = symbolScope.GetTypeElementsByCLRName(className);
                        using (ReadLockCookie.Create())
                        {
                            foreach (var typeElement in elements)
                            {
                                if (typeElement.IsClassLike())
                                {
                                    foreach (var typeElementMethod in typeElement.Methods)
                                    {
                                        if (typeElementMethod.ShortName.Equals(methodName))
                                        {
                                            if (typeElementMethod.GetSourceFiles().SingleItem != null)
                                            {
                                                var fileUri = typeElementMethod.GetSourceFiles().SingleItem.GetLocation()
                                                    .ToUri().ToString();
                                                uris.Add(new CodeObjectIdUriPair(methodId, fileUri));
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            result.Set(uris);
            return result;
        }


        //todo: very bad performance. find a was to query resharper caches or build a cache for that
        [NotNull]
        private RdTask<List<CodeObjectIdUriOffsetTrouple>> GetSpansWorkspaceUris(Lifetime _, List<string> list)
        {
            var result = new RdTask<List<CodeObjectIdUriOffsetTrouple>>();
            var uris = new List<CodeObjectIdUriOffsetTrouple>();
            
            using (ReadLockCookie.Create())
            {
                try
                {
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
                            document = BuildDocumentOnDemand(psiSourceFile);
                        }

                        if (document == null)
                        {
                            Log(_logger,
                                "Still could not find Document for psiSourceFile '{0}' in the cache. Aborting operation.",
                                psiSourceFile);
                            continue;
                        }

                        foreach (var riderMethodInfo in document.Methods)
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
                }
                catch (Exception e)
                {
                    //todo: maybe throw an error to notify the fronend ?
                    _logger.Error(e, "Error searching documents uris");
                }
            }

            result.Set(uris);
            return result;
        }

        
        private RdTask<bool> IsCsharpMethod(Lifetime _, string methodCodeObjectId)
        {
            var result = new RdTask<bool>();
            result.Set(false);

            using (CompilationContextCookie.GetExplicitUniversalContextIfNotSet())
            {
                if (methodCodeObjectId.StartsWith("method:"))
                {
                    methodCodeObjectId = methodCodeObjectId.SubstringAfter("method:");
                }
                var className = methodCodeObjectId.SubstringBefore("$_$");
                var methodName = methodCodeObjectId.SubstringAfter("$_$").SubstringBefore("(");
                var symbolScope = _psiServices.Symbols.GetSymbolScope(LibrarySymbolScope.FULL, false);
                var elements = symbolScope.GetTypeElementsByCLRName(className);
                using (ReadLockCookie.Create())
                {
                    foreach (var typeElement in elements)
                    {
                        if (typeElement.IsClassLike())
                        {
                            foreach (var typeElementMethod in typeElement.Methods)
                            {
                                if (typeElementMethod.ShortName.Equals(methodName))
                                {
                                    result = new RdTask<bool>();
                                    result.Set(true);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            return result;
        }


        private void NavigateToMethod(string message)
        {
            _methodNavigator.Navigate(message);
        }

        
        //its a dilemma: to return null or not ?
        [CanBeNull]
        // [NotNull]
        private RiderDocumentInfo BuildDocumentOnDemand([NotNull] IPsiSourceFile psiSourceFile)
        {
            using (CompilationContextCookie.GetOrCreate(psiSourceFile.ResolveContext))
            {
                Log(_logger, "Building document on demand for PsiSourceFile '{0}'", psiSourceFile);
                //_codeObjectsCache.Build may return null because it doesn't build for interfaces or classes with no
                // methods.
                var document = (RiderDocumentInfo)_codeObjectsCache.Build(psiSourceFile, false);
                Log(_logger, "Document on demand for PsiSourceFile '{0}' is {1}", psiSourceFile, document);
                //only add the document to the cache if it has code objects
                if (document != null && document.HasCodeObjects())
                {
                    //todo: instead of calling merge we can just call MarkAsDirty if this is a cached file.
                    // then fix code that expects an immediate merge like GetSpansWorkspaceUris
                    Log(_logger, "Merging Document to cache for PsiSourceFile '{0}'", psiSourceFile);
                    _codeObjectsCache.Merge(psiSourceFile, document);
                }

                //still may return null 
                document = _codeObjectsCache.Map.TryGetValue(psiSourceFile);

                // if (document == null)
                // {
                //     Log(_logger, "Document from cache after building on demand for PsiSourceFile '{0}' was null ,probably an interface or class with no methods. returning empty document",
                //         psiSourceFile);
                //     document = new RiderDocumentInfo(true, Identities.ComputeFileUri(psiSourceFile),new List<RiderMethodInfo>());
                // }
                // else
                // {
                    Log(_logger, "Document from cache after building on demand for PsiSourceFile '{0}'  is {1}",
                        psiSourceFile, document);
                // }
                return document;
            }
        }
    }
}