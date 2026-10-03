# SuPOS Windows Bridge

This project is the controlled boundary between SuPOS Android Center and the
legacy Windows POS services.

The first executable milestone is deliberately read-only:

- `GET /health` reports Bridge and local TradeService availability.
- `GET /v1/trades` returns a minimal, non-customer trade summary.
- Every non-GET request is rejected with `MUTATIONS_DISABLED`.
- The listener defaults to `127.0.0.1`; LAN binding requires a later security
  review and an explicit environment opt-in.

No vendor binaries or production POS data are modified by this project.

The minimum product target is narrower than a full Android checkout:

1. Read the complete Windows POS trade snapshot required by Android.
2. Submit an Android-created trade with `isSaveToTemp=true` so it appears in
   the Windows POS temporary-trade list.
3. Read the Windows trade back by its stable identifier before Android reports
   success.

The future write route is reserved as `POST /v1/temporary-trades`. It must stay
disabled until a de-identified Windows fixture proves the required Trade and
product fields, the PTC amount unit, and the exact temporary-trade read-back
behavior.

## Windows build environment

- Windows POS host
- .NET Framework 4.5
- C# 5 compiler:
  `C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe`

Run `build.cmd` from this directory on the Windows POS host. The output is
written to `build\SuPOS.WindowsBridge.exe`.

## Local read-only startup

Set a temporary API key in the process environment. Do not write it into this
repository or `App.config`.

```bat
set SUPOS_BRIDGE_API_KEY=<temporary-secret>
build\SuPOS.WindowsBridge.exe
```

The default URL is `http://127.0.0.1:18080/`. The trades endpoint requires:

```text
X-SuPOS-Bridge-Key: <temporary-secret>
```

## Safety boundary

`BridgeMode` must remain `ReadOnly` until the mutation contract, authentication,
idempotency store, Windows/HQ read-back, and rollback behavior have passed
fixture and staging verification. Binding to a non-loopback address additionally
requires `SUPOS_BRIDGE_ALLOW_LAN=1`; this is not approval to deploy it.

The PTC `Amount` unit is intentionally returned as `PTC_UNVERIFIED`. Monetary
normalization must be added only after a captured fixture confirms whether the
specific WCF service returns currency units or scaled integers.
