package com.example.printercounters.epson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Snmp;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import java.util.LinkedHashMap;

import com.example.printercounters.controllers.PrinterModel;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class EpsonL3250 extends PrinterModel {

    private static final Logger LOGGER = Logger.getLogger(EpsonL3250.class.getName());

    public EpsonL3250(String ip, TextField macField, TextField serialField, TextField nameprinterField,
            TextArea webInfoArea) {
        super(ip, macField, serialField, nameprinterField, webInfoArea);
        LOGGER.info("Instância EpsonL3250 criada com IP: " + ip);
    }

    @Override
    public String getMacAddress() {
        LOGGER.info("getMacAddress chamado");
        return macField.getText() != null ? macField.getText() : "MAC Desconhecido";
    }

    @Override
    public String getSerialNumber() {
        LOGGER.info("getSerialNumber chamado");
        return "SN1234567890"; // Simulação
    }

    @Override
    public String getWebCounters() {
        LOGGER.info("getWebCounters chamado");
        return "Contadores EpsonL3250"; // Simulação
    }

    @Override
    public void fetchPrinterInfo() {
        try {
            LOGGER.info("Iniciando fetchPrinterInfo...");
            Snmp snmp = new Snmp(new org.snmp4j.transport.DefaultUdpTransportMapping());
            snmp.listen();

            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new org.snmp4j.smi.OctetString("public"));
            target.setAddress(new UdpAddress(ip + "/161"));
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(org.snmp4j.mp.SnmpConstants.version2c);

            String mac = getSnmpValue("1.3.6.1.2.1.2.2.1.6.1", snmp, target); // Endereço MAC
            LOGGER.info("Endereço MAC capturado: " + mac);
            macField.setText(mac);

            String serial = getSnmpValue("1.3.6.1.2.1.43.5.1.1.17.1", snmp, target); // Número de Série
            LOGGER.info("Número de Série capturado: " + serial);
            serialField.setText(serial);

            String name = getSnmpValue("1.3.6.1.2.1.43.5.1.1.16.1", snmp, target); // Nome da Impressora
            LOGGER.info("Nome da impressora capturado: " + name);
            nameprinterField.setText(name);

            snmp.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar informações SNMP", e);
            macField.setText("Erro");
            serialField.setText("Erro");
            nameprinterField.setText("Erro");
            showMessage("Erro ao buscar informações SNMP.", Alert.AlertType.ERROR);
        }
    }

    @Override
    public void fetchWebPageData() {
        try {
            LOGGER.info("Iniciando fetchWebPageData...");
            String url = "https://" + ip + "/PRESENTATION/ADVANCED/INFO_MENTINFO/TOP";

            // Verifica se a página está acessível
            if (!isWebPageAccessible(url)) {
                LOGGER.warning("A página da impressora não está acessível: " + url);
                webInfoArea.setText("Erro: A página da impressora não está acessível.");
                return;
            }

            // Obtém os dados da página web
            Map<String, String> webData = getWebPageData(url);

            // Exibe os dados capturados
            if (webData.isEmpty()) {
                LOGGER.warning("Nenhuma informação encontrada na página web.");
                webInfoArea.setText("Nenhuma informação encontrada na página web.");
            } else {
                LOGGER.info("Dados do painel web capturados com sucesso.");
                String formattedData = formatWebDataAsText(webData);
                webInfoArea.setText(formattedData); // Apresenta no TextArea
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao acessar o painel web de contadores", e);
            webInfoArea.setText("Erro ao acessar a página web: " + e.getMessage());
        }
    }

    private Map<String, String> getWebPageData(String url) throws IOException {
        PrinterModel.disableSSLCertificateChecking();

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(5000)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .get();

        // Loga o conteúdo da página para depuração
        LOGGER.info("Conteúdo da página carregado: " + doc.outerHtml());

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
            PrinterModel.disableSSLCertificateChecking(); // Use the static method here
            HttpsURLConnection connection = (HttpsURLConnection) new java.net.URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            return (responseCode == 200);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao verificar acessibilidade da página", e);
            return false;
        }
    }

    private void showMessage(String message, Alert.AlertType type) {
        LOGGER.info("Exibindo mensagem: " + message);
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Erro" : "Informação");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}