using JetBrains.Annotations;
using JetBrains.ReSharper.Feature.Services.Html;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Resolve;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Util;

namespace Digma.Rider.Discovery
{

    public class SpanDiscovery
    {
        private readonly string _methodName;
        private const string ActivitySourceClassName = "System.Diagnostics.ActivitySource";
        private const string StartActivityMethodName = "StartActivity";

        public string InstLibrary { get; private set; }
        public string SpanName { get; private set; }

        public bool HasReferenceResolvingErrors { get; private set; } = false;


        public SpanDiscovery([NotNull] ILocalVariableDeclaration localVariableDeclaration , string methodName)
        {
            _methodName = methodName;
            var expressionInitializer = localVariableDeclaration.Children<IExpressionInitializer>().FirstNotNull();
            var value = expressionInitializer?.Value;
            if (value is IInvocationExpression expression)
            {
                DiscoverFromInvocationExpression(expression);
            }
        }

        private void DiscoverFromInvocationExpression([NotNull] IInvocationExpression invocationExpression)
        {
            if (StartActivityMethodName.Equals(invocationExpression.Reference.GetName()) &&
                IsStartActivityMethodReference(invocationExpression.Reference) && 
                invocationExpression.InvokedExpression != null)
            {
                //invocationExpression.InvokedExpression is Activity.StartActivity
                var instLib =
                    DiscoverInstrumentationLibraryFromInvokedExpression(
                        (IReferenceExpression)invocationExpression.InvokedExpression);
                if (instLib != null)
                {
                    InstLibrary = instLib;
                    var sName = DiscoverSpanNameFromInvocationArguments(invocationExpression.ArgumentList);
                    SpanName = sName;
                }
            }
        }

        /*
         * the arguments to Activity.StartActivity("the argument")
         * or Activity.StartActivity()
        */
        private string DiscoverSpanNameFromInvocationArguments([NotNull] IArgumentList invocationExpressionArgumentList)
        {
            if (invocationExpressionArgumentList.Arguments.Count is 1 or 2)
            {
                return GetTextFromArgument(invocationExpressionArgumentList.Arguments[0]);
            }

            return invocationExpressionArgumentList.Arguments.Count == 0 ? _methodName : null;
        }

        private string DiscoverInstrumentationLibraryFromInvokedExpression(
            [NotNull] IReferenceExpression invocationExpressionInvokedExpression)
        {
            //invocationExpressionInvokedExpression is the reference to Activity.StartActivity
            if (invocationExpressionInvokedExpression.IsQualified &&
                invocationExpressionInvokedExpression.QualifierExpression is IReferenceExpression referenceExpression)
            {
                var reference = ResolveReference(referenceExpression.Reference);
                var declaration = reference?.GetDeclarations().FirstNotNull();
                if (declaration is IFieldDeclaration fieldDeclaration)
                {
                    return GetTextFromFieldDeclaration(fieldDeclaration);
                }
                else if (declaration is ILocalVariableDeclaration variableDeclaration)
                {
                    return GetTextFromLocalVariableDeclaration(variableDeclaration);
                }
            }

            return null;
        }

        private string GetTextFromLocalVariableDeclaration(
            [NotNull] ILocalVariableDeclaration localVariableDeclaration)
        {
            var expressionInitializer = localVariableDeclaration.Children<IExpressionInitializer>().FirstNotNull();
            return expressionInitializer == null ? null : GetTextFromExpressionInitializer(expressionInitializer);
        }

        
        //the field declaration of the ActivitySource
        private string GetTextFromFieldDeclaration([NotNull] IFieldDeclaration fieldDeclaration)
        {
            var expressionInitializer = fieldDeclaration.Children<IExpressionInitializer>().FirstNotNull();
            return expressionInitializer == null ? null : GetTextFromExpressionInitializer(expressionInitializer);
        }
        
        
        
