// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.help.perl.reader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.talend.help.Activator;
import org.talend.help.perl.model.EProperty;
import org.talend.help.perl.model.EType;
import org.talend.help.perl.model.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * IndexParser.java.
 * 
 */
public class IndexParser extends DefaultHandler {

    private static final String PATH = "guide/perl/perl_func_list.xml";

    public static final String FILE = "/home/wiu/work/talend/workspace-talend-svn/org.talend.help/" + PATH;

    private Node root;

    private Stack<Node> current = new Stack<Node>();

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        // TODO Auto-generated method stub
        super.characters(ch, start, length);
        String value = new String(ch, start, length).trim();
        if (value.length() != 0) {
            // System.out.println(current.peek()+"\t"+value);
            current.peek().getProperties().put(EProperty.VALUE, value);           
        }
    }

    @Override
    public void endDocument() throws SAXException {
        // TODO Auto-generated method stub
        super.endDocument();
        current = null;
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        // TODO Auto-generated method stub
        super.endElement(uri, localName, qName);
        current.pop();
    }

    @Override
    public void startDocument() throws SAXException {
        // TODO Auto-generated method stub
        super.startDocument();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        // TODO Auto-generated method stub
        super.startElement(uri, localName, qName, attributes);
        EType type = EType.find(qName);
        if (type != null) {
            if (root == null) {
                root = new Node(type, null);
                current.push(root);
            } else {
                Node node = new Node(type, current.peek());
                current.push(node);
                String value = attributes.getValue("name");
                if (value != null) {
                    node.getProperties().put(EProperty.LABEL, value);
                }
            }
        }
    }

    public Node getRoot() {
        return root;
    }

    public static Node parse() throws IOException, ParserConfigurationException, SAXException {
        InputStream stream = FileLocator.openStream(Activator.getDefault().getBundle(), new Path(PATH), false);
        try {
            return parse(stream);
        } finally {
            stream.close();
        }
    }

    public static Node parse(InputStream in) throws ParserConfigurationException, SAXException, IOException {
        IndexParser saxHandler = new IndexParser();

        SAXParserFactory newInstance = SAXParserFactory.newInstance();
        SAXParser parser = newInstance.newSAXParser();
        parser.parse(in, saxHandler);
        return saxHandler.getRoot();

    }

    public static void main(String[] args) {
        try {
            IndexParser saxHandler = new IndexParser();

            SAXParserFactory newInstance = SAXParserFactory.newInstance();
            SAXParser parser = newInstance.newSAXParser();
            File file = new File(FILE);
            parser.parse(file, saxHandler);
            System.out.println();
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }
}
