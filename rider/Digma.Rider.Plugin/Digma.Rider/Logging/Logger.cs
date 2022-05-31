using Digma.Rider.Protocol;
using JetBrains.Annotations;
using JetBrains.Util;

namespace Digma.Rider.Logging
{
    public static class Logger
    {
        public static void Log(ILogger logger, string message, params object[] parameters)
        {
            logger.Trace("Digma: " + message, parameters);
        }
        
        
        public static void LogFoundMethodsForDocument(ILogger logger,[NotNull] Document document)
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