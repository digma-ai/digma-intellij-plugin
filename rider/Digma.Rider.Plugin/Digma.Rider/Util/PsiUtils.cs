using System;
using System.Diagnostics;
using System.Linq;
using Digma.Rider.Protocol;
using JetBrains.Annotations;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.RdBackend.Common.Features.ProjectModel.View;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.Util;
using static Digma.Rider.Logging.Logger;

namespace Digma.Rider.Util
{
    public static class PsiUtils
    {
        
        private static readonly ILogger Logger = JetBrains.Util.Logging.Logger.GetLogger(typeof (PsiUtils));

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
            Log(Logger, "Got request to find IPsiSourceFile for {0}",psiFileId);
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

            if (psiSourceFile != null)
            {
                Log(Logger, "Found IPsiSourceFile for {0}, {1}",psiFileId,psiSourceFile);
            }
            else
            {
                Log(Logger, "Could not find IPsiSourceFile for {0}",psiFileId);
            }
            return psiSourceFile;
        }

        [CanBeNull]
        private static IPsiSourceFile FindPsiSourceFileByUri(PsiFileID psiFileId, ISolution solution)
        {
            Log(Logger, "Trying to find IPsiSourceFile by uri for {0}",psiFileId);
            var uri = new Uri(psiFileId.PsiUri);
            if (uri.IsFile)
            {
                Log(Logger, "Uri for {0} is file, trying to find it by absolute path {1}",psiFileId,uri.AbsolutePath);
                var path = VirtualFileSystemPath.CreateByCanonicalPath(uri.AbsolutePath, InteractionContext.Local);
                var projectFile = solution.FindProjectItemsByLocation(path).OfType<IProjectFile>().SingleItem();
                var file = projectFile?.GetPrimaryPsiFile(file => file.PrimaryPsiLanguage.Equals(CSharpLanguage.Instance));
                if (file != null)
                {
                    Log(Logger, "Found File '{0}' by uri for {1}",file,psiFileId);
                }
                return file?.GetSourceFile();
            }
            else
            {
                Log(Logger, "Uri for {0} is NOT file, uri {1} is {2}",psiFileId,uri,uri.GetType());
            }

            Log(Logger, "Could not find IPsiSourceFile by uri for {0}",psiFileId);
            return null;
        }

        [CanBeNull]
        private static IPsiSourceFile FindPsiSourceFileByProjectModelId(PsiFileID psiFileId, ISolution solution)
        {
            Log(Logger, "Trying to find IPsiSourceFile with ProjectModelId for {0}",psiFileId);
            Debug.Assert(psiFileId.ProjectModelId != null, "psiFileId.ProjectModelId != null");
            var projectFile = solution.GetComponent<ProjectModelViewHost>().GetItemById<IProjectFile>((int)psiFileId.ProjectModelId);
            var file = projectFile?.GetPrimaryPsiFile(file => file.PrimaryPsiLanguage.Equals(CSharpLanguage.Instance));
            if (file != null)
            {
                Log(Logger, "Found File '{0}' by ProjectModelId for {1}",file,psiFileId);
            }
            var psiSourceFile = file?.GetSourceFile();
            if (psiSourceFile == null)
            {
                Log(Logger, "Could not find IPsiSourceFile with ProjectModelId for {0}, fallback to find by uri",psiFileId);  
            }
            //if not found fall back to find by uri
            return psiSourceFile ?? FindPsiSourceFileByUri(psiFileId, solution);
        }
    }
}