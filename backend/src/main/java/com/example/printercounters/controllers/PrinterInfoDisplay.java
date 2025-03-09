package com.example.printercounters.controllers;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class PrinterInfoDisplay extends Application {

    private TextField ipField; // Campo de entrada do IP
    private TextField macField; // Exibe o endereço MAC
    private TextField serialField; // Exibe o endereço MAC
    private TextField nameprinterField; // Exibe o endereço MAC
    private Label serialLabel; // Exibe o número de série
    private Label printerNameLabel; // Exibe o nome da impressora
    private TextArea webInfoArea; // Exibe informações da página web

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Informações da Impressora");

        // Layout principal
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(15));

        // Campo de entrada do IP e botão de buscar informações
        HBox inputBox = new HBox(10);
        Label ipLabel = new Label("IP da Impressora:");
        ipField = new TextField();
        ipField.setPromptText("Exemplo: 192.168.1.1");
        Button fetchButton = new Button("Buscar Informações");
        inputBox.getChildren().addAll(ipLabel, ipField, fetchButton);

        // Campo para exibir o Nome da Impressora
        HBox nameprinterBox = new HBox(10);
        Label nameprinterLabel = new Label("Nome da impressora:");
        nameprinterField = new TextField();
        nameprinterField.setEditable(false); // Apenas exibição
        nameprinterField.setPromptText("aparecerá aqui...");
        nameprinterBox.getChildren().addAll(nameprinterLabel, nameprinterField);

        // Campo para exibir o MAC Address
        HBox macBox = new HBox(10);
        Label macLabel = new Label("MAC:");
        macField = new TextField();
        macField.setEditable(false); // Apenas exibição
        macField.setPromptText("aparecerá aqui...");
        macBox.getChildren().addAll(macLabel, macField);

        // Campo para exibir o Numero da Serial
        HBox serialBox = new HBox(10);
        Label serialLabel = new Label("Serial da impressora:");
        serialField = new TextField();
        serialField.setEditable(false); // Apenas exibição
        serialField.setPromptText("aparecerá aqui...");
        serialBox.getChildren().addAll(serialLabel, serialField);

        // Caixa para informações adicionais
        GridPane printerInfoGrid = new GridPane();
        printerInfoGrid.setHgap(10);
        printerInfoGrid.setVgap(10);

        // Informações da página web
        Label webInfoLabel = new Label("Contadores:");
        webInfoArea = new TextArea();
        webInfoArea.setEditable(false);
        webInfoArea.setWrapText(true);

        // Adiciona elementos ao layout principal
        mainLayout.getChildren().addAll(inputBox, macBox, serialBox, nameprinterBox,
                printerInfoGrid,
                webInfoLabel, webInfoArea);

        // Configuração da cena
        Scene scene = new Scene(mainLayout, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Ação do botão para buscar informações
        fetchButton.setOnAction(event -> {
            String ip = ipField.getText();
            if (ip.isEmpty()) {
                showMessage("Erro: Por favor, insira um IP válido.", Alert.AlertType.ERROR);
            } else {
                // Limpa os campos antes de buscar informações
                macField.setText(""); // Limpa o campo do MAC Address
                serialField.setText(""); // Limpa o campo do Numero de Serial
                nameprinterField.setText(""); // Limpa o campo do Noma da impressora
                webInfoArea.clear();

                fetchPrinterInfo(ip); // Busca informações SNMP
                fetchWebPageData(ip); // Busca informações da página web
            }
        });
    }

    private void fetchPrinterInfo(String ip) {
        try {
            // Configuração SNMP
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            Address targetAddress = GenericAddress.parse("udp:" + ip + "/161");
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString("public"));
            target.setAddress(targetAddress);
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(SnmpConstants.version2c);

            // Obtém informações SNMP
            String macAddress = getSnmpValue("1.3.6.1.2.1.2.2.1.6.1", snmp, target);
            macField.setText(macAddress); // Atualiza o campo do MAC Address
            String serialAddress = getSnmpValue("1.3.6.1.2.1.43.5.1.1.17.1", snmp, target);
            serialField.setText(serialAddress); // Atualiza o campo do Numero de Serial
            String nameprinterAddress = getSnmpValue("1.3.6.1.2.1.25.3.2.1.3.1", snmp, target);
            nameprinterField.setText(nameprinterAddress); // Atualiza o campo do Nome da Impressroa

            snmp.close();
        } catch (Exception e) {
            macField.setText("Erro");
            serialLabel.setText("Número de Série: Erro");
            printerNameLabel.setText("Nome da Impressora: Erro");
            showMessage("Erro ao buscar informações SNMP.", Alert.AlertType.ERROR);
        }
    }

    private void fetchWebPageData(String ip) {
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

        // Extrai informações
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

    private void disableSSLCertificateChecking() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
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

    private String getSnmpValue(String oid, Snmp snmp, CommunityTarget target) {
        try {
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);

            ResponseEvent response = snmp.get(pdu, target);
            if (response != null && response.getResponse() != null) {
                return response.getResponse().get(0).getVariable().toString();
            }
        } catch (Exception e) {
            return "Erro";
        }
        return "Erro";
    }

    private void showMessage(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Erro" : "Informação");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
