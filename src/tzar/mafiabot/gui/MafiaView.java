package tzar.mafiabot.gui;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import net.miginfocom.swing.MigLayout;
import tzar.mafiabot.engine.Parser;


@SuppressWarnings("serial")
public class MafiaView extends JFrame {

	private File cacheFile;

	private final JPanel contentPanel = new JPanel();
	private final JLabel titleLabel = new JLabel("MafiaBot");
	private final JLabel helpLabel = new JLabel("");
	private final JTextField urlField = new JTextField();
	private final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
	private final JProgressBar progressBar = new JProgressBar();
	private final JButton btnStop = new JButton("Stop");
	private final JButton btnReparse = new JButton("Reparse Thread");
	private final JButton btnDeleteCacheAndReparse = new JButton("Delete Cache & Reparse Thread");

	private static boolean canSpecifyThread = false;

	// TRS
	//String thread = "http://www.mlponies.com/forums/viewtopic.php?f=13&t=714"; // Glaed's game
	//String thread = "http://www.mlponies.com/forums/viewtopic.php?f=13&t=961"; // WW's game
	//String thread = "http://www.mlponies.com/forums/viewtopic.php?f=13&t=1045"; // Chunky's game
	//String thread = "http://www.mlponies.com/forums/viewtopic.php?f=13&t=1429"; // Grilox's game
	//String thread = "http://www.mlponies.com/forums/viewtopic.php?f=6&t=1647"; // Westy's game
	//String thread = "http://www.mlponies.com/forums/viewtopic.php?f=6&t=2151";	// Chunky's CYOR
	//String thread = "http://www.roundstable.com/forums/viewtopic.php?f=6&t=3158"; // Sharkmafia's game (no bot support)

	// BHP
	//String thread = "http://www.bluehellproductions.com/forum/index.php?showtopic=25403"; // Glaed's game
	//String thread = "http://www.bluehellproductions.com/forum/index.php?showtopic=25548"; // Westy's game
	//String thread = "https://www.bluehellproductions.com/forum/index.php?showtopic=25603"; // Nodlied's game
	String thread = "https://secure.w3dhub.com/forum/index.php?s=f40a222bdf2d20ddcd14f862b482d759&showtopic=415598";

