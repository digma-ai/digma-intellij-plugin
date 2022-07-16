using JetBrains.Annotations;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.RiderTutorials.Utils;

namespace Digma.Rider.Util
{
    public static class PsiUtils
    {

        public static string GetNamespace(ICSharpFunctionDeclaration functionDeclaration)
        {
            //a method may be declared in a file without namespace, so if we can't find the 
            // namespace assume 'global'
            var namespaceDeclaration =
                functionDeclaration.GetParentOfType<ICSharpNamespaceDeclaration>();
            return namespaceDeclaration == null ? "global" : namespaceDeclaration.QualifiedName;
        }

        public static string GetClassName(ICSharpFunctionDeclaration functionDeclaration)
        {
            //todo: urgent : check is the parent of the method is a class.
            //see class  CodeObjectError and ErrorFlowDetail in digma collector backend
            //the parent can be a record
            /*
             
             
             com.jetbrains.rdclient.util.BackendException: Object reference not set to an instance of an object.

--- EXCEPTION #1/2 [NullReferenceException]
Message = “Object reference not set to an instance of an object.”
ExceptionPath = Root.InnerException
ClassName = System.NullReferenceException
Data.ThreadLocalDebugInfo = GroupingEventHost.ExecuteExpiredEvents
HResult = E_POINTER=COR_E_NULLREFERENCE=80004003
Source = Digma.Rider
StackTraceString = “
  at Digma.Rider.Util.PsiUtils.GetClassName(ICSharpFunctionDeclaration functionDeclaration) in /home/shalom/workspace/digma/digma-intellij-plugin/rider/Digma.Rider.Plugin/Digma.Rider/Util/PsiUtils.cs:line 23
     at Digma.Rider.Discovery.Identities.ComputeFqn(ICSharpFunctionDeclaration functionDeclaration, Boolean& managedToResolveReferences) in /home/shalom/workspace/digma/digma-intellij-plugin/rider/Digma.Rider.Plugin/Digma.Rider/Discovery/Identities.cs:line 146
     at Digma.Rider.Discovery.Identities.ComputeFqn(ICSharpFunctionDeclaration functionDeclaration) in /home/shalom/workspace/digma/digma-intellij-plugin/rider/Digma.Rider.Plugin/Digma.Rider/Discovery/Identities.cs:line 160
     at Digma.Rider.Protocol.ElementUnderCaretHost.<>c__DisplayClass13_0.<OnChange>b__0() in /home/shalom/workspace/digma/digma-intellij-plugin/rider/Digma.Rider.Plugin/Digma.Rider/Protocol/ElementUnderCaretHost.cs:line 238
     at JetBrains.ReSharper.Psi.Files.PsiFilesExtensions.DoOnCommitedPsi(IPsiFiles psiFiles, Lifetime lifetime, Action action)
     at Digma.Rider.Protocol.ElementUnderCaretHost.OnChange(ITextControl textControl, IPsiSourceFile psiSourceFile) in /home/shalom/workspace/digma/digma-intellij-plugin/rider/Digma.Rider.Plugin/Digma.Rider/Protocol/ElementUnderCaretHost.cs:line 230
     at Digma.Rider.Protocol.ElementUnderCaretHost.OnChange(ITextControl textControl) in /home/shalom/workspace/digma/digma-intellij-plugin/rider/Digma.Rider.Plugin/Digma.Rider/Protocol/ElementUnderCaretHost.cs:line 217
     at Digma.Rider.Protocol.ElementUnderCaretHost.OnChange() in /home/shalom/workspace/digma/digma-intellij-plugin/rider/Digma.Rider.Plugin/Digma.Rider/Protocol/ElementUnderCaretHost.cs:line 191
     at JetBrains.Threading.GroupingEvent.Execute()
”

              
             */
            
            
            return functionDeclaration.GetParentOfType<IClassDeclaration>().DeclaredName;
        }

        public static string GetDeclaredName(ICSharpFunctionDeclaration functionDeclaration)
        {
            return functionDeclaration.DeclaredName;
        }

        // public static string GetContainingFile(ICSharpFunctionDeclaration functionDeclaration)
        // {
        //     return functionDeclaration.GetSourceFile().GetLocation().FullPath;
        // }
        
        public static bool IsPsiSourceFileApplicable([NotNull] IPsiSourceFile psiSourceFile)
        {
            var properties = psiSourceFile.Properties;
            var primaryPsiLanguage = psiSourceFile.PrimaryPsiLanguage;
            var isApplicable = !primaryPsiLanguage.IsNullOrUnknown() &&
                               !properties.IsGeneratedFile &&
                               primaryPsiLanguage.Is<CSharpLanguage>() &&
                               properties.ShouldBuildPsi &&
                               properties.ProvidesCodeModel &&
                               !properties.IsNonUserFile;

            return isApplicable;
        }

        
        
        
    }
}