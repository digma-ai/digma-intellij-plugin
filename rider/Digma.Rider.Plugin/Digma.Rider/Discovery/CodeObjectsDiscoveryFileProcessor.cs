using System.Diagnostics.CodeAnalysis;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;
using static JetBrains.Util.Logging.Logger;

namespace Digma.Rider.Discovery
{
    internal class CodeObjectsDiscoveryFileProcessor : CodeObjectsDiscoveryProcessor
    {
        private readonly ICSharpFile _cSharpFile;
        private static readonly ILogger Logger = GetLogger(typeof(CodeObjectsDiscoveryFileProcessor));
      
        public CodeObjectsDiscoveryFileProcessor(ICSharpFile cSharpFile,DocumentDiscoveryContext discoveryContext) : base(discoveryContext)
        {
            _cSharpFile = cSharpFile;
        }


        [SuppressMessage("ReSharper", "UnusedVariable")]
        public override bool InteriorShouldBeProcessed(ITreeNode element)
        {
            switch (element)
            {
                case ICSharpNamespaceDeclaration cSharpNamespaceDeclaration:
                case INamespaceBody namespaceBody:
                    return true;
            }

            return false;
        }

        public override void ProcessBeforeInterior(ITreeNode element)
        {
            switch (element)
            {
                //collecting code objects only for classes,ignoring interfaces.
                case IClassDeclaration classDeclaration:
                {
                    Log(Logger, "in '{0}' for file '{1}'",element,DiscoveryContext.PsiSourceFile.Name);
                    var classProcessor = new CodeObjectsDiscoveryClassProcessor(classDeclaration,DiscoveryContext);
                    classDeclaration.ProcessDescendants(classProcessor);
                    break;
                }
            }
        }
    }

}