	// ELP
	//String thread = "http://eridanipony.com/viewtopic.php?f=30&t=3316"; // Hylius's game
	//String thread = "http://eridanipony.com/viewtopic.php?f=30&t=3415"; // Westy's game

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		MafiaView gui = new MafiaView();
		gui.maximize();
		if (!canSpecifyThread) {
			gui.reparse();
		} else {
			gui.parseCompleted();
		}
	}

	/**
	 * Create the frame.
	 */
	public MafiaView() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) { }
		setVisible(false);
		setTitle("MafiaBot by Tzar469");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 505, 560);
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
		helpLabel.setIcon(new ImageIcon(MafiaView.class.getResource("/javax/swing/plaf/metal/icons/ocean/question.png")));

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
				parseCompleted();
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

		if (!canSpecifyThread) {
			urlField.setBackground(new Color(240,240,240));
			urlField.setBorder(javax.swing.BorderFactory.createEmptyBorder());
		}
		redirectSystemStreams();
		
	}
	
	private void maximize() {
		setExtendedState(MAXIMIZED_BOTH);
		setVisible(true);
	}

	private void reparse() {
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

	private void parse() {
		new Thread() {
			public void run() {
				if (!canSpecifyThread) {
					urlField.setText(thread);
				} else {
					thread = urlField.getText();
				}

				cacheFile = new File("MafiaBot-" + thread.hashCode() + ".cache");
				Parser bot = new Parser(thread, MafiaView.this);
				if (!isStopped())
					bot.start();
			}
		}.start();
	}

	public void setProgress(int progress) {
		progressBar.setValue(progress);
	}

	public void setPhase(final String phase) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JTextPane errorPane = new JTextPane();
				errorPane.setEditable(false);

				StyledDocument errorDoc = errorPane.getStyledDocument();
				addStyles(errorDoc);
				insertStyledString(errorDoc, "bold", "Debugging & Error Log (" + phase + "):\n");


				JTextPane outputTextPane = new JTextPane();
				//outputTextPane.setDragEnabled(true);
				makeUndoable(outputTextPane);
				//outputTextArea.setEditable(false);

				StyledDocument outputDoc = outputTextPane.getStyledDocument();
				addStyles(outputDoc);
				insertStyledString(outputDoc, "bold", "Action log (" + phase + "):\n");

				JSplitPane splitPane = new JSplitPane();
				splitPane.setLeftComponent(new JScrollPane(errorPane));
				splitPane.setRightComponent(new JScrollPane(outputTextPane));
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

	public void parseCompleted() {
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

	public boolean isStopped() {
		// mutual exclusion: the reparse button is only enabled when the parsing has stopped
		return btnReparse.isEnabled();
	}

	private void makeUndoable(JTextComponent textComponent) {
		// code from: http://stackoverflow.com/a/12030993
		final UndoManager undoManager = new UndoManager();
		Document doc = textComponent.getDocument();
		doc.addUndoableEditListener(new UndoableEditListener() {
			@Override
			public void undoableEditHappened(UndoableEditEvent e) {
				//System.out.println("Add edit");
				undoManager.addEdit(e.getEdit());
			}
		});

		InputMap im = textComponent.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap am = textComponent.getActionMap();

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "Undo");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "Redo");

		am.put("Undo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (undoManager.canUndo()) {
						undoManager.undo();
					}
				} catch (CannotUndoException exp) {
					exp.printStackTrace();
				}
			}
		});
		am.put("Redo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (undoManager.canRedo()) {
						undoManager.redo();
					}
				} catch (CannotUndoException exp) {
					exp.printStackTrace();
				}
			}
		});
	}

	private void addStyles(StyledDocument doc) {
		//Initialize some styles.
		Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

		Style regular = doc.addStyle("regular", def);
		StyleConstants.setFontFamily(def, "Courier New");
		StyleConstants.setFontSize(def, 12);

		Style s = doc.addStyle("italic", regular);
		StyleConstants.setItalic(s, true);

		s = doc.addStyle("bold", regular);
		StyleConstants.setBold(s, true);

		s = doc.addStyle("small", regular);
		StyleConstants.setFontSize(s, 10);

		s = doc.addStyle("large", regular);
		StyleConstants.setFontSize(s, 16);

		// style for JButton
		/*
		s = doc.addStyle("button", regular);
        StyleConstants.setAlignment(s, StyleConstants.ALIGN_CENTER);
        ImageIcon soundIcon = createImageIcon("images/sound.gif",
                                              "sound icon");
        JButton button = new JButton();
        if (soundIcon != null) {
            button.setIcon(soundIcon);
        } else {
            button.setText("BEEP");
        }
        button.setCursor(Cursor.getDefaultCursor());
        button.setMargin(new Insets(0,0,0,0));
        button.setActionCommand(buttonString);
        button.addActionListener(this);
        StyleConstants.setComponent(s, button);
		 */
	}

	private void insertStyledString(StyledDocument doc, String style, String str) {
		try {
			doc.insertString(doc.getLength(), str, doc.getStyle(style));
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	private void redirectSystemStreams() {  
		OutputStream out = new JTextComponentOutputStream(false);
		OutputStream error = new JTextComponentOutputStream(true);
		System.setOut(new PrintStream(out, true));  
		System.setErr(new PrintStream(error, true));  
	}  

	private class JTextComponentOutputStream extends OutputStream {
		boolean isErrorStream = false;

		private JTextComponentOutputStream(boolean isErrorStream) {
			this.isErrorStream = isErrorStream;
		}

		@Override  
		public void write(int b) throws IOException {  
			updateTextComponent(String.valueOf((char) b), isErrorStream);  
		} 
		@Override
		public void write(byte[] b) throws IOException {  
			write(b, 0, b.length);  
		}
		@Override  
		public void write(byte[] b, int off, int len) throws IOException {  
			updateTextComponent(new String(b, off, len), isErrorStream);  
		}

		// methods to select the right text component
		private void updateTextComponent(final String text, final boolean isError) {  
			SwingUtilities.invokeLater(new Runnable() {  
				public void run() {  
					insertStyledString(getTextComponent(isError).getStyledDocument(), "regular", text);  
				}  
			});
		}

		private JTextPane getTextComponent(boolean errorPane) {
			JSplitPane splitPane = (JSplitPane) tabbedPane.getComponentAt(tabbedPane.getTabCount() - 1);
			JScrollPane scrollPane = (JScrollPane) (errorPane ? splitPane.getLeftComponent() : splitPane.getRightComponent());
			return (JTextPane) scrollPane.getViewport().getView();
		}

	}

}
