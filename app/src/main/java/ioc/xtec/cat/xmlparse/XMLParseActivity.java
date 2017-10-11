package ioc.xtec.cat.xmlparse;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;

import ioc.xtec.cat.xmlparse.StackOverflowXmlParser.Entrada;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import java.net.URL;


public class XMLParseActivity extends ActionBarActivity {

    //   RSS FEED
    //   public static final String WIFI = "Wi-Fi";
    //   public static final String ANY = "Any";
    private static final String URL = "http://stackoverflow.com/feeds/tag?tagnames=android&sort=newest";


    // Si existeix una connecció wifi
    private static boolean connectatWifi = false;
    // Si existeix una conneció 3G
    private static boolean connectat3G = false;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xmlparse);

        //Mirem si hi ha connexió de xarxa
        actualitzaEstatXarxa();

        ////////////
        //Amb fils//
        ////////////

        //Carreguem les noticies a un fil independent fent servir AsyncTask

        //Carreguem noticies
        carregaNoticies();

        //////////////
        //Sense fils//
        //////////////
        //Fem la crida directament sense fer servir AsyncTask
        /*
        try
        {
        	//Carreguem l'XML
        	String HTMLCodi = carregaXMLdelaXarxa(URL);

        	 WebView myWebView = (WebView) findViewById(R.id.webView1);
             myWebView.loadData(HTMLCodi, "text/html", null);

        } catch (IOException e)
        {
        	//Error de connexió

        	Toast.makeText(this, "Error de connexio", Toast.LENGTH_LONG).show();
            //return getResources().getString(R.string.connection_error);
        }
        catch (XmlPullParserException e)
        {
        	//Error de parse

        	Toast.makeText(this, "Error de parse", Toast.LENGTH_LONG).show();
            //return getResources().getString(R.string.xml_error);
        }
        */
    }

    private void actualitzaEstatXarxa() {
        //Obtenim un gestor de les connexions de xarxa
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        //Obtenim l'estat de la xarxa mÃ²bil
        NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (networkInfo == null)
            connectat3G = false;
        else
            connectat3G = networkInfo.isConnected();

        //Obtenim l'estat de la xarxa Wi-Fi
        networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo == null)
            connectatWifi = false;
        else
            connectatWifi = networkInfo.isConnected();
    }

    //Fa servir AsyncTask per descarregar el feed XML de stackoverflow.com
    public void carregaNoticies() {

        //Si tenim connexió al dispositiu
        if ((connectatWifi || connectat3G)) {
            new DescarregaXmlTask().execute(URL);
        } else {
            Toast.makeText(this, "No hi ha connexio", Toast.LENGTH_LONG).show();
        }

    }


    //Implementació d'AsyncTask per descarregar el feed XML de stackoverflow.com
    private class DescarregaXmlTask extends AsyncTask<String, Void, String> {
        @Override

        //El que s'executar en el background
        protected String doInBackground(String... urls) {
            try {
                //Carreguem l'XML
                return carregaXMLdelaXarxa(urls[0]);
            } catch (IOException e) {
                //Error de connexió
                return "Error de connexió";
                //return getResources().getString(R.string.connection_error);
            } catch (XmlPullParserException e) {
                //Error de parse
                return "Error a l'analitzar l'XML";
                //return getResources().getString(R.string.xml_error);
            }
        }

        @Override
        //Una vegada descarregada la informació XML i convertida a HTML l'enllacem al WebView
        protected void onPostExecute(String result) {
            setContentView(R.layout.activity_xmlparse);

            //Mostra la cadena HTML en la UI a travs del WebView
            WebView myWebView = (WebView) findViewById(R.id.webView1);
            myWebView.loadData(result, "text/html", null);
        }
    }

    //Descarrega XML d'stackoverflow.com, l'analitza i crea amb ell un codi HTML que retorna com String
    private String carregaXMLdelaXarxa(String urlString) throws XmlPullParserException, IOException {
        InputStream stream = null;
        //Creem una instncia de l'analitzador
        StackOverflowXmlParser analitzador = new StackOverflowXmlParser();

        //Llista de entrades de noticies
        List<Entrada> entrades = null;

        //Cadena on construirem el codi HTML que mostrara el widget webView
        StringBuilder htmlString = new StringBuilder();


        try {
            //Obrim la connexio
            //stream = obreConnexioHTTP(urlString);
            stream = ObreConnexioHTTP(urlString);

            //Obtenim la llista d'entrades a partir de l'stream de dades
            entrades = analitzador.analitza(stream);
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();

            e.printStackTrace();
        } finally {
            //Tanquem l'stream una vegada hem terminat de treballar amb ell
            if (stream != null) {
                stream.close();
            }
        }

        //analitzador.parse() retorna una llista (entrades) d'entrades de noticies (objectes
        //de la classe Entrada. Cada objecte representa un post de l'XML Feed. Ara es processen
        //les entrades de la llista per crear un codi HTML. Per cada entrada es crea un enllaç
        //a la noticia completa

        //Si tenim noticies
        if (entrades != null) {

            ///////////////////////////////////////////////////////////
            //Creem l'HTML a partir dels continguts del List<Entrada>//
            ///////////////////////////////////////////////////////////

            //Per indicar quan s'ha actualitzat el RSS
            Calendar ara = Calendar.getInstance();
            DateFormat formatData = new SimpleDateFormat("dd MMM h:mmaa");


            //Títol de la pgina
            htmlString.append("<h3> Noticies </h3>");
            //htmlString.append("<h3>" + getResources().getString(R.string.page_title) + "</h3>");

            //Data d'actualització
            htmlString.append("<em>Actualitzat el " + formatData.format(ara.getTime()) + "</em>");
            //htmlString.append("<em>" + getResources().getString(R.string.updated) + " " + formatData.format(ara.getTime()) + "</em>");


            //Per cada noticia de la llista
            for (Entrada noticia : entrades) {
                //Creem un títol de la noticia que ser un enllaç HTML a la noticia completa
                htmlString.append("<p> <a href='");
                htmlString.append(noticia.enllac);
                htmlString.append("'>" + noticia.titol + "</a>");

                //Si la noticia t un resum, l'afegim
                //htmlString.append(noticia.resum);

                //String prova = noticia.resum;
                String trencat = noticia.resum.substring(0, 70);

                htmlString.append("<br><i>Resum:</i>" + trencat + "...");
                htmlString.append("</p> <hr>");
            }
        }

        //Retornem un String amb el contingut HTML que mostrar el widget
        return htmlString.toString();
    }

