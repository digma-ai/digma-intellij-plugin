using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using Digma.Rider.Protocol;
using Digma.Rider.Util;
using JetBrains.Annotations;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;
using static JetBrains.Util.Logging.Logger;
using static Digma.Rider.Util.StringUtils;

namespace Digma.Rider.Discovery
{
    internal class CodeObjectsDiscoveryMethodProcessor : IRecursiveElementProcessor
    {
        private static readonly ILogger Logger = GetLogger(typeof(CodeObjectsDiscoveryMethodProcessor));
        
        private readonly string _fileUri;

        private readonly ICSharpFunctionDeclaration _functionDeclaration;
        private readonly RiderMethodInfo _methodInfo;
        
        [NotNull]
        public RiderMethodInfo MethodInfo => _methodInfo;
        
        public bool ProcessingIsFinished => false;

        public CodeObjectsDiscoveryMethodProcessor(string fileUri,
            ICSharpFunctionDeclaration functionDeclaration)
        {
            _fileUri = fileUri;
            _functionDeclaration = functionDeclaration;
            _methodInfo = BuildMethodInfo(functionDeclaration);
        }

        private RiderMethodInfo BuildMethodInfo(ICSharpFunctionDeclaration functionDeclaration)
        {
            var methodFqn = Identities.ComputeFqn(functionDeclaration);
            var declaredName = PsiUtils.GetDeclaredName(functionDeclaration);
            var containingClassName = PsiUtils.GetClassName(functionDeclaration);
            var containingNamespace = PsiUtils.GetNamespace(functionDeclaration);
            var containingFileUri = _fileUri;
            
            return new RiderMethodInfo(methodFqn,declaredName,containingClassName,containingNamespace,containingFileUri,new List<RiderSpanInfo>());
        }


        [SuppressMessage("ReSharper", "UnusedVariable")]
        public bool InteriorShouldBeProcessed(ITreeNode element)
        {
            //Log(Logger, "InteriorShouldBeProcessed for tree node {0} for file {1}",element,GetShortFileUriName(_fileUri));
            switch (element)
            {
                case IBlock block:
                case ICSharpStatement cSharpStatement:
                case IMultipleLocalVariableDeclaration multipleLocalVariableDeclaration:
                    return true;
            }

            return false;
        }

        public void ProcessBeforeInterior(ITreeNode element)
        {
            //Log(Logger, "ProcessBeforeInterior for tree node {0} for file {1}",element,GetShortFileUriName(_fileUri));
            switch (element)
            {
                case ILocalVariableDeclaration localVariableDeclaration:
                {
                    Log(Logger, "in ILocalVariableDeclaration {0} for method {1} in file {2}",element,_functionDeclaration,GetShortFileUriName(_fileUri));
                    Log(Logger, "in ILocalVariableDeclaration {0} ,Trying to discover span",element);
                    var spanDiscovery = new SpanDiscovery(localVariableDeclaration);
                    if (spanDiscovery.InstLibrary == null || spanDiscovery.SpanName == null)
                    {
                        Log(Logger, "No span discovered in ILocalVariableDeclaration {0} for method {1} in file {2}",element,_functionDeclaration,GetShortFileUriName(_fileUri));
                        return;
                    }
                    var instLibrary = spanDiscovery.InstLibrary;
                    var spanName = spanDiscovery.SpanName;
                    var id = Identities.ComputeSpanFqn(instLibrary, spanName);
                    var spanInfo = new RiderSpanInfo(id,spanName,_methodInfo.Id,_fileUri);
                    Log(Logger, "Found span {0} for method {1} in file {2}",spanInfo,_functionDeclaration,GetShortFileUriName(_fileUri));
                    _methodInfo.Spans.Add(spanInfo);
                    
                    break;
                }
            }
        }

        public void ProcessAfterInterior(ITreeNode element)
        {
            //Log(Logger, "ProcessAfterInterior for tree node {0} for file {1}",element,GetShortFileUriName(_fileUri));
        }

     
    }

  
}