package com.cloudwithvarjosh;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.OutputStream;

public class App {

    private static final String API_KEY = "CVWJ_DEMO_SECRET";

    public static void main(String[] args) throws Exception {

        System.out.println("Starting CVWJ DevSecOps Demo...");

        String username = "guest"; // safe fallback for Docker/K8s (no stdin available)

        if (username.equals("admin")) {
            System.out.println("Welcome, admin!");
        } else {
            System.out.println("Hello, " + username);
        }

        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> {
            String body = brandHtml();
            byte[] out = body.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, out.length);
            OutputStream os = exchange.getResponseBody();
            os.write(out);
            os.close();
        });

        System.out.println("CVWJ app running on port " + port);
        server.start();
    }

    public static String brandHtml() {
        return "<h1>Hello bro</h1><p>Simple DevSecOps Demo App</p>";
    }
}