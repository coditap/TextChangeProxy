package main.java;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ProxyReader extends Thread {

    private static final Logger LOG = Logger.getLogger(ProxyReader.class);

    private final int BUFFER_SIZE = 65535;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private ProxyHTTPSession connection;
    private boolean textChanged = false;
    ProxyClientInputStream clientInputStream;

    public ProxyReader(ProxyHTTPSession connection, BufferedInputStream l_in, BufferedOutputStream l_out, ProxyClientInputStream clientInputStream) {
        /**
         * przypisujemy streamy
         */
        in = l_in;
        out = l_out;
        this.clientInputStream = clientInputStream;
        /**
         * przypisujemy connecrtion
         */
        this.connection = connection;
        /**
         * startujemy watek
         */
        start();
    }

    public void run() {
        read();
        /**
         * ustawiamy poczatkowo obiekty na nulla
         */
        connection = null;
        in = null;
        out = null;

    }

    /**
     * metoda ktora czyta odpowiedz serwera
     *
     */
    private void read() {
        int bytes_read = 0; // ilosc wczytanych bajtow
        byte[] buf = new byte[BUFFER_SIZE]; // inicjalizacja bufora
        byte[] replacedResponse = null; // bufor na nasza przetworzona wiadomosc
        try {
            while (true) { // nieskonczona petla
                bytes_read = in.read(buf); // wczytujemy do bufora "buf" bajty ze strumienia "in"

                if (bytes_read != -1) { // jesli nie bylo bledu
                    try {
                        textChanged = false;
                        replacedResponse = processResponseContent(buf); // probojemy, czy zawartosc posiada text
                    } catch (Exception e) {
                        System.out.println("Error while processing response content");
                    }

                    if (textChanged) {
                        /**
                         * zamienilismy text, wiec musimy podmienic caly content
                         */
                        out.write(replacedResponse, 0, replacedResponse.length); // zwracamy podmieniona zawartosc
                        System.out.println("zwracanie zmienionej odpowiedzi");
                        // FileOutputStream oryginalna = new FileOutputStream("oryginalna");
                        // String oryginalnaString = new String(buf);
                        // oryginalna.write(oryginalnaString.getBytes(), 0, bytes_read);
                        // oryginalna.close();
                        //
                        // oryginalna = new FileOutputStream("zmieniona");
                        // oryginalna.write(replacedResponse);
                        // oryginalna.close();
                        // System.exit(0);

                    } else {
                        // jesli nie zostala dokonana zadna zamiana
                        out.write(buf, 0, bytes_read); // do strumiena out odpisujemy spowrotem to, co przeczytalismy
                        // System.out.println("zwracanie oryginalnej odpowiedzi");
                    }

                    out.flush(); // flushujemy strumien, zeby z bufora autentycznie sie zatwierdzil
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            // e.printStackTrace();
        }

        try {
            if (connection.getStatus() != connection.SC_CONNECTING_TO_HOST) {
                connection.getLocalSocket().close(); // jesli status to nie laczenie z hostem, to zamykamy socket
                throw new IOException();
            }
        } catch (IOException e_socket_close) {
        }

        buf = null;
        replacedResponse = null; // czyscimy bufory
    }

    private byte[] processResponseContent(byte[] buf) {

        /**
         * ustawiamy wartosci zmiennych pomocnicznych
         */
        String line = "";
        boolean firstLine = true;
        boolean contentTypeLineSent = false;
        boolean contentLengthLineSent = false;
        boolean contentSuitableForTextReplace = false;
        boolean headersSent = false;

        String wholeBodyContent = "";
        String wholeHeadersContent = "";

        for (Integer i = 0; i < buf.length; i++) { // iterujemy po wczytanym buforze

            byte bajt = buf[i]; // pobieramy kolejny bajt
            int c = bajt; // przerabiamy go na integera
            if (c != -1 && c != 0) { // jesli nie jest pisty
                line += (char) c; // to do przetwarzanej lini dodajemy go
            } else {
                /**
                 * jesli bajt jest 0 lub -1 to przerywamy proces - nie ma wiecej
                 * danych
                 */
                break;
            }

            if (line.contains("\n") || c == 10 || c == '\n') { // jesli linia się skonczyla
                if (firstLine) { // jesli przetwarzamny pierwsza linie
                    if (line.startsWith("HTTP/1.1 200")) { // jesli zwrocony zostal status ok
                        firstLine = false; // koniec przetwarzania pierwszej lini
                    } else {
                        /**
                         * Content nie b�dzie nadawa� si� do zmiany, nie
                         * bylo poprawnej odpowiedzi
                         */
                        break;
                    }
                } else { // jesli nie pierwsza linia

                    if (!contentTypeLineSent) { // jesli typ contentuy nie zostal jeszcse wyslany
                        if (line.startsWith("Content-Type:")) { // jesli linia zaczyna sie od typu contentu
                            contentTypeLineSent = true; // oznaczamy ze zostal przeslany
                            if (line.startsWith("Content-Type: text/html")) { // jesli jest to text
                                contentSuitableForTextReplace = true; // oznaczamy ze nadaje sie do zamiany
                            } else {
                                break;
                                /**
                                 * takiego contentu nie bedziemy przetwarzac, to
                                 * nie jest tekst
                                 */
                            }
                        }

                    } else {
                        /**
                         * znamy juz content type,
                         */
                    }
                    if (!contentLengthLineSent) { // jesli nie zostala wyslana linia z oznaczeniem dlugosci
                        if (line.startsWith("Content-Length:")) { // jesli jest to linia z onaczeniem dlugosci
                            line = ""; // usuwamy ta linie, nie bedziemy oznaczali dlugosci tresci
                            contentLengthLineSent = true; // oznaczamy, ze zostala juz wyslana
                        }
                    }

                    if (line.startsWith("<!DOCTYPE html>") || line.startsWith("<html>")) {
                        headersSent = true; // sprawdamy, czy zaczal sie content i odpowiednio oznaczamy ten fakt
                    } else {
                        if (!headersSent) {
                            if (!line.isEmpty()) { // naprawa naglowkow, dodajemy znak powrotu karetki i konca lini
                                if (!line.endsWith("\r\n")) {
                                    line = line + "\r\n";
                                }

                            }
                        }
                    }

                }

                if (headersSent && contentSuitableForTextReplace) {
                    /**
                     * jesli nie przetwarzamy naglowkow i content type byl
                     * odpowiedni, to dopisujemy sobie aktualny znak do body
                     * contentuy
                     */
                    wholeBodyContent += line;
                } else {
                    /**
                     * w przeciwnym razie dopisujemy go do naglowkow
                     */
                    wholeHeadersContent += line;
                }
                line = ""; // czyscimy line

            }
        }
        if (wholeBodyContent.length() > 0 && contentSuitableForTextReplace) { // jesli tekst nadawal sie do zmiany i byl jakikolwiek
            /**
             * bylo jakies body w dokumencie, ktore moglo zostac przetworzone
             */
            // wholeBodyContent = replaceText(wholeBodyContent); //wersja z parsowaniem
            wholeBodyContent = replaceTextDumbMode(wholeBodyContent); // wersja z glupia podmiana
            //wholeBodyContent = replaceTextDownloadPageContent();
            String response = wholeHeadersContent;
            response += wholeBodyContent; // tworzymy nowa odpowiedz

            return response.getBytes();
        }
        return null;

    }

    /**
     * zmienia text pobierajac go na nowo z serwera - rozwiazuje problemy z
     * encodingiem chyba
     *
     * @return
     */
    public String replaceTextDownloadPageContent() {
        try {
            System.out.println(clientInputStream.getFullUrl());
            String html = Jsoup.connect(clientInputStream.getFullUrl()).get().html();
            return replaceText(html);
            //return html;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * zmienia text robiac po prostu replace'a
     */
    public String replaceTextDumbMode(String inputBodyContent) {
        String resultText = inputBodyContent;
        for (String wordToChange : WordReplacementDictionary.replacementMap.keySet()) {
            if (resultText.contains(wordToChange)) {
                resultText = resultText.replaceAll(wordToChange, WordReplacementDictionary.replacementMap.get(wordToChange));
                textChanged = true;
                System.out.println("zmieniono zawartosc");
            }
        }
        return resultText;
    }

    /**
     * metoda do zmiany teksu
     */
    public String replaceText(String inputBodyContent) {
        Document document = Jsoup.parse(inputBodyContent); // parsujemy dokument
        processChildren(document); // przetwrzamay go
        return document.html();

    }

    /**
     * metoda do przetwarzania wezlow html
     */
    public void processChildren(Element element) {
        if (element.hasText() && element.children().select("*").size() == 0) {
            /**
             * jesli element nie ma wiecej dzieci
             */
            /**
             * iterujmey po wszystkich slowach do zamiany ze slownka
             */
            for (String wordToChange : WordReplacementDictionary.replacementMap.keySet()) {
                if (element.text().contains(wordToChange)) {

                    String text = element.text();
                    text = text.replaceAll(wordToChange, WordReplacementDictionary.replacementMap.get(wordToChange));
                    element.text(text);
                    textChanged = true;
                }
            }

        }
        /**
         * przetwarzamy dzieci danego elementuy (rekurencja)
         */
        if (element.children().select("*").size() > 0) {
            for (Element el : element.children().select("*")) {
                processChildren(el);
            }
        }
    }

    /**
     * metoda zamykajaca socket
     */
    public void close() {
        try {
            in.close();
        } catch (Exception e) {
        }
    }

}
