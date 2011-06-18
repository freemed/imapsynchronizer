package org.freemedsoftware.util.imapsync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import com.sun.mail.imap.IMAPFolder;

public class ImapSynchronizer {

	public static String[] FOLDER_SKIP = { "Deleted Items", "Junk E-mail",
			"Sync Issues/Conflicts", "Sync Issues/Local Failures",
			"Sync Issues/Server Failures", "Sync Issues" };

	public static String[] MAPI_FOLDERS = { "Contacts", "Tasks", "Calendar" };

	public static String[] AVAILABLE_PROTOCOLS = { "imap", "imaps" };

	public static boolean sameSizeSkip = false;

	public static boolean noMapiSync = false;

	public static boolean showProgress = false;

	public static boolean inboxOnly = false;

	public static boolean debug = false;

	/**
	 * @param args
	 */
	public static void main(String[] argv) throws Exception {
		String srcProtocol = "imaps";
		String srcHost = null;
		String srcUser = null;
		String srcPassword = null;
		String srcPort = "993";

		String destProtocol = "imaps";
		String destHost = null;
		String destUser = null;
		String destPassword = null;
		String destPort = "993";

		for (int optind = 0; optind < argv.length; optind++) {
			if (argv[optind].equals("--srchost")) {
				srcHost = argv[++optind];
			} else if (argv[optind].equals("--srcprotocol")) {
				srcProtocol = argv[++optind];
				if (!Arrays.asList(AVAILABLE_PROTOCOLS).contains(srcProtocol)) {
					System.out.println("'" + srcProtocol
							+ "' is not a valid protocol");
					System.exit(1);
				}
			} else if (argv[optind].equals("--srcusername")) {
				srcUser = argv[++optind];
			} else if (argv[optind].equals("--srcpassword")) {
				srcPassword = argv[++optind];
			} else if (argv[optind].equals("--srcport")) {
				srcPort = argv[++optind];
			} else if (argv[optind].equals("--desthost")) {
				destHost = argv[++optind];
			} else if (argv[optind].equals("--destprotocol")) {
				destProtocol = argv[++optind];
				if (!Arrays.asList(AVAILABLE_PROTOCOLS).contains(destProtocol)) {
					System.out.println("'" + destProtocol
							+ "' is not a valid protocol");
					System.exit(1);
				}
			} else if (argv[optind].equals("--destusername")) {
				destUser = argv[++optind];
			} else if (argv[optind].equals("--destpassword")) {
				destPassword = argv[++optind];
			} else if (argv[optind].equals("--destport")) {
				destPort = argv[++optind];
			} else if (argv[optind].equals("--samesizeskip")) {
				sameSizeSkip = true;
			} else if (argv[optind].equals("--nomapisync")) {
				noMapiSync = true;
			} else if (argv[optind].equals("--progress")) {
				showProgress = true;
			} else if (argv[optind].equals("--inboxonly")) {
				inboxOnly = true;
			} else if (argv[optind].equals("--debug")) {
				debug = true;
			} else if (argv[optind].equals("--")) {
				optind++;
				break;
			} else if (argv[optind].startsWith("-")) {
				printSyntax();
			} else {
				break;
			}
		}

		if (srcUser == null || srcPassword == null || srcHost == null
				|| destUser == null || destPassword == null || destHost == null) {
			printSyntax();
		}

		String pattern = "*";
		boolean recursive = true;

		// Create properties objects to set atomic parameters
		Properties srcProps = new Properties();
		srcProps.setProperty("mail.imap.port", srcPort);
		srcProps.setProperty("mail.imap.auth.plain.disable", "false");
		Properties destProps = new Properties();
		destProps.setProperty("mail.imap.port", destPort);
		destProps.setProperty("mail.imap.auth.plain.disable", "false");

		String srcUrl = srcProtocol + "://" + srcHost + ":" + srcPort;
		String destUrl = destProtocol + "://" + destHost + ":" + destPort;

		// Status to explain what we're doing
		System.out.println("Source      : " + srcUrl + " as " + srcUser
				+ " secret " + srcPassword);
		System.out.println("Destination : " + destUrl + " as " + destUser
				+ " secret " + destPassword);

		// Get a Session object for each
		Session srcSession = Session.getInstance(srcProps, null);
		srcSession.setDebug(debug);
		Session destSession = Session.getInstance(destProps, null);
		destSession.setDebug(debug);

		// Get a Store object (source)
		Store srcStore = null;
		Folder srcRf = null;
		if (srcProtocol != null) {
			srcStore = srcSession.getStore(srcProtocol);
		} else {
			srcStore = srcSession.getStore();
		}

		// Get a Store object (destination)
		Store destStore = null;
		if (destProtocol != null) {
			destStore = destSession.getStore(destProtocol);
		} else {
			destStore = destSession.getStore();
		}

		// Connect
		System.out.println("Connecting to " + srcUrl);
		srcStore.connect(srcHost, srcUser, srcPassword);
		System.out.println("Connecting to " + destUrl);
		destStore.connect(destHost, destUser, destPassword);

		// List namespace
		if (!inboxOnly) {
			srcRf = srcStore.getDefaultFolder();
		} else {
			srcRf = srcStore.getFolder("INBOX");
		}

		scanAndHandleFolder(srcRf, false, "", destStore);
		if ((srcRf.getType() & Folder.HOLDS_FOLDERS) != 0) {
			Folder[] f = srcRf.list(pattern);
			for (int i = 0; i < f.length; i++) {
				scanAndHandleFolder(f[i], recursive, "    ", destStore);
			}
		}

		srcStore.close();
		destStore.close();
	}

