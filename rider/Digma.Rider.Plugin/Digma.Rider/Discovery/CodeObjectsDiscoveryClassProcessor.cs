using System.Diagnostics.CodeAnalysis;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Util;
using static JetBrains.Util.Logging.Logger;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Discovery
{
    internal class CodeObjectsDiscoveryClassProcessor : CodeObjectsDiscoveryProcessor
    {
        private static readonly ILogger Logger = GetLogger(typeof(CodeObjectsDiscoveryClassProcessor));
        
        private readonly IClassDeclaration _classDeclaration;

        public CodeObjectsDiscoveryClassProcessor(IClassDeclaration classDeclaration,
            DocumentDiscoveryContext discoveryContext) : base(discoveryContext)
        {
            _classDeclaration = classDeclaration;
        }

       

        [SuppressMessage("ReSharper", "UnusedVariable")]
        public override bool InteriorShouldBeProcessed(ITreeNode element)
        {
            switch (element)
            {
                case IClassDeclaration classDeclaration:
                case IClassBody classBody:
                    return true;
            }

            return false;
        }

        public override void ProcessBeforeInterior(ITreeNode element)
        {
            switch (element)
            {
                case ICSharpFunctionDeclaration functionDeclaration:
                {
                    Log(Logger, "in '{0}' for file '{1}'",element,DiscoveryContext.PsiSourceFile.Name);
                    var methodProcessor = new CodeObjectsDiscoveryMethodProcessor(functionDeclaration,DiscoveryContext);
                    functionDeclaration.ProcessDescendants(methodProcessor);
                    break;
                }
            }
        }

    }

}