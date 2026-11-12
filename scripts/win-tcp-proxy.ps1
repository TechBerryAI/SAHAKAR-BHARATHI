# Forwards one HTTP request at a time from Windows localhost to the API in WSL.
# The emulator reaches this process at 10.0.2.2. A byte-copy in both directions
# never finishes for OkHttp: the phone keeps the socket open until it sees a
# response, and the copy waits for the phone to close. Sign-in then fails with
# "The server could not be reached" even though the API is healthy.
param(
    [Parameter(Mandatory = $true)][int]$ListenPort,
    [Parameter(Mandatory = $true)][string]$TargetHost,
    [Parameter(Mandatory = $true)][int]$TargetPort
)

$ErrorActionPreference = 'Stop'
Add-Type -TypeDefinition @"
using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;

public static class SahakarTcpProxy {
    const int HeaderLimit = 64 * 1024;
    const int BodyLimit = 2 * 1024 * 1024;

    public static void Run(int listenPort, string targetHost, int targetPort) {
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
            ThreadPool.QueueUserWorkItem(_ => Handle(incoming, targetHost, targetPort));
        }
    }

    static void Handle(TcpClient incoming, string targetHost, int targetPort) {
        TcpClient outgoing = null;
        try {
            incoming.NoDelay = true;
            incoming.ReceiveTimeout = 20000;
            incoming.SendTimeout = 20000;
            var local = incoming.GetStream();
            var request = ReadMessage(local);
            request = ForceConnectionClose(request);

            outgoing = new TcpClient();
            outgoing.NoDelay = true;
            outgoing.ReceiveTimeout = 60000;
            outgoing.SendTimeout = 20000;
            outgoing.Connect(targetHost, targetPort);
            var remote = outgoing.GetStream();
            remote.Write(request, 0, request.Length);
            remote.Flush();

            var response = ReadMessage(remote);
            local.Write(response, 0, response.Length);
            local.Flush();
        } catch (Exception) {
        } finally {
            try { incoming.Close(); } catch (Exception) {}
            if (outgoing != null) {
                try { outgoing.Close(); } catch (Exception) {}
            }
        }
    }

    // Headers plus the body declared by Content-Length or chunked encoding.
    // A response with neither is read until the server closes.
    static byte[] ReadMessage(NetworkStream stream) {
        var data = new List<byte>(2048);
        int headerEnd = ReadUntil(stream, data, new byte[] { 13, 10, 13, 10 }, HeaderLimit, 0);
        if (headerEnd < 0) throw new IOException("incomplete headers");
        int bodyStart = headerEnd + 4;
        string headers = Encoding.ASCII.GetString(data.ToArray(), 0, bodyStart);
        bool chunked = HeaderHas(headers, "Transfer-Encoding", "chunked");
        int contentLength = HeaderInt(headers, "Content-Length");
        if (chunked) {
            ReadChunked(stream, data, bodyStart);
        } else if (contentLength >= 0) {
            ReadExact(stream, data, bodyStart + contentLength);
        } else if (!IsRequestHead(headers)) {
            // A response with no length is finished when the server closes.
            ReadUntilClose(stream, data);
        }
        return data.ToArray();
    }

    static bool IsRequestHead(string headers) {
        return headers.StartsWith("GET ", StringComparison.Ordinal)
            || headers.StartsWith("HEAD ", StringComparison.Ordinal)
            || headers.StartsWith("DELETE ", StringComparison.Ordinal)
            || headers.StartsWith("OPTIONS ", StringComparison.Ordinal);
    }

    static void ReadChunked(NetworkStream stream, List<byte> data, int bodyStart) {
        while (data.Count < BodyLimit) {
            int lineEnd = ReadUntil(stream, data, new byte[] { 13, 10 }, BodyLimit, bodyStart);
            if (lineEnd < bodyStart) throw new IOException("bad chunk");
            string sizeText = Encoding.ASCII.GetString(data.ToArray(), bodyStart, lineEnd - bodyStart);
            int semi = sizeText.IndexOf(';');
            if (semi >= 0) sizeText = sizeText.Substring(0, semi);
            int size;
            if (!Int32.TryParse(sizeText.Trim(), System.Globalization.NumberStyles.HexNumber, null, out size)) {
                throw new IOException("bad chunk size");
            }
            if (size == 0) {
                ReadUntil(stream, data, new byte[] { 13, 10, 13, 10 }, BodyLimit, lineEnd);
                return;
            }
            int dataStart = data.Count;
            ReadExact(stream, data, dataStart + size + 2);
            bodyStart = data.Count;
        }
        throw new IOException("body too large");
    }

    static int ReadUntil(NetworkStream stream, List<byte> data, byte[] marker, int limit, int searchFrom) {
        var buf = new byte[4096];
        while (data.Count <= limit) {
            int found = IndexOf(data, marker, searchFrom);
            if (found >= 0) return found;
            int n = stream.Read(buf, 0, buf.Length);
            if (n <= 0) return -1;
            for (int i = 0; i < n; i++) data.Add(buf[i]);
        }
        throw new IOException("message too large");
    }

    static void ReadExact(NetworkStream stream, List<byte> data, int target) {
        var buf = new byte[8192];
        while (data.Count < target) {
            if (data.Count > BodyLimit) throw new IOException("body too large");
            int want = Math.Min(buf.Length, target - data.Count);
            int n = stream.Read(buf, 0, want);
            if (n <= 0) throw new IOException("closed before body ended");
            for (int i = 0; i < n; i++) data.Add(buf[i]);
        }
    }

    static void ReadUntilClose(NetworkStream stream, List<byte> data) {
        var buf = new byte[8192];
        try {
            while (data.Count <= BodyLimit) {
                int n = stream.Read(buf, 0, buf.Length);
                if (n <= 0) return;
                for (int i = 0; i < n; i++) data.Add(buf[i]);
            }
        } catch (IOException) {
            if (data.Count == 0) throw;
        }
    }

    static byte[] ForceConnectionClose(byte[] message) {
        int headerEnd = IndexOf(message, new byte[] { 13, 10, 13, 10 }, 0);
        if (headerEnd < 0) return message;
        string headers = Encoding.ASCII.GetString(message, 0, headerEnd);
        var kept = new StringBuilder();
        string[] lines = headers.Split(new string[] { "\r\n" }, StringSplitOptions.None);
        foreach (string line in lines) {
            if (line.Length == 0) continue;
            if (line.StartsWith("Connection:", StringComparison.OrdinalIgnoreCase)) continue;
            if (line.StartsWith("Expect:", StringComparison.OrdinalIgnoreCase)) continue;
            kept.Append(line).Append("\r\n");
        }
        kept.Append("Connection: close\r\n\r\n");
        byte[] head = Encoding.ASCII.GetBytes(kept.ToString());
        int bodyLen = message.Length - (headerEnd + 4);
        var result = new byte[head.Length + bodyLen];
        Buffer.BlockCopy(head, 0, result, 0, head.Length);
        Buffer.BlockCopy(message, headerEnd + 4, result, head.Length, bodyLen);
        return result;
    }

    static bool HeaderHas(string headers, string name, string value) {
        string[] lines = headers.Split(new string[] { "\r\n" }, StringSplitOptions.None);
        string prefix = name + ":";
        foreach (string line in lines) {
            if (line.StartsWith(prefix, StringComparison.OrdinalIgnoreCase)
                && line.IndexOf(value, StringComparison.OrdinalIgnoreCase) >= 0) return true;
        }
        return false;
    }

    static int HeaderInt(string headers, string name) {
        string[] lines = headers.Split(new string[] { "\r\n" }, StringSplitOptions.None);
        string prefix = name + ":";
        foreach (string line in lines) {
            if (line.StartsWith(prefix, StringComparison.OrdinalIgnoreCase)) {
                int parsed;
                if (Int32.TryParse(line.Substring(prefix.Length).Trim(), out parsed)) return parsed;
            }
        }
        return -1;
    }

    static int IndexOf(IList<byte> data, byte[] marker, int start) {
        if (start < 0) start = 0;
        int last = data.Count - marker.Length;
        for (int i = start; i <= last; i++) {
            bool match = true;
            for (int j = 0; j < marker.Length; j++) {
                if (data[i + j] != marker[j]) { match = false; break; }
            }
            if (match) return i;
        }
        return -1;
    }

    static int IndexOf(byte[] data, byte[] marker, int start) {
        int last = data.Length - marker.Length;
        for (int i = start; i <= last; i++) {
            bool match = true;
            for (int j = 0; j < marker.Length; j++) {
                if (data[i + j] != marker[j]) { match = false; break; }
            }
            if (match) return i;
        }
        return -1;
    }
}
"@

[SahakarTcpProxy]::Run($ListenPort, $TargetHost, $TargetPort)
