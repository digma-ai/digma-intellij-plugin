using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using Digma.Rider.Protocol;
using JetBrains.Annotations;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.ReSharper.PsiGen.Util;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;
using static Digma.Rider.Util.StringUtils;
using static JetBrains.Util.Logging.Logger;

namespace Digma.Rider.Discovery
{
    internal class CodeObjectsDiscoveryFileProcessor : IRecursiveElementProcessor
    {
        private static readonly ILogger Logger = GetLogger(typeof(CodeObjectsDiscoveryFileProcessor));
        
        private readonly string _fileUri;
        private readonly ICSharpFile _cSharpFile;
        private readonly IList<RiderMethodInfo> _methodInfos = new List<RiderMethodInfo>();
        
        public bool ProcessingIsFinished => false;

        public CodeObjectsDiscoveryFileProcessor(string fileUri, ICSharpFile cSharpFile)
        {
            _fileUri = fileUri;
            _cSharpFile = cSharpFile;
        }

        [NotNull]
        public IEnumerable<RiderMethodInfo> MethodInfos => _methodInfos;

        [SuppressMessage("ReSharper", "UnusedVariable")]
        public bool InteriorShouldBeProcessed(ITreeNode element)
        {
            //Log(Logger, "InteriorShouldBeProcessed for tree node {0} in file {1}",element,GetShortFileUriName(_fileUri));
            switch (element)
            {
                case ICSharpNamespaceDeclaration cSharpNamespaceDeclaration:
                case INamespaceBody namespaceBody:
                    return true;
            }

            return false;
        }

        public void ProcessBeforeInterior(ITreeNode element)
        {
            //Log(Logger, "ProcessBeforeInterior for tree node {0} in file {1}",element,GetShortFileUriName(_fileUri));
            switch (element)
            {
                //collecting code objects only for classes,ignoring interfaces.
                case IClassDeclaration classDeclaration:
                {
                    Log(Logger, "in IClassDeclaration {0} for file {1}",element,GetShortFileUriName(_fileUri));
                    var classProcessor = new CodeObjectsDiscoveryClassProcessor(_fileUri, classDeclaration);
                    classDeclaration.ProcessDescendants(classProcessor);
                    var classMethodInfos = classProcessor.MethodInfos;
                    Log(Logger, "Found {0} methods for {1} in file {2}",classMethodInfos.Count,element,GetShortFileUriName(_fileUri));
                    _methodInfos.AddAll(classMethodInfos);
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