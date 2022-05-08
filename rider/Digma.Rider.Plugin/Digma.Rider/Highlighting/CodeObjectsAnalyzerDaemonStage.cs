using System;
using JetBrains.Application.Settings;
using JetBrains.ReSharper.Feature.Services.CSharp.Daemon;
using JetBrains.ReSharper.Feature.Services.Daemon;
using JetBrains.ReSharper.Psi.CSharp.Tree;

namespace Digma.Rider.Highlighting
{
    [Obsolete(message: "Experimental: maybe we can call a daemon directly instead of calling Reanalyze for all daemons")]
    // [DaemonStage]
    public class CodeObjectsAnalyzerDaemonStage : CSharpDaemonStageBase
    {
        private CodeObjectsAnalyzerDaemonStageProcess _myDaemonStageProcess;
        protected override IDaemonStageProcess CreateProcess(IDaemonProcess process, IContextBoundSettingsStore settings,
            DaemonProcessKind processKind, ICSharpFile file)
        {
            _myDaemonStageProcess = new CodeObjectsAnalyzerDaemonStageProcess(process, file);
            return _myDaemonStageProcess;
        }
        
        
    }
}