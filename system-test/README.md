
How to handle assertions from lambda? Maybe moving to JUnit4 solves the problem? Maybe helps with EDT?
Move to JUnit4.

Mocking all environment, insights, spans... so the returned json is valid.
Find the relevant events to trigger for all actions.

Multiple dispatch - how can you tell that all events were processed?

Though the test pass, we get ugly trace printout during tearDown because some backgroundable threads try to access services that are no longer exist.
- Can we remove all backgroundable threads before starting tearDown?
- Ig not, can we remove the printout? At least for testing env? 


BackendConnectionMonitor:
```kotlin
    fun isConnectionError(): Boolean {
        Log.test(logger::warn,"isConnectionError")
        return false
    //        return hasConnectionError // this is the real implementation
    
        }

    fun isConnectionOk(): Boolean {
        Log.test(logger::info,"isConnectionOk")
        return true
    //        return !hasConnectionError  // this is the real implementation
}
```
Either we mock the service:
To do so we need to remove the Service annotation from the class and register it in plugin.xml.
Or we mock other calls to make sure we never get connection error.



Moving target...
