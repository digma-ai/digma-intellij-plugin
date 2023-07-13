using JetBrains.Annotations;
using JetBrains.ReSharper.Psi.Tree;

namespace Digma.Rider.Util;

public static class TreeNodeExtensions
{
    public static T GetParentOfType<T>([NotNull] this ITreeNode node) where T : class, ITreeNode
    {
        for (; node != null; node = node.Parent)
        {
            if (node is T obj)
                return obj;
        }
        return default(T);
    }

}