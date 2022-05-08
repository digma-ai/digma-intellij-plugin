using System;
using JetBrains.Annotations;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.RiderTutorials.Utils;
using JetBrains.Util;
using JetBrains.Util.Logging;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Discovery
{
    public static class PsiUtils
    {
        private static readonly ILogger MyLogger = Logger.GetLogger(typeof(PsiUtils).ToString());
        
        [NotNull]
        public static string GetNamespace([NotNull] IMethodDeclaration methodDeclaration)
        {
            try
            {
                //a method may be declared in a file without namespace
                return methodDeclaration.GetParentOfType<INamespaceDeclaration>().QualifiedName;
            }
            catch (NullReferenceException e)
            {
                MyLogger.Warn(e);
                Log(MyLogger,"Could not get namespace for method {0}, using 'global'",methodDeclaration);
                return "global";
            }
            
        }
        
        [NotNull]
        public static string GetClassName([NotNull] IMethodDeclaration methodDeclaration)
        {
            try
            {
                //todo: probably shouldn't happen that a method is out of a class/interface body
                return methodDeclaration.GetParentOfType<ICSharpTypeDeclaration>().DeclaredName;
            }
            catch (NullReferenceException e)
            {
                MyLogger.Warn(e);
                Log(MyLogger,"Could not get class name for method {0}",methodDeclaration);
                return "";
            }
            
        }
    }
}