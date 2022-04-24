using JetBrains.Annotations;
using JetBrains.Application.Threading;
using JetBrains.Core;
using JetBrains.DataFlow;
using JetBrains.Diagnostics;
using JetBrains.DocumentManagers;
using JetBrains.DocumentManagers.Transactions;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.RdBackend.Common.Features;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.RiderTutorials.Utils;
using JetBrains.TextControl;
using JetBrains.TextControl.CodeWithMe;
using JetBrains.Util;

namespace Digma.Rider.Protocol
{
    [SolutionComponent]
    public class ElementUnderCaretHost
    {
        private readonly DocumentManager _documentManager;

        private readonly ILogger _logger;

        public ElementUnderCaretHost(Lifetime lifetime, ISolution solution,
            DocumentManager documentManager,
            ITextControlManager textControlManager, IShellLocks shellLocks, ILogger logger)
        {
            _documentManager = documentManager;
            _logger = logger;

            ElementUnderCaretModel model = solution.GetProtocolSolution().GetElementUnderCaretModel();

            //todo: does not catch non C# files like json
            textControlManager.FocusedTextControlPerClient.ForEachValue_NotNull_AllClients(lifetime,
                (lifetime1, textControl) =>
                {
                    OnCaretPositionChanged(textControl, lifetime1, model);
                });
            // textControlManager.FocusedTextControl.ForEachValue_NotNull(lifetime,
            //     (lifetime1, textControl) => { OnCaretPositionChanged(textControl, lifetime1, model); });
        }


        private void OnCaretPositionChanged(ITextControl textControl, Lifetime lifetime,
            ElementUnderCaretModel model)
        {
            textControl?.Caret.Position.Change.Advise(lifetime, cph =>
            {
                ICSharpFunctionDeclaration declaration = GetMethodUnderCaret(textControl);
                if (declaration != null)
                {
                    var namespaceName = declaration.DeclaredElement?.ContainingType?.GetContainingNamespace().QualifiedName;
                    var className = declaration.DeclaredElement?.ContainingType?.ShortName;
                    var methodName = declaration.DeclaredElement?.ShortName;
                    var fqn = namespaceName + "/" + className + "/" + methodName;
                    var fileName = declaration.GetSourceFile()?.DisplayName;
                    if (fileName != null)
                    {
                        model.ElementUnderCaret.Value = new ElementUnderCaret(fqn, fileName);
                        
                    }
                }
                else
                {
                    model.ElementUnderCaret.Value = new ElementUnderCaret(string.Empty, string.Empty);
                }
                
                model.Refresh.Fire(Unit.Instance);
            });
        }
       
        
        
        [CanBeNull]
        private ICSharpFunctionDeclaration GetMethodUnderCaret(ITextControl textControl)
        {
            using (ReadLockCookie.Create())
            {
                var node = GetTreeNodeUnderCaret(textControl);
                var parentMethod = node?.GetParentOfType<ICSharpFunctionDeclaration>();
                return parentMethod;
            }
        }


        [CanBeNull]
        private ITreeNode GetTreeNodeUnderCaret(ITextControl textControl)
        {
            var projectFile = _documentManager.TryGetProjectFile(textControl.Document);
            if (projectFile == null)
                return null;

            if (!projectFile.LanguageType.Is<CSharpProjectFileType>()) return null;

            var range = new TextRange(textControl.Caret.Offset());
            var psiSourceFile = projectFile.ToSourceFile().NotNull("File is null");
            var documentRange = range.CreateDocumentRange(projectFile);
            var file = psiSourceFile.GetPsiFile(psiSourceFile.PrimaryPsiLanguage, documentRange);
            var element = file?.FindNodeAt(documentRange);
            return element;
        }
    }
}