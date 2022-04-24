using System;
using JetBrains.Annotations;
using JetBrains.Application.Threading;
using JetBrains.Core;
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
using JetBrains.Threading;
using JetBrains.Util;

namespace Digma.Rider.Protocol
{
    [SolutionComponent]
    public class ElementUnderCaretHost
    {
        private readonly ElementUnderCaretModel _model;
        private readonly Lifetime _lifetime;
        private readonly ISolution _solution;
        private readonly DocumentManager _documentManager;
        private readonly ITextControlManager _textControlManager;
        private readonly IShellLocks _shellLocks;
        private GroupingEvent _groupingEvent;

        public ElementUnderCaretHost(Lifetime lifetime, ISolution solution,
            DocumentManager documentManager,
            ITextControlManager textControlManager, IShellLocks shellLocks, ILogger logger)
        {
            _lifetime = lifetime;
            _solution = solution;
            _documentManager = documentManager;
            _textControlManager = textControlManager;
            _shellLocks = shellLocks;
            _model = solution.GetProtocolSolution().GetElementUnderCaretModel();


            Register();
        }

        private void Register()
        {
            _textControlManager.FocusedTextControlPerClient.BeforeAddRemove.Advise(_lifetime, h =>
                // _textControlManager.FocusedTextControl.Change.Advise(_lifetime, h =>
            {
                OnChange();
            });

            _textControlManager.FocusedTextControlPerClient.ForEachValue_NotNull_AllClients(_lifetime,
                (lifetime1, textControl) =>
                {
                    _groupingEvent = _shellLocks.CreateGroupingEvent(_lifetime,
                        "ElementUnderCaretHost::CaretPositionChanged", TimeSpan.FromMilliseconds(500),
                        OnChange);
                    textControl?.Caret.Position.Change.Advise(_lifetime, cph => { _groupingEvent.FireIncoming(); });
                });
        }


        private void OnChange()
        {
            var textControl = _textControlManager.FocusedTextControlPerClient.ForCurrentClient();
            if (textControl == null)
            {
                EmptyModel();
                return;
            }

            ICSharpFunctionDeclaration declaration = GetMethodUnderCaret(textControl);
            if (declaration != null)
            {
                var namespaceName = declaration.DeclaredElement?.ContainingType?.GetContainingNamespace().QualifiedName;
                var className = declaration.DeclaredElement?.ContainingType?.ShortName;
                var methodName = declaration.DeclaredElement?.ShortName;
                var fqn = namespaceName + "/" + className + "/" + methodName;
                var fileName = declaration.GetSourceFile()?.DisplayName;
                _model.ElementUnderCaret.Value = new ElementUnderCaret(fqn, fileName ?? string.Empty);
                _model.Refresh.Fire(Unit.Instance);
            }
            else
            {
                EmptyModel();
            }
        }


        private void EmptyModel()
        {
            _model.ElementUnderCaret.Value = new ElementUnderCaret(string.Empty, string.Empty);
            _model.Refresh.Fire(Unit.Instance);
        }


        [CanBeNull]
        private ICSharpFunctionDeclaration GetMethodUnderCaret(ITextControl textControl)
        {
            using (ReadLockCookie.Create())
            {
                var node = GetTreeNodeUnderCaret(textControl);
                return node?.GetParentOfType<ICSharpFunctionDeclaration>();
            }
        }


        [CanBeNull]
        private ITreeNode GetTreeNodeUnderCaret(ITextControl textControl)
        {
            var projectFile = _documentManager.GetProjectFile(textControl.Document);

            if (!projectFile.LanguageType.Is<CSharpProjectFileType>()) return null;

            var range = new TextRange(textControl.Caret.Offset());
            var documentRange = range.CreateDocumentRange(projectFile);
            var psiSourceFile = projectFile.ToSourceFile();
            var file = psiSourceFile?.GetPsiFile(psiSourceFile.PrimaryPsiLanguage, documentRange);
            var element = file?.FindNodeAt(documentRange);
            return element;
        }
    }
}