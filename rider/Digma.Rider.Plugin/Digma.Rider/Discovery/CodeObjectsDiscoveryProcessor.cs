using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Tree;

namespace Digma.Rider.Discovery;

internal abstract class CodeObjectsDiscoveryProcessor : IRecursiveElementProcessor
{
    protected readonly DocumentDiscoveryContext DiscoveryContext;
    public abstract bool InteriorShouldBeProcessed(ITreeNode element);
    public abstract void ProcessBeforeInterior(ITreeNode element);

    public virtual void ProcessAfterInterior(ITreeNode element)
    {
    }

    public bool ProcessingIsFinished { get; protected set; } = false;


    internal CodeObjectsDiscoveryProcessor(DocumentDiscoveryContext discoveryContext)
    {
        DiscoveryContext = discoveryContext;
    }
}