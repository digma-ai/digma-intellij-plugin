using Digma.Rider.Protocol;
using JetBrains.Annotations;
using JetBrains.ProjectModel;
using JetBrains.Util;


namespace Digma.Rider.Logging
{
    public static class Logger
    {
        public static void Log(ILogger logger, string message, params object[] parameters)
        {
            //todo:
            //change to debug. first find out how to change the level in resharper
            logger.Info("Digma: " + message, parameters);
        }
        
        public static void Log(ILogger logger,ISolution solution, string message, params object[] parameters)
        {
            //todo:
            //change to debug. first find out how to change the level in resharper
            logger.Info("Digma: "+"Solution:"+solution.Name+": " + message, parameters);
        }
        
        
        public static void LogFoundMethodsForDocument(ILogger logger,[NotNull] RiderDocumentInfo document)
        {
            var methodInfos = document.Methods;
            if (methodInfos.IsEmpty())
            {
                Log(logger, "Didn't find methods for document {0}", document.FileUri);
            }
            else
            {
                if (logger.IsTraceEnabled())
                {
                    var methodInfosStr = string.Join("", methodInfos);
                    Log(logger, "Found Methods for document {0}, methods: {1}", document.FileUri,
                        methodInfosStr);
                }
            }
        }
    }
}