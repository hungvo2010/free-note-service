import socket
import base64
import hashlib
import os
import struct
import selectors

HOST = "localhost"
PORT = 8189
PATH = "/update"

sel = selectors.DefaultSelector()

def create_handshake(host, port, path):
    key = base64.b64encode(os.urandom(16)).decode()
    headers = (
        f"GET {path} HTTP/1.1\r\n"
        f"Host: {host}:{port}\r\n"
        "Upgrade: websocket\r\n"
        "Connection: Upgrade\r\n"
        f"Sec-WebSocket-Key: {key}\r\n"
        "Sec-WebSocket-Version: 13\r\n"
        "\r\n"
    )
    return headers, key

def build_frame(fin, opcode, payload):
    b1 = (fin << 7) | opcode
    mask_bit = 0x80
    length = len(payload)
    if length < 126:
        header = struct.pack("!BB", b1, mask_bit | length)
    elif length < (1 << 16):
        header = struct.pack("!BBH", b1, mask_bit | 126, length)
    else:
        header = struct.pack("!BBQ", b1, mask_bit | 127, length)

    mask_key = os.urandom(4)
    masked_payload = bytes(b ^ mask_key[i % 4] for i, b in enumerate(payload))
    return header + mask_key + masked_payload

def read_frame(data: bytes):
    """Very minimal WebSocket frame parser (no fragmentation support)."""
    if len(data) < 2:
        return None, data

    b1, b2 = data[0], data[1]
    fin = (b1 >> 7) & 1
    opcode = b1 & 0x0F
    masked = (b2 >> 7) & 1
    length = b2 & 0x7F

    offset = 2
    if length == 126:
        if len(data) < 4: return None, data
        length = struct.unpack("!H", data[2:4])[0]
        offset = 4
    elif length == 127:
        if len(data) < 10: return None, data
        length = struct.unpack("!Q", data[2:10])[0]
        offset = 10

    mask = b""
    if masked:
        if len(data) < offset + 4: return None, data
        mask = data[offset:offset+4]
        offset += 4

    if len(data) < offset + length:
        return None, data  # not enough data yet

    payload = data[offset:offset+length]
    if masked:
        payload = bytes(b ^ mask[i % 4] for i, b in enumerate(payload))

    rest = data[offset+length:]
    return payload, rest

def handle_read(sock, mask):
    global buffer
    try:
        chunk = sock.recv(4096)
        if not chunk:
            print("Connection closed by server")
            sel.unregister(sock)
            sock.close()
            return
        buffer += chunk

        while True:
            payload, buffer_new = read_frame(buffer)
            if payload is None:
                break
            buffer = buffer_new
            print("Received message:", payload.decode("utf-8", errors="replace"))
    except Exception as e:
        print("Read error:", e)

def main():
    global buffer
    buffer = b""

    # Connect socket
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((HOST, PORT))

    # WebSocket handshake
    headers, key = create_handshake(HOST, PORT, PATH)
    s.send(headers.encode())
    resp = s.recv(4096).decode()
    if "101 Switching Protocols" not in resp:
        print("Handshake failed:")
        print(resp)
        return
    print("Handshake successful")

    # Send fragmented frames
    frame1 = build_frame(fin=0, opcode=0x2, payload=b"Hello, ")
    frame2 = build_frame(fin=1, opcode=0x0, payload=b"world!")
    s.send(frame1)
    s.send(frame2)
    print("Sent fragmented message")

    # Register with selector
    s.setblocking(False)
    sel.register(s, selectors.EVENT_READ, handle_read)

    try:
        while True:
            events = sel.select(timeout=15)
            if not events:
                print("No events, timeout")
                break
            for key, mask in events:
                callback = key.data
                callback(key.fileobj, mask)
    finally:
        sel.unregister(s)
        s.close()

if __name__ == "__main__":
    main()
