using System;
using System.Web.Script.Serialization;

namespace SuPOS.WindowsBridge
{
    internal static class BridgeSelfTest
    {
        public static int Run()
        {
            try
            {
                ApiEnvelope envelope = new ApiEnvelope
                {
                    success = true,
                    code = "OK",
                    message = "Success",
                    requestId = "selftest",
                    data = new HealthData
                    {
                        bridgeMode = "ReadOnly",
                        bridgeStatus = "READY",
                        tradeServiceStatus = "NOT_CHECKED",
                        checkedAt = "2000-01-01T00:00:00.0000000+00:00"
                    }
                };

                string json = new JavaScriptSerializer().Serialize(envelope);
                if (String.IsNullOrEmpty(json) || json.IndexOf("\"requestId\":\"selftest\"", StringComparison.Ordinal) < 0)
                {
                    Console.Error.WriteLine("Self-test failed: response contract serialization.");
                    return 1;
                }
                if (!String.Equals("abc", "abc", StringComparison.Ordinal))
                {
                    Console.Error.WriteLine("Self-test failed: ordinal comparison.");
                    return 1;
                }

                Console.WriteLine("SuPOS Windows Bridge self-test passed.");
                return 0;
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine("Self-test failed: {0}", ex.Message);
                return 1;
            }
        }
    }
}
