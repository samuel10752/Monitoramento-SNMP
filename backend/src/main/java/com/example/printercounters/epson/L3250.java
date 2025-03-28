package com.example.printercounters.epson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

public class L3250 extends PrinterModel {

    private Snmp snmp;

    public L3250(String ip, TextField macField, TextField serialField, TextField nameprinterField,
            TextArea webInfoArea) {
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
        return getSnmpValue("1.3.6.1.2.1.2.2.1.6.1", snmp, null); // OID de MAC Address para este modelo
    }

    @Override
    public String getSerialNumber() {
        return getSnmpValue("1.3.6.1.2.1.43.5.1.1.17.1", snmp, null); // OID do número de série
    }

    @Override
    public String getWebCounters() {
        return "Contadores L3250"; // Ajuste conforme a necessidade // OID do contador de páginas
    }

    @Override
    public void fetchPrinterInfo() {
        try {
            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new OctetString("public"));
            target.setAddress(new UdpAddress(ip + "/161"));
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(org.snmp4j.mp.SnmpConstants.version1);

            macField.setText(getSnmpValue("1.3.6.1.2.1.2.2.1.6.1", snmp, target));
            serialField.setText(getSnmpValue("1.3.6.1.2.1.43.5.1.1.17.1", snmp, target));
            nameprinterField.setText(getSnmpValue("1.3.6.1.2.1.25.3.2.1.3.1 ", snmp, target)); // Nome da impressora

        } catch (Exception e) {
            macField.setText("Erro");
            serialField.setText("Erro");
            nameprinterField.setText("Erro");
            showMessage("Erro ao buscar informações SNMP: " + e.getMessage(), Alert.AlertType.ERROR);
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

    public void fetchWebPageData() {
        try {
            String url = "https://" + ip + "/PRESENTATION/ADVANCED/INFO_MENTINFO/TOP";
            disableSSLCertificateChecking();

            System.out.println("Conectando à URL: " + url);

            Document doc = Jsoup.connect(url)
                    .header("Authorization",
                            "Basic " + java.util.Base64.getEncoder().encodeToString("usuario:senha".getBytes()))
                    .followRedirects(true)
                    .get();

            Map<String, String> webData = new HashMap<>();

            // Buscar o total de páginas
            Element totalPages = doc.selectFirst("dt.key:contains(Total Number of Pages)");
            if (totalPages != null && totalPages.nextElementSibling() != null) {
                String totalPageCount = totalPages.nextElementSibling().selectFirst("div.preserve-white-space").text()
                        .trim();
                webData.put("Geral", totalPageCount);
            }

            // Buscar o total de páginas Preto e Branco
            Element bwPages = doc.selectFirst("dt.key:contains(Total Number of B&W Pages)");
            if (bwPages != null && bwPages.nextElementSibling() != null) {
                String bwPageCount = bwPages.nextElementSibling().selectFirst("div.preserve-white-space").text().trim();
                webData.put("Impressões P&B", bwPageCount);
            }

            // Buscar o total de páginas Coloridas
            Element colorPages = doc.selectFirst("dt.key:contains(Total Number of Color Pages)");
            if (colorPages != null && colorPages.nextElementSibling() != null) {
                String colorPageCount = colorPages.nextElementSibling().selectFirst("div.preserve-white-space").text()
                        .trim();
                webData.put("Impressõess Coloridas", colorPageCount);
            }

            // Buscar o total de páginas Digitalizações Preto e Branco
            Element scanBW = doc.selectFirst("dt.key:contains(B&W Scan)");
            if (scanBW != null && scanBW.nextElementSibling() != null) {
                String colorPageCount = scanBW.nextElementSibling().selectFirst("div.preserve-white-space").text()
                        .trim();
                webData.put("Digitalizações B&W", colorPageCount);
            }

            // Buscar o total de páginas Digitalizações Color
            Element scancolor = doc.selectFirst("dt.key:contains(Color Scan)");
            if (scancolor != null && scancolor.nextElementSibling() != null) {
                String colorPageCount = scancolor.nextElementSibling().selectFirst("div.preserve-white-space").text()
                        .trim();
                webData.put("Digitalizações Colorido", colorPageCount);
            }

            // Exibir os dados coletados na interface
            if (webData.isEmpty()) {
                webInfoArea.setText("Nenhuma informação encontrada na página web.");
            } else {
                StringBuilder builder = new StringBuilder();
                webData.forEach((key, value) -> builder.append(key).append(": ").append(value).append("\n"));
                webInfoArea.setText(builder.toString());
            }
        } catch (IOException e) {
            System.out.println("Erro ao acessar a página web: " + e.getMessage());
            webInfoArea.setText("Erro ao acessar a página web: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Erro ao processar os valores numéricos: " + e.getMessage());
            webInfoArea.setText("Erro ao processar os valores numéricos: " + e.getMessage());
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
