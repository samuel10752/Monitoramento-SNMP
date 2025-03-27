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
import org.snmp4j.TransportMapping;
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

public class ES4172LP extends PrinterModel {

    private Snmp snmp;

    public ES4172LP(String ip, TextField macField, TextField serialField, TextField nameprinterField,
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
        return getSnmpValue("1.3.6.1.4.1.2001.1.2.1.1.140.0", snmp, null); // OID de MAC Address para este modelo
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
            // Configuração do SNMP
            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new OctetString("public"));
            target.setAddress(new UdpAddress(ip + "/161"));
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(org.snmp4j.mp.SnmpConstants.version1);

            // Buscar o nome da impressora via OID
            String printerNameFromOID = getSnmpValue("1.3.6.1.2.1.25.3.2.1.3.1", snmp, target);

            // Buscar o nome da impressora via página web
            String printerNameFromWeb = fetchPrinterNameFromWeb();

            // Validação e escolha do valor correto
            String printerName = validatePrinterName(printerNameFromOID, printerNameFromWeb);
            nameprinterField.setText(printerName);

            macField.setText(getSnmpValue("1.3.6.1.4.1.2001.1.2.1.1.140.0", snmp, target));
            serialField.setText(getSnmpValue("1.3.6.1.2.1.43.5.1.1.17.1", snmp, target));
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

    // Método para buscar o nome da impressora pela página web
    private String fetchPrinterNameFromWeb() {
        try {
            String url = "https://" + ip + "/countsum.htm";
            disableSSLCertificateChecking();

            // Conectar e buscar o título da página
            Document doc = Jsoup.connect(url)
                    .header("Authorization",
                            "Basic " + java.util.Base64.getEncoder().encodeToString("usuario:senha".getBytes()))
                    .get();
            return doc.title(); // Nome da impressora extraído do título
        } catch (IOException e) {
            System.err.println("Erro ao buscar nome pela web: " + e.getMessage());
            return "Erro (Web)";
        }
    }

    // Método para validar e escolher o nome correto
    private String validatePrinterName(String nameFromOID, String nameFromWeb) {
        // Exemplo simples: Priorizar o nome via OID se válido, caso contrário, usar o
        // da web
        if (nameFromOID != null && !nameFromOID.isEmpty() && !nameFromOID.startsWith("Erro")) {
            return nameFromOID;
        } else if (nameFromWeb != null && !nameFromWeb.isEmpty() && !nameFromWeb.startsWith("Erro")) {
            return nameFromWeb;
        }
        return "Nome Indisponível"; // Valor padrão se ambas as fontes falharem
    }

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

            // Inicializar variáveis para armazenar as contagens
            int tray1Count = 0;
            int mpTrayCount = 0;
            int blackCopyCount = 0; // Contagem de cópias P&B usando SNMP
            int blackPrintCount = 0; // Contagem de impressões P&B usando SNMP
            int totalScannedPages = 0; // Páginas digitalizadas

            // Capturar informações de "Tray1" e "MP Tray"
            Element tray1Elem = doc.select("tr:contains(Tray1)").first();
            Element mpTrayElem = doc.select("tr:contains(MP Tray)").first();

            if (tray1Elem != null && tray1Elem.children().size() > 1) {
                tray1Count = Integer.parseInt(tray1Elem.child(1).text().trim());
            }

            if (mpTrayElem != null && mpTrayElem.children().size() > 1) {
                mpTrayCount = Integer.parseInt(mpTrayElem.child(1).text().trim());
            }

            Element totalScannedElem = doc.select("tr:contains(Total Scanned Pages)").first();

            if (totalScannedElem != null && totalScannedElem.children().size() > 1) {
                totalScannedPages = Integer.parseInt(totalScannedElem.child(1).text().trim());
            }

            // Consultar OIDs para contagem de cópias e impressões P&B
            try {
                blackCopyCount = fetchSNMPValue(ip, "1.3.6.1.4.1.2001.1.1.1.1.11.1.10.170.1.17.3"); // Cópias P&B
                blackPrintCount = fetchSNMPValue(ip, "1.3.6.1.4.1.2001.1.1.1.1.11.1.10.170.1.21.1"); // Impressões P&B
            } catch (Exception snmpError) {
                webInfoArea.setText("Erro ao consultar SNMP: " + snmpError.getMessage());
            }

            // Calcular a contagem total das bandejas
            int totalTrayCount = blackCopyCount + blackPrintCount;

            // Exibir informações diretamente na interface
            StringBuilder builder = new StringBuilder();
            builder.append("Bandeja 1: ").append(tray1Count).append("\n");
            builder.append("Bandeja Multiuso: ").append(mpTrayCount).append("\n");
            builder.append("Total das Bandejas: ").append(totalTrayCount).append("\n");
            builder.append("Digitalição geral: ").append(totalScannedPages).append("\n");
            builder.append("Cópias Preto e Branco: ").append(blackCopyCount).append("\n");
            builder.append("Impressões Preto e Branco: ").append(blackPrintCount).append("\n");

            webInfoArea.setText(builder.toString());

        } catch (IOException e) {
            webInfoArea.setText("Erro ao acessar a página web: " + e.getMessage());
        } catch (NumberFormatException e) {
            webInfoArea.setText("Erro ao processar os valores numéricos: " + e.getMessage());
        }
    }

    // Método para consultar um valor SNMP
    private int fetchSNMPValue(String ip, String oid) throws Exception {
        TransportMapping transport = new DefaultUdpTransportMapping();
        transport.listen();

        Snmp snmp = new Snmp(transport);

        // Configuração do alvo SNMP
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new org.snmp4j.smi.OctetString("public")); // Substitua pela sua comunidade SNMP
        target.setAddress(new UdpAddress(ip + "/161")); // Substitua pela porta SNMP padrão, se necessário
        target.setRetries(2);
        target.setTimeout(1000);
        target.setVersion(org.snmp4j.mp.SnmpConstants.version1);

        // Configuração da solicitação SNMP
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oid)));
        pdu.setType(PDU.GET);

        // Enviar solicitação e receber resposta
        PDU response = snmp.send(pdu, target).getResponse();

        if (response != null && response.size() > 0) {
            return Integer.parseInt(response.get(0).getVariable().toString());
        } else {
            throw new Exception("Sem resposta ou dados inválidos para OID: " + oid);
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