	public static void printSyntax() {
		System.out.println("Usage: java -jar imapsynchronizer.jar [options]");
		System.out.println("\tOptions:");
		System.out.println("\t[--srchost source hostname]");
		System.out.println("\t[--srcport source port (defaults to 993)]");
		System.out
				.println("\t[--srcprotocol source protocol (defaults to imaps)]");
		System.out.println("\t[--srcusername source username]");
		System.out.println("\t[--srcpassword source password]");
		System.out.println("\t[--desthost destination hostname]");
		System.out.println("\t[--destport destination port (defaults to 993)]");
		System.out
				.println("\t[--destprotocol destination protocol (defaults to imaps)]");
		System.out.println("\t[--destusername destination username]");
		System.out.println("\t[--destpassword destination password]");
		System.out
				.println("\t[--samesizeskip skip folders with equal number of messages]");
		System.out
				.println("\t[--nomapisync skip content copy for MAPI folders]");
		System.out.println("\t[--inboxonly only copy INBOX]");
		System.out.println("\t[--debug turn on debug mode]");
		System.out.println("\t[--progress turn on graphical progress]");
		System.exit(1);
	}

	public static void processFolder(Folder src, Folder dest) throws Exception {
		System.out.println("Checking folder " + src.getFullName());
		if (Arrays.asList(FOLDER_SKIP).contains(src.getFullName())) {
			System.out.println("Skipping folder " + src.getFullName()
					+ " present in folder skip list.");
			return;
		}

		if (noMapiSync) {
			if (Arrays.asList(MAPI_FOLDERS).contains(src.getFullName())) {
				System.out.println("Skipping MAPI folder " + src.getFullName());
				return;
			}
		}

		if (sameSizeSkip) {
			System.out.println("Checking folders for identical count");
			if (checkCountForSkip(src, dest)) {
				return;
			}
		}

		System.out.println("Reading all headers from destination");
		ArrayList<String> messageIds = hashFolder(dest);

		System.out.println("Opening source folder " + src.getFullName());
		src.open(Folder.READ_ONLY);

		System.out.println("Opening destination folder " + dest.getFullName());
		dest.open(Folder.READ_WRITE);

		int messageCount = src.getMessageCount();
		System.out.println(" -- Fetching " + messageCount + " messages");
		Message[] m = src.getMessages();
		for (int iter = 0; iter < m.length; iter++) {
			try {
				// Extract source message id from the head
				String messageId = getMessageId(m[iter]);

				// If the message exists in the destination, skip
				if (messageIds.contains(messageId)) {
					if (showProgress) {
						displayProgress(messageCount, iter + 1, "SKIP");
					} else {
						System.out.println(" ---- [" + src.getFullName()
								+ "] SKIP MESSAGE (" + (iter + 1) + "/"
								+ messageCount + ")");
					}
				} else {
					if (showProgress) {
						displayProgress(messageCount, iter + 1, "MOVE");
					} else {
						System.out.println(" ---- [" + src.getFullName()
								+ "] TRANSFER MESSAGE (" + (iter + 1) + "/"
								+ messageCount + ")");
					}
					dest.appendMessages(new Message[] { m[iter] });
				}
			} catch (Exception ex) {
				if (showProgress) {
					displayProgress(messageCount, iter + 1, "ERROR");
				} else {
					System.out.println(" ---- [" + src.getFullName()
							+ "] MESSAGE ERROR (" + (iter + 1) + "/"
							+ messageCount + ")");
				}
			}
		}
		src.close(true);
		dest.close(true);
	}

