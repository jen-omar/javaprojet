package com.marketplace.services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class PaymentServer {

    private HttpServer server;
    private final Runnable onSuccess;
    private final Runnable onCancel;

    private int port;

    public PaymentServer(Runnable onSuccess, Runnable onCancel) {
        this.onSuccess = onSuccess;
        this.onCancel = onCancel;
    }

    public int getPort() {
        return port;
    }

    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        
        server.createContext("/success", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "<html><body style='background-color:#1a1a1a;color:#c0c0c0;font-family:sans-serif;text-align:center;padding:50px;'>" +
                                  "<h2>Paiement Reussi !</h2><p>Vous pouvez fermer cet onglet et retourner a l'application.</p></body></html>";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                
                // Notify
                if(onSuccess != null) {
                    onSuccess.run();
                }
                stopServerAsync();
            }
        });

        server.createContext("/cancel", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "<html><body style='background-color:#1a1a1a;color:#ff6b6b;font-family:sans-serif;text-align:center;padding:50px;'>" +
                                  "<h2>Paiement Annule.</h2><p>Vous pouvez fermer cet onglet.</p></body></html>";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                
                if(onCancel != null) {
                    onCancel.run();
                }
                stopServerAsync();
            }
        });

        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if(server != null) {
            server.stop(0);
            server = null;
        }
    }
    
    private void stopServerAsync() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                stop();
            } catch (Exception ignored) {}
        }).start();
    }
}
