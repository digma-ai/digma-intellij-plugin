using System;
using JetBrains.Annotations;
using JetBrains.ReSharper.Daemon.CSharp.Stages;
using JetBrains.ReSharper.Feature.Services.Daemon;
using JetBrains.ReSharper.Psi.CSharp.Tree;

namespace Digma.Rider.Highlighting
{
    [Obsolete(message: "Experimental: maybe we can call a daemon directly instead of calling Reanalyze for all daemons")]
    public class CodeObjectsAnalyzerDaemonStageProcess : CSharpDaemonStageProcessBase
    {
        [NotNull] private readonly IDaemonProcess _process;
        [NotNull] private readonly ICSharpFile _file;

        public CodeObjectsAnalyzerDaemonStageProcess([NotNull] IDaemonProcess process, [NotNull] ICSharpFile file) : base(process, file)
        {
            _process = process;
            _file = file;
        }

        public override void Execute(Action<DaemonStageResult> committer)
        {
           
        }
    }
}