package com.beligum.blocks.fs.atomic.orig;

import java.util.*;
import java.io.*;

/* TODO: 
  Change rep to textual, for ease of reconstruction after bad crash.
*/


/* Sequence that shows that we can't look at the last action locally
     to determine if it succeeded or failed:

        delete A
        create A
				delete A

	 If delete looks at the file and determines that the action failed
	 if the file exists, then:
     If a exists, it could be because delete A failed, or because
	    1. it succeeded
      2. there was a crash
      3. recovery started
      4. recovery crashed just before the journal was deleted.
   In this case, A will be present because the original delete A was undone, 
   putting A back.
     But it doesn't matter if we don't undo the last delete during recovery: 
   we will forcibly undo the remaining actions, and this will put us in the
	 right original state.
     For that matter, a journal containing only delete A would show the same
   thing: if recovery crashed at the end, then there would be a file there.

	 In general: successfully undoing an action looks the same as if the action
   failed.  Not performing the undo is correct in both cases: in the second
	 obviously; in the first, because the undo already happened.  Even if
   subsequent undos (i.e. actions that actually occurred earlier in time)
	 make it look like the last action failed when it did not, causing the
	 undo of that action to be skipped on subsequent recovery, that is fine:
	 all other undos will happen, resulting in the original initial state.

	 Consider:

				create A
				rename A B

   and say B exists beforehand.  The create succeeds, but the rename fails
	 and the system crashes before this failure makes it to the journal.
	 On first recovery, we notice that A exists so we don't do the rename,
	 but this recovery crashes just before journal deletion, leaving no A file.
	 On second recovery, the non-existence of A and the presence of B makes
	 it look like the rename succeeded, so it is undone: ERROR.

	 In general: initial state looks like final state would look like if last
	 action succeeded, but last action actually failed.

	 Solution: first recovery appends something to the journal saying whether
	 last action truly succeeded or failed.  (It can tell from filesystem
	 state.)
	 
*/

/**
 * An undo log for atomic file transactions.
 * Each transaction has its own log.
 * Invariant: every action in the journal, except possibly the last
 * one, completed successfully.
 */
class Journal
{
    // Markers at end of journal
    private static final int COMMIT = -1;
    private static final int UNDO = -2;

    private File journalFile;
    private RandomAccessFile raf;
    private boolean closed;
    private int transactionNumber;
    private ArrayList actions;        // actions that ran successfully
    // null -> actions not read/...

    Journal(File f, int tn) throws IOException
    {
        journalFile = f;
        transactionNumber = tn;
        actions = new ArrayList();
        open();
    }

    /**
     * Used for recovery only.
     */
    private Journal(File f) throws IOException
    {
        this(f,
             Integer.parseInt(f.getName().substring(f.getName().indexOf('-') + 1)));
    }

    private void open() throws IOException
    {
        raf = new RandomAccessFile(journalFile, "rw");
        closed = false;
    }

    private void close() throws IOException
    {
        if (!closed) {                            // make close idempotent
            raf.close();
            closed = true;
        }
    }

    ArrayList getActions()
    {
        return actions;
    }

    void addAction(Action a)
    {
        actions.add(a);
    }

    /* Journal file format consists of the following, repeated:
         4-byte length
         serialized Action
         TODO: Switch to a textual representation (XML?  Not much point--XML
         tools will choke on partial files, which will be the most likely
         situation.)
    */
    void writeAction(Action a) throws IOException
    {
        mark();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(a);
        oos.close();
        byte[] ser = baos.toByteArray();
        raf.writeInt(ser.length);
        raf.write(ser);
        flush();
        if (actions == null)
            actions = new ArrayList();
        addAction(a);
    }

    void actionFailed() throws IOException
    {
        reset();
        flush();
        removeLastAction();
    }

    private void removeLastAction()
    {
        actions.remove(actions.size() - 1);
    }

    ////////////////////////////////////////////////////////////////
    /// Low-level Journal Output

    private void flush() throws IOException
    {
        raf.getFD().sync();
    }

    private long markPoint;

    private void mark() throws IOException
    {
        markPoint = raf.getFilePointer();
    }

    private void reset() throws IOException
    {
        raf.seek(markPoint);
        raf.setLength(markPoint);
    }

    private void writeMarker(int marker) throws IOException
    {
        raf.writeInt(marker);
        flush();
    }

    /**
     * Returns a List of cleanup exceptions.
     */
    List commit() throws TransactionException, InconsistentStateException
    {
        try {
            writeMarker(COMMIT);
        }
        catch (Exception e) {
            rollback();
            throw new TransactionException(e);
        }
        // Txn is now committed.
        return cleanup();
    }

    /**
     * Returns a List of cleanup exceptions.
     */
    List rollback() throws InconsistentStateException
    {
        try {
            flush();
        }
        catch (Exception e) {
            throw new InconsistentStateException(e);
        }
        return rollback(0);
    }

