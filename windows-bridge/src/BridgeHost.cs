using System;
using System.IO;
using System.Net;
using System.Text;
using System.Threading;
using System.Web.Script.Serialization;

namespace SuPOS.WindowsBridge
{
    internal sealed class BridgeHost : IDisposable
    {
        private readonly BridgeSettings _settings;
        private readonly ITradeReader _tradeReader;
        private readonly HttpListener _listener;
        private Thread _worker;
        private volatile bool _running;

        public BridgeHost(BridgeSettings settings, ITradeReader tradeReader)
        {
            _settings = settings;
            _tradeReader = tradeReader;
            _listener = new HttpListener();
            _listener.Prefixes.Add(settings.ListenPrefix);
        }

        public void Start()
        {
            _listener.Start();
            _running = true;
            _worker = new Thread(ListenLoop);
            _worker.IsBackground = true;
            _worker.Name = "SuPOSBridgeListener";
            _worker.Start();
        }

        private void ListenLoop()
        {
            while (_running)
            {
                try
                {
                    HttpListenerContext context = _listener.GetContext();
                    Handle(context);
                }
                catch (HttpListenerException)
                {
                    if (_running)
                    {
                        Console.Error.WriteLine("Bridge listener stopped unexpectedly.");
                    }
                }
                catch (ObjectDisposedException)
                {
                    return;
                }
            }
        }

        private void Handle(HttpListenerContext context)
        {
            string requestId = Guid.NewGuid().ToString("N");

            try
            {
                string path = context.Request.Url.AbsolutePath.TrimEnd('/');
                if (path.Length == 0)
                {
                    path = "/";
                }

                if (!String.Equals(context.Request.HttpMethod, "GET", StringComparison.OrdinalIgnoreCase))
                {
                    Write(context.Response, 405, Failure("MUTATIONS_DISABLED", "Bridge is read-only.", requestId));
                    return;
                }

                if (String.Equals(path, "/health", StringComparison.OrdinalIgnoreCase))
                {
                    HealthData health = new HealthData
                    {
                        bridgeMode = _settings.BridgeMode,
                        bridgeStatus = "READY",
                        tradeServiceStatus = _tradeReader.CanReachTradeService() ? "REACHABLE" : "UNREACHABLE",
                        checkedAt = DateTimeOffset.UtcNow.ToString("o")
                    };
                    Write(context.Response, 200, Success(health, requestId));
                    return;
                }

                if (String.Equals(path, "/v1/trades", StringComparison.OrdinalIgnoreCase))
                {
                    if (!IsAuthorized(context.Request))
                    {
                        Write(context.Response, 401, Failure("UNAUTHORIZED", "A valid Bridge key is required.", requestId));
                        return;
                    }
                    Write(context.Response, 200, Success(_tradeReader.ReadTrades(), requestId));
                    return;
                }

                Write(context.Response, 404, Failure("NOT_FOUND", "Unknown Bridge route.", requestId));
            }
            catch (Exception)
            {
                Write(context.Response, 503, Failure(
                    "TRADE_SERVICE_UNAVAILABLE",
                    "The local Windows POS trade service could not be verified.",
                    requestId));
            }
        }

        private bool IsAuthorized(HttpListenerRequest request)
        {
            if (String.IsNullOrEmpty(_settings.ApiKey))
            {
                return false;
            }
            string supplied = request.Headers["X-SuPOS-Bridge-Key"];
            return FixedTimeEquals(_settings.ApiKey, supplied);
        }

        private static bool FixedTimeEquals(string expected, string supplied)
        {
            if (expected == null || supplied == null)
            {
                return false;
            }

            int difference = expected.Length ^ supplied.Length;
            int max = Math.Max(expected.Length, supplied.Length);
            for (int i = 0; i < max; i++)
            {
                char left = i < expected.Length ? expected[i] : (char)0;
                char right = i < supplied.Length ? supplied[i] : (char)0;
                difference |= left ^ right;
            }
            return difference == 0;
        }

        private static ApiEnvelope Success(object data, string requestId)
        {
            return new ApiEnvelope
            {
                success = true,
                code = "OK",
                message = "Success",
                requestId = requestId,
                data = data
            };
        }

        private static ApiEnvelope Failure(string code, string message, string requestId)
        {
            return new ApiEnvelope
            {
                success = false,
                code = code,
                message = message,
                requestId = requestId,
                data = null
            };
        }

        private static void Write(HttpListenerResponse response, int statusCode, ApiEnvelope envelope)
        {
            JavaScriptSerializer serializer = new JavaScriptSerializer();
            serializer.MaxJsonLength = 1024 * 1024;
            byte[] body = Encoding.UTF8.GetBytes(serializer.Serialize(envelope));
            response.StatusCode = statusCode;
            response.ContentType = "application/json; charset=utf-8";
            response.ContentEncoding = Encoding.UTF8;
            response.ContentLength64 = body.Length;
            response.Headers["Cache-Control"] = "no-store";
            using (Stream output = response.OutputStream)
            {
                output.Write(body, 0, body.Length);
            }
        }

        public void Dispose()
        {
            _running = false;
            if (_listener.IsListening)
            {
                _listener.Stop();
            }
            _listener.Close();
            if (_worker != null && _worker.IsAlive)
            {
                _worker.Join(1000);
            }
        }
    }
}