        private string GetTextFromExpressionInitializer([NotNull] IExpressionInitializer expressionInitializer)
        {
            var value = expressionInitializer.Value;
            if (value is IObjectCreationExpression objectCreationExpression)
            {
                return GetTextFromObjectCreationExpression(objectCreationExpression);
            }else if (value is ICSharpLiteralExpression literalExpression)
            {
                return GetTextFromLiteralExpression(literalExpression);
            } 

            return null;
        }
        
        
        
        
        

        private string GetTextFromObjectCreationExpression(
            [NotNull] IObjectCreationExpression objectCreationExpression)
        {
            var declaredElement = ResolveReference(objectCreationExpression.Reference);
            if (declaredElement is IConstructor)
            {
                if (objectCreationExpression.ArgumentList.Arguments.Count == 1)
                {
                    return GetTextFromArgument(objectCreationExpression.ArgumentList.Arguments[0]);
                }
            }

            return null;
        }
        
        
        private string GetTextFromConstantDeclaration([NotNull] IConstantDeclaration constantDeclaration)
        {
            var value = constantDeclaration.ValueExpression;
            if (value is ILiteralExpression literalExpression)
            {
                return GetTextFromLiteralExpression(literalExpression);
            }

            return null;
        }


        private string GetTextFromLiteralExpression([NotNull] ILiteralExpression literalExpression)
        {
            return literalExpression.Literal.GetText().Replace("\"", "");
        }
        

        private string GetTextFromArgument([NotNull] ICSharpArgument argument)
        {
            var expression = argument.Value;
            switch (expression)
            {
                case ICSharpLiteralExpression literalExpression:
                {
                    return GetTextFromLiteralExpression(literalExpression);
                }
                case IInvocationExpression invocationExpression:
                {
                    if (invocationExpression.ArgumentList.Arguments.Count == 1)
                    {
                        return GetTextFromArgument(invocationExpression.Arguments[0]);
                    }
                    //don't know how to handle more then 1 argument
                    break;
                }
                case IObjectCreationExpression objectCreationExpression:
                {
                    return GetTextFromObjectCreationExpression(objectCreationExpression);
                }
                case IReferenceExpression referenceExpression:
                {
                    var element = ResolveReference(referenceExpression.Reference);
                    var declaration = element?.GetDeclarations().FirstNotNull();
                    if (declaration != null)
                    {
                        return GetTextFromDeclaration(declaration);
                    }

                    return referenceExpression.NameIdentifier.Name;

                }
                
            }

            return null;
        }




        private string GetTextFromDeclaration([NotNull] IDeclaration declaration)
        {
            switch (declaration)
            {
                case IConstantDeclaration constantDeclaration:
                {
                    return GetTextFromConstantDeclaration(constantDeclaration);
                }
                case IFieldDeclaration fieldDeclaration:
                {
                    return GetTextFromFieldDeclaration(fieldDeclaration);
                }
                case ILocalVariableDeclaration localVariableDeclaration:
                {
                    return GetTextFromLocalVariableDeclaration(localVariableDeclaration);
                }
                default:
                {
                    return declaration.DeclaredName;
                }
            }
        }


      
        
        private bool IsStartActivityMethodReference([NotNull] IInvocationExpressionReference invocationExpressionReference)
        {
            var declaredElement = ResolveReference(invocationExpressionReference);
            if (declaredElement is IMethod method &&
                StartActivityMethodName.Equals(method.ShortName) &&
                (method.ContainingType is IClass @class) &&
                ActivitySourceClassName.Equals(@class.GetClrName().FullName))
            {
                return true;
            }

            return false;
        }

        
        private IDeclaredElement ResolveReference(IReference reference)
        {
            if (reference == null)
            {
                return null;
            }
            
            var declaredElement = reference.Resolve().DeclaredElement;
            if (declaredElement == null)
            {
                //if any reference did not succeed mark this span discovery AllReferencesResolved = false 
                HasReferenceResolvingErrors = true;
            }

            return declaredElement;
        }


    }
}