/*
 * Copyright (C) 2016 Federico Dossena
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dosse.stickynotes;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;

/**
 * This class coordinates all the notes. It contains code to load, save, create,
 * delete. It also performs autosaving and initializes the UI.
 *
 * @author Federico
 */
public class Main {
	private static final ArrayList<Note> notes = new ArrayList<>(); // currently open notes
	//These variables will contain the paths to the files used by the application, initialized below
	private static final String STORAGE_PATH, BACKUP_PATH, BACKUP2_PATH, LOCK_PATH;
	//If set to true, an empty note will not be created if the app is started on an empty storage. enabled by the -autostartup parameter
	private static boolean noAutoCreate = false;
	private static ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	
	public static final float SCALE = calculateScale(); // used for DPI scaling. multiply each size by this factor.
	public static final float TEXT_SIZE = 12f * SCALE, TEXT_SIZE_SMALL = 11f * SCALE, BUTTON_TEXT_SIZE = 11f * SCALE; // default scaling
	public static final Font BASE_FONT = loadFont("/fonts/OpenSans-Regular.ttf").deriveFont(TEXT_SIZE),
			SMALL_FONT = BASE_FONT.deriveFont(TEXT_SIZE_SMALL),
			BUTTON_FONT = loadFont("/fonts/OpenSans-Bold.ttf").deriveFont(BUTTON_TEXT_SIZE),
			BUTTON_BIG_FONT = BUTTON_FONT.deriveFont(BUTTON_TEXT_SIZE*1.3f);

	private static final ColorUIResource METAL_PRIMARY1 = new ColorUIResource(220, 220, 220),
			METAL_PRIMARY2 = new ColorUIResource(220, 220, 220), METAL_PRIMARY3 = new ColorUIResource(220, 220, 220),
			METAL_SECONDARY1 = new ColorUIResource(240, 240, 240),
			METAL_SECONDARY2 = new ColorUIResource(240, 240, 240),
			DEFAULT_BACKGROUND = new ColorUIResource(255, 255, 255);
	
	public static BufferedImage LOGO; // not final because of compiler bs
	public static final BufferedImage NULL_IMAGE; // empty Image, used for errors XXX kindof a dirty way to handle errors isn't it?

