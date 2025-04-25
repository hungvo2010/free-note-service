package com.freenote.app.server.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EchoServer {
    public static void main(String[] args) throws IOException {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : 8189;
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try (var s = new ServerSocket(port)) {
            while (true) {
                var incommingSocket = s.accept();
                System.out.println(incommingSocket.getRemoteSocketAddress());
                executorService.submit(() -> {
                    try {
                        serve(incommingSocket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private static void serve(Socket incommingSocket) throws IOException {
        try (var in = new Scanner(incommingSocket.getInputStream()); var out = new PrintWriter(incommingSocket.getOutputStream(), true);) {
            out.println(incommingSocket.getInetAddress().getHostAddress());
            boolean done = false;
            while (!done && in.hasNextLine()) {
                String line = in.nextLine();
                out.println(line);
                if (line.strip().equals("BYE")) {
                    done = true;
                }
            }
        }
    }
}
