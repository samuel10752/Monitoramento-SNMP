package com.example.printercounters.hp;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.example.printercounters.controllers.PrinterModel;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class HP4303 extends PrinterModel {

    public HP4303(String ip, TextField macField, TextField serialField, TextField nameprinterField, TextArea webInfoArea) {
        super(ip, macField, serialField, nameprinterField, webInfoArea);
    }

    @Override
    public String getWebCounters() {
        return "Contadores HP4303"; // Ajuste conforme a necessidade
    }

    @Override
    public String getSerialNumber() {
        // Usa o OID correto para capturar o serial da HP4303
        return getSnmpValue("1.3.6.1.4.1.11.2.3.9.4.2.1.1.3.3.0", ip);
    }

    @Override
    public String getMacAddress() {
        return macField.getText() != null ? macField.getText() : "MAC Desconhecido";
    }

    @Override
    public void fetchPrinterInfo() {
        try {
            Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
            snmp.listen();

            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new OctetString("public"));
            target.setAddress(new UdpAddress(ip + "/161"));
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(org.snmp4j.mp.SnmpConstants.version2c);

            // Busca os valores utilizando os OIDs corretos
            macField.setText(getSnmpValue("1.3.6.1.2.1.2.2.1.6.2", snmp, target));
            serialField.setText(getSnmpValue("1.3.6.1.4.1.11.2.3.9.4.2.1.1.3.3.0", snmp, target));
            nameprinterField.setText(getSnmpValue("1.3.6.1.2.1.25.3.2.1.3.1", snmp, target));

            snmp.close();
        } catch (IOException e) {
            macField.setText("Erro");
            serialField.setText("Erro");
            nameprinterField.setText("Erro");
            showMessage("Erro ao buscar informações SNMP.", Alert.AlertType.ERROR);
        }
    }

    @Override
    public void fetchWebPageData() {
        try {
            String url = "https://" + ip + "/hp/device/InternalPages/Index?id=UsagePage";
            if (!isWebPageAccessible(url)) {
                webInfoArea.setText("Erro: A página da impressora não está acessível.");
                return;
            }
            Map<String, String> webData = getWebPageData(url);
            if (webData.isEmpty()) {
                webInfoArea.setText("Nenhuma informação encontrada na página web.");
            } else {
                StringBuilder builder = new StringBuilder();
                webData.forEach((key, value) -> builder.append(key).append(": ").append(value).append("\n"));
                webInfoArea.setText(builder.toString());
            }
        } catch (IOException e) {
            webInfoArea.setText("Erro ao acessar a página web: " + e.getMessage());
        }
    }

    private SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{ new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao configurar SSL", e);
        }
    }

    private Map<String, String> getWebPageData(String url) throws IOException {
        disableSSLCertificateChecking();
        Document doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
            .ignoreHttpErrors(true)
            .ignoreContentType(true)
            .sslSocketFactory(getSSLSocketFactory())
            .get();
        Map<String, String> webData = new HashMap<>();
        webData.put("Geral", doc.select("[id=\"UsagePage.EquivalentImpressionsTable.Total.Total\"]").text());
        webData.put("Impressão P$B", doc.select("[id=\"UsagePage.ImpressionsByMediaSizeTable.Print.TotalTotal\"]").text());
        webData.put("Copia P&B", doc.select("[id=\"UsagePage.ImpressionsByMediaSizeTable.Copy.TotalTotal\"]").text());
        webData.put("Digitalização Geral", doc.select("[id=\"UsagePage.ScanCountsDestinationTable.Send.Value\"]").text());
        return webData;
    }

    private boolean isWebPageAccessible(String url) {
        try {
            disableSSLCertificateChecking();
            HttpsURLConnection connection = (HttpsURLConnection) new java.net.URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            return (responseCode == 200);
        } catch (Exception e) {
            System.err.println("Erro ao verificar acesso à página: " + e.getMessage());
            return false;
        }
    }

    // Método para realizar um SNMP GET usando uma instância SNMP já configurada
    protected String getSnmpValue(String oid, Snmp snmp, CommunityTarget<?> target) {
        try {
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);
            ResponseEvent response = snmp.get(pdu, target);
            if (response != null && response.getResponse() != null) {
                return response.getResponse().get(0).getVariable().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Desconhecido";
    }

    // Método auxiliar para facilitar SNMP GET passando o IP
    private String getSnmpValue(String oid, String ip) {
        try {
            Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
            snmp.listen();
            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new OctetString("public"));
            target.setAddress(new UdpAddress(ip + "/161"));
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(org.snmp4j.mp.SnmpConstants.version2c);
            String result = getSnmpValue(oid, snmp, target);
            snmp.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Desconhecido";
    }

    private void showMessage(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Erro" : "Informação");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
// Implemente a lógica de desabilitar a verificação de certificados SSL, se necessário.

}