//Donat un String amb l'URL, obre una connexió i retorna un InputStream
 /*private InputStream obreConnexioHTTP(String urlString) throws IOException
 {
     URL url = new URL(urlString);
     HttpURLConnection conn = (HttpURLConnection) url.openConnection();
     conn.setReadTimeout(10000 );
     conn.setConnectTimeout(15000 );
     conn.setRequestMethod("GET");
     conn.setDoInput(true);

     //Connectem
     conn.connect();
     InputStream stream = conn.getInputStream();

     return stream;
 }

*/

    //Obre una connexió HTTP a partir d'un URL i retorna un InputStream
    private InputStream ObreConnexioHTTP(String adrecaURL) throws IOException {
        InputStream in = null;        //Buffer de recepció
        int resposta = -1;            //Resposta de la connexió

        //Obtenim un URL a partir de l'String proporcionat
        URL url = new URL(adrecaURL);

        //Obtenim una nova connexió al recurs referenciat per la URL
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

        try {
            ///////////////////////
            //Preparem la petició//
            ///////////////////////

            httpConn.setReadTimeout(10000);            //Timeout de lectura en milisegons
            httpConn.setConnectTimeout(15000);        //Timeout de connexió en milisegons
            httpConn.setRequestMethod("GET");        //Petició al servidor
            httpConn.setDoInput(true);                //Si la connexió permet entrada

            //Es connecta al recurs.
            httpConn.connect();

            //Obtenim el codi de resposta obtingut del servidor remot HTTP
            resposta = httpConn.getResponseCode();

            //Comprovem si el servidor ens ha retornat un codi de resposta OK,
            //que correspon a que el contingut s'ha descarregat correctament
            if (resposta == HttpURLConnection.HTTP_OK) {
                //Obtenim un Input stream per llegir del servidor
                //in = new BufferedInputStream(httpConn.getInputStream());
                in = httpConn.getInputStream();
            }
        } catch (Exception ex) {
            //Hi ha hagut un problema al connectar
            throw new IOException("Error connectant");
        }

        //Retornem el flux de dades
        return in;
    }


    public void onStart() {
        super.onStart();

        //Tornem a actualitzar l'estat de la xarxa
        actualitzaEstatXarxa();
    }
}
