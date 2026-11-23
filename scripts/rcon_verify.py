import argparse
import socket
import struct
import sys


def send_packet(sock, req_id, ptype, payload):
    data = payload.encode("utf-8") + b"\x00\x00"
    pkt = struct.pack("<iii", len(data) + 8, req_id, ptype) + data
    sock.sendall(pkt)


def recv_exact(sock, length):
    buf = bytearray()
    while len(buf) < length:
        chunk = sock.recv(length - len(buf))
        if not chunk:
            raise EOFError("Truncated RCON packet")
        buf.extend(chunk)
    return bytes(buf)


def recv_packet(sock):
    length = struct.unpack("<i", recv_exact(sock, 4))[0]
    if length < 10 or length > 4 * 1024 * 1024:
        raise ValueError("Invalid RCON packet length")
    buf = recv_exact(sock, length)
    if buf[-2:] != b"\x00\x00":
        raise ValueError("Invalid RCON packet terminator")
    req_id, ptype = struct.unpack("<ii", buf[:8])
    body = buf[8:-2].decode("utf-8", errors="replace")
    return req_id, ptype, body


def main():
    parser = argparse.ArgumentParser(description="Run Mycelial Mind RCON verification checks.")
    parser.add_argument("host", nargs="?", default="127.0.0.1")
    parser.add_argument("port", nargs="?", type=int, default=25575)
    parser.add_argument("password", nargs="?", default="mycelialtest")
    parser.add_argument("--worldgen-audit", action="store_true", help="Run optional advanced worldgen audit")
    parser.add_argument("--worldgen-radius", type=int, default=8, help="Chunk radius for worldgen audit (1..24)")
    args = parser.parse_args()

    cmds = ["mycelial status", "mycelial verify all"]
    if args.worldgen_audit:
        cmds.append(f"mycelial worldgen audit {args.worldgen_radius} 64")

    with socket.create_connection((args.host, args.port), timeout=60) as sock:
        send_packet(sock, 1, 3, args.password)
        rid, _, _ = recv_packet(sock)
        if rid == -1:
            raise RuntimeError("RCON authentication failed")

        for idx, cmd in enumerate(cmds, start=2):
            send_packet(sock, idx, 2, cmd)
            _, _, resp = recv_packet(sock)
            print(f"[RCON {cmd}] {resp}")
            if cmd == "mycelial verify all" and "ALL 23 automated scenario checks PASSED" not in resp:
                send_packet(sock, 99, 2, "stop")
                raise RuntimeError(f"Verification command failed: {resp}")
            if cmd.startswith("mycelial worldgen audit") and "[Mycelial WorldgenAudit] PASS" not in resp:
                send_packet(sock, 99, 2, "stop")
                raise RuntimeError(f"Worldgen audit failed: {resp}")

        send_packet(sock, 99, 2, "stop")
        try:
            recv_packet(sock)
        except Exception:
            pass


if __name__ == "__main__":
    main()
