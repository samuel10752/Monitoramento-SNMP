package com.example.printercounters.server;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class PrinterServer {

    private final Snmp snmp;
    private final String printerAddress;

    public PrinterServer() throws Exception {
        this.printerAddress = "udp:192.168.1.12/161"; // Endereço da impressora
        TransportMapping<?> transport = new DefaultUdpTransportMapping();
        this.snmp = new Snmp(transport);
        transport.listen();
    }

    public static void main(String[] args) throws Exception {
        PrinterServer server = new PrinterServer();
        server.startHttpServer();
    }

    public void startHttpServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/counters", new CountersHandler());
        server.setExecutor(null); // Usa o executor padrão
        server.start();
        System.out.println("Servidor HTTP iniciado na porta 8080.");
    }

    class CountersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> counters = getCounters();
            String response = mapToJson(counters); // Converte o mapa para JSON
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private Map<String, String> getCounters() {
        Map<String, String> counters = new HashMap<>();

        // OIDs para a Epson L3250 (verifique o manual para OIDs exatos)
        Map<String, String> oids = new HashMap<>();
        oids.put("TotalPages", "1.3.6.1.2.1.43.10.2.1.4.1.1"); // Total de páginas impressas
        oids.put("BlackPages", "1.3.6.1.2.1.43.10.2.1.4.1.1.1"); // Páginas em preto e branco
        oids.put("ColorPages", "1.3.6.1.2.1.43.10.2.1.4.1.1.2"); // Páginas coloridas
        oids.put("BlackInkLevel", "1.3.6.1.2.1.43.11.1.1.9.1.1"); // Nível de tinta preta
        oids.put("ColorInkLevel", "1.3.6.1.2.1.43.11.1.1.9.1.2"); // Nível de tinta colorida

        for (Map.Entry<String, String> entry : oids.entrySet()) {
            try {
                String value = getAsString(new OID(entry.getValue()));
                counters.put(entry.getKey(), value);
            } catch (Exception e) {
                counters.put(entry.getKey(), "Erro: " + e.getMessage());
            }
        }

        return counters;
    }

    private String getAsString(OID oid) throws Exception {
        ResponseEvent event = get(new OID[]{oid});
        if (event != null && event.getResponse() != null) {
            return event.getResponse().get(0).getVariable().toString();
        }
        throw new RuntimeException("GET timed out or no response received");
    }

    private ResponseEvent get(OID[] oids) throws Exception {
        PDU pdu = new PDU();
        for (OID oid : oids) {
            pdu.add(new VariableBinding(oid));
        }
        pdu.setType(PDU.GET);
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));
        Address targetAddress = GenericAddress.parse(printerAddress);
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(5000);
        target.setVersion(SnmpConstants.version2c);
        return snmp.send(pdu, target);
    }

    private String mapToJson(Map<String, String> map) {
        StringBuilder json = new StringBuilder("{");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\",");
        }
        if (json.length() > 1) {
            json.deleteCharAt(json.length() - 1); // Remove a última vírgula
        }
        json.append("}");
        return json.toString();
    }
}