	static {
		String os = System.getProperty("os.name").toLowerCase();
		String home = "";
		
		try {
			if (os.startsWith("win")) {
				if (os.contains("xp")) { // on windows xp, we use %appdata%\NoteBot
					home = System.getenv("APPDATA") + "\\NoteBot\\";
				} else { // on newer windows, we use %userprofile%\AppData\Local\NoteBot
					home = System.getProperty("user.home") + "\\AppData\\Local\\NoteBot\\";
				}
			} else home = System.getProperty("user.home") + "/.notebot/"; // on other systems, we use ~/.notebot
			
			// check if the folder exists: if it doesn't exist, create it; if a file already exists with that name, use fallback paths
			File f = new File(home);
			if (f.exists()) {
				if (!f.isDirectory()) throw new Exception();
			} else {
				Path p = Paths.get(home);
				Files.createDirectories(p);
			}
		} catch (Exception e) {
			// fallback path, local folder and hope for the best
			home = "";
			System.out.println("Couldn't find nice storage folder, falling back to local folder");
		}
		STORAGE_PATH = home + "sticky.dat"; // main storage
		BACKUP_PATH = home + "sticky.dat.bak"; // backup in case main storage is corrupt
		BACKUP2_PATH = home + "sticky.dat.bak.2"; // temp path for previous backup while current one is being backed up
		LOCK_PATH = home + "lock"; // lock file to prevent multiple instances of StickyNotes to run on the same storage
		
		NULL_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		NULL_IMAGE.setRGB(0, 0, 0);
		
		try { LOGO = ImageIO.read(Main.class.getResource("/icon.png")); }
		catch (Exception ex) {
			System.err.println("Something went wrong while loading the logo");
			ex.printStackTrace();
			LOGO = NULL_IMAGE;
		}
	}
	
	
	public static void main(String args[]) {
		if (!Desktop.isDesktopSupported()) {
			System.out.println("This application doesn't work on headless systems.");
			System.exit(1);
		}
		
		if (alreadyRunning()) { // if the app is already running, it terminates the current instance
			System.out.println("Dying because I exist");
			System.exit(1);
		}
		
		/*
		 * If the app is started with the -autostartup flag, it doesn't create an empty note 
		 * (on windows the app is run when the system starts and it would be silly to create a new note when the system boots)
		 */
		noAutoCreate = (args.length == 1 && args[0].equalsIgnoreCase("-autostartup"));
		
		//Apply swing MetalTheme, scroll down and ignore
		// <editor-fold defaultstate="collapsed" desc="MetalTheme">
		MetalLookAndFeel.setCurrentTheme(new MetalTheme() {
			private final FontUIResource REGULAR_FONT = new FontUIResource(Main.BASE_FONT),
					 					 SMALL_FONT = new FontUIResource(Main.SMALL_FONT);
			
			@Override protected ColorUIResource getPrimary1() { return METAL_PRIMARY1; }
			@Override protected ColorUIResource getPrimary2() { return METAL_PRIMARY2; }
			@Override protected ColorUIResource getPrimary3() { return METAL_PRIMARY3; }
			@Override protected ColorUIResource getSecondary1() { return METAL_SECONDARY1; }
			@Override protected ColorUIResource getSecondary2() { return METAL_SECONDARY2; }
			@Override protected ColorUIResource getSecondary3() { return DEFAULT_BACKGROUND; }
			@Override public String getName() { return "Metal Theme"; }
			@Override public FontUIResource getControlTextFont() { return REGULAR_FONT; }
			@Override public FontUIResource getSystemTextFont() { return REGULAR_FONT; }
			@Override public FontUIResource getUserTextFont() { return REGULAR_FONT; }
			@Override public FontUIResource getMenuTextFont() { return SMALL_FONT; }
			@Override public FontUIResource getWindowTitleFont() { return REGULAR_FONT; }
			@Override public FontUIResource getSubTextFont() { return REGULAR_FONT; }
		});
		
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
		} catch (ReflectiveOperationException e) {
			System.out.println("WARNING: Metal look and feel doesn't exist on this JRE for some reason");
		} catch (UnsupportedLookAndFeelException e) {
			System.out.println("System doesn't support Metal look and feel");
		}
		// </editor-fold>
		
		if (!loadState()) if (!noAutoCreate) newNote();
		saveState();
		
