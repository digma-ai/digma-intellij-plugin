using System;
using System.Diagnostics;
using System.Linq;
using Digma.Rider.Protocol;
using JetBrains.Annotations;
using JetBrains.Lifetimes;
using JetBrains.Platform.MsBuildTask.Utils;
using JetBrains.ProjectModel;
using JetBrains.RdBackend.Common.Features.ProjectModel.View;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.RiderTutorials.Utils;
using JetBrains.Util;

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


        [CanBeNull]
        public static IPsiSourceFile FindPsiSourceFile([NotNull] PsiFileID psiFileId, [NotNull] ISolution solution)
        {
            IPsiSourceFile psiSourceFile = null;
            Lifetime.Using(lifetime =>
            {
                solution.GetPsiServices().Files.DoOnCommitedPsi(lifetime, () =>
                {
                    //always prefer to find by ProjectModelId
                    psiSourceFile = psiFileId.ProjectModelId != null
                        ? FindPsiSourceFileByProjectModelId(psiFileId, solution)
                        : FindPsiSourceFileByUri(psiFileId, solution);
                });
                
            });

            return psiSourceFile;
        }

        [CanBeNull]
        private static IPsiSourceFile FindPsiSourceFileByUri(PsiFileID psiFileId, ISolution solution)
        {
            var uri = new Uri(psiFileId.PsiUri);
            if (uri.IsFile)
            {
                var path = VirtualFileSystemPath.CreateByCanonicalPath(uri.AbsolutePath, InteractionContext.Local);
                var projectFile = solution.FindProjectItemsByLocation(path).OfType<IProjectFile>().TryGetSingleItem();
                var file = projectFile?.GetPrimaryPsiFile(file => file.PrimaryPsiLanguage.Equals(CSharpLanguage.Instance));
                return file?.GetSourceFile();
            }

            return null;
        }

        [CanBeNull]
        private static IPsiSourceFile FindPsiSourceFileByProjectModelId(PsiFileID psiFileId, ISolution solution)
        {
            Debug.Assert(psiFileId.ProjectModelId != null, "psiFileId.ProjectModelId != null");
            var projectFile = solution.GetComponent<ProjectModelViewHost>().GetItemById<IProjectFile>((int)psiFileId.ProjectModelId);
            var file = projectFile?.GetPrimaryPsiFile(file => file.PrimaryPsiLanguage.Equals(CSharpLanguage.Instance));
            var psiSourceFile = file?.GetSourceFile();
            //if not found fall back to find by uri
            return psiSourceFile ?? FindPsiSourceFileByUri(psiFileId, solution);
        }
    }
}