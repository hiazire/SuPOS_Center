using System;
using System.Text;

namespace SuPOS.WindowsBridge
{
    internal static class Program
    {
        private static int Main(string[] args)
        {
            Console.OutputEncoding = Encoding.UTF8;

            if (args != null && args.Length == 1 && args[0] == "--self-test")
            {
                return BridgeSelfTest.Run();
            }

            try
            {
                BridgeSettings settings = BridgeSettings.Load();
                using (BridgeHost host = new BridgeHost(settings, new PtcTradeReader(settings)))
                {
                    host.Start();
                    Console.WriteLine("SuPOS Windows Bridge is running in {0} mode.", settings.BridgeMode);
                    Console.WriteLine("Listening on {0}", settings.ListenPrefix);
                    Console.WriteLine("Press Enter to stop.");
                    Console.ReadLine();
                }
                return 0;
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine("Bridge startup failed: {0}", ex.Message);
                return 1;
            }
        }
    }
}
