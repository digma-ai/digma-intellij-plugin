package org.digma.intellij.plugin.psi;


public class CanInstrumentMethodResult{

    private final IFailureCause failureCause;

    public CanInstrumentMethodResult(){
        this.failureCause = null;
    }

    public CanInstrumentMethodResult(IFailureCause failureCause){

        this.failureCause = failureCause;
    }

    public static CanInstrumentMethodResult Failure(){
        return new CanInstrumentMethodResult(new GenericFailureCause());
    }

    public boolean wasSucceeded() { return failureCause == null; }

    public IFailureCause getFailureCause(){ return failureCause; }


    public interface IFailureCause {}

    public record MissingDependencyCause(String dependency) implements IFailureCause {}

    public record GenericFailureCause() implements IFailureCause {}
}
