using System.Collections.Generic;
using Digma.Rider.Protocol;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;

namespace Digma.Rider.Discovery;

internal class DocumentDiscoveryContext
{
    public readonly IPsiSourceFile PsiSourceFile;
    public readonly bool IsStartup;
    public readonly string FileUri;
    
    public bool HasReferenceResolvingErrors { get; set; } = false;

    public readonly IDictionary<string, RiderMethodInfo> Methods = new Dictionary<string, RiderMethodInfo>();

    public DocumentDiscoveryContext(IPsiSourceFile psiSourceFile, bool isStartup, string fileUri)
    {
        PsiSourceFile = psiSourceFile;
        IsStartup = isStartup;
        FileUri = fileUri;
    }
    
    
    
}