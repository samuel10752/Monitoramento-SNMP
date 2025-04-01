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

public class L15150 extends PrinterModel {

    private Snmp snmp;

    public L15150(String ip, TextField macField, TextField serialField, TextField nameprinterField,
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
        return "Contadores L15150"; // Ajuste conforme a necessidade // OID do contador de páginas
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
            Element totalPages = doc.selectFirst("dt.key:contains(Número total de páginas)");
            if (totalPages != null && totalPages.nextElementSibling() != null) {
                // Extrai o texto do próximo elemento
                String totalPageCount = totalPages.nextElementSibling()
                        .selectFirst("div.preserve-white-space") // Seleciona o <div> com classe específica
                        .text() // Extrai o texto do elemento
                        .trim(); // Remove espaços em branco
                // Armazena o valor no mapa com a chave "Geral"
                webData.put("Geral", totalPageCount);
            }

            // Buscar o total de páginas Preto e Branco
            Element totalPB = doc.selectFirst("dt.key:contains(Número total de páginas a P&B)");
            if (totalPB != null && totalPB.nextElementSibling() != null) {
                // Extrai o texto do próximo elemento
                String totalPageCount = totalPB.nextElementSibling()
                        .selectFirst("div.preserve-white-space") // Seleciona o <div> com classe específica
                        .text() // Extrai o texto do elemento
                        .trim(); // Remove espaços em branco
                // Armazena o valor no mapa com a chave "Geral"
                webData.put("Geral P&B", totalPageCount);
            }

            // Buscar o total de páginas Coloridas
            Element totalColor = doc.selectFirst("dt.key:contains(Número total de páginas a Cor)");
            if (totalColor != null && totalColor.nextElementSibling() != null) {
                // Extrai o texto do próximo elemento
                String totalPageCount = totalColor.nextElementSibling()
                        .selectFirst("div.preserve-white-space") // Seleciona o <div> com classe específica
                        .text() // Extrai o texto do elemento
                        .trim(); // Remove espaços em branco
                // Armazena o valor no mapa com a chave "Geral"
                webData.put("Geral Cor Total", totalPageCount);
            }

            // Buscar o número total de páginas A3 Preto e Branco
            Element totalA3BW = doc.selectFirst("td.value:contains(A3/Ledger)");
            int bwCount = 0;
            if (totalA3BW != null && totalA3BW.nextElementSibling() != null) {
                Element divBW = totalA3BW.nextElementSibling().selectFirst("div.preserve-white-space");
                if (divBW != null) {
                    String bwText = divBW.text().trim();
                    bwCount = Integer.parseInt(bwText); // Converte para inteiro
                    webData.put("A3 P&B", String.valueOf(bwCount)); // Adiciona ao mapa como String
                }
            } else {
                System.out.println("Elemento A3 Preto e Branco não encontrado ou está nulo!");
            }

            // Buscar o número total de páginas A3 Cor
            Element totalA3Color = totalA3BW.nextElementSibling(); // Pula para a próxima coluna
            int colorCount = 0;
            if (totalA3Color != null && totalA3Color.nextElementSibling() != null) {
                Element divColor = totalA3Color.nextElementSibling().selectFirst("div.preserve-white-space");
                if (divColor != null) {
                    String colorText = divColor.text().trim();
                    colorCount = Integer.parseInt(colorText); // Converte para inteiro
                    webData.put("A3 Cor total", String.valueOf(colorCount)); // Adiciona ao mapa como String
                }
            } else {
                System.out.println("Elemento A3 Cor não encontrado ou está nulo!");
            }

            // Calcular soma total
            int totalSum = bwCount + colorCount;
            webData.put("A3:", String.valueOf(totalSum)); // Adiciona como String

            // Buscar o número total de páginas A4 Preto e Branco
            Element totalA4BW = doc.selectFirst("td.value:contains(A4/Letter)");
            int bwA4Count = 0; // Primeiro contador Preto e Branco
            int bwA4CountExtra = 0; // Segundo contador Preto e Branco
            if (totalA4BW != null && totalA4BW.nextElementSibling() != null) {
                // Primeiro valor (1 face Preto e Branco)
                Element divBW = totalA4BW.nextElementSibling().selectFirst("div.preserve-white-space");
                if (divBW != null) {
                    String bwText = divBW.text().trim();
                    bwA4Count = Integer.parseInt(bwText); // Converte para inteiro
                }

                // Segundo valor (2 faces Preto e Branco)
                Element divBWExtra = totalA4BW.nextElementSibling().nextElementSibling()
                        .selectFirst("div.preserve-white-space");
                if (divBWExtra != null) {
                    String bwTextExtra = divBWExtra.text().trim();
                    bwA4CountExtra = Integer.parseInt(bwTextExtra); // Converte para inteiro
                }

                // Armazena os dois vablores separadamente no mapa
                webData.put("A4 P&B", String.valueOf(bwA4Count));
                webData.put("A4 Colorido", String.valueOf(bwA4CountExtra));
            } else {
                System.out.println("Elemento A4 Preto e Branco não encontrado ou está nulo!");
            }

            // Buscar o número total de páginas A4 Cor
            int colorA4Count = 0; // Primeiro contador Cor
            int colorA4CountExtra = 0; // Segundo contador Cor
            if (totalA4BW != null && totalA4BW.nextElementSibling() != null) {
                // Primeiro valor (1 face Cor)
                Element divColor = totalA4BW.nextElementSibling().nextElementSibling().nextElementSibling()
                        .selectFirst("div.preserve-white-space");
                if (divColor != null) {
                    String colorText = divColor.text().trim();
                    colorA4Count = Integer.parseInt(colorText); // Converte para inteiro
                }

                // Segundo valor (2 faces Cor)
                Element divColorExtra = totalA4BW.nextElementSibling().nextElementSibling().nextElementSibling()
                        .nextElementSibling()
                        .selectFirst("div.preserve-white-space");
                if (divColorExtra != null) {
                    String colorTextExtra = divColorExtra.text().trim();
                    colorA4CountExtra = Integer.parseInt(colorTextExtra); // Converte para inteiro
                }

                // Armazena os dois valores separadamente no mapa
                webData.put("A4 P&B Duplex", String.valueOf(colorA4Count));
                webData.put("A4 Cor Duplex Total", String.valueOf(colorA4CountExtra));
            } else {
                System.out.println("Elemento A4 Cor não encontrado ou está nulo!");
            }

            // Buscar o número total de páginas "Outra" Preto e Branco
            Element totalOutra = doc.selectFirst("td.value:contains(Outra)");
            int outraBWCount = 0; // Primeiro contador Preto e Branco
            int outraBWCountExtra = 0; // Segundo contador Preto e Branco
            if (totalOutra != null && totalOutra.nextElementSibling() != null) {
                // Primeiro valor (1 face Preto e Branco)
                Element divBW = totalOutra.nextElementSibling().selectFirst("div.preserve-white-space");
                if (divBW != null) {
                    String bwText = divBW.text().trim();
                    outraBWCount = Integer.parseInt(bwText); // Converte para inteiro
                }

                // Segundo valor (2 faces Preto e Branco)
                Element divBWExtra = totalOutra.nextElementSibling().nextElementSibling()
                        .selectFirst("div.preserve-white-space");
                if (divBWExtra != null) {
                    String bwTextExtra = divBWExtra.text().trim();
                    outraBWCountExtra = Integer.parseInt(bwTextExtra); // Converte para inteiro
                }

                // Armazena os dois valores separadamente no mapa
                webData.put("Outros Formatos de papel P&B", String.valueOf(outraBWCount));
                webData.put("Outros Formatos de papel Colorido", String.valueOf(outraBWCountExtra));
            } else {
                System.out.println("Elemento Outra Preto e Branco não encontrado ou está nulo!");
            }

            // Buscar o número total de digitalizações em Preto e Branco
            Element totalDigitBW = doc.selectFirst("dt.key:contains(Digit. a P&B)");
            int digitBWCount = 0;
            if (totalDigitBW != null && totalDigitBW.nextElementSibling() != null) {
                Element divBW = totalDigitBW.nextElementSibling().selectFirst("div.preserve-white-space");
                if (divBW != null) {
                    String bwText = divBW.text().trim();
                    digitBWCount = Integer.parseInt(bwText); // Converte para inteiro
                }
                webData.put("Total Digitalização P&B", String.valueOf(digitBWCount)); // Adiciona ao mapa
            } else {
                System.out.println("Elemento Digitalização P&B não encontrado ou está nulo!");
            }

            // Buscar o número total de digitalizações em Cor
            Element totalDigitColor = doc.selectFirst("dt.key:contains(Digit. a Cor)");
            int digitColorCount = 0;
            if (totalDigitColor != null && totalDigitColor.nextElementSibling() != null) {
                Element divColor = totalDigitColor.nextElementSibling().selectFirst("div.preserve-white-space");
                if (divColor != null) {
                    String colorText = divColor.text().trim();
                    digitColorCount = Integer.parseInt(colorText); // Converte para inteiro
                }
                webData.put("Total Digitalização Cor", String.valueOf(digitColorCount)); // Adiciona ao mapa
            } else {
                System.out.println("Elemento Digitalização Cor não encontrado ou está nulo!");
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
