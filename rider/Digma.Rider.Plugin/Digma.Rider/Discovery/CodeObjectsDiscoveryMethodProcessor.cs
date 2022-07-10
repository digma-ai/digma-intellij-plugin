using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using Digma.Rider.Protocol;
using Digma.Rider.Util;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;
using static JetBrains.Util.Logging.Logger;

namespace Digma.Rider.Discovery
{
    internal class CodeObjectsDiscoveryMethodProcessor : CodeObjectsDiscoveryProcessor
    {
        private static readonly ILogger Logger = GetLogger(typeof(CodeObjectsDiscoveryMethodProcessor));

        private readonly ICSharpFunctionDeclaration _functionDeclaration;
        private readonly RiderMethodInfo _methodInfo;

        public CodeObjectsDiscoveryMethodProcessor(ICSharpFunctionDeclaration functionDeclaration,
            DocumentDiscoveryContext discoveryContext) : base(discoveryContext)
        {
            _functionDeclaration = functionDeclaration;
            _methodInfo = BuildMethodInfo(_functionDeclaration, out bool managedToResolveReferences);
            UpdateDiscoveryContext(managedToResolveReferences);
        }

        private void UpdateDiscoveryContext(bool managedToResolveReferences)
        {
            if (DiscoveryContext.Methods.ContainsKey(_methodInfo.Id))
            {
                ProcessingIsFinished = true;
            } 
            else 
            {
                if (!managedToResolveReferences && DiscoveryContext.IsStartup)
                {
                    Log(Logger, "method {0} has unresolved parameter type. flagging 'HasReferenceResolvingErrors'",
                        _functionDeclaration);
                    DiscoveryContext.HasReferenceResolvingErrors = true;

                    ProcessingIsFinished = true;
                }
                else
                {
                    DiscoveryContext.Methods.Add(_methodInfo.Id, _methodInfo);
                }
            }
        }

        private RiderMethodInfo BuildMethodInfo(ICSharpFunctionDeclaration functionDeclaration, out bool managedToResolveReferences)
        {
            var methodFqn = Identities.ComputeFqn(functionDeclaration, out managedToResolveReferences);
            var declaredName = PsiUtils.GetDeclaredName(functionDeclaration);
            var containingClassName = PsiUtils.GetClassName(functionDeclaration);
            var containingNamespace = PsiUtils.GetNamespace(functionDeclaration);
            var containingFileUri = DiscoveryContext.FileUri;
            var offsetAtFileUri = functionDeclaration.GetNavigationRange().StartOffset.Offset;

            return new RiderMethodInfo(methodFqn, declaredName, containingClassName, containingNamespace,
                containingFileUri, offsetAtFileUri, new List<RiderSpanInfo>());
        }

        [SuppressMessage("ReSharper", "UnusedVariable")]
        public override bool InteriorShouldBeProcessed(ITreeNode element)
        {
            switch (element)
            {
                case IBlock block:
                case ICSharpStatement cSharpStatement:
                case IMultipleLocalVariableDeclaration multipleLocalVariableDeclaration:
                    return true;
            }

            return false;
        }

        public override void ProcessBeforeInterior(ITreeNode element)
        {
            //Log(Logger, "Got TreeNode element '{0}'", element.ToString());
            switch (element)
            {
                case ILocalVariableDeclaration localVariableDeclaration:
                {
                    Log(Logger, "in '{0}' for method '{1}' in file '{2}'", 
                        element,
                        _functionDeclaration, DiscoveryContext.PsiSourceFile.Name);
                    var spanDiscovery = new SpanDiscovery(localVariableDeclaration);
                    if (spanDiscovery.InstLibrary == null || spanDiscovery.SpanName == null)
                    {
                        if (spanDiscovery.HasReferenceResolvingErrors)
                        {
                            Log(Logger, "Could not resolve all references in SpanDiscovery for '{0}' for method '{1}' in file '{2}'. Marking Document incomplete.",element,_functionDeclaration,DiscoveryContext.PsiSourceFile.Name);
                            DiscoveryContext.HasReferenceResolvingErrors = true;
                        }

                        Log(Logger, "No span discovered in '{0}' for method '{1}' in file '{2}'",
                            element, _functionDeclaration, DiscoveryContext.PsiSourceFile.Name);
                        return;
                    }

                    var offset = localVariableDeclaration.GetNavigationRange().StartOffset.Offset;
                    var instLibrary = spanDiscovery.InstLibrary;
                    var spanName = spanDiscovery.SpanName;
                    var id = Identities.ComputeSpanFqn(instLibrary, spanName);
                    var spanInfo = new RiderSpanInfo(id, spanName, _methodInfo.Id, DiscoveryContext.FileUri,offset);
                    Log(Logger, "Found span '{0}' for method '{1}' in file '{2}'", spanInfo, _functionDeclaration,
                        DiscoveryContext.PsiSourceFile.Name);
                    _methodInfo.Spans.Add(spanInfo);

                    break;
                }
            }
        }
    }
}