	/**
	 * Get message ids for all messages in a destination folder.
	 * 
	 * @param folder
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<String> hashFolder(Folder folder) throws Exception {
		ArrayList<String> mids = new ArrayList<String>();
		folder.open(Folder.READ_ONLY);
		String folderName = folder.getFullName();
		int messageCount = folder.getMessageCount();
		System.out.println(" -- Fetching " + messageCount + " messages");
		Message[] m = folder.getMessages();
		for (int iter = 0; iter < m.length; iter++) {
			String messageId = getMessageId(m[iter]);
			mids.add(messageId);
			if (showProgress) {
				displayProgress(messageCount, iter + 1, null);
			} else {
				System.out.println(folderName + " fetching headers for ("
						+ (iter + 1) + "/" + messageCount + ") = " + messageId);
			}
		}
		folder.close(true);
		return mids;
	}

	public static void displayProgress(int total, int current, String status) {
		double percentage = ((double) current / (double) total) * 100;
		int curPercentage = (int) percentage;

		StringBuilder s = new StringBuilder();
		s.append("\r");
		s.append(curPercentage);
		s.append("% [");
		s.append(current);
		s.append("/");
		s.append(total);
		s.append("] ");
		if (status != null) {
			s.append(status);
			s.append(" ");
		}
		try {
			for (int x = 0; x < ((int) curPercentage / 2); x++) {
				s.append("#");
			}
			System.out.print(s + " |");
			Thread.sleep(80);
			System.out.print(s + " /");
			Thread.sleep(80);
			System.out.print(s + " -");
			Thread.sleep(80);
		} catch (Exception e) {
			System.err.println(e);
		}
		// System.out.print("\r");
		// System.out.println(s);
		if (total == current) {
			System.out.println("\n");
		}
	}

	public static boolean checkCountForSkip(Folder srcFolder, Folder dstFolder)
			throws Exception {
		boolean skip = false;
		srcFolder.open(Folder.READ_ONLY);
		dstFolder.open(Folder.READ_ONLY);
		String folderName = srcFolder.getFullName();
		int srcMessageCount = srcFolder.getMessageCount();
		int dstMessageCount = dstFolder.getMessageCount();
		System.out.println(" -- Source has " + srcMessageCount + " messages");
		System.out.println(" -- Destination has " + dstMessageCount
				+ " messages");
		if (srcMessageCount <= dstMessageCount) {
			System.out.println("Skipping folder " + folderName
					+ ", ostensibly has already been transferred.");
			skip = true;
		}
		srcFolder.close(true);
		dstFolder.close(true);
		return skip;
	}

	/**
	 * Get message id for a <Message> object
	 * 
	 * @param message
	 * @return
	 * @throws MessagingException
	 */
	@SuppressWarnings("unchecked")
	public static String getMessageId(Message message)
			throws MessagingException {
		Enumeration<Header> h = message.getAllHeaders();
		while (h.hasMoreElements()) {
			Header e = h.nextElement();
			if (e.getName().equalsIgnoreCase("Message-ID")) {
				return e.getValue();
			}
		}
		return null;
	}

	public static void scanAndHandleFolder(Folder folder, boolean recurse,
			String tab, Store dest) throws Exception {
		System.out.println(tab + "Name:      " + folder.getName());
		System.out.println(tab + "Full Name: " + folder.getFullName());
		System.out.println(tab + "URL:       " + folder.getURLName());

		List<String> attrs = new ArrayList<String>();
		boolean hasNoChildren = false;

		// Kludge to check to see if attribute '\HasNoChildren' is set,
		// otherwise will falsely assume all folders
		if (folder instanceof IMAPFolder) {
			IMAPFolder f = (IMAPFolder) folder;
			attrs = Arrays.asList(f.getAttributes());
			if (attrs.contains("\\HasNoChildren")) {
				hasNoChildren = true;
			}
		}

		// if ((folder.getType() & Folder.HOLDS_FOLDERS) != 0) {
		// if (hasNoChildren == false) {
		if (((folder.getType() & Folder.HOLDS_FOLDERS) != 0)
				&& hasNoChildren == false) {
			Folder destinationFolder = dest.getFolder(folder.getFullName());
			if (!destinationFolder.exists()) {
				System.out
						.println("Creating destination folder (holding folders) "
								+ folder.getFullName());
				destinationFolder.create(Folder.HOLDS_FOLDERS);
			}

			System.out.println(tab + "Is Directory");

			try {
				processFolder(folder, destinationFolder);
			} catch (MessagingException ex) {
				System.out
						.println("Caught MessagingException " + ex.toString());
			}

			if (recurse) {
				Folder[] f = folder.list();
				for (int i = 0; i < f.length; i++) {
					scanAndHandleFolder(f[i], recurse, tab + "    ", dest);
				}
			}
		} else {
			Folder destinationFolder = dest.getFolder(folder.getFullName());
			if (!destinationFolder.exists()) {
				System.out
						.println("Creating destination folder (holding messages) "
								+ folder.getFullName());
				destinationFolder.create(Folder.HOLDS_MESSAGES);
			}
			try {
				processFolder(folder, destinationFolder);
			} catch (MessagingException ex) {
				System.out
						.println("Caught MessagingException " + ex.toString());
			}
		}

		// System.out.println();
	}

}
