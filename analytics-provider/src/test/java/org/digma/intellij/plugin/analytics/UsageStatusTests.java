package org.digma.intellij.plugin.analytics;

import org.digma.intellij.plugin.model.rest.usage.UsageStatusRequest;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class UsageStatusTests extends AbstractAnalyticsProviderTest {

    //run against running env just for local test
    //@Test
    public void getUsageStatusTemp() {
        {
            List<String> ids = new ArrayList<>();
            ids.add("method:Sample.MoneyTransfer.API.Controllers.TransferController$_$TransferFunds(TransferRequest)");
            ids.add("span:MoneyTransferDomainService$_$Peristing balance transfer");
            UsageStatusRequest usageStatusRequest = new UsageStatusRequest(ids);
            AnalyticsProvider analyticsProvider = new RestAnalyticsProvider("https://localhost:5051");
            UsageStatusResult usageStatusResult = analyticsProvider.getUsageStatus(usageStatusRequest);

            System.out.println(usageStatusResult);
        }
    }

    @Test
    public void doNothing() {
    }
}
