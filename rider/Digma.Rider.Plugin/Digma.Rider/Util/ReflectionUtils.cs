using System;
using Digma.Rider.Logging;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Model2.Assemblies.Impl;
using JetBrains.Rider.Model;
using JetBrains.Util;
using Assembly = System.Reflection.Assembly;

namespace Digma.Rider.Util;

public class ReflectionUtils
{



    // public static Solution GetProtocolSolution(ISolution solution, ILogger logger)
    // {
    //     // Type type = Type.GetType("JetBrains.RdBackend.Common.Features.ProjectModelExtensions");
    //     // var protocolSolution = type.GetMethod("GetProtocolSolution",new Type[]{typeof(ISolution)})
    //     //     .Invoke(null, new object[] { solution });
    //     try
    //     {
    //         //Type.GetType("JetBrains.RdBackend.Common.Features.ProjectModelExtensions, JetBrains.RdBackend.Common")
    //         #if ( PROFILE_2022_3) // FIX_WHEN_MIN_IS_232
    //             var type = typeof(JetBrains.ReSharper.Feature.Services.Protocol)
    //         #else
    //             var type = typeof(JetBrains.RdBackend.Common.Features.ProjectModelExtensions);
    //         #endif
    //         
    //         var protocolSolution = type.GetMethod("GetProtocolSolution", new Type[] { typeof(ISolution) })!
    //             .Invoke(null, new object[] { solution });
    //
    //         return (Solution)protocolSolution;
    //     }
    //     catch (Exception e)
    //     {
    //         Logger.Log(logger, "Exception ");
    //     }
    //
    //     return null;
    // }
}