import socket
import time

def upgrade(pad):
    s = socket.create_connection(("127.0.0.1", 8189))
    s.sendall((
        "GET /echo HTTP/1.1\r\n"
        "Host: localhost\r\n"
        "Upgrade: websocket\r\n"
        "Connection: Upgrade\r\n"
        f"X-Pad: {'a' * pad}\r\n"
        "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n"
        "Sec-WebSocket-Version: 13\r\n"
        "\r\n"
    ).encode())
    print(pad, s.recv(4096)[:120])
    s.close()

for n in (60000, 80000):
    upgrade(n)
    time.sleep(2)