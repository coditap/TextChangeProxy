package main.java;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.charset.Charset;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

public class proxyWindow extends javax.swing.JFrame {

	private JPanel bottomPanel; // dolny panel
	private JScrollPane tableScrollPane;
	private JPanel topPanel; // gorny panel
	private JTextField wordToChange; // pole tekstowe do wprowadzania slowa do zamiany
	private JTextField wordReplacement; // pole tekstowe do wprowadzania nowego slowa
	private JButton addButton; // przycisk dodawania
	private JButton startButton; // przycisk uruchomiania proxy
	private static final Object dictionaryColumnNames[] = { "bazowe", "zmienione" }; // nazwy kolumn w tabelce
	private boolean proxyRunning = false;

	/**
	 * konstuktor
	 */
	public proxyWindow() {
		WordReplacementDictionary.replacementMap = new HashMap<>(); // inicjalizujemy mape do przechowywania slow do zamiany
		initComponents(); // wywolujemy inicjalizacje komponentow widoku
	}

	/**
	 * metoda inicjalizujaca komponenty widoku
	 */
	private void initComponents() {

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE); // ustawiamy wylaczenie aplikacji po zamknieciu oknba
		setMinimumSize(new Dimension(400, 500)); // ustawiamy minimalny rozmiar okna na 400x500
		setTitle("Proxy Zamiana Tekstu - Marek"); // ustawiamy tytul okienka

		/**
		 * tworzymy obiekty
		 */
		bottomPanel = new JPanel();
		topPanel = new JPanel();
		wordToChange = new JTextField();
		wordReplacement = new JTextField();
		addButton = new JButton();
		startButton = new JButton();
		/**
		 * ustawiamy tekst na elementach widoku
		 */
		wordToChange.setText("slowo do zamiany");
		wordReplacement.setText("nowe slowo");
		addButton.setText("dodaj");
		startButton.setText("uruchom proxy");
		/**
		 * dodajemy listener - akcja po wcisnieciu przycisku
		 */
		addButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				addNewWord(); // po wywolaniu akcji na przycisku uruchomiana zostanie metoda "addNewWord()"
			}
		});
		startButton.addActionListener(new ActionListener() {
			// po wcisnieciu start button odpalamy proxy
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!proxyRunning) {
					proxyRunning = true;
					ProxyServer server = new ProxyServer(); // uruchomienie serwera proxy
					server.run();

				}
			}

		});

		getContentPane().setLayout(new GridBagLayout());// dla glownego panelu okna ustawiamy layout typu "Grid bag layout"
		/**
		 * definijemy restrykcje widoku
		 */
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.FIRST_LINE_START; // rozmieszczanie zaczete od gory
		gbc.fill = GridBagConstraints.BOTH; // automatyczne rozciaganie
		gbc.gridx = 0; // zerowa kolumna
		gbc.gridy = 0; // zerowy wiersz
		gbc.weightx = 1; // 100% szerokosci
		gbc.weighty = 0.2; // 20% wysokosci
		getContentPane().add(topPanel, gbc); // dodajemy gorny panel z powyzszymi restrykcjami
		gbc.gridy = 1; // przechodzimy do pierwszego wiersza
		gbc.weighty = 0.6; // 60% wysokosci
		getContentPane().add(bottomPanel, gbc); // dodajemy dolny panel z powyzszymi restrykcjami
		gbc.gridy = 2; // przechodzimy do 2 wiersza
		gbc.weighty = 0.2; // 20% wysokosci
		getContentPane().add(startButton, gbc);// dodajemy przycisk uruchomiana proxy

		topPanel.setLayout(new GridLayout(1, 3)); // dla gornego panelu ustalamy layout - 1 wiersz, 3 kolumny
		bottomPanel.setLayout(new GridLayout(1, 1)); // dla dolnego 1 wiersz, 1 kolumna
		/**
		 * do gornego panelu dodajemy pola tekstowe i przycisk
		 */
		topPanel.add(wordToChange);
		topPanel.add(wordReplacement);
		topPanel.add(addButton);

		pack(); // kazemy ramce si� przerysowa�
	}

	/**
	 * metoda dodajaca nowe slowo do slownika
	 */
	private void addNewWord() {
		WordReplacementDictionary.replacementMap.put(wordToChange.getText(), wordReplacement.getText()); // do mapy dodajemy wartosci z pol tekstowych z widoku
		refreshDictionaryView(); // odswiezamy widok tabelki ze slownikiem
	}

	/**
	 * odswiezenie widoku slownika zamiany slow
	 */
	private void refreshDictionaryView() {
		bottomPanel.setPreferredSize(bottomPanel.getSize()); // ustawiamy prefereowany rozmiar na aktualny, po to zeby scrolle sie pojawily
		Object dictionaryWords[][] = new Object[WordReplacementDictionary.replacementMap.size()][2]; // tworzymy tablice z danymi
		int index = 0; // zmienna pomocnicza do iterowania do calej tablicy
		for (String wordToChange : WordReplacementDictionary.replacementMap.keySet()) { // iterujemy po wszystkich slowach ze slownika
			Object row[] = new Object[2];
			row[0] = wordToChange; // w pierwszej kolumnie ustawiamy slowo
			row[1] = WordReplacementDictionary.replacementMap.get(wordToChange); // w drugiej jego odpowiednik
			dictionaryWords[index++] = row; // dodaemy do tablicy z danymi stworzony wiersz
		}
		bottomPanel.removeAll(); // usuwamy poprzednie tabelki z panelu
		JTable dictionaryViewTable = new JTable(dictionaryWords, dictionaryColumnNames); // tworzymy tabelke
		tableScrollPane = new JScrollPane(dictionaryViewTable); // tworzymy nowy scroll pane z tabelka
		bottomPanel.add(tableScrollPane); // dodajemy scroll pane do dolnego panelu
		pack(); // kazemy ramce sie przerysowac

	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		
		
    	//System.setProperty("file.encoding", "UTF-8");
    	System.out.println(Charset.defaultCharset());
		/**
		 * ustawiamy ladniejsza skorke
		 */
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(proxyWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(proxyWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(proxyWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(proxyWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}

		/**
		 * uruchamiamy okienko
		 */
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new proxyWindow().setVisible(true);
			}
		});

	}

}
