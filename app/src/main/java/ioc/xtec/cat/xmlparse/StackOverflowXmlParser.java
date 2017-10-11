package ioc.xtec.cat.xmlparse;


import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StackOverflowXmlParser {

    // No fem servir namespaces
    private static final String ns = null;

    //Aquesta classe representa una entrada de noticia del RSS Feed
    public static class Entrada {
        public final String titol;        //Títol de la notcia
        public final String enllac;        //Enllaç a la notcia completa
        public final String resum;        //Resum de la notícia

        private Entrada(String title, String summary, String link) {
            this.titol = title;
            this.resum = summary;
            this.enllac = link;
        }
    }

    public List<Entrada> analitza(InputStream in) throws XmlPullParserException, IOException {
        try {
            //Obtenim analitzador
            XmlPullParser parser = Xml.newPullParser();

            //No fem servir namespaces
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

            //Especifica l'entrada de l'analitzador
            parser.setInput(in, null);

            //Obtenim la primera etiqueta
            parser.nextTag();

            //Retornem la llista de noticies
            return llegirNoticies(parser);
        } finally {
            in.close();
        }
    }


    //Llegeix una llista de noticies d'StackOverflow a partir del parser i retorna una llista d'Entrades
    private List<Entrada> llegirNoticies(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Entrada> llistaEntrades = new ArrayList<Entrada>();

        //Comprova si l'event actual és del tipus esperat (START_TAG) i del nom "feed"
        parser.require(XmlPullParser.START_TAG, ns, "feed");

        //Mentre que no arribem al final d'etiqueta
        while (parser.next() != XmlPullParser.END_TAG) {
            //Ignorem tots els events que no siguin un comenament d'etiqueta
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                //Saltem al seguent event
                continue;
            }

            //Obtenim el nom de l'etiqueta
            String name = parser.getName();

            // Si aquesta etiqueta és una entrada de noticia
            if (name.equals("entry")) {
                //Afegim l'entrada a la llista
                llistaEntrades.add(llegirEntrada(parser));
            } else {
                //Si és una altra cosa la saltem
                saltar(parser);
            }
        }
        return llistaEntrades;
    }


    //Aquesta funció serveix per saltar-se una etiqueta i les seves subetiquetes aniuades.
    private void saltar(XmlPullParser parser) throws XmlPullParserException, IOException {
        //Si no és un comenament d'etiqueta: ERROR
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;

        //Comprova que ha passat per tantes etiquetes de començament com acabament d'etiqueta

        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    //Cada vegada que es tanca una etiqueta resta 1
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    //Cada vegada que s'obre una etiqueta augmenta 1
                    depth++;
                    break;
            }
        }
    }

    //Analitza el contingut d'una entrada. Si troba un ttol, resum o enllaç, crida els mètodes de lectura
    //propis per processar-los. Si no, ignora l'etiqueta.
    private Entrada llegirEntrada(XmlPullParser parser) throws XmlPullParserException, IOException {
        String titol = null;
        String resum = null;
        String enllac = null;

        //L'etiqueta actual ha de ser "entry"
        parser.require(XmlPullParser.START_TAG, ns, "entry");

        //Mentre que no acabe l'etiqueta de "entry"
        while (parser.next() != XmlPullParser.END_TAG) {
            //Ignora fins que no trobem un començament d'etiqueta
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            //Obtenim el nom de l'etiqueta
            String etiqueta = parser.getName();

            //Si és un títol de noticia
            if (etiqueta.equals("title")) {
                titol = llegirTitol(parser);
            }
            //Si l'etiqueta és un resum de notcia
            else if (etiqueta.equals("summary")) {
                resum = llegirResum(parser);
            }
            //Si és un enllaç
            else if (etiqueta.equals("link")) {
                enllac = llegirEnllac(parser);
            } else {
                //les altres etiquetes les saltem
                saltar(parser);
            }
        }

        //Creem una nova entrada amb aquestes dades i la retornem
        return new Entrada(titol, resum, enllac);
    }

    //Llegeix el títol de una notcia del feed i el retorna com String
    private String llegirTitol(XmlPullParser parser) throws IOException, XmlPullParserException {
        //L'etiqueta actual ha de ser "title"
        parser.require(XmlPullParser.START_TAG, ns, "title");

        //Llegeix
        String titol = llegeixText(parser);

        //Fi d'etiqueta
        parser.require(XmlPullParser.END_TAG, ns, "title");
        return titol;
    }

    //Llegeix l'enllaç de una notícia del feed i el retorna com String
    private String llegirEnllac(XmlPullParser parser) throws IOException, XmlPullParserException {
        String enllac = "";

        //L'etiqueta actual ha de ser "link"
        parser.require(XmlPullParser.START_TAG, ns, "link");

        //Obtenim l'etiqueta
        String tag = parser.getName();

        //Obtenim l'atribut rel (mirar l'XML d'Stackoverflow)
        String relType = parser.getAttributeValue(null, "rel");

        //Si l'enllaç és link

        if (tag.equals("link")) {
            //Obtenim l'enlla del valor de l'atribut "href". Revisar format XML stackoverflow
            if (relType.equals("alternate")) {
                enllac = parser.getAttributeValue(null, "href");
                parser.nextTag();
            }
        }

        //Fi d'etiqueta
        parser.require(XmlPullParser.END_TAG, ns, "link");

        return enllac;
    }

    //Llegeix el resum de una notícia del feed i el retorna com String
    private String llegirResum(XmlPullParser parser) throws IOException, XmlPullParserException {
        //L'etiqueta actual ha de ser "summary"
        parser.require(XmlPullParser.START_TAG, ns, "summary");

        String resum = llegeixText(parser);

        parser.require(XmlPullParser.END_TAG, ns, "summary");
        return resum;
    }


    //Extrau el valor de text per les etiquetes titol, resum
    private String llegeixText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String resultat = "";

        if (parser.next() == XmlPullParser.TEXT) {
            resultat = parser.getText();
            parser.nextTag();
        }
        return resultat;
    }

}

//Llegeix una llista de noticies d'StackOverflow a partir del parser i retorna una llista d'Entrades