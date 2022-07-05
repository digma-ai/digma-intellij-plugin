package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.InsightsRequest;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummaryRequest;
import org.digma.intellij.plugin.notifications.NotificationUtil;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.ui.model.environment.EnvComboModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLException;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsService implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(AnalyticsService.class);

    private final Environment environment;
    private String myApiUrl;
    @Nullable
    private String myApiToken;
    private final Project project;

    private AnalyticsProvider analyticsProviderProxy;
    private final SettingsState settingsState;
    private final PersistenceService persistenceService;


    public AnalyticsService(@NotNull Project project) {
        settingsState = project.getService(SettingsState.class);
        persistenceService = project.getService(PersistenceService.class);
        environment = new Environment(project, this, persistenceService.getState());
        this.project = project;
        myApiUrl = settingsState.apiUrl;
        myApiToken = settingsState.apiToken;
        replaceClientAndFireChange(myApiUrl, myApiToken);
        EnvComboModel.INSTANCE.initialize(environment);
        settingsState.addChangeListener(settingsState -> {
            if (!Objects.equals(settingsState.apiUrl, myApiUrl)) {
                myApiUrl = settingsState.apiUrl;
                myApiToken = settingsState.apiToken;
                replaceClientAndFireChange(myApiUrl, myApiToken);
            }
            if (!Objects.equals(settingsState.apiToken, myApiToken)) {
                myApiToken = settingsState.apiToken;
                replaceClient(myApiUrl, myApiToken);
            }
        });
    }


    //just replace the client and do not fire any events
    private void replaceClient(String url, String token) {
        if (analyticsProviderProxy != null) {
            try {
                analyticsProviderProxy.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        analyticsProviderProxy = newAnalyticsProviderProxy(new RestAnalyticsProvider(url, token), project);
    }


    private void replaceClientAndFireChange(String url, String token) {

        replaceClient(url,token);

        List<String> envs = getEnvironments();
        if (envs == null) {
            envs = new ArrayList<>();
        }

        environment.replaceEnvironmentsList(envs);
    }


    List<String> getEnvironments() {
        try {
            return analyticsProviderProxy.getEnvironments();
        } catch (Throwable e) {
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

    public CodeObjectErrorDetails getErrorDetails(String errorUid) throws AnalyticsServiceException {
        return analyticsProviderProxy.getCodeObjectErrorDetails(errorUid);
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
        private final Project project;

        //ObjectMapper here is only used for printing the result to log as json
        private final ObjectMapper objectMapper = new ObjectMapper();

        //this status is only used for helping with reporting messages only when necessary and keep the log clean
        private final Status status = new Status();

        public AnalyticsInvocationHandler(AnalyticsProvider analyticsProvider,Project project) {
            this.analyticsProvider = analyticsProvider;
            this.project = project;
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

                //reset status on success call
                status.ok();
                return result;

            } catch (InvocationTargetException e) {

                //handle only InvocationTargetException, other exceptions are probably a bug.
                //log to error only the first time of ConnectException, this form of error log will popup a balloon error
                //message, its not necessary to popup the balloon too many times. following exceptions will just be logged
                //to the log file
                boolean isConnectionException = isConnectionException(e) || isSslConnectionException(e);
                if ( status.isOk() && isConnectionException ){
                    status.connectError();
                    Log.log(LOGGER::warn, "Connect exception: error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), e);
                    LOGGER.warn(e);
                    var message = isConnectionException(e) ? getConnectExceptionMessage(e):getSslExceptionMessage(e);
                    NotificationUtil.notifyError(project,"<html>Connection error with Digma backend api for method "+method.getName()+".<br> "
                                + message + ".<br> See logs for details.");
                }else if (status.isOk()){
                    status.error();
                    Log.log(LOGGER::debug,"Error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), e.getCause().getMessage());
                    var message = getExceptionMessage(e);
                    NotificationUtil.notifyError(project,"<html>Error with Digma backend api for method "+method.getName()+".<br> "
                            + message + ".<br> See logs for details.");

                }else if(!status.hadError(e)){
                    status.error();
                    Log.log(LOGGER::debug,"Error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), e.getCause().getMessage());
                }


                throw new AnalyticsServiceException(e);
            }
        }

        private boolean isConnectionException(InvocationTargetException e) {
            
            var ex = e.getCause();
            while (ex != null && !(ex instanceof ConnectException)){
                ex = ex.getCause();
            }
            if (ex != null){
                return true;
            }

            return e.getCause().getMessage() != null &&
                    e.getCause().getMessage().contains("Error 404");
        }


        private String getConnectExceptionMessage(InvocationTargetException e) {
            var ex = e.getCause();
            while (ex != null && !(ex instanceof ConnectException)){
                ex = ex.getCause();
            }
            if (ex != null){
                return ex.getMessage();
            }
            
            return e.getCause() != null? e.getCause().getMessage():e.getMessage();
        }

        private boolean isSslConnectionException(InvocationTargetException e) {

            var ex = e.getCause();
            while (ex != null && !(ex instanceof SSLException)){
                ex = ex.getCause();
            }
            return ex != null;
        }

        private String getSslExceptionMessage(InvocationTargetException e) {
            var ex = e.getCause();
            while (ex != null && !(ex instanceof SSLException)){
                ex = ex.getCause();
            }
            if (ex != null){
                return ex.getMessage();
            }

            return e.getCause() != null? e.getCause().getMessage():e.getMessage();
        }


        private String getExceptionMessage(InvocationTargetException e) {
            var ex = e.getCause();
            while (ex != null && !(ex instanceof AnalyticsProviderException)){
                ex = ex.getCause();
            }
            if (ex != null){
                return ex.getMessage();
            }

            return e.getCause() != null? e.getCause().getMessage():e.getMessage();
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



    private AnalyticsProvider newAnalyticsProviderProxy(AnalyticsProvider obj, Project project) {
        return (AnalyticsProvider) java.lang.reflect.Proxy.newProxyInstance(
                obj.getClass().getClassLoader(),
                new Class[]{AnalyticsProvider.class, Closeable.class},
                new AnalyticsInvocationHandler(obj,project));
    }


    private static class Status{

        private final Map<String,Boolean> errorsHistory = new HashMap<>();

        private boolean hadConnectException = false;
        private boolean hadError = false;

        boolean isInError(){
            return hadConnectException || hadError;
        }
        boolean isOk(){
            return !isInError();
        }

        public void ok() {
            hadConnectException = false;
            hadError = false;
            errorsHistory.clear();
        }

        public void connectError() {
            hadConnectException = true;
        }


        public void error() {
            hadError = true;
        }

        public boolean hadError(InvocationTargetException e) {
            var ex = e.getCause();
            while (ex != null && !(ex instanceof AnalyticsProviderException)){
                ex = ex.getCause();
            }

            if (ex != null){
                var errorName = ex.getClass().getSimpleName();
                if (errorsHistory.containsKey(errorName)){
                    return true;
                }
                errorsHistory.put(errorName,true);
            }

            return false;
        }
    }

}
