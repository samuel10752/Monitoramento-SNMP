package com.example.printercounters.hp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
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

public class E52645Flow extends PrinterModel {

    private Snmp snmp;

    public E52645Flow(String ip, TextField macField, TextField serialField, TextField nameprinterField,
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
    public String getWebCounters() {
        return "Contadores Epson"; // Simulação
    }

    @Override
    public String getSerialNumber() {
        return "SN123456"; // Simulação
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
            target.setVersion(org.snmp4j.mp.SnmpConstants.version1);

            macField.setText(getSnmpValue("1.3.6.1.2.1.2.2.1.6.2", snmp, target));
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
    public void fetchOidData(String oid) {
        try {
            // Fetch data using the given OID
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);

            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new OctetString("public"));
            target.setAddress(new UdpAddress(ip + "/161"));
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(org.snmp4j.mp.SnmpConstants.version1);

            ResponseEvent response = snmp.get(pdu, target);
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

    // getIp() is already provided by the PrinterModel superclass

    @Override
    public void fetchWebPageData() {
        try {
            // URL da página web
            String url = "https://" + ip + "/hp/device/InternalPages/Index?id=UsagePage";

            // Testa se a página está acessível antes de tentar buscar os dados
            if (!isWebPageAccessible(url)) {
                webInfoArea.setText("Erro: A página da impressora não está acessível.");
                return;
            }

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

    private Map<String, String> getWebPageData(String url) throws IOException {
        disableSSLCertificateChecking();

        Document doc = Jsoup.connect(url)
                .userAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .sslSocketFactory(getSSLSocketFactory())
                .get();

        Map<String, String> webData = new HashMap<>();

        webData.put("Geral:",
                doc.select("[id=\"UsagePage.EquivalentImpressionsTable.Total.Total\"]").text());
        webData.put("Impressão P$B:",
                doc.select("[id=\"UsagePage.ImpressionsByMediaSizeTable.Print.TotalTotal\"]").text());
        webData.put("Copia P&B:",
                doc.select("[id=\"UsagePage.ImpressionsByMediaSizeTable.Copy.TotalTotal\"]").text());
        webData.put("Digitalização Geral:",
                doc.select("[id=\"UsagePage.ScanCountsDestinationTable.Send.Value\"]").text());

        return webData;
    }

    private boolean isWebPageAccessible(String url) {
        try {
            disableSSLCertificateChecking();
            HttpsURLConnection connection = (HttpsURLConnection) new java.net.URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // Tempo limite de 5 segundos
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

    private void showMessage(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Erro" : "Informação");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
