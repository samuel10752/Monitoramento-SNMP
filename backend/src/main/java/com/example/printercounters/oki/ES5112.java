package com.example.printercounters.oki;

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
import org.jsoup.nodes.Element;
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

public class ES5112 extends PrinterModel {

    private Snmp snmp;

    public ES5112(String ip, TextField macField, TextField serialField, TextField nameprinterField, TextArea webInfoArea) {
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
            return getSnmpValue("1.3.6.1.4.1.2001.1.2.1.1.140.0", snmp, null); // OID de MAC Address para este modelo
        }
    
        @Override
        public String getSerialNumber() {
            return getSnmpValue("1.3.6.1.2.1.43.5.1.1.17.1", snmp, null); // OID do número de série
        }
    
        @Override
        public String getWebCounters() {
            return getSnmpValue("1.3.6.1.2.1.43.5.1.1.16.1", snmp, null); // OID do contador de páginas
        }
        

    @Override
    public void fetchPrinterInfo() {
        try {
            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new OctetString("public"));
            target.setAddress(new UdpAddress(ip + "/161"));
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(org.snmp4j.mp.SnmpConstants.version2c);

            macField.setText(getSnmpValue("1.3.6.1.4.1.2001.1.2.1.1.140.0", snmp, target));
            serialField.setText(getSnmpValue("1.3.6.1.2.1.43.5.1.1.17.1", snmp, target));
            nameprinterField.setText(getSnmpValue("1.3.6.1.2.1.43.5.1.1.16.1", snmp, target)); // Nome da impressora

        } catch (Exception e) {
            macField.setText("Erro");
            serialField.setText("Erro");
            nameprinterField.setText("Erro");
            showMessage("Erro ao buscar informações SNMP: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @Override
    public void fetchWebPageData() {
        try {
            String url = "https://" + ip + "/count.htm";
            disableSSLCertificateChecking();
    
            Document doc = Jsoup.connect(url).get();
            Map<String, String> webData = new HashMap<>();
    
            // Capturar o nome da impressora pelo título da página
            String printerName = doc.title();
            nameprinterField.setText(printerName);
    
            // Buscar contadores de impressão
            Element geral = doc.getElementById("PRINT_COUNT");
            webData.put("Contagem Total", geral != null ? geral.text() : "Não encontrado");
    
            Element bandeja1 = doc.getElementById("TRAY_1");
            webData.put("Bandeja 1", bandeja1 != null ? bandeja1.text() : "Não encontrado");
    
            Element bandejaMultiuso = doc.getElementById("MP_TRAY");
            webData.put("Bandeja Multiuso", bandejaMultiuso != null ? bandejaMultiuso.text() : "Não encontrado");
    
            // Exibir dados na interface
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

        Document doc = Jsoup.connect(url).get();
        Map<String, String> webData = new HashMap<>();

        // Buscar contadores de impressão
        Element geral = doc.getElementById("PRINT_COUNT");
        webData.put("Contagem Total", geral != null ? geral.text() : "Não encontrado");

        Element bandeja1 = doc.getElementById("TRAY_1");
        webData.put("Bandeja 1", bandeja1 != null ? bandeja1.text() : "Não encontrado");

        Element bandejaMultiuso = doc.getElementById("MP_TRAY");
        webData.put("Bandeja Multiuso", bandejaMultiuso != null ? bandejaMultiuso.text() : "Não encontrado");

        return webData;
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
        } catch (IOException e) {
            System.err.println("Erro de IO ao buscar OID " + oid + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro ao buscar OID " + oid + ": " + e.getMessage());
        }
        return "Erro na consulta";
    }

    private void showMessage(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Erro" : "Informação");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
