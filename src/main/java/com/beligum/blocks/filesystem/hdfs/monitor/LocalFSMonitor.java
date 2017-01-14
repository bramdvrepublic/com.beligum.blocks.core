package com.beligum.blocks.filesystem.hdfs.monitor;

import com.beligum.base.utils.Logger;
import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Created by bram on 12/17/14.
 */
public class LocalFSMonitor extends AbstractFsMonitor
{
    //-----CONSTANTS-----
    private static final int NEW_FILE_CREATED_RECHECK_MILLIS = 500;

    //-----VARIABLES-----
    private Path filesystemRoot;
    private WatchService watcher;
    private Set<String> registeredDirs;
    private Map<Path, Timer> timers;
    private AtomicBoolean running;
    private AtomicBoolean error;
    private Thread watchThread;
    private boolean relativizeToRoot;
    private boolean includeHidden;

    /**
     * Creates a WatchService and registers the given directory
     *
     * @param filesystemRoot the absolute root folder to monitor
     * @param relativizeToRoot whether the returned paths should be absolute or relativized to the filesystemRoot (with leading slashes). Note: this will also set the root path to "/"
     * @param includeHidden whether to emit events for hidden files
     * @throws IOException
     */
    public LocalFSMonitor(Path filesystemRoot, boolean relativizeToRoot, boolean includeHidden) throws IOException
    {
        super(relativizeToRoot ? new org.apache.hadoop.fs.Path("/") : new org.apache.hadoop.fs.Path(filesystemRoot.toUri()));

        this.filesystemRoot = filesystemRoot;
        this.registeredDirs = Collections.synchronizedSet(new HashSet<String>());
        this.timers = Collections.synchronizedMap(new HashMap<Path, Timer>());
        this.running = new AtomicBoolean(false);
        this.error = new AtomicBoolean(false);
        this.watchThread = null;
        this.relativizeToRoot = relativizeToRoot;
        this.includeHidden = includeHidden;
    }

    //-----CONSTRUCTORS-----
    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event)
    {
        return (WatchEvent<T>) event;
    }

    //-----PUBLIC METHODS-----
    @Override
    public synchronized void start() throws IOException
    {
        this.watcher = this.filesystemRoot.getFileSystem().newWatchService();

        this.watchThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                running.set(true);

                //Note: we moved this inside  the thread because it can take some time of there's a lot of folders in the system
                try {
                    Logger.info("Recursively registering all folders for watching under (this might take some time) " + filesystemRoot);
                    registerAll(filesystemRoot);
                }
                catch (Exception e) {
                    Logger.error("Error while registering all folders below directory watch loop, quitting", e);
                    running.set(false);
                    error.set(true);
                }

                Logger.info("Starting to recursively watch the filesystem events of folder " + filesystemRoot);
                while (running.get()) {
                    try {
                        // wait for key to be signalled
                        WatchKey key;
                        try {
                            key = watcher.take();
                        }
                        //means we're shutting down
                        catch (ClosedWatchServiceException e) {
                            continue;
                        }
                        catch (InterruptedException e) {
                            continue;
                        }

                        Path dir = (Path) key.watchable();

                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind kind = event.kind();
                            if (kind == OVERFLOW) {
                                Logger.debug("Missed filesystem watch event (overflow)" + key);
                                continue;
                            }

                            WatchEvent<Path> ev = cast(event);
                            Path name = ev.context();
                            Path child = dir.resolve(name);

                            //Logger.debug(event.count() + " " + event.kind().name() + " " + child);

                            if ((includeHidden && Files.isHidden(child)) || (!includeHidden && !Files.isHidden(child))) {

                                if (kind == ENTRY_CREATE) {
                                    try {
                                        // if directory is created, and watching recursively, then
                                        // register it and its sub-directories
                                        if (Files.isDirectory(child)) {
                                            //Note: metadata is handled in the register methods
                                            registerAll(child);

                                            Logger.debug("Directory created; " + child);
                                            for (Listener listener : listeners) {
                                                listener.onDirectoryCreated(LocalFSMonitor.this, toHdfsPath(child));
                                            }
                                        }
                                        // if a file is created, we start up a timer to follow it's size growth
                                        // so we can detect when it's "really" created
                                        else if (Files.isRegularFile(child)) {
                                            //if (for what reason whatsoever) the timer exists, stop and delete it first; we're no longer interested
                                            if (timers.containsKey(child)) {
                                                timers.get(child).cancel();
                                                timers.remove(child);
                                            }
                                            Timer timer = new Timer("GrowTimerNew " + child);
                                            timers.put(child, timer);
                                            //Note: metadata is handled in the WatchGrowTimer
                                            TimerTask task = new WatchGrowTimer(child, true);
                                            timer.scheduleAtFixedRate(task, 0, NEW_FILE_CREATED_RECHECK_MILLIS);
                                        }
                                        else {
                                            Logger.error("Encountered unsupported Path type; " + child);
                                        }
                                    }
                                    catch (IOException e) {
                                        Logger.error("Exception while handling ENTRY_CREATE", e);
                                    }
                                }
                                else if (kind == ENTRY_MODIFY) {
                                    if (Files.isDirectory(child)) {
                                        Logger.debug("Directory modified; " + child);
                                        for (Listener listener : listeners) {
                                            listener.onDirectoryModified(LocalFSMonitor.this, toHdfsPath(child));
                                        }
                                    }
                                    // if a file is created, we start up a timer to follow it's size growth
                                    // so we can detect when it's "really" created
                                    else if (Files.isRegularFile(child)) {
                                        if (!timers.containsKey(child)) {
                                            Timer timer = new Timer("GrowTimerModify " + child);
                                            timers.put(child, timer);
                                            //Note: metadata is handled in the WatchGrowTimer
                                            TimerTask task = new WatchGrowTimer(child, false);
                                            timer.scheduleAtFixedRate(task, 0, NEW_FILE_CREATED_RECHECK_MILLIS);
                                        }
                                    }
                                    else {
                                        Logger.error("Encountered unsupported Path type modification; " + child);
                                    }
                                }
                                else if (kind == ENTRY_DELETE) {
                                    //we have a problem that the real filesystem entry might have been deleted already,
                                    //so if we want to check if this is a file or a dir, it will always return false (because it's gone)
                                    //solution is to check the registeredDirs set
                                    boolean isDir = unregister(child);

                                    if (isDir) {
                                        Logger.debug("Directory deleted; " + child);
                                        for (Listener listener : listeners) {
                                            listener.onDirectoryDeleted(LocalFSMonitor.this, toHdfsPath(child));
                                        }
                                    }
                                    else {
                                        Logger.debug("File deleted; " + child);
                                        for (Listener listener : listeners) {
                                            listener.onFileDeleted(LocalFSMonitor.this, toHdfsPath(child));
                                        }
                                    }
                                }
                                else {
                                    Logger.warn("Encountered unsupported file system watch event; " + kind);
                                }
                            }
                        }

                        // It’s very important that the key must be reset after the events have been processed.
                        // If not, the key won’t receive further events. If the reset() method returns false, the directory is inaccessible (might be deleted).
                        boolean valid = key.reset();

                        //Hmmm, do we need this?
                        synchronized (registeredDirs) {
                            // all directories are inaccessible
                            if (registeredDirs.isEmpty()) {
                                Logger.error("No more directories available to watch; exiting.");
                                running.set(false);
                                error.set(true);
                            }
                        }
                    }
                    catch (Exception e) {
                        Logger.error("Error in directory watch loop, quitting", e);
                        //TODO bram: maybe not cancel?
                        running.set(false);
                        error.set(true);
                    }
                }

                if (error.get()) {
                    Logger.error("Stopped watching the file system because of an internal error, this is bad and should be fixed");
                }
                else {
                    Logger.info("Stopped watching the file system because of a shutdown");
                }
            }
        }, "Watcher");

        watchThread.setDaemon(true);
        watchThread.start();
    }

    @Override
    public synchronized void stop()
    {
        if (watchThread != null) {

            running.set(false);

            try {
                watcher.close();
            }
            catch (IOException e) {
                Logger.error("Error while stopping java watch service", e);
            }

            watchThread.interrupt();
        }
    }

    @Override
    public synchronized void stopAndWait()
    {
        this.stop();

        if (watchThread != null) {
            try {
                this.watchThread.join();
            }
            catch (InterruptedException e) {
                // Don't care
            }
        }

    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    /**
     * Register the given directory with the WatchService
     */
    private FileVisitResult register(Path dir) throws IOException
    {
        if ((this.includeHidden && Files.isHidden(dir)) || (!this.includeHidden && !Files.isHidden(dir))) {
            //WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            /**
             * FIXME response times on a os x are painfully slow. So replaced above with below. Might not be needed on linux.
             */
            WatchKey key = dir.register(watcher, new WatchEvent.Kind[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY }, SensitivityWatchEventModifier.HIGH);
            synchronized (registeredDirs) {
                //note: we can't just register the dir itself, because we use the entries when a dir gets deleted
                //and the equals method returns false when comparing to a deleted Path
                registeredDirs.add(dir.toString());
                Logger.debug("Registering '" + dir.toString() + "'");
            }

            return FileVisitResult.CONTINUE;
        }
        else {
            //TODO: this will not register the meta folders, do they won't work in our listener signaling mechanism (not in registeredDirs, so always returned as files)
            return FileVisitResult.SKIP_SUBTREE;
        }
    }

    /**
     * This will un-register the specified directory.
     * Returns if it was really registered.
     */
    private boolean unregister(Path dir)
    {
        boolean retVal = false;

        synchronized (registeredDirs) {
            retVal = registeredDirs.remove(dir.toString());
        }

        return retVal;
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException
    {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException
            {
                return register(dir);
            }
        });
    }

    private org.apache.hadoop.fs.Path toHdfsPath(java.nio.file.Path absJavaPath)
    {
        org.apache.hadoop.fs.Path retVal = null;

        if (absJavaPath != null) {
            if (relativizeToRoot) {
                //subtle: HDFS (especially chroot'ed FS) expect the resulting path to be absolute (with leading slash)
                absJavaPath = Paths.get("/").resolve(this.filesystemRoot.relativize(absJavaPath));
            }

            retVal = new org.apache.hadoop.fs.Path(absJavaPath.toUri());
        }

        return retVal;
    }

    //-----PRIVATE CLASSES-----
    private class WatchGrowTimer extends TimerTask
    {
        private long lastSize;
        private Path path;
        private boolean creatingFile;

        public WatchGrowTimer(Path path, boolean creatingFile) throws IOException
        {
            this.path = path;
            //always let it run once (avoid slow starts and 0==0)
            this.lastSize = -1;
            this.creatingFile = creatingFile;
        }

        /**
         * The action to be performed by this timer task.
         */
        @Override
        public void run()
        {
            try {
                long size = Files.size(this.path);
                //the -1 makes sure we run at least one time
                if (this.lastSize != -1 && size == this.lastSize) {
                    //done
                    timers.get(this.path).cancel();
                    timers.remove(this.path);

                    if (creatingFile) {
                        Logger.debug("File created; " + this.path);
                        for (Listener listener : listeners) {
                            listener.onFileCreated(LocalFSMonitor.this, toHdfsPath(this.path));
                        }
                    }
                    else {
                        Logger.debug("File modified; " + this.path);
                        for (Listener listener : listeners) {
                            listener.onFileModified(LocalFSMonitor.this, toHdfsPath(this.path));
                        }
                    }
                }
                else {
                    //not done yet
                    //wait two seconds
                    Thread.sleep(2000);
                    this.lastSize = size;
                }
            }
            catch (Exception e) {
                Logger.error("Caught error while monitoring file size of file, aborting " + this.path, e);

                //don't forget to clean up
                timers.get(this.path).cancel();
                timers.remove(this.path);
            }
        }
    }
}