		//If there are no saved notes and none were created automatically (-autostartup flag), close the app
		if (notes.isEmpty()) {
			System.out.println("No saved notes and -autostartup flag meaning my life is pointless. Goodbye :)");
			System.exit(0);
		}
		
		
		if (SystemTray.isSupported()) {
			int trayIconWidth = new TrayIcon(LOGO).getSize().width;
			TrayIcon tray = new TrayIcon(LOGO.getScaledInstance(trayIconWidth, -1, Image.SCALE_SMOOTH));
			
			tray.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					for (Note n : notes) n.toFront();
				}
			});
			
			try { SystemTray.getSystemTray().add(tray); }
			catch (AWTException e) { throw new Error("The system-tray isn't supported but it passed the SystemTray.isSupported test", e); }
		} else System.out.println("No system-tray supported, that's gonna make things harder for the user (not that I really care)");
		
		
		//This thread autosaves the notes every 60 seconds
		new Thread() {
			@Override
			public void run() {
				setPriority(Thread.MIN_PRIORITY);
				while (true) try {
					sleep(60000L);
					synchronized (notes) {
						if (notes.isEmpty()) {
							return;
						}
						saveState();
					}
				} catch (InterruptedException e) { e.printStackTrace(); }
			}
		}.start();
		
		//This thread checks the notes every 1 second to make sure they're still inside the screen (in case the resolution changes) CHECK performance hit? -> seperate rescue method
		new Thread() {
			@Override
			public void run() {
				setPriority(Thread.MIN_PRIORITY);
				while (true) try {
					sleep(1000L);
					synchronized (notes) {
						if (notes.isEmpty()) return;
						for (Note n : notes) n.setLocation(n.getPreferredLocation());
					}
				} catch (InterruptedException e) { e.printStackTrace(); }
			}
		}.start();
	}
	
	
	/**
	 * Saves currently open notes to the main storage, and turns the previous
	 * storage to the backup storage.
	 *
	 * FORMAT: The .dat file will hold data as serialized objects. A Float contains
	 * the SCALE at which the save was made, then an Integer contains the number of
	 * notes, then for each note we have its location (Point), its size (Dimension),
	 * the color scheme (Color[8]), the text (String). Finally, a float for each
	 * note with the text scale for that note. This data is written at the end to
	 * ensure compatibility with older versions of the program.
	 *
	 * errors are ignored.
	 */
	public static void saveState() {
		synchronized (notes) {
			File st = new File(STORAGE_PATH);
			File bk = new File(BACKUP_PATH);
			File bkTemp = new File(BACKUP2_PATH);
			if (bkTemp.exists()) bkTemp.delete();
			if (bk.exists()) bk.renameTo(bkTemp);
			if (bkTemp.exists()) bkTemp.delete();
			if (st.exists()) st.renameTo(bk);
			st = new File(STORAGE_PATH);
			
			try (FileOutputStream fos = new FileOutputStream(st);
				 ObjectOutputStream oos = new ObjectOutputStream(fos)) {
				
				oos.writeObject(SCALE);
				oos.writeObject(notes.size());
				for (Note n : notes) {
					oos.writeObject(n.getPreferredLocation());
					oos.writeObject(n.getSize());
					oos.writeObject(n.getColorScheme());
					oos.writeObject(n.getText());
				}
				// text scales are written at the end of the file for backwards compatibility
				for (Note n : notes) oos.writeObject(n.getTextScale());
				oos.flush();
			} catch (IOException e) {
				System.err.println("IOException while saving state");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Attempts to load the notes in the specified storage. notes loaded from the
	 * file will also be adapted to the current screen DPI
	 *
	 * @param f
 *            	storage
	 * @return true if loading was successful, false if it was unsuccessful (file
	 *         not found or corrupt). if the storage is loaded correctly but there
	 *         are no notes inside it, it returns true and creates a new empty note
	 *         unless noAutoCreate is set to true, in which case it returns true and
	 *         does nothing
	 */
	private static boolean attemptLoad(File f) {
		synchronized (notes) {
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));) {
				float savScale = (Float) (ois.readObject());
				float scaleMul = SCALE / savScale;
				int n = (Integer) (ois.readObject());
				
				if (n == 0) {
					if (!noAutoCreate) {
						Note note = new Note();
						note.setVisible(true);
						notes.add(note);
					}
					return true;
				}
				
				if (n < 0)  return false;
				
				for (int i = 0; i < n; i++) {
					Note note = new Note();
					
					Point p = (Point) (ois.readObject());
					note.setLocation(p);
					
					Dimension d = (Dimension) (ois.readObject());
					d.height *= scaleMul;
					d.width *= scaleMul;
					note.setSize(d);
					
					note.setColorScheme((Color[]) (ois.readObject()));
					note.setText((String) (ois.readObject()));
					notes.add(note);
				}

				//Triggers an OptionalDataException when loading a save-file from an older version
				for (int i = 0; i < n; i++) notes.get(i).setTextScale((Float) (ois.readObject()));
				
				for (Note note : notes) note.setVisible(true);
			} catch (OptionalDataException e) {
				System.out.println("WARNING: Invalid save format, most likely caused by the loading of an older .dat format");
				for (Note n : notes) n.dispose();
				notes.clear();
				return false;
			} catch (ClassNotFoundException e) {
				System.err.println("ERROR: Incompatible save file format");
				for (Note n : notes) n.dispose();
				notes.clear();
				return false;
			} catch (IOException e) {
				System.err.println("I/O Exception while loading .dat save");
				for (Note n : notes) n.dispose();
				notes.clear();
				return false;
			}

			return true;
		}
	}
	
	/**
	 * Load notes from storage (main or backup)
	 *
	 * @return true if loading was successful, false otherwise
	 */
	private static boolean loadState() {
		if (!attemptLoad(new File(STORAGE_PATH))) {
			if (!attemptLoad(new File(BACKUP_PATH))) {
				if (!attemptLoad(new File(BACKUP2_PATH))) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Creates a new empty note
	 *
	 * @return the newly created note
	 */
	public static Note newNote() {
		synchronized (notes) {
			Note n = new Note();
			n.setVisible(true);
			
			notes.add(n);
			saveState();
			return n;
		}
	}
	
	/**
	 * Deletes the specified note
	 *
	 * @param n
	 *            note to be deleted
	 */
	public static void delete(Note n) {
		synchronized (notes) {
			notes.remove(n);
			n.dispose();
			saveState();
			if (notes.isEmpty()) {
				System.out.println("0 notes on screen: exiting.");
				System.exit(0);
			}
		}
	}
	
	public static void bringToFront(Note n) {
		synchronized (notes) {
			notes.remove(n);
			notes.add(n);
		}
	}
	
	/**
	 * Checks if the application is already running by attempting to lock the lockfile
	 *
	 * @return true if the application is already running, false otherwise (in this case, the lockfile remains locked until the process is closed)
	 */
	private static boolean alreadyRunning() {
		try {
			@SuppressWarnings("resource")
			FileChannel ch = new RandomAccessFile(new File(LOCK_PATH), "rw").getChannel();
			if (ch.tryLock() != null) return false;
			else {
				ch.close();
				return true;
			}
		} catch (Exception t) { return true; }
	}
	
	public static Dimension getExtendedScreenResolution() {
		// get screen resolution, also works with multiple screens
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] screens = ge.getScreenDevices();
		Dimension s = new Dimension(0, 0);
		
		for (GraphicsDevice screen : screens) {
			Rectangle r = screen.getDefaultConfiguration().getBounds();
			r.width += r.x;
			r.height += r.y;
			if (r.width > s.width) s.width = r.width;
			if (r.height > s.height) s.height = r.height;
		}
		return s;
	}
	
	/**
	 * loads font from classpath
	 *
	 * @param pathInClasspath
	 *            path in classpath
	 * @return Font or null if it doesn't exist
	 */
	private static Font loadFont(String pathInClasspath) {
		try {
			return Font.createFont(Font.TRUETYPE_FONT, Main.class.getResourceAsStream(pathInClasspath));
		} catch (FontFormatException | IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * calculates SCALE based on screen DPI. target DPI is 80, so if DPI=80,
	 * SCALE=1. Min DPI is 64
	 *
	 * @return scale
	 */
	private static float calculateScale() {
		float dpi = Toolkit.getDefaultToolkit().getScreenResolution();
		return (dpi < 64 ? 64 : dpi) / 80f;
	}
	
	/** @author HoldYourWaffle */
	public static long calcVersionId(Class<?> cls) {
		try(InputStream fis = cls.getResourceAsStream("/"+cls.getName().replace('.', '/')+".class")) {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			int n = 0;
			byte[] buffer = new byte[8192];
			while (n != -1) {
			    n = fis.read(buffer);
			    if (n > 0) digest.update(buffer, 0, n);
			}
			return bytesToLong(Arrays.copyOf(digest.digest(), Long.BYTES));
		} catch (IOException e) {
			System.err.println("Could not load own class as stream, did java change?");
			System.exit(1);
			return -1;
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Java forgot how SHA-1 works");
			System.exit(1);
			return -1;
		}
	}
	
	public static long bytesToLong(byte[] bytes) {
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        long r = buffer.getLong();
        buffer.clear();
        return r;
    }

	/** @author HoldYourWaffle */
	public static int countNotes() { return notes.size(); }
}
