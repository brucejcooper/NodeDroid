package com.eightbitcloud.internode.util;

import org.w3c.dom.Element;

public class XMLTools {
    public static String getChildText(Element e, String n) {
        return getNode(e, n).getFirstChild().getNodeValue();
    }

    public static Element getNode(Element e, String name) {
        return (Element) e.getElementsByTagName(name).item(0);
    }

    public static Element getAPINode(Element response) {
        return getNode(getNode(response, "internode"), "api");
    }

}
