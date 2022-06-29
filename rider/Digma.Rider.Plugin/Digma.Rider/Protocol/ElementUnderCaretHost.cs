using System;
using Digma.Rider.Discovery;
using Digma.Rider.Util;
using JetBrains.Annotations;
using JetBrains.Application.Threading;
using JetBrains.Core;
using JetBrains.DataFlow;
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
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Protocol
{
    /// <summary>
    /// This class listens to caret events and updates ElementUnderCaretModel when necessary.
    /// ElementUnderCaretModel will generate events that the frontend catches and changes the plugin context.
    /// </summary>
    [SolutionComponent]
    public class ElementUnderCaretHost
    {
        private readonly ElementUnderCaretModel _model;
        private readonly Lifetime _lifetime;
        private readonly DocumentManager _documentManager;
        private readonly ITextControlManager _textControlManager;
        private readonly IShellLocks _shellLocks;
        private readonly ILogger _logger;

        // ReSharper disable once NotAccessedField.Local
        // make a dependency on EditorListener to hopefully start here
        // only after EditorListener has started so it will be first in the listener list for text documents. probably 
        // not really necessary!
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


            //just for debugging
            ///new TextControlsLoggerUtil(_textControlManager, _logger, _lifetime);


            //todo: Currently one scenario is not covered: sometimes a document is opened but does not gain focus,
            // our current behaviour in that case is that the model is cleared when the text control is added, and if the text control
            // does not gain focus and does not generate a caret event it will be the opened document but our plugin
            // context will be empty. touching the editor with the mouse will generate the caret event and the plugin context
            // will change correctly.
            // the desired behaviour is probably even if the text control didn't gain focus we still want to show the methods
            // preview list.
            // to fix that we can send an ElementUnderCaret event with only fileUri when the text control is added to VisibleTextControls
            // so that our context will change and the insights preview list will show. but then when clicking a link in the list
            // the MethodNavigator will not find the method in LastFocusedTextControl and needs to try also the text controls in 
            // VisibleTextControls

            Register();
        }

        private void Register()
        {
            _textControlManager.TextControls.BeforeAddRemove.Advise(_lifetime, h =>
            {
                if (h.IsAdding)
                {
                    //always clear the context when adding a text control. if the text control will gain focus it will 
                    //generate a caret event and the context will be changed.
                    Log(_logger, "Clearing model on TextControls BeforeAddRemove, adding {0}", h.Value?.Document);
                    ClearModel();
                }
            });

            _textControlManager.TextControls.AddRemove.Advise(_lifetime, h =>
            {
                if (h.IsRemoving)
                {
                    if (_textControlManager.TextControls.Count == 0)
                    {
                        //need to clear the context when the last text control is removed. i.e when closing 
                        //the last one or 'close all tabs'.
                        Log(_logger, "Last text control removed, clearing model");
                        ClearModel();
                    }
                }
            });


            _textControlManager.FocusedTextControlPerClient.ForEachValue_NotNull_AllClients(_lifetime,
                (_, textControl) =>
                {
                    _groupingEvent = _shellLocks.CreateGroupingEvent(textControl.Lifetime,
                        "ElementUnderCaretHost::CaretPositionChanged", TimeSpan.FromMilliseconds(100),
                        OnChange);
                    textControl.Caret.Position.Change.Advise(textControl.Lifetime,
                        h =>
                        {
                            Log(_logger, "Got Caret.Position.Change event for document {0}, client id {1} new pos:{2}, old pos:{3}",textControl.Document,textControl.GetClientId(),h.GetNewOrNull(),h.GetOldOrNull());
                            _groupingEvent.FireIncoming();
                        });
                });
        }


        private void OnChange()
        {
            Log(_logger, "OnChange invoked");
            var textControl = _textControlManager.LastFocusedTextControlPerClient.ForCurrentClient();
            if (textControl == null || textControl.Lifetime.IsNotAlive)
            {
                Log(_logger, "OnChange LastFocusedTextControl is null or not alive, clearing model");
                ClearModel();
                return;
            }

            if (!_textControlManager.VisibleTextControls.Contains(textControl))
            {
                //if textControl is not visible then clear the context.
                //sometimes there is a caret event for a document that lost focus even if the caret is not placed 
                //on another document.
                Log(_logger, "textControl {0} had a caret event but is not visible,Clearing model",
                    textControl.Document);
                ClearModel();
                return;
            }

            OnChange(textControl);
        }

        private void OnChange(ITextControl textControl)
        {
            Log(_logger, "Trying to discover method under caret for {0}", textControl.Document);
            using (ReadLockCookie.Create())
            {
                var psiSourceFile = _documentManager.GetProjectFile(textControl.Document).ToSourceFile();
                if (psiSourceFile == null || !psiSourceFile.GetPsiServices().Files.IsCommitted(psiSourceFile))
                {
                    Log(_logger, "PsiSourceFile not found for textControl {0},Clearing model", textControl.Document);
                    ClearModel();
                    return;
                }

                if (!PsiUtils.IsPsiSourceFileApplicable(psiSourceFile))
                {
                    Log(_logger, "PsiSourceFile {0} is not applicable for method under caret,Clearing model",
                        psiSourceFile);
                    ClearModel();
                    return;
                }


                var functionDeclaration = GetFunctionUnderCaret(textControl, psiSourceFile);
                if (functionDeclaration != null)
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
                    NotifyWithFileUri(psiSourceFile);
                }
            }
        }


        private void NotifyWithFileUri([CanBeNull] IPsiSourceFile psiSourceFile)
        {
            if (psiSourceFile == null)
            {
                ClearModel();
            }
            else
            {
                var fileUri = Identities.ComputeFileUri(psiSourceFile);
                Log(_logger, "Updating model with fileUri {0}", fileUri);
                var newElementUnderCaret =
                    new MethodUnderCaretEvent(string.Empty, string.Empty, string.Empty, fileUri);
                UpdateModel(newElementUnderCaret);
            }
        }


        private void UpdateModel([NotNull] MethodUnderCaretEvent newMethodUnderCaretEvent)
        {
            if (!newMethodUnderCaretEvent.Equals(_model.ElementUnderCaret.Maybe.ValueOrDefault))
            {
                Log(_logger, "Updating model with {0}", newMethodUnderCaretEvent);
                _model.ElementUnderCaret.Value = newMethodUnderCaretEvent;
                _model.NotifyElementUnderCaret.Fire(Unit.Instance);
            }
            else
            {
                Log(_logger, "Not Updating model because method under caret is the same as previous: {0}",
                    newMethodUnderCaretEvent);
            }
        }

        private void ClearModel()
        {
            Log(_logger, "Clearing model to empty values");
            _model.ElementUnderCaret.Value =
                new MethodUnderCaretEvent(string.Empty, string.Empty, string.Empty, string.Empty);
            _model.NotifyElementUnderCaret.Fire(Unit.Instance);
        }


        [CanBeNull]
        private ICSharpFunctionDeclaration GetFunctionUnderCaret([NotNull] ITextControl textControl,
            [NotNull] IPsiSourceFile psiSourceFile)
        {
            var node = GetTreeNodeUnderCaret(textControl, psiSourceFile);
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

            var range = new TextRange(textControl.Caret.Offset());
            var documentRange = range.CreateDocumentRange(projectFile);
            var file = psiSourceFile.GetPsiFile(psiSourceFile.PrimaryPsiLanguage, documentRange);
            return file?.FindNodeAt(documentRange);
        }
    }
}