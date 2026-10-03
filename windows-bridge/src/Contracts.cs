using System;
using System.Collections.Generic;
using System.Runtime.Serialization;

namespace SuPOS.WindowsBridge
{
    internal sealed class ApiEnvelope
    {
        public bool success { get; set; }
        public string code { get; set; }
        public string message { get; set; }
        public string requestId { get; set; }
        public object data { get; set; }
    }

    internal sealed class HealthData
    {
        public string bridgeMode { get; set; }
        public string bridgeStatus { get; set; }
        public string tradeServiceStatus { get; set; }
        public string checkedAt { get; set; }
    }

    internal sealed class TradeListData
    {
        public int count { get; set; }
        public bool truncated { get; set; }
        public List<TradeSummary> trades { get; set; }
    }

    internal sealed class TradeSummary
    {
        public int localId { get; set; }
        public string onlineOrderId { get; set; }
        public string amountRaw { get; set; }
        public string amountUnit { get; set; }
        public string tableName { get; set; }
        public string createTime { get; set; }
        public bool isSaveToTemp { get; set; }
    }

    [ServiceContract(Namespace = "http://tempuri.org/", ConfigurationName = "ITradeWcfService")]
    internal interface ITradeWcfService
    {
        [OperationContract(
            Action = "http://tempuri.org/ITradeWcfService/GetTrade",
            ReplyAction = "http://tempuri.org/ITradeWcfService/GetTradeResponse")]
        PtcTrade[] GetTrade();
    }

    [DataContract(Namespace = "http://schemas.datacontract.org/2004/07/Ptc.iPos.Common.Trade")]
    internal sealed class PtcTrade
    {
        [DataMember]
        public int ID { get; set; }

        [DataMember]
        public string OnlineOrderId { get; set; }

        [DataMember]
        public decimal Amount { get; set; }

        [DataMember]
        public string TableName { get; set; }

        [DataMember]
        public DateTime CreateTime { get; set; }

        [DataMember]
        public bool isSaveToTemp { get; set; }
    }
}
