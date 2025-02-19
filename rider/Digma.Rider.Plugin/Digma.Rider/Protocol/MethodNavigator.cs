using System.Diagnostics.CodeAnalysis;
using Digma.Rider.Discovery;
using JetBrains.Annotations;
using JetBrains.Application.Parts;
using JetBrains.DocumentManagers;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.CSharp.CompleteStatement;
using JetBrains.ReSharper.Feature.Services.Navigation;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Caches;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.ReSharper.TestRunner.Abstractions.Extensions;
using JetBrains.TextControl;
using JetBrains.TextControl.CodeWithMe;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Protocol
{
#if (PROFILE_2024_3)
    [SolutionComponent(Instantiation.ContainerAsyncPrimaryThread)]
#else
    [SolutionComponent]
#endif    

    public class MethodNavigator
    {
        private readonly ITextControlManager _textControlManager;
        private readonly DocumentManager _documentManager;
        private readonly IPsiServices _psiServices;
        private readonly ILogger _logger;

        public MethodNavigator(ITextControlManager textControlManager,
            DocumentManager documentManager,
            IPsiServices psiServices,
            ILogger logger)
        {
            _textControlManager = textControlManager;
            _documentManager = documentManager;
            _psiServices = psiServices;
            _logger = logger;
        }

        public void Navigate(string methodId)
        {
            Log(_logger, "Got navigate request to {0}",methodId);

            var found = false;
            using (CompilationContextCookie.GetExplicitUniversalContextIfNotSet())
            {

                var className = methodId.SubstringBefore("$_$");
                var methodName = methodId.SubstringAfter("$_$").SubstringBefore("(");
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
                                    if (typeElementMethod.GetSingleDeclaration() != null &&
                                        typeElementMethod.GetSingleDeclaration()!.DeclaredElement != null)
                                    {
                                        Log(_logger, "Navigating to method {0}",typeElementMethod);
                                        typeElementMethod.GetSingleDeclaration()!.DeclaredElement!.Navigate(true);
                                        found = true;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (!found)
            {
                Log(_logger, "Could not navigate to {0}, trying fallback",methodId);
                NavigateFallback(methodId);
            }

        }
        
        
        private void NavigateFallback(string methodId)
        {
           
            Log(_logger, "Got NavigateFallback request to {0}",methodId);

            var textControl = _textControlManager.LastFocusedTextControlPerClient.ForCurrentClient();
            if (textControl == null )
            {
                Log(_logger, "LastFocusedTextControl is null, not navigating to method {0}", methodId);
                return;
            }
            
            Log(_logger, "Found textControl for method {0} , document {1}",methodId,textControl.Document);
            ICSharpFunctionDeclaration declaration = null;
            using (ReadLockCookie.Create())
            {
                var psiSourceFile = FindPsiSourceFile(textControl,methodId);
                if (psiSourceFile == null)
                {
                    return;
                }
                
                Log(_logger, "Found psiSourceFile {0} for method {1}",psiSourceFile,methodId);
                
                var psiFiles = psiSourceFile.GetPsiFiles<CSharpLanguage>();
                foreach (var psiFile in psiFiles)
                {
                    var cSharpFile = psiFile.Is<ICSharpFile>();
                    if (cSharpFile == null) 
                        continue;
                
                    Log(_logger, "Searching for method {0} in file {1}",methodId,cSharpFile);
                    var methodFinderProcessor = new MethodFinderProcessor(methodId);
                    cSharpFile.ProcessDescendants(methodFinderProcessor);

                    if (methodFinderProcessor.FoundFunctionDeclaration != null)
                    {
                        Log(_logger, "Found method {0} in file {1}",methodId,cSharpFile);
                        declaration = methodFinderProcessor.FoundFunctionDeclaration;
                        break;
                    }
                }
            }

            if (declaration != null)
            {
                FocusOn(textControl,declaration);
            }
            else
            {
                Log(_logger, "Method {0} was not found in {1}",methodId,textControl.Document);
            }
        }

        private void FocusOn(ITextControl textControl,[JetBrains.Annotations.NotNull] ICSharpFunctionDeclaration declaration)
        {
            Log(_logger, "Moving caret in {0} to {1}",textControl.Document,declaration);

            using (ReadLockCookie.Create())
            {
                using (CompilationContextCookie.GetExplicitUniversalContextIfNotSet())
                {
                    declaration.DeclaredElement?.Navigate(true);
                }
            }
        }




        private IPsiSourceFile FindPsiSourceFile(ITextControl textControl, string methodId)
        {
            var psiSourceFile = _documentManager.GetProjectFile(textControl.Document).ToSourceFile();
            if (psiSourceFile == null || !psiSourceFile.GetPsiServices().Files.IsCommitted(psiSourceFile))
            {
                Log(_logger, "No psiSourceFile found for {0}",methodId);
                return null;
            }
            var properties = psiSourceFile.Properties;
            var primaryPsiLanguage = psiSourceFile.PrimaryPsiLanguage;
            var isApplicable = !primaryPsiLanguage.IsNullOrUnknown() &&
                               !properties.IsGeneratedFile &&
                               primaryPsiLanguage.Is<CSharpLanguage>() &&
                               properties.ShouldBuildPsi &&
                               properties.ProvidesCodeModel;

            if (!isApplicable)
            {
                Log(_logger, "PsiSourceFile {0} is not applicable for method navigation", psiSourceFile);
                return null;
            }

            return psiSourceFile;
        }
        
        
        


        private class MethodFinderProcessor: IRecursiveElementProcessor
        {
            private readonly string _methodId;

            public MethodFinderProcessor(string methodId)
            {
                _methodId = methodId;
                ProcessingIsFinished = false;
            }

            public bool ProcessingIsFinished { get; private set; }

            public ICSharpFunctionDeclaration FoundFunctionDeclaration { get; private set; }

            [SuppressMessage("ReSharper", "UnusedVariable")]
            public bool InteriorShouldBeProcessed(ITreeNode element)
            {
                switch (element)
                {
                    //visiting only IClassDeclaration effectively ignores interfaces
                    case ICSharpNamespaceDeclaration cSharpNamespaceDeclaration:
                    case INamespaceBody namespaceBody:
                    case IClassDeclaration classDeclaration:
                    case IClassBody classBody:
                        return true;
                }

                return false;
            }

            public void ProcessBeforeInterior(ITreeNode element)
            {
                switch (element)
                {
                    case ICSharpFunctionDeclaration functionDeclaration:
                    {
                        var methodFqn = Identities.ComputeFqn(functionDeclaration);
                        if (methodFqn.Equals(_methodId))
                        {
                            FoundFunctionDeclaration = functionDeclaration;
                            ProcessingIsFinished = true;
                        }
                        break;
                    }
                }
            }

            public void ProcessAfterInterior(ITreeNode element)
            {
                
            }

            
        }
        
    }
    
    
}