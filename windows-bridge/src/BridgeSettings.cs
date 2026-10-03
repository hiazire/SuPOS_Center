using System;
using System.Configuration;

namespace SuPOS.WindowsBridge
{
    internal sealed class BridgeSettings
    {
        public string BridgeMode { get; private set; }
        public string ListenPrefix { get; private set; }
        public string TradeServiceEndpoint { get; private set; }
        public int MaxResponseItems { get; private set; }
        public string ApiKey { get; private set; }

        public static BridgeSettings Load()
        {
            BridgeSettings settings = new BridgeSettings();
            settings.BridgeMode = ReadRequired("BridgeMode");
            settings.ListenPrefix = ReadRequired("ListenPrefix");
            settings.TradeServiceEndpoint = ReadRequired("TradeServiceEndpoint");
            settings.MaxResponseItems = ReadPositiveInt("MaxResponseItems", 200);
            settings.ApiKey = Environment.GetEnvironmentVariable("SUPOS_BRIDGE_API_KEY") ?? String.Empty;
            settings.Validate();
            return settings;
        }

        private void Validate()
        {
            if (!String.Equals(BridgeMode, "ReadOnly", StringComparison.Ordinal))
            {
                throw new ConfigurationErrorsException("Only ReadOnly mode is enabled in this milestone.");
            }

            Uri listenUri;
            if (!Uri.TryCreate(ListenPrefix, UriKind.Absolute, out listenUri) || listenUri.Scheme != Uri.UriSchemeHttp)
            {
                throw new ConfigurationErrorsException("ListenPrefix must be an absolute HTTP prefix.");
            }

            bool loopback = listenUri.IsLoopback;
            bool lanOptIn = String.Equals(
                Environment.GetEnvironmentVariable("SUPOS_BRIDGE_ALLOW_LAN"),
                "1",
                StringComparison.Ordinal);
            if (!loopback && !lanOptIn)
            {
                throw new ConfigurationErrorsException(
                    "Non-loopback binding is disabled. Complete the transport security review first.");
            }

            Uri tradeUri;
            if (!Uri.TryCreate(TradeServiceEndpoint, UriKind.Absolute, out tradeUri) ||
                !String.Equals(tradeUri.Scheme, "net.tcp", StringComparison.OrdinalIgnoreCase) ||
                !tradeUri.IsLoopback)
            {
                throw new ConfigurationErrorsException("TradeServiceEndpoint must be a local net.tcp endpoint.");
            }
        }

        private static string ReadRequired(string key)
        {
            string value = ConfigurationManager.AppSettings[key];
            if (String.IsNullOrWhiteSpace(value))
            {
                throw new ConfigurationErrorsException("Missing appSetting: " + key);
            }
            return value.Trim();
        }

        private static int ReadPositiveInt(string key, int defaultValue)
        {
            int value;
            string raw = ConfigurationManager.AppSettings[key];
            if (String.IsNullOrWhiteSpace(raw))
            {
                return defaultValue;
            }
            if (!Int32.TryParse(raw, out value) || value <= 0)
            {
                throw new ConfigurationErrorsException("Invalid positive integer appSetting: " + key);
            }
            return value;
        }
    }
}
