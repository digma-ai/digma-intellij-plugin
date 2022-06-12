package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.InsightsRequest;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummaryRequest;
import org.digma.intellij.plugin.ui.model.environment.EnvComboModel;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AnalyticsService implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(AnalyticsService.class);

    private final Environment environment;

    private final AnalyticsProvider analyticsProviderProxy;


    public AnalyticsService(@NotNull Project project) {
        environment = new Environment(project,this);
        analyticsProviderProxy = newAnalyticsProviderProxy(new RestAnalyticsProvider(environment.getBaseUrl()));
        List<String> envs = getEnvironments();
        if (envs != null) {
            environment.setEnvironmentsList(envs);
        }
        EnvComboModel.INSTANCE.initialize(environment);
    }


    List<String> getEnvironments() {
        try {
            return analyticsProviderProxy.getEnvironments();
        }catch (Throwable e){
            //getEnvironments should never throw exception. it is called only from the constructor or be the
            //Environment object that can handle null.
            return null;
        }
    }

    private String getCurrentEnvironment() throws AnalyticsServiceException {
        String currentEnv = environment.getCurrent();
        if (currentEnv == null || currentEnv.isEmpty()){
            throw new AnalyticsServiceException("No selected environment");
        }
        return currentEnv;
    }


    public List<CodeObjectSummary> getSummaries(List<String> objectIds) throws AnalyticsServiceException {
        return analyticsProviderProxy.getSummaries(new CodeObjectSummaryRequest(getCurrentEnvironment(), objectIds));
    }

    public List<CodeObjectInsight> getInsights(List<String> objectIds) throws AnalyticsServiceException {
        return analyticsProviderProxy.getInsights(new InsightsRequest(getCurrentEnvironment(), objectIds));
    }

    public List<CodeObjectError> getErrorsOfCodeObject( String codeObjectId) throws AnalyticsServiceException {
        return analyticsProviderProxy.getErrorsOfCodeObject(getCurrentEnvironment(), codeObjectId);
    }

    @Override
    public void dispose() {
        try {
            analyticsProviderProxy.close();
        } catch (Exception e) {
            Log.log(LOGGER::error, "exception while closing AnalyticsProvider {}", e.getMessage());
        }
    }




    /**
     * A proxy to swallow all AnalyticsProvider exceptions.
     * the AnalyticsService class is an IDE component and better not to throw exceptions.
     */
    private static class AnalyticsInvocationHandler implements InvocationHandler {

        private final AnalyticsProvider analyticsProvider;

        //ObjectMapper here is only used for printing the result to log as json
        private final ObjectMapper objectMapper = new ObjectMapper();

        private boolean hadConnectException = false;

        public AnalyticsInvocationHandler(AnalyticsProvider analyticsProvider) {
            this.analyticsProvider = analyticsProvider;
        }


        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable{
            try {

                if (LOGGER.isDebugEnabled()) {
                    Log.log(LOGGER::debug, "Sending request to {}: args '{}'",method.getName(), argsToString(args));
                }

                Object result = method.invoke(analyticsProvider, args);

                if (LOGGER.isDebugEnabled()) {
                    Log.log(LOGGER::debug, "Got response from {}: args '{}', -----------------" +
                            "Result '{}'",method.getName(), argsToString(args),resultToString(result));
                }

                //reset hadConnectException to false on success call
                hadConnectException = false;
                return result;

            } catch (InvocationTargetException e) {

                //handle only InvocationTargetException, other exceptions are probably a bug.
                //log to error only the first time of ConnectException, this form of error log will popup a balloon error
                //message, its not necessary to popup the balloon too many times. following exceptions will just be logged
                //to the log file
                if (isConnectionException(e) && !hadConnectException){
                    hadConnectException = true;
                    Log.error(LOGGER, e, "error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), e);
                }
                else{
                   Log.log(LOGGER::warn,"error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), e.getCause().getMessage());
                }

                throw new AnalyticsServiceException(e);
            }
        }

        private boolean isConnectionException(InvocationTargetException e) {
            if (e.getCause() instanceof AnalyticsProviderException){
                Throwable realCause = e.getCause().getCause();
                return realCause instanceof ConnectException;
            }
            return false;
        }


        private String resultToString(Object result) {
            try{
                //pretty print doesn't work in intellij logs, line end cause the text to disappear.
                return objectMapper.writeValueAsString(result);
            }catch (Exception e){
                return "Error parsing object "+e.getMessage();
            }
        }
        private String argsToString(Object[] args){
            try {
                return (args == null || args.length == 0) ? "" : Arrays.stream(args).map(Object::toString).collect(Collectors.joining(","));
            }catch (Exception e){
                return "Error parsing args "+e.getMessage();
            }
        }

    }

    private AnalyticsProvider newAnalyticsProviderProxy(AnalyticsProvider obj) {
        return (AnalyticsProvider) java.lang.reflect.Proxy.newProxyInstance(
                obj.getClass().getClassLoader(),
                new Class[]{AnalyticsProvider.class, Closeable.class},
                new AnalyticsInvocationHandler(obj));
    }

}
