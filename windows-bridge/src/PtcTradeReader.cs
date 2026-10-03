using System;
using System.Collections.Generic;
using System.Globalization;
using System.ServiceModel;
using System.Xml;

namespace SuPOS.WindowsBridge
{
    internal interface ITradeReader
    {
        bool CanReachTradeService();
        TradeListData ReadTrades();
    }

    internal sealed class PtcTradeReader : ITradeReader
    {
        private readonly BridgeSettings _settings;

        public PtcTradeReader(BridgeSettings settings)
        {
            _settings = settings;
        }

        public bool CanReachTradeService()
        {
            try
            {
                ReadTrades();
                return true;
            }
            catch
            {
                return false;
            }
        }

        public TradeListData ReadTrades()
        {
            NetTcpBinding binding = CreateBinding();
            EndpointAddress address = new EndpointAddress(_settings.TradeServiceEndpoint);
            ChannelFactory<ITradeWcfService> factory = new ChannelFactory<ITradeWcfService>(binding, address);
            ITradeWcfService client = null;

            try
            {
                client = factory.CreateChannel();
                PtcTrade[] source = client.GetTrade() ?? new PtcTrade[0];
                int count = Math.Min(source.Length, _settings.MaxResponseItems);
                List<TradeSummary> summaries = new List<TradeSummary>(count);

                for (int i = 0; i < count; i++)
                {
                    PtcTrade trade = source[i];
                    if (trade == null)
                    {
                        continue;
                    }

                    summaries.Add(new TradeSummary
                    {
                        localId = trade.ID,
                        onlineOrderId = SafeText(trade.OnlineOrderId),
                        amountRaw = trade.Amount.ToString(CultureInfo.InvariantCulture),
                        amountUnit = "PTC_UNVERIFIED",
                        tableName = SafeText(trade.TableName),
                        createTime = trade.CreateTime == DateTime.MinValue
                            ? null
                            : trade.CreateTime.ToString("o", CultureInfo.InvariantCulture),
                        isSaveToTemp = trade.isSaveToTemp
                    });
                }

                CloseSafely(client as ICommunicationObject);
                factory.Close();

                return new TradeListData
                {
                    count = summaries.Count,
                    truncated = source.Length > count,
                    trades = summaries
                };
            }
            catch
            {
                AbortSafely(client as ICommunicationObject);
                factory.Abort();
                throw;
            }
        }

        private static NetTcpBinding CreateBinding()
        {
            NetTcpBinding binding = new NetTcpBinding(SecurityMode.None);
            binding.OpenTimeout = TimeSpan.FromSeconds(3);
            binding.CloseTimeout = TimeSpan.FromSeconds(3);
            binding.SendTimeout = TimeSpan.FromSeconds(5);
            binding.ReceiveTimeout = TimeSpan.FromSeconds(5);
            binding.MaxReceivedMessageSize = 1024 * 1024;
            binding.MaxBufferSize = 1024 * 1024;
            binding.ReaderQuotas = new XmlDictionaryReaderQuotas();
            binding.ReaderQuotas.MaxDepth = 32;
            binding.ReaderQuotas.MaxArrayLength = 10000;
            binding.ReaderQuotas.MaxStringContentLength = 65536;
            binding.ReaderQuotas.MaxBytesPerRead = 4096;
            binding.ReaderQuotas.MaxNameTableCharCount = 16384;
            return binding;
        }

        private static string SafeText(string value)
        {
            if (String.IsNullOrWhiteSpace(value))
            {
                return null;
            }
            string trimmed = value.Trim();
            return trimmed.Length <= 128 ? trimmed : trimmed.Substring(0, 128);
        }

        private static void CloseSafely(ICommunicationObject channel)
        {
            if (channel == null)
            {
                return;
            }
            try
            {
                channel.Close();
            }
            catch
            {
                channel.Abort();
            }
        }

        private static void AbortSafely(ICommunicationObject channel)
        {
            if (channel != null)
            {
                channel.Abort();
            }
        }
    }
}
