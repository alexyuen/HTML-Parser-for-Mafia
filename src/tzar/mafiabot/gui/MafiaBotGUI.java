package tzar.mafiabot.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.miginfocom.swing.MigLayout;
import tzar.mafiabot.engine.Parser;


public class MafiaBotGUI extends JFrame {

	private static final long serialVersionUID = 1L;

	private static File cacheFile;

	
	private static JPanel contentPanel;
	private static final JLabel titleLabel = new JLabel("MafiaBot");
	private static final JLabel helpLabel = new JLabel("");
	private static final JTextField urlField = new JTextField();
	private static final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
	private static final JProgressBar progressBar = new JProgressBar();
	private static final JButton btnStop = new JButton("Stop");
	private static final JButton btnReparse = new JButton("Reparse Thread");
	private static final JButton btnDeleteCacheAndReparse = new JButton("Delete Cache & Reparse Thread");
	
	private static boolean canSpecifyThread = false;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		MafiaBotGUI gui = new MafiaBotGUI();
		gui.maximize();
		if (!canSpecifyThread) {
			urlField.setBackground(new Color(240,240,240));
			urlField.setBorder(javax.swing.BorderFactory.createEmptyBorder());
			gui.reparse();
		} else {
			stop();
		}
	}

	private void maximize() {
		setExtendedState(MAXIMIZED_BOTH);
		setVisible(true);
	}

	private void parse() {
		new Thread() {
			public void run() {
				// TRS
				//String thread = "http://www.mlponies.com/forums/viewtopic.php?f=13&t=714"; // Glaed's game
				//String thread = "http://www.mlponies.com/forums/viewtopic.php?f=13&t=961"; // WW's game
				//String thread = "http://www.mlponies.com/forums/viewtopic.php?f=13&t=1045"; // Chunky's game
				//String thread = "http://www.mlponies.com/forums/viewtopic.php?f=13&t=1429"; // Grilox's game
				//String thread = "http://www.mlponies.com/forums/viewtopic.php?f=6&t=1647"; // Westy's game
				//String thread = "http://www.mlponies.com/forums/viewtopic.php?f=6&t=2151";	// Chunky's CYOR
				//String thread = "http://www.roundstable.com/forums/viewtopic.php?f=6&t=3158"; // Sharkmafia's game
				
				// BHP
				//String thread = "http://www.bluehellproductions.com/forum/index.php?showtopic=25403"; // Glaed's game
				//String thread = "http://www.bluehellproductions.com/forum/index.php?showtopic=25548"; // Westy's game
				//String thread = "https://www.bluehellproductions.com/forum/index.php?showtopic=25603"; // Nodlied's game
				
				// ELP
				String thread = "http://eridanipony.com/viewtopic.php?f=30&t=3316"; // Hylius's game
				//String thread = "http://eridanipony.com/viewtopic.php?f=30&t=3415"; // Westy's game

				if (!canSpecifyThread) {
					urlField.setText(thread);
				} else {
					thread = urlField.getText();
				}

				cacheFile = new File("MafiaBot-" + thread.hashCode() + ".cache");
				Parser bot = new Parser(thread);
				bot.start();
			}
		}.start();
	}

	public void reparse() {
		tabbedPane.removeAll();
		setPhase("Day 0");
		setProgress(0);
		if (canSpecifyThread) {
			if (urlField.getText().contains("URL") || urlField.getText().trim().isEmpty()) {
				urlField.setBackground(Color.YELLOW);
				return;
			} else {
				urlField.setBackground(Color.WHITE);
			}
		}
		btnStop.setText("Stop");
		btnStop.setEnabled(true);
		btnReparse.setEnabled(false);
		urlField.setEditable(false);
		parse();
	}

	/**
	 * Create the frame.
	 */
	public MafiaBotGUI() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) { }
		setVisible(false);
		setTitle("MafiaBot by Tzar469");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 503, 558);
		contentPanel = new JPanel();
		contentPanel.setForeground(Color.LIGHT_GRAY);
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPanel);
		contentPanel.setLayout(new MigLayout("insets 0", "[477px,grow]", "[35px][23px][330px,grow][23px][23px][23px][23px]"));
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		titleLabel.setFont(new Font("Papyrus", Font.PLAIN, 21));
		contentPanel.add(titleLabel, "flowx,cell 0 0,grow");
		helpLabel.setToolTipText("Help");
		helpLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				try {
					Desktop d = Desktop.getDesktop();
					d.browse(new URI("https://docs.google.com/document/d/1URv0ozD0kBYfDm3SmbY8_ceiXnIe_ofBzWlC_pQQ08c/pub"));
				} catch (Exception ignored) { }
			}
		});
		helpLabel.setIcon(new ImageIcon(MafiaBotGUI.class.getResource("/javax/swing/plaf/metal/icons/ocean/question.png")));
		
		contentPanel.add(helpLabel, "cell 0 0");
		urlField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				try {
					if (!canSpecifyThread) {
						Desktop d = Desktop.getDesktop();
						d.browse(new URI(urlField.getText()));
					}
				} catch (Exception ignored) { }
			}
		});
		urlField.setFont(new Font("Palatino Linotype", Font.PLAIN, 12));
		urlField.setBackground(Color.WHITE);
		urlField.setText("Paste URL of thread here, then press Enter key.");
		urlField.setEditable(canSpecifyThread);
		urlField.setHorizontalAlignment(SwingConstants.CENTER);
		urlField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent key) {
				if (key.getKeyCode() == KeyEvent.VK_ENTER) {
					reparse();
				}
			}
		});
		contentPanel.add(urlField, "cell 0 1,growx,aligny center");
		contentPanel.add(tabbedPane, "cell 0 2,grow");
		progressBar.setStringPainted(true);
		contentPanel.add(progressBar, "cell 0 3,growx,aligny center");
		btnStop.setEnabled(false);
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stop();
			}
		});
		contentPanel.add(btnStop, "cell 0 4,growx,aligny center");
		btnReparse.setEnabled(false);
		btnReparse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				reparse();
			}
		});
		contentPanel.add(btnReparse, "cell 0 5,growx,aligny center");
		btnDeleteCacheAndReparse.setEnabled(false);
		btnDeleteCacheAndReparse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (cacheFile != null && cacheFile.delete()) {
					btnDeleteCacheAndReparse.setEnabled(false);
				}
				reparse();
			}
		});
		contentPanel.add(btnDeleteCacheAndReparse, "cell 0 6,growx,aligny center");

		redirectSystemStreams();
	}

	public static void setPhase(final String phase) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JTextArea errorTextArea = new JTextArea();
				errorTextArea.setFont(new Font("Courier New", Font.PLAIN, 12));
				errorTextArea.append("Debugging & Error Log (" + phase + "):\n");
				errorTextArea.setEditable(false);

				JTextArea outputTextArea = new JTextArea();
				outputTextArea.setFont(new Font("Courier New", Font.PLAIN, 12));
				outputTextArea.append("Action log (" + phase + "):\n");
				//outputTextArea.setEditable(false);

				JSplitPane splitPane = new JSplitPane();
				splitPane.setLeftComponent(new JScrollPane(errorTextArea));
				splitPane.setRightComponent(new JScrollPane(outputTextArea));
				splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

				splitPane.setContinuousLayout(true);
				splitPane.setOneTouchExpandable(true);
				splitPane.setDividerLocation(0.0d);
				tabbedPane.addTab(phase, splitPane);
				if (tabbedPane.getSelectedIndex() == tabbedPane.getTabCount() - 2)
					tabbedPane.setSelectedComponent(splitPane);
			}
		});
	}

	public static void stop() {
		//getTextArea(false).setCaretPosition(0);
		//btnStop.setText("Save action log");
		btnStop.setEnabled(false);
		btnReparse.setEnabled(true);

		if (canSpecifyThread) {
			urlField.setEditable(true);
		}

		if (cacheFile != null && cacheFile.exists()) {
			btnDeleteCacheAndReparse.setEnabled(true);
		}
	}

	public static boolean isStopped() {
		// mutual exclusion: the reparse button is only enabled when the parsing has stopped
		return btnReparse.isEnabled();
	}

	public static void setProgress(int progress) {
		progressBar.setValue(progress);
	}

	// make sure gui is stopped before calling this method
	private static void saveActionLog() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// initialize the file chooser to the current directory, set to text files only, and give default file name
				File currentDirectory = new File(System.getProperty("user.dir"));
				JFileChooser fileChooser = new JFileChooser(currentDirectory);
				fileChooser.setSelectedFile(new File("MafiaBot Action Log.txt"));
				FileFilter filter = new FileNameExtensionFilter("Text Documents (*.txt)", "txt");
				fileChooser.setFileFilter(filter);
				fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);

				// let the user choose where to save to, and prompt to overwrite if the file already exists
				File saveToFile = null;
				while (saveToFile == null) {
					// show the save dialog
					int saveDialogResult = fileChooser.showSaveDialog(null);
					if (saveDialogResult == JFileChooser.APPROVE_OPTION) {
						File selectedFile = fileChooser.getSelectedFile();
						String fileName = selectedFile.getName();
						if (!fileName.matches(".*\\.txt$")) {
							fileName += ".txt";
							selectedFile = new File(selectedFile.getParentFile(), fileName);
						}
						if (selectedFile.exists()) {
							int response = JOptionPane.showConfirmDialog(null,
									"The file \"" + selectedFile.getName() + 
									"\" already exists. Do you want to replace it?",
									"Ovewrite file", JOptionPane.YES_NO_OPTION,
									JOptionPane.WARNING_MESSAGE);
							if (response == JOptionPane.NO_OPTION) {
								continue;
							}
						}
						saveToFile = selectedFile;
					} else {
						return;
					}
				}
				// write the log to the file
				FileWriter out = null;
				try {
					out = new FileWriter(saveToFile, false);
					for (Component c : tabbedPane.getComponents()) {
						JSplitPane splitPane = (JSplitPane) c;
						JScrollPane scrollPane = (JScrollPane) splitPane.getRightComponent();
						JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();
						textArea.write(out);
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (out != null) {
						try {
							out.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});
	}

	private JTextArea getTextArea(boolean errorPane) {
		JSplitPane splitPane = (JSplitPane) tabbedPane.getComponentAt(tabbedPane.getTabCount() - 1);
		JScrollPane scrollPane = (JScrollPane) (errorPane ? splitPane.getLeftComponent() : splitPane.getRightComponent());
		return (JTextArea) scrollPane.getViewport().getView();
	}

	private void redirectSystemStreams() {  
		OutputStream out = new JTextAreaOutputStream(false);
		OutputStream error = new JTextAreaOutputStream(true);
		System.setOut(new PrintStream(out, true));  
		System.setErr(new PrintStream(error, true));  
	}  

	private void updateTextArea(final String text, final boolean isError) {  
		SwingUtilities.invokeLater(new Runnable() {  
			public void run() {  
				getTextArea(isError).append(text);  
			}  
		});
	}

	private class JTextAreaOutputStream extends OutputStream {
		boolean isErrorStream = false;

		private JTextAreaOutputStream(boolean isErrorStream) {
			this.isErrorStream = isErrorStream;
		}

		@Override  
		public void write(int b) throws IOException {  
			updateTextArea(String.valueOf((char) b), isErrorStream);  
		} 
		@Override
		public void write(byte[] b) throws IOException {  
			write(b, 0, b.length);  
		}
		@Override  
		public void write(byte[] b, int off, int len) throws IOException {  
			updateTextArea(new String(b, off, len), isErrorStream);  
		}  

	}

}
