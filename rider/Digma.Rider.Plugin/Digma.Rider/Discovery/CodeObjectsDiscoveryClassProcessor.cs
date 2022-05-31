using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using Digma.Rider.Protocol;
using JetBrains.Annotations;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;
using static Digma.Rider.Util.StringUtils;
using static JetBrains.Util.Logging.Logger;

namespace Digma.Rider.Discovery
{
    internal class CodeObjectsDiscoveryClassProcessor : IRecursiveElementProcessor
    {
        private static readonly ILogger Logger = GetLogger(typeof(CodeObjectsDiscoveryClassProcessor));
        private readonly string _fileUri;
        private readonly IClassDeclaration _classDeclaration;

        public bool ProcessingIsFinished => false;
        
        private readonly IList<RiderMethodInfo> _methodInfos = new List<RiderMethodInfo>();

        [NotNull]
        public IList<RiderMethodInfo> MethodInfos => _methodInfos;
        
        public CodeObjectsDiscoveryClassProcessor(string fileUri,IClassDeclaration classDeclaration)
        {
            _fileUri = fileUri;
            _classDeclaration = classDeclaration;
        }

       

        [SuppressMessage("ReSharper", "UnusedVariable")]
        public bool InteriorShouldBeProcessed(ITreeNode element)
        {
            //Log(Logger, "InteriorShouldBeProcessed for tree node {0} for file {1}",element,GetShortFileUriName(_fileUri));
            switch (element)
            {
                case IClassDeclaration classDeclaration:
                case IClassBody classBody:
                    return true;
            }

            return false;
        }

        public void ProcessBeforeInterior(ITreeNode element)
        {
            //Log(Logger, "ProcessBeforeInterior for tree node {0} in file {1}",element,_fileUri.SubstringAfterLast("/"));
            switch (element)
            {
                case ICSharpFunctionDeclaration functionDeclaration:
                {
                    Log(Logger, "in ICSharpFunctionDeclaration {0} for file '{1}'",element,GetShortFileUriName(_fileUri));
                    var methodProcessor = new CodeObjectsDiscoveryMethodProcessor(_fileUri, functionDeclaration);
                    functionDeclaration.ProcessDescendants(methodProcessor);
                    var methodInfo = methodProcessor.MethodInfo;
                    Log(Logger, "Found MethodInfo {0} for class {1} in file '{2}'",methodInfo,_classDeclaration,GetShortFileUriName(_fileUri));
                    _methodInfos.Add(methodInfo);
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