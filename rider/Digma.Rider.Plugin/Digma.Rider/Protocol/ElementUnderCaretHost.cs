using System;
using System.Runtime.CompilerServices;
using Digma.Rider.Discovery;
using JetBrains.Annotations;
using JetBrains.Application.Threading;
using JetBrains.Core;
using JetBrains.DocumentManagers;
using JetBrains.DocumentManagers.Transactions;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.RdBackend.Common.Features;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.RiderTutorials.Utils;
using JetBrains.TextControl;
using JetBrains.TextControl.CodeWithMe;
using JetBrains.Threading;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Protocol
{
    [SolutionComponent]
    public class ElementUnderCaretHost
    {
        private readonly ElementUnderCaretModel _model;
        private readonly Lifetime _lifetime;
        private readonly DocumentManager _documentManager;
        private readonly ITextControlManager _textControlManager;
        private readonly IShellLocks _shellLocks;
        private readonly ILogger _logger;
        private readonly EditorListener _editorListener;
        private GroupingEvent _groupingEvent;

        public ElementUnderCaretHost(Lifetime lifetime, ISolution solution,
            DocumentManager documentManager,
            ITextControlManager textControlManager, IShellLocks shellLocks, ILogger logger,
            EditorListener editorListener)
        {
            _lifetime = lifetime;
            _documentManager = documentManager;
            _textControlManager = textControlManager;
            _shellLocks = shellLocks;
            _logger = logger;
            _editorListener = editorListener;
            _model = solution.GetProtocolSolution().GetElementUnderCaretModel();


            Register();
        }

        private void Register()
        {
            
            _textControlManager.TextControls.BeforeAddRemove.Advise(_lifetime, h => {
                if (h.IsAdding)
                {
                    Log(_logger, "BeforeAddRemove adding {0}",h.Value?.Document);
                }else if (h.IsRemoving)
                {
                    Log(_logger, "BeforeAddRemove removing {0}",h.Value?.Document);
                }
                else
                {
                    Log(_logger, "BeforeAddRemove not adding and nor removing??? {}",h.Value);
                }
            });
            
            _textControlManager.FocusedTextControlPerClient.ForEachValue_NotNull_AllClients(_lifetime,
                (lifetime1, textControl) =>
                {
                
                    _groupingEvent = _shellLocks.CreateGroupingEvent(textControl.Lifetime,
                        "ElementUnderCaretHost::CaretPositionChanged", TimeSpan.FromMilliseconds(300),
                        OnChange);
                    textControl.Caret.Position.Change.Advise(textControl.Lifetime,
                        cph => { _groupingEvent.FireIncoming(); });
                });
        }


        private void OnChange()
        {
            var textControl = _textControlManager.LastFocusedTextControlPerClient.ForCurrentClient();
            if (textControl == null || textControl.Lifetime.IsNotAlive)
            {
                Log(_logger, "OnChange TextControl is null");
                EmptyModel(null);
                return;
            }

            Log(_logger, "Trying to discover method under caret for {0}", textControl.Document);
            using (ReadLockCookie.Create())
            {
                var psiSourceFile = _documentManager.GetProjectFile(textControl.Document).ToSourceFile();
                var functionDeclaration = GetFunctionUnderCaret(textControl);
                if (functionDeclaration != null && psiSourceFile != null)
                {
                    Log(_logger, "Got function under caret: {0} for {1}", functionDeclaration, textControl.Document);
                    var methodFqn = Identities.ComputeFqn(functionDeclaration);
                    var methodName = PsiUtils.GetDeclaredName(functionDeclaration);
                    var className = PsiUtils.GetClassName(functionDeclaration);
                    var fileUri = Identities.ComputeFileUri(psiSourceFile);
                    var newElementUnderCaret =
                        new MethodUnderCaretEvent(methodFqn, methodName, className, fileUri);
                    UpdateModel(newElementUnderCaret);
                }
                else
                {
                    Log(_logger, "No function under caret for {0}", textControl.Document);
                    EmptyModel(psiSourceFile);
                }
            }
        }


        private void EmptyModel([CanBeNull] IPsiSourceFile psiSourceFile)
        {
            var fileUri = string.Empty;
            if (psiSourceFile != null)
            {
                fileUri = Identities.ComputeFileUri(psiSourceFile);
            }
            var newElementUnderCaret =
                new MethodUnderCaretEvent(string.Empty, string.Empty, string.Empty, fileUri );
            UpdateModel(newElementUnderCaret);
        }


        private void UpdateModel([NotNull] MethodUnderCaretEvent newMethodUnderCaretEvent)
        {
            if (!newMethodUnderCaretEvent.Equals(_model.ElementUnderCaret.Maybe.ValueOrDefault))
            {
                _model.ElementUnderCaret.Value = newMethodUnderCaretEvent;
                _model.NotifyElementUnderCaret.Fire(Unit.Instance);
            }
            
        }
        
        
        

        [CanBeNull]
        private ICSharpFunctionDeclaration GetFunctionUnderCaret(ITextControl textControl)
        {
            using (ReadLockCookie.Create())
            {
                var node = GetTreeNodeUnderCaret(textControl);
                if (node?.GetParentOfType<IInterfaceDeclaration>() != null)
                {
                    //Ignore interfaces
                    return null;
                }

                return node?.GetParentOfType<ICSharpFunctionDeclaration>();
            }
        }


        [CanBeNull]
        private ITreeNode GetTreeNodeUnderCaret(ITextControl textControl)
        {
            var psiSourceFile = _documentManager.GetProjectFile(textControl.Document).ToSourceFile();
            if (psiSourceFile == null || !psiSourceFile.GetPsiServices().Files.IsCommitted(psiSourceFile))
            {
                Log(_logger, "PsiSourceFile {0} is null or not committed", psiSourceFile);
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
                Log(_logger, "PsiSourceFile {0} is not applicable for method under caret", psiSourceFile);
                return null;
            }

            var projectFile = _documentManager.GetProjectFile(textControl.Document);
            if (!projectFile.LanguageType.Is<CSharpProjectFileType>())
                return null;

            var range = new TextRange(textControl.Caret.Offset());
            var documentRange = range.CreateDocumentRange(projectFile);
            var file = psiSourceFile.GetPsiFile(psiSourceFile.PrimaryPsiLanguage, documentRange);
            return file?.FindNodeAt(documentRange);
        }
    }
}