using System.Diagnostics.CodeAnalysis;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.DocumentManagers;
using JetBrains.DocumentManagers.Transactions;
using JetBrains.ProjectModel;
using JetBrains.Rd.Tasks;
using JetBrains.RdBackend.Common.Features;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.ReSharper.Psi.Resolve;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.RiderTutorials.Utils;
using JetBrains.TextControl;
using JetBrains.Util;

namespace Digma.Rider.Protocol
{
    [SolutionComponent]
    [SuppressMessage("ReSharper", "PrivateFieldCanBeConvertedToLocalVariable")]
    public class MethodUnderCaretDetector
    {
        private readonly DocumentManager _documentManager;
        private readonly ITextControlManager _textControlManager;
        private readonly ILogger _logger;

        public MethodUnderCaretDetector(ISolution solution,
            DocumentManager documentManager,
            ITextControlManager textControlManager, ILogger logger)
        {
            _documentManager = documentManager;
            _textControlManager = textControlManager;
            _logger = logger;

            // NodeUnderCaret = new Property<ITreeNode>("NodeUnderCaretDetector.NodeUnderCaret");
            // NodeReferencedElements =
            //     new Property<IEnumerable<IDeclaredElement>>("NodeUnderCaretDetector.NodeReferencedElements");
            //
            // EventHandler caretMoved = (sender, args) =>
            // {
            //     _shellLocks.QueueReadLock("NodeUnderCaretDetector.CaretMoved", Refresh);
            // };

            // lifetime.AddBracket(
            //     () => _textControlManager.Legacy.CaretMoved += caretMoved,
            //     () => _textControlManager.Legacy.CaretMoved -= caretMoved);


            _logger.Info("Getting MethodInfoModel");
            var model = solution.GetProtocolSolution().GetMethodInfoModel();
            _logger.Info("Preparing GetMethodUnderCaret task");

            model.GetMethodUnderCaret.Set((lifetime, request) =>
            {
                _logger.Info("Running GetMethodUnderCaret task");
                var task = new RdTask<MethodInfo>();

                ICSharpFunctionDeclaration declaration = GetMethodUnderCaret();
                var methodInfo = new MethodInfo(declaration?.DeclaredElement?.ShortName ?? string.Empty,
                    declaration?.GetSourceFile()?.ToString() ?? string.Empty);
                task.Set(methodInfo);
                return task;
            });
        }


        [CanBeNull]
        private ICSharpFunctionDeclaration GetMethodUnderCaret()
        {
            using (ReadLockCookie.Create())
            {
                var node = GetTreeNodeUnderCaret();
                var parentMethod = node?.GetParentOfType<ICSharpFunctionDeclaration>();
                return parentMethod;
            }
        }


        [CanBeNull]
        private ITreeNode GetTreeNodeUnderCaret()
        {
            // var textControl = _textControlManager.LastFocusedTextControlPerClient.
            // var textControl = _textControlManager.LastFocusedTextControl.Value;
            var textControl = _textControlManager.FocusedTextControl.Value;
            if (textControl == null)
                return null;

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