    /**
     * Returns a List of cleanup exceptions.
     */
	/* We do cleanup at the end, rather than after each action,
		 mostly so that cleanup exceptions don't interfere with undo,
		 and we can simply return a list of them at the end.
	*/
    private List rollback(int nUndos) throws InconsistentStateException
    {
        try {
            play(nUndos);
        }
        catch (Exception e) {
            try {
                close();
            }
            catch (IOException e2) { /* ignore, this is worse */ }
            throw new InconsistentStateException(e);
        }
        return cleanup();
    }

    ////////////////////////////////////////////////////////////////
    // Playback for undo.

    public static int playCrashCount = -1;

    /**
     * Play back the contents of the journal, undoing the actions.
     */
    void play(int nUndos) throws IOException, ClassNotFoundException
    {
        int start = actions.size() - 1 - nUndos;
        for (int i = start; i >= 0; i--) {
            Action a = (Action) actions.get(i);
            a.undo();
            // Indicate the action has been undone, so we don't undo it again.
            // This means actions need only be locally idempotent, i.e.
            // idempotent only if nothing else happened in between.
            writeMarker(UNDO);

            // for testing
            if (playCrashCount > 0 && --playCrashCount == 0)
                Runtime.getRuntime().halt(-1);
        }
    }

    ////////////////////////////////////////////////////////////////
    /// Recovery.

    /* Truncates the journal to eliminate the last record if it's partial.
         Returns a list of cleanup exceptions.
    */
    static List recover(File journalFile) throws InconsistentStateException
    {
        try {
            Journal j = new Journal(journalFile);
            return j.recover();
        }
        catch (Exception e) {
            throw new InconsistentStateException(e);
        }
    }

    private List recover() throws InconsistentStateException
    {
        int nUndos;
        try {
            nUndos = read(); // actions also filled with Action objects
        }
        catch (Exception e) {
            throw new InconsistentStateException(e);
        }
        if (nUndos == COMMIT)
            return cleanup();
        else
            return rollback(nUndos);
    }

    /* Returns COMMIT or # of undone actions
       side-effects:
     - fills actions ArrayList with actions yet to be undone
         - resets file length to eliminate partial junk at end
         Note: we can't actually remove the undone actions from the list,
         because we need them there for cleanup.
    */
    private int read() throws IOException, ClassNotFoundException
    {
        int recordSize;
        long pos, fileLength;
        byte[] buf = new byte[100];
        int undoCount = 0;

        fileLength = raf.length();
        raf.seek(0);
        while ((pos = raf.getFilePointer()) < fileLength) {
            try {
                recordSize = raf.readInt();
            }
            catch (EOFException e) {
                // There was a crash while writing the int.  Ignore this record.
                break;
            }
            if (undoCount > 0) { // we are reading the undo markers at the end
                if (recordSize != UNDO)
                    throw new IOException("journal corrupted: non-undo after undo");
                else
                    undoCount++;
            }
            else if (recordSize == COMMIT) {
                if (raf.getFilePointer() == fileLength)
                    return COMMIT;
                else
                    throw new IOException("journal corrupted: COMMIT marker not at end");
            }
            else if (recordSize == UNDO) {
                undoCount++;
            }
            else if (recordSize <= 0) {
                throw new IOException("journal corrupted: record size <= 0");
            }
            else if (pos + recordSize > fileLength) {
                // There was a crash while writing the record.  Ignore the record.
                break;
            }
            else {
                // Read the record.
                if (recordSize > buf.length)
                    buf = new byte[recordSize];
                raf.read(buf, 0, recordSize);
                ByteArrayInputStream bain = new ByteArrayInputStream(buf);
                ObjectInputStream oin = new ObjectInputStream(bain);
                Action a = (Action) oin.readObject(); // This will check whether
                // it is in fact an Action.
                actions.add(a);
            }
        } // end while
        if (pos < fileLength) { // remove partial junk at end
            raf.setLength(pos);
            flush();
        }
        return undoCount;
    }

    /* Cleanup IOExceptions are considered minor enough that they don't
         get thrown, but instead accumulated into a list for optional
         processing by clients.
    */
    private List cleanup()
    {
        // Delete all temporary files created as part of this transaction.
        ArrayList exceptions = new ArrayList();
        for (int i = 0; i < actions.size(); i++) {
            try {
                ((Action) actions.get(i)).cleanup();
            }
            catch (IOException e) {
                exceptions.add(e);
            }
        }
        // Close and delete the journal file itself.
        try {
            close();
        }
        catch (IOException e) {
            exceptions.add(e);
        }
        try {
            Action.delete(journalFile);
        }
        catch (IOException e) {
            exceptions.add(e);
        }
        return exceptions;
    }

    /**
     * Display file in readable form on System.out
     */
    public static void display(File journalFile)
                    throws IOException, ClassNotFoundException
    {
        Journal j = new Journal(journalFile);
        j.read();
        for (int i = 0; i < j.actions.size(); i++)
            System.out.println(j.actions.get(i));
        System.out.println("file length  = " + journalFile.length());
    }
}
	
