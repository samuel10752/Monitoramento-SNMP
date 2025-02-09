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

        // OIDs para a Epson L3250 (verifique o manual para OIDs exatos)
        Map<String, String> oids = new HashMap<>();
        oids.put("TotalPages", "1.3.6.1.2.1.43.10.2.1.4.1.1"); // Total de páginas impressas
        oids.put("BlackPages", "1.3.6.1.2.1.43.10.2.1.4.1.1.1"); // Páginas em preto e branco
        oids.put("ColorPages", "1.3.6.1.2.1.43.10.2.1.4.1.1.2"); // Páginas coloridas
        oids.put("BlackInkLevel", "1.3.6.1.2.1.43.11.1.1.9.1.1"); // Nível de tinta preta
        oids.put("ColorInkLevel", "1.3.6.1.2.1.43.11.1.1.9.1.2"); // Nível de tinta colorida

        for (Map.Entry<String, String> entry : oids.entrySet()) {
            try {
                String value = getAsString(new OID(entry.getValue()));
                counters.put(entry.getKey(), value);
            } catch (Exception e) {
                counters.put(entry.getKey(), "Erro: " + e.getMessage());
            }
        }

        return counters;
    }

    private String getAsString(OID oid) throws Exception {
        ResponseEvent event = get(new OID[]{oid});
        if (event != null && event.getResponse() != null) {
            return event.getResponse().get(0).getVariable().toString();
        }
        throw new RuntimeException("GET timed out or no response received");
    }

    private ResponseEvent get(OID[] oids) throws Exception {
        PDU pdu = new PDU();
        for (OID oid : oids) {
            pdu.add(new VariableBinding(oid));
        }
        pdu.setType(PDU.GET);
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));
        Address targetAddress = GenericAddress.parse(printerAddress);
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(5000);
        target.setVersion(SnmpConstants.version2c);
        return snmp.send(pdu, target);
    }
}