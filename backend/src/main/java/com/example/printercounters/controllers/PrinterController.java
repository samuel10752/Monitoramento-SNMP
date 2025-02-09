package com.example.printercounters.controllers;

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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PrinterController {

    private final Snmp snmp;
    private final String printerAddress;

    public PrinterController() throws Exception {
        this.printerAddress = "udp:192.168.1.12/161"; // Endereço da impressora
        TransportMapping<?> transport = new DefaultUdpTransportMapping();
        this.snmp = new Snmp(transport);
        transport.listen();
    }

    @GetMapping("/counters")
    public Map<String, String> getCounters() {
        Map<String, String> counters = new HashMap<>();

        // OIDs para a Epson L3250
        Map<String, String> oids = new HashMap<>();
        oids.put("Total de Paginas", "1.3.6.1.2.1.43.10.2.1.4.1.1"); // Total de páginas impressas
        oids.put("Paginas Preto e Branco", "1.3.6.1.4.1.1248.1.2.2.27.1.1.3.1.1"); // Páginas em preto e branco
        oids.put("Paginas Colorido", "1.3.6.1.2.1.43.10.2.1.5.1.1"); // Páginas coloridas
        oids.put("MAC", "1.3.6.1.2.1.2.2.1.6.1"); // Nível de MAC
        oids.put("ColorInkLevel", "1.3.6.1.2.1.43.11.1.1.9.1.2"); // Nível de tinta colorida

        for (Map.Entry<String, String> entry : oids.entrySet()) {
            String key = entry.getKey();
            String oidValue = entry.getValue();
            try {
                String value = getSnmpValue(oidValue);
                counters.put(key, value);
            } catch (IOException e) {
                e.printStackTrace();
                counters.put(key, "Erro ao obter valor");
            }
        }
        return counters;
    }

    private String getSnmpValue(String oid) throws IOException {
        Address targetAddress = GenericAddress.parse(printerAddress);
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public")); // Comunidade SNMP (verifique se está correta para sua impressora)
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);

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
}
