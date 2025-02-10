package com.example.printercounters.controllers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@RestController
@RequestMapping("/api")
public class PrinterController {

    private final Snmp snmp;

    public PrinterController() throws Exception {
        TransportMapping<?> transport = new DefaultUdpTransportMapping();
        this.snmp = new Snmp(transport);
        transport.listen();
    }

    @GetMapping("/counters")
    public Map<String, String> getCounters() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite o IP da impressora: ");
        String ip = scanner.nextLine();

        Map<String, String> counters = new HashMap<>();

        // Adiciona o IP capturado
        counters.put("IP da Impressora", ip);

        // Adiciona conteúdo da página web
        String url = "https://" + ip + "/PRESENTATION/ADVANCED/INFO_MENTINFO/TOP";
        Map<String, String> webData = getWebPageData(url);

        // Adiciona os dados da página web aos contadores
        counters.putAll(webData);

        // Captura informações SNMP
        Address targetAddress = GenericAddress.parse("udp:" + ip + "/161");
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public")); // Comunidade SNMP (verifique se está correta para sua impressora)
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);

        // Captura o endereço MAC
        String macOid = "1.3.6.1.2.1.2.2.1.6.1";
        String macAddress = getSnmpValue(macOid, target);
        counters.put("Endereço MAC", macAddress);

        // Captura o número de série
        String serialNumberOid = "1.3.6.1.2.1.43.5.1.1.17.1";
        String serialNumber = getSnmpValue(serialNumberOid, target);
        counters.put("Número de Série", serialNumber);

        // Captura o modelo
        String modelOid = "1.3.6.1.2.1.43.5.1.1.16.1";
        String model = getSnmpValue(modelOid, target);
        counters.put("Modelo", model);

        // Captura a marca
        String brandOid = "1.3.6.1.2.1.25.3.2.1.3.1";
        String brand = getSnmpValue(brandOid, target);
        counters.put("Marca", brand);

        return counters;
    }

    private String getSnmpValue(String oid, CommunityTarget target) throws IOException {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oid)));
        pdu.setType(PDU.GET);

        ResponseEvent response = snmp.get(pdu, target);
        if (response != null && response.getResponse() != null) {
            return response.getResponse().get(0).getVariable().toString();
        } else {
            throw new IOException("Erro ao obter valor SNMP para OID: " + oid);
        }
    }

    private static void disableSSLCertificateChecking() {
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

            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> getWebPageData(String url) throws IOException {
        // Desabilitar verificação de certificado SSL
        disableSSLCertificateChecking();

        // Conecta à página web e extrai o conteúdo
        Document doc = Jsoup.connect(url).get();
        Map<String, String> webData = new HashMap<>();

        // Extrai informações da página
        String totalNumberOfPages = doc.select("dt:contains(Total Number of Pages) + dd .preserve-white-space").text();
        String totalNumberOfBWPages = doc.select("dt:contains(Total Number of B&W Pages) + dd .preserve-white-space").text();
        String totalNumberOfColorPages = doc.select("dt:contains(Total Number of Color Pages) + dd .preserve-white-space").text();
        String totalNumberOfBWScan = doc.select("dt:contains(B&W Scan) + dd .preserve-white-space").text();
        String totalNumberOfColorScan = doc.select("dt:contains(Color Scan) + dd .preserve-white-space").text();

        // Adiciona as informações ao mapa
        webData.put("Total Number of Pages", totalNumberOfPages);
        webData.put("Total Number of B&W Pages", totalNumberOfBWPages);
        webData.put("Total Number of Color Pages", totalNumberOfColorPages);
        webData.put("B&W Scan", totalNumberOfBWScan);
        webData.put("Color Scan", totalNumberOfColorScan);

        return webData;
    }
}
