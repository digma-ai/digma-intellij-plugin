using Digma.Rider.Discovery;
using Digma.Rider.Util;
using JetBrains.Annotations;
using JetBrains.DocumentManagers;
using JetBrains.DocumentManagers.Transactions;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.RiderTutorials.Utils;
using JetBrains.TextControl;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Protocol
{
    
    
    [SolutionComponent]
    public class MethodUnderCaretDetector
    {
        private readonly Lifetime _lifetime;
        private readonly ISolution _solution;
        private readonly DocumentManager _documentManager;
        private readonly ITextControlManager _textControlManager;
        private readonly ILogger _logger;

        public MethodUnderCaretDetector(Lifetime lifetime, 
            ISolution solution,
            DocumentManager documentManager,
            ITextControlManager textControlManager, 
            ILogger logger)
        {
            _lifetime = lifetime;
            _solution = solution;
            _documentManager = documentManager;
            _textControlManager = textControlManager;
            _logger = logger;
        }


        [CanBeNull]    
        public RiderMethodUnderCaret Detect(MethodUnderCaretRequest methodUnderCaretRequest)
        {
            //the LastFocusedTextControl is not always updated on time, sometimes its too early to detect
            // method under caret from the text control. sometimes the frontend sends the request
            // before LastFocusedTextControl is updated and it may still contain the previous text control.
            //This method will detect by finding the IPsiSourceFile and the offset sent from the frontend.
            
            Log(_logger, "Detecting method under caret by psi file and offset for {0}",methodUnderCaretRequest.PsiId);
            return DetectByPsiFile(methodUnderCaretRequest);
            
            //an attempt to detect from the LastFocusedTextControlPerClient proves to be too early 
            // and may find the previous text control
            // Log(_logger, "Detect invoked");
            // var textControl = _textControlManager.LastFocusedTextControlPerClient.ForCurrentClient();
            // if (textControl == null || textControl.Lifetime.IsNotAlive)
            // {
            //     Log(_logger, "LastFocusedTextControlPerClient is null or not alive for {0}",methodUnderCaretRequest.PsiId.PsiUri);
            //     return DetectByPsiFile(methodUnderCaretRequest);
            // }
            // else
            // {
            //     Log(_logger, "LastFocusedTextControlPerClient is found {0}",textControl.Document);
            //     return DetectFromTextControl(textControl,methodUnderCaretRequest);    
            // }
        }
        
        
        

        [CanBeNull]
        private RiderMethodUnderCaret DetectFromTextControl([NotNull] ITextControl textControl,
            MethodUnderCaretRequest methodUnderCaretRequest)
        {
            Log(_logger, "Trying to detect method under caret for text control {0}", textControl.Document);
            using (ReadLockCookie.Create())
            {
                var psiSourceFile = _documentManager.GetProjectFile(textControl.Document).ToSourceFile();

                if (psiSourceFile == null)
                {
                    Log(_logger, "PsiSourceFile not found for textControl {0}", textControl.Document);
                    return null;
                }
                
                if (PsiUtils.IsPsiSourceFileApplicable(psiSourceFile))
                {
                    Log(_logger, "PsiSourceFile {0} is applicable for method under caret.",psiSourceFile);
                    return DetectFromTextControl(textControl, psiSourceFile,methodUnderCaretRequest);
                }
                else
                {
                    Log(_logger, "PsiSourceFile {0} is not applicable for method under caret.",psiSourceFile);
                    var fileUri = textControl.Document.ToString();
                    var emptyMethodUnderCaret =
                        new RiderMethodUnderCaret(string.Empty, string.Empty, string.Empty, fileUri,false);
                    return emptyMethodUnderCaret;
                }
            }
        }

        
        [NotNull]
        private RiderMethodUnderCaret DetectFromTextControl(ITextControl textControl, IPsiSourceFile psiSourceFile,MethodUnderCaretRequest methodUnderCaretRequest)
        {
            if (!psiSourceFile.GetPsiServices().Files.IsCommitted(psiSourceFile))
            {
                Log(_logger,
                    "PsiSourceFile is not committed for textControl {0}, will execute after all documents are committed.",
                    textControl.Document);
            }

            RiderMethodUnderCaret foundMethodUnderCaret = null;
            psiSourceFile.GetPsiServices().Files.DoOnCommitedPsi(_lifetime, () =>
            {
                Log(_logger, "Task DoOnCommitedPsi for {0}", psiSourceFile);
                var functionDeclaration = GetFunctionUnderCaret(textControl, psiSourceFile);
                foundMethodUnderCaret = GetMethodUnderCaretFromFunction(methodUnderCaretRequest, functionDeclaration, psiSourceFile);
            });

            return foundMethodUnderCaret;

        }

        
        
        

        [CanBeNull]
        private RiderMethodUnderCaret DetectByPsiFile(MethodUnderCaretRequest methodUnderCaretRequest)
        {
            Log(_logger, "Trying to detect method under caret for by psi file {0}", methodUnderCaretRequest.PsiId);
            RiderMethodUnderCaret methodUnderCaret = null;
            using (ReadLockCookie.Create())
            {
                var psiSourceFile = PsiUtils.FindPsiSourceFile(methodUnderCaretRequest.PsiId, _solution);
                if (psiSourceFile != null)
                {
                    psiSourceFile.GetPsiServices().Files.DoOnCommitedPsi(_lifetime, () =>
                    {
                        Log(_logger, "Found IPsiSourceFile for {0}", methodUnderCaretRequest.PsiId);
                        var functionDeclaration = GetFunctionUnderCaret(psiSourceFile, methodUnderCaretRequest);
                        methodUnderCaret = GetMethodUnderCaretFromFunction(methodUnderCaretRequest, functionDeclaration,
                            psiSourceFile);
                    });
                }
                else
                {
                    Log(_logger, "Could not find IPsiSourceFile for {0}.", methodUnderCaretRequest.PsiId);
                }
            }

            if (methodUnderCaret == null)
            {
                Log(_logger, "Found RiderMethodUnderCaret for {0}, '{1}'", methodUnderCaretRequest.PsiId,methodUnderCaret);
            }
            else
            {
                Log(_logger, "Could not find RiderMethodUnderCaret for {0}", methodUnderCaretRequest.PsiId);
            }
            return methodUnderCaret;
        }

        
        
        [NotNull]
        private RiderMethodUnderCaret GetMethodUnderCaretFromFunction(MethodUnderCaretRequest methodUnderCaretRequest,
            ICSharpFunctionDeclaration functionDeclaration, IPsiSourceFile psiSourceFile)
        {
            RiderMethodUnderCaret foundMethodUnderCaret;
            if (functionDeclaration != null)
            {
                Log(_logger, "Found function under caret: {0} for {1}", functionDeclaration,
                    methodUnderCaretRequest.PsiId);
                var methodFqn = Identities.ComputeFqn(functionDeclaration);
                var methodName = PsiUtils.GetDeclaredName(functionDeclaration);
                var className = PsiUtils.GetClassName(functionDeclaration);
                var fileUri = Identities.ComputeFileUri(psiSourceFile);
                var isSupportedFile = PsiUtils.IsPsiSourceFileApplicable(psiSourceFile);
                var methodUnderCaret =
                    new RiderMethodUnderCaret(methodFqn, methodName, className, fileUri, isSupportedFile);
                Log(_logger, "Creating RiderMethodUnderCaret {0} for function {1}", methodUnderCaret,
                    functionDeclaration);
                foundMethodUnderCaret = methodUnderCaret;
            }
            else
            {
                Log(_logger, "No function under caret for {0}", methodUnderCaretRequest.PsiId);
                var fileUri = Identities.ComputeFileUri(psiSourceFile);
                var isSupportedFile = PsiUtils.IsPsiSourceFileApplicable(psiSourceFile);
                foundMethodUnderCaret = new RiderMethodUnderCaret("", "", "", fileUri, isSupportedFile);
            }

            return foundMethodUnderCaret;
        }

        
        
        
        
        private ICSharpFunctionDeclaration GetFunctionUnderCaret(IPsiSourceFile psiSourceFile, MethodUnderCaretRequest methodUnderCaretRequest)
        {
            var node = GetTreeNodeUnderCaret(psiSourceFile, psiSourceFile.ToProjectFile(),
                methodUnderCaretRequest.Offset);
            return GetFunctionFromNode(node);
        }


        [CanBeNull]
        private ICSharpFunctionDeclaration GetFunctionUnderCaret([NotNull] ITextControl textControl,
            [NotNull] IPsiSourceFile psiSourceFile)
        {
            var node = GetTreeNodeUnderCaret(textControl, psiSourceFile);
            return GetFunctionFromNode(node);
        }
        
        
        private ICSharpFunctionDeclaration GetFunctionFromNode(ITreeNode node)
        {
            if (node?.GetParentOfType<IInterfaceDeclaration>() != null)
            {
                //Ignore interfaces
                return null;
            }

            return node?.GetParentOfType<ICSharpFunctionDeclaration>();
        }
        
        
        
        
        
        [CanBeNull]
        private ITreeNode GetTreeNodeUnderCaret(ITextControl textControl, IPsiSourceFile psiSourceFile)
        {
            var projectFile = _documentManager.GetProjectFile(textControl.Document);
            if (!projectFile.LanguageType.Is<CSharpProjectFileType>())
                return null;
            return GetTreeNodeUnderCaret(psiSourceFile,projectFile,textControl.Caret.Offset());
        }
        
        
        [CanBeNull]
        private ITreeNode GetTreeNodeUnderCaret(IPsiSourceFile psiSourceFile, IProjectFile projectFile, int offset)
        {
            var range = new TextRange(offset);
            var documentRange = range.CreateDocumentRange(projectFile);
            var file = psiSourceFile.GetPsiFile(psiSourceFile.PrimaryPsiLanguage, documentRange);
            return file?.FindNodeAt(documentRange);
        }
        

    }
}