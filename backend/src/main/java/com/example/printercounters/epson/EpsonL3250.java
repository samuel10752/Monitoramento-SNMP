package com.example.printercounters.epson;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
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

public class EpsonL3250 extends PrinterModel {

    public EpsonL3250(String ip, TextField macField, TextField serialField, TextField nameprinterField, TextArea webInfoArea) {
        super(ip, macField, serialField, nameprinterField, webInfoArea);
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

            macField.setText(getSnmpValue("1.3.6.1.2.1.2.2.1.6.1", snmp, target));
            serialField.setText(getSnmpValue("1.3.6.1.2.1.43.5.1.1.17.1", snmp, target));
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
            // URL da página web
            String url = "https://" + ip + "/PRESENTATION/ADVANCED/INFO_MENTINFO/TOP";

            // Obtém os dados da página web
            Map<String, String> webData = getWebPageData(url);

            // Exibe as informações na área de texto
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

    private Map<String, String> getWebPageData(String url) throws IOException {
        disableSSLCertificateChecking();

        // Conecta à página web e extrai o conteúdo
        Document doc = Jsoup.connect(url).get();
        Map<String, String> webData = new HashMap<>();

        // Extrai informações da página
        webData.put("Número total de páginas",
                doc.select("dt:contains(Total Number of Pages) + dd .preserve-white-space").text());
        webData.put("Número de páginas P&B",
                doc.select("dt:contains(Total Number of B&W Pages) + dd .preserve-white-space").text());
        webData.put("Número de páginas coloridas",
                doc.select("dt:contains(Total Number of Color Pages) + dd .preserve-white-space").text());
        webData.put("Scanner P&B", doc.select("dt:contains(B&W Scan) + dd .preserve-white-space").text());
        webData.put("Scanner Color", doc.select("dt:contains(Color Scan) + dd .preserve-white-space").text());

        return webData;
    }

    protected void disableSSLCertificateChecking() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
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
            }
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
}
