package com.example.printercounters.epson;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.snmp4j.Snmp;
import org.snmp4j.PDU;
import org.snmp4j.CommunityTarget;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.event.ResponseEvent;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;

import java.util.LinkedHashMap;

import com.example.printercounters.controllers.PrinterModel;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class EpsonL3250 extends PrinterModel {

    private Snmp snmp;

    public EpsonL3250(String ip, TextField macField, TextField serialField, TextField nameprinterField,
            TextArea webInfoArea) {
        super(ip, macField, serialField, nameprinterField, webInfoArea);
        try {
            this.snmp = new Snmp(new DefaultUdpTransportMapping());
            this.snmp.listen(); // Start SNMP session
        } catch (IOException e) {
            System.err.println("Erro ao inicializar SNMP: " + e.getMessage());
        }
    }

    @Override
    public String getMacAddress() {
        return macField.getText() != null ? macField.getText() : "MAC Desconhecido";
    }

    @Override
    public String getSerialNumber() {
        return "SN1234567890"; // Simulação
    }

    @Override
    public String getWebCounters() {
        return "Contadores EpsonL3250"; // Simulação
    }

    @Override
    public void fetchPrinterInfo() {
        try {
            Snmp snmp = new Snmp(new org.snmp4j.transport.DefaultUdpTransportMapping());
            snmp.listen();

            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new org.snmp4j.smi.OctetString("public"));
            target.setAddress(new UdpAddress(ip + "/161"));
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(org.snmp4j.mp.SnmpConstants.version1);

            macField.setText(getSnmpValue("1.3.6.1.2.1.2.2.1.6.1", snmp, target)); // Endereço MAC

            serialField.setText(getSnmpValue("1.3.6.1.2.1.43.5.1.1.17.1", snmp, target)); // Número de Série

            nameprinterField.setText(getSnmpValue("1.3.6.1.4.1.1248.1.1.3.1.14.4.1.2.1", snmp, target)); // Nome da
                                                                                                         // Impressora

            snmp.close();
        } catch (Exception e) {
            macField.setText("Erro");
            serialField.setText("Erro");
            nameprinterField.setText("Erro");
            showMessage("Erro ao buscar informações SNMP.", Alert.AlertType.ERROR);
        }
    }

    @Override
    public void fetchOidData(String oid) {
        try {
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);

            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new OctetString("public"));
            target.setAddress(new UdpAddress(ip + "/161"));
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(org.snmp4j.mp.SnmpConstants.version1);

            ResponseEvent response = this.snmp.get(pdu, target); // Reuse the class-level snmp instance
            if (response != null && response.getResponse() != null) {
                VariableBinding vb = response.getResponse().get(0);
                System.out.println("OID " + oid + " retornou: " + vb.getVariable());
            } else {
                System.err.println("Nenhuma resposta para OID " + oid);
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar OID: " + oid + " - " + e.getMessage());
        }
    }

    @Override
    public void fetchWebPageData() {
        try {
            String url = "https://" + ip + "/PRESENTATION/ADVANCED/INFO_MENTINFO/TOP";

            // Verifica se a página está acessível
            if (!isWebPageAccessible(url)) {
                webInfoArea.setText("Erro: A página da impressora não está acessível.");
                return;
            }

            // Obtém os dados da página web
            Map<String, String> webData = getWebPageData(url);

            // Exibe os dados capturados
            if (webData.isEmpty()) {
                webInfoArea.setText("Nenhuma informação encontrada na página web.");
            } else {
                String formattedData = formatWebDataAsText(webData);
                webInfoArea.setText(formattedData); // Apresenta no TextArea
            }
        } catch (Exception e) {
            webInfoArea.setText("Erro ao acessar a página web: " + e.getMessage());
        }
    }

    private SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            } }, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao configurar SSL", e);
        }
    }

    protected String getSnmpValue(String oid, Snmp snmp, CommunityTarget<?> target) {
        try {
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);

            ResponseEvent response = snmp.get(pdu, target);
            if (response != null && response.getResponse() != null) {
                return response.getResponse().get(0).getVariable().toString();
            } else {
                System.err.println("Erro: SNMP response null para OID " + oid);
            }
        } catch (Exception e) {
            System.err.println("Erro ao obter OID " + oid + ": " + e.getMessage());
            e.printStackTrace();
        }
        return "Desconhecido";
    }

    private Map<String, String> getWebPageData(String url) throws IOException {
        disableSSLCertificateChecking();

        Document doc = Jsoup.connect(url)
                .userAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .get();

        // Extrai os dados
        Map<String, String> webData = new LinkedHashMap<>();
        webData.put("Geral:", extractData(doc, "Total Number of Pages"));
        webData.put("Geral P&B:", extractData(doc, "Total Number of B&W Pages"));
        webData.put("Geral Cor Total:", extractData(doc, "Total Number of Color Pages"));
        webData.put("Digitalização P&B:", extractData(doc, "B&W Scan"));
        webData.put("Digitalização Colorida:", extractData(doc, "Color Scan"));

        return webData;
    }

    private String formatWebDataAsText(Map<String, String> webData) {
        StringBuilder outputBuilder = new StringBuilder();

        // Adiciona os dados com formato tabular
        webData.forEach((key, value) -> outputBuilder.append(key).append("\t").append(value).append("\n"));

        return outputBuilder.toString();
    }

    private String extractData(Document doc, String label) {
        // Seleciona o texto associado ao rótulo
        String data = doc.select("dt:contains(" + label + ") + dd").text();
        return data.isEmpty() ? "Dado não encontrado" : data;
    }

    private boolean isWebPageAccessible(String url) {
        try {
            disableSSLCertificateChecking(); // Use the static method here
            HttpsURLConnection connection = (HttpsURLConnection) new java.net.URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            return (responseCode == 200);
        } catch (Exception e) {
            return false;
        }
    }

    private void showMessage(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Erro" : "Informação");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}