package com.example.printercounters.hp;

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

public class HP4303 extends PrinterModel {
    private Snmp snmp;

    public HP4303(String ip, TextField macField, TextField serialField, TextField nameprinterField, TextArea webInfoArea) {
        super(ip, macField, serialField, nameprinterField, webInfoArea);
        try {
            this.snmp = new Snmp(new DefaultUdpTransportMapping());
            this.snmp.listen();
        } catch (IOException e) {
            System.out.println("Erro ao inicializar SNMP: " + e.getMessage());
        }
    }

    @Override
    public String getMacAddress() {
        return getSnmpValue("1.3.6.1.2.1.2.2.1.6.2", snmp, null); // OID de MAC Address para este modelo
    }

    @Override
    public String getSerialNumber() {
        return getSnmpValue("1.3.6.1.4.1.11.2.3.9.1.1.7.0", snmp, null); // OID do número de série
    }

    @Override
    public String getWebCounters() {
        return getSnmpValue("1.3.6.1.2.1.25.3.2.1.3.1", snmp, null); // OID do contador de páginas
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

            macField.setText(getSnmpValue("1.3.6.1.2.1.2.2.1.6.2", snmp, target));
            serialField.setText(getSnmpValue("1.3.6.1.4.1.11.2.3.9.1.1.7.0", snmp, target));
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
            String url = "https://" + ip + "/hp/device/InternalPages/Index?id=UsagePage";

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

        // Extrai informações da página utilizando o id especificado
        webData.put("Geral",
        doc.select("#UsagePage\\.EquivalentImpressionsTable\\.Total\\.Total").text());
        webData.put("Impressão P$B",
        doc.select("#UsagePage\\.ImpressionsByMediaSizeTable\\.Print\\.TotalTotal").text());
        webData.put("Copia P&B",
        doc.select("#UsagePage\\.ImpressionsByMediaSizeTable\\.Copy\\.TotalTotal").text());
        webData.put("Digitalização Geral",
        doc.select("#UsagePage\\.ScanCountsDestinationTable\\.Send\\.Value").text());

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
