using JetBrains.Annotations;
using JetBrains.Application.UI.Controls.Lists.TreeList;
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
            if (functionDeclaration.GetParentOfType<IClassDeclaration>() != null)
            {
                return functionDeclaration.GetParentOfType<IClassDeclaration>().DeclaredName;
            }
            else if (functionDeclaration.GetParentOfType<IRecordDeclaration>() != null)
            {
                return functionDeclaration.GetParentOfType<IRecordDeclaration>().DeclaredName;
            }
            else if (functionDeclaration.GetParentOfType<IStructDeclaration>() != null)
            {
                return functionDeclaration.GetParentOfType<IStructDeclaration>().DeclaredName;
            }
            else if (functionDeclaration.GetParentOfType<IClassLikeDeclaration>() != null)
            {
                return functionDeclaration.GetParentOfType<IClassLikeDeclaration>().DeclaredName;
            }
            else if (functionDeclaration.GetParentOfType<ICSharpDeclaration>() != null)
            {
                return functionDeclaration.GetParentOfType<ICSharpDeclaration>().DeclaredName;
            }
            else
            {
                return functionDeclaration.Parent != null ? functionDeclaration.Parent.ToString() : "";
            }
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
            var isApplicable = psiSourceFile.IsValid() &&
                               !primaryPsiLanguage.IsNullOrUnknown() &&
                               !properties.IsGeneratedFile &&
                               !properties.IsNonUserFile &&
                               primaryPsiLanguage.Is<CSharpLanguage>() &&
                               properties.ShouldBuildPsi &&
                               properties.ProvidesCodeModel &&
                               !properties.IsNonUserFile;

            return isApplicable;
        }

        
        
        
    }
}