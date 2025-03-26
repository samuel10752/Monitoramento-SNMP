package com.example.printercounters.oki;

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

public class ES5112 extends PrinterModel {

    private Snmp snmp;

    public ES5112(String ip, TextField macField, TextField serialField, TextField nameprinterField,
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
        return "Contadores HP4303"; // Ajuste conforme a necessidade // OID do contador de páginas
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

            macField.setText(getSnmpValue("1.3.6.1.4.1.2001.1.2.1.1.140.0", snmp, target));
            serialField.setText(getSnmpValue("1.3.6.1.2.1.43.5.1.1.17.1", snmp, target));
            nameprinterField.setText(getSnmpValue("1.3.6.1.2.1.1.5.0", snmp, target)); // Nome da impressora

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

    @Override
    public void fetchWebPageData() {
        try {
            String url = "https://" + ip + "/countsum.htm";
            disableSSLCertificateChecking();

            // Adiciona autenticação no cabeçalho
            Document doc = Jsoup.connect(url)
                    .header("Authorization",
                            "Basic " + java.util.Base64.getEncoder().encodeToString("usuario:senha".getBytes()))
                    .get();

            Map<String, String> webData = new HashMap<>();
            int tray1Count = 0;
            int mpTrayCount = 0;

            // Capturar o nome da impressora pelo título da página
            String printerName = doc.title();
            nameprinterField.setText(printerName);

            // Buscar contadores de impressão
            Element tray1 = doc.getElementById("TRAY_1");
            if (tray1 != null && tray1.nextElementSibling() != null) {
                tray1Count = Integer.parseInt(tray1.nextElementSibling().text().trim());
            }
            webData.put("Bandeja 1", String.valueOf(tray1Count));

            Element mpTray = doc.getElementById("MP_TRAY");
            if (mpTray != null && mpTray.nextElementSibling() != null) {
                mpTrayCount = Integer.parseInt(mpTray.nextElementSibling().text().trim());
            }
            webData.put("Bandeja Multiuso", String.valueOf(mpTrayCount));

            // Calcular o total de contagem das bandejas
            int totalTrayCount = tray1Count + mpTrayCount;
            webData.put("Contagem Total: Tray Count", String.valueOf(totalTrayCount));

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
        } catch (NumberFormatException e) {
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
