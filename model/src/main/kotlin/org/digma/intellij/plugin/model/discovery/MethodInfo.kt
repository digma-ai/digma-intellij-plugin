package org.digma.intellij.plugin.model.discovery

data class MethodInfo(override val id: String,
                      val name: String,
                      val containingClass: String,
                      val containingNamespace: String,
                      val containingFileUri: String) : CodeObjectInfo {


    fun getRelatedCodeObjectIds(): List<String> {


        val relatedIds: MutableList<String> = ArrayList()


        //todo: temp. hard coded span ids until we have related code objects discovery
        if ("Sample.MoneyTransfer.API.Domain.Services.MoneyTransferDomainService\$_\$TransferFunds" == id) {
            relatedIds.add("span:MoneyTransferDomainService\$_\$Peristing balance transfer")
            relatedIds.add("span:MoneyTransferDomainService\$_\$Creating record of transaction")
        } else if ("Sample.MoneyTransfer.API.Controllers.TransferController\$_\$TransferFunds" == id) {
            relatedIds.add("span:TransferController\$_\$Process transfer")
        }else if ("Sample.MoneyTransfer.API.Controllers.SampleInsightsController\$_\$SpanBottleneck" == id) {
            relatedIds.add("span:SampleInsightsController\$_\$SpanBottleneck 1")
            relatedIds.add("span:SampleInsightsController\$_\$SpanBottleneck 2")
        }
        return relatedIds
    }

    override fun idWithType(): String {
        return "method:$id"
    }
}