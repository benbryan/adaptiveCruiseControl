package info.benbryan.cruseControl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class MarkerParser {

    public static ArrayList<SpeedLimitSign> parse(String s) {
        ArrayList<SpeedLimitSign> speedLimitSigns = new ArrayList<>();
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = null;

            docBuilder = docBuilderFactory.newDocumentBuilder();

            InputStream is = new ByteArrayInputStream(s.getBytes());
            Document doc = docBuilder.parse(is);
            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("markers".equalsIgnoreCase(n.getNodeName())) {
                    speedLimitSigns.addAll(parseSpeedLimitSigns(n));
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException  e) {
            e.printStackTrace();
        }
        return speedLimitSigns;
    }

    private static ArrayList<SpeedLimitSign> parseSpeedLimitSigns(Node nodeIn) {
        ArrayList<SpeedLimitSign> speedLimitSigns = new ArrayList<>();
        for (Node n = nodeIn.getFirstChild(); n != null; n = n.getNextSibling()) {
            if ("marker".equalsIgnoreCase(n.getNodeName())) {
                NamedNodeMap a = n.getAttributes();
                String label = a.getNamedItem("label").getNodeValue();
                String latitude = a.getNamedItem("lat").getNodeValue();
                String longitude = a.getNamedItem("lng").getNodeValue();
                String speedLimit = a.getNamedItem("mph").getNodeValue();
                String bearing = a.getNamedItem("cog").getNodeValue();
                Node nv = a.getNamedItem("alt_meters");
                String altitude;
                if (nv == null){
                    altitude = "-1";
                } else {
                    altitude = nv.getNodeValue();
                }
                nv = a.getNamedItem("deletedOn");
                String deletedOn;
                if (nv == null){
                    deletedOn = "-1";
                } else {
                    deletedOn = nv.getNodeValue();
                }
                Date dateDeleteOn = new Date();
                // 2015-07-04
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                try {
                    dateDeleteOn = df.parse(deletedOn);
                } catch (ParseException e) {
                    dateDeleteOn.setTime(-1);
                    e.printStackTrace();
                }

                SpeedLimitSign sign = new SpeedLimitSign(   Double.parseDouble(latitude),
                        Double.parseDouble(longitude),
                        Double.parseDouble(altitude),
                        Double.parseDouble(bearing),
                        Integer.parseInt(speedLimit),
                        -1,
                        dateDeleteOn.getTime());
                speedLimitSigns.add(sign);
            }
        }
        return speedLimitSigns;
    }

}
