# Listens on Windows localhost and copies bytes to the API running inside WSL.
# Windows localhost:8000 is often already taken (for example by an editor port
# forward that accepts the connection and returns nothing). The emulator reaches
# this proxy through `adb reverse`, then the proxy dials the WSL address.
param(
    [Parameter(Mandatory = $true)][int]$ListenPort,
    [Parameter(Mandatory = $true)][string]$TargetHost,
    [Parameter(Mandatory = $true)][int]$TargetPort
)

$ErrorActionPreference = 'Stop'
Add-Type -TypeDefinition @"
using System;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;

public static class SahakarTcpProxy {
    public static void Run(int listenPort, string targetHost, int targetPort) {
        // adb reverse dials localhost, which is 127.0.0.1 on some machines and ::1 on others.
        var v4 = new TcpListener(IPAddress.Loopback, listenPort);
        v4.Start();
        Console.WriteLine("listening " + listenPort);
        try {
            var v6 = new TcpListener(IPAddress.IPv6Loopback, listenPort);
            v6.Server.SetSocketOption(SocketOptionLevel.IPv6, SocketOptionName.IPv6Only, true);
            v6.Start();
            ThreadPool.QueueUserWorkItem(_ => AcceptLoop(v6, targetHost, targetPort));
        } catch (Exception) {
        }
        AcceptLoop(v4, targetHost, targetPort);
    }

    static void AcceptLoop(TcpListener listener, string targetHost, int targetPort) {
        while (true) {
            var incoming = listener.AcceptTcpClient();
            ThreadPool.QueueUserWorkItem(_ => Pump(incoming, targetHost, targetPort));
        }
    }

    static void Pump(TcpClient incoming, string targetHost, int targetPort) {
        TcpClient outgoing = null;
        try {
            outgoing = new TcpClient();
            outgoing.Connect(targetHost, targetPort);
            var local = incoming.GetStream();
            var remote = outgoing.GetStream();
            // Wait for both directions. Closing when the request finishes drops
            // the response, which is what the phone is waiting to read.
            var upload = local.CopyToAsync(remote);
            var download = remote.CopyToAsync(local);
            try { Task.WaitAll(upload, download); } catch (Exception) {}
        } catch (Exception) {
        } finally {
            try { incoming.Close(); } catch (Exception) {}
            if (outgoing != null) {
                try { outgoing.Close(); } catch (Exception) {}
            }
        }
    }
}
"@

[SahakarTcpProxy]::Run($ListenPort, $TargetHost, $TargetPort)
