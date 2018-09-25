package com.beligum.blocks.filesystem.hdfs.monitor.local;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.ifaces.FsMonitor;
import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static java.nio.file.StandardWatchEventKinds.*;

public class LocalProcessingThread extends Thread
{
    //-----CONSTANTS-----
    private static final int NEW_FILE_CREATED_RECHECK_MILLIS = 500;

    //-----VARIABLES-----
    private LocalFSMonitor fsMonitor;

    private WatchService watcher;
    private ConcurrentHashMap<Path, Timer> timers;
    private ConcurrentHashMap<Path, WatchKey> registeredDirs;

    private AtomicBoolean keepRunning;
    private AtomicBoolean error;

    //-----CONSTRUCTORS-----
    public LocalProcessingThread(LocalFSMonitor fsMonitor) throws IOException
    {
        this.fsMonitor = fsMonitor;

        this.watcher = this.fsMonitor.getFilesystemRoot().getFileSystem().newWatchService();
        this.timers = new ConcurrentHashMap<>();
        //Note that the key of the watcher.take() event doesn't seem to be the same
        //as the original one when calling .register(), that's why we need to keep them
        //in a hashmap
        this.registeredDirs = new ConcurrentHashMap<>();

        this.keepRunning = new AtomicBoolean(false);
        this.error = new AtomicBoolean(false);
    }

    //-----PUBLIC METHODS-----
    public void shutdown()
    {
        this.keepRunning.set(false);

        try {
            //notify the watcher so the loop below throws the ClosedWatchServiceException
            this.watcher.close();
        }
        catch (IOException e) {
            //ignore
        }
    }
    @Override
    public void run()
    {
        try {
            Logger.info("Recursively registering all folders for watching under (this might take some time) " + this.fsMonitor.getFilesystemRoot());
            registerAll(this.fsMonitor.getFilesystemRoot());

            //only flag the start of the loop when all was well
            this.keepRunning.set(true);
        }
        catch (Exception e) {
            Logger.error("Error while registering all folders below directory watch loop, quitting", e);
            this.error.set(true);
        }

        try {
            Logger.info("Starting to recursively watch the filesystem events of folder " + this.fsMonitor.getFilesystemRoot());

            while (this.keepRunning.get()) {
                try {
                    // wait for key to be signalled
                    WatchKey key;
                    try {
                        key = this.watcher.take();
                    }
                    //means we're shutting down
                    catch (ClosedWatchServiceException e) {
                        continue;
                    }

                    try {
                        Path dir = (Path) key.watchable();

                        //Retrieves and removes all pending events for this watch key, returning a List of the events that were retrieved.
                        //Note that this method does not wait if there are no events pending.
                        for (WatchEvent<?> event : key.pollEvents()) {

                            WatchEvent<Path> ev = cast(event);
                            Path name = ev.context();
                            Path child = dir.resolve(name);

                            WatchEvent.Kind kind = event.kind();
                            if (kind == OVERFLOW) {
                                Logger.warn("Missed filesystem watch event (overflow) for " + child);
                                continue;
                            }

                            //Logger.debug(event.count() + " " + event.kind().name() + " " + child);

                            boolean childIsHidden = Files.isHidden(child);
                            if ((this.fsMonitor.isIncludeHidden() && childIsHidden) || (!this.fsMonitor.isIncludeHidden() && !childIsHidden)) {

                                if (kind == ENTRY_CREATE) {
                                    try {
                                        // if directory is created, and watching recursively, then
                                        // register it and its sub-directories
                                        if (Files.isDirectory(child)) {
                                            //Note: metadata is handled in the register methods
                                            registerAll(child);

                                            Logger.debug("Directory created; " + child);
                                            for (FsMonitor.Listener listener : this.fsMonitor.getListeners()) {
                                                listener.onDirectoryCreated(this.fsMonitor, this.fsMonitor.toHdfsPath(child));
                                            }
                                        }
                                        // if a file is created, we start up a timer to follow it's size growth
                                        // so we can detect when it's "really" created
                                        else if (Files.isRegularFile(child)) {

                                            this.timers.compute(child, new BiFunction<Path, Timer, Timer>()
                                            {
                                                @Override
                                                public Timer apply(Path path, Timer timer)
                                                {
                                                    //if (for what reason whatsoever) the timer exists, stop and delete it first;
                                                    // we're no longer interested because we're booting a new one
                                                    // (Note that the 'creatingFile' flag may be changing!)
                                                    if (timer != null) {
                                                        timer.cancel();
                                                    }

                                                    Timer newTimer = new Timer("GrowTimerNew " + child);

                                                    //Note: metadata is handled in the WatchGrowTimer
                                                    newTimer.scheduleAtFixedRate(new LocalWatchGrowTimer(child, true, timers, fsMonitor),
                                                                                 0, NEW_FILE_CREATED_RECHECK_MILLIS);

                                                    return newTimer;
                                                }
                                            });
                                        }
                                        else {
                                            throw new IOException("Encountered unsupported Path type (not a directory nor a file); " + child);
                                        }
                                    }
                                    catch (Exception e) {
                                        Logger.error("Exception while handling ENTRY_CREATE; watching will continue, but you should look into this", e);
                                        //note: it's okay we continue with the watching, right?
                                    }
                                }
                                else if (kind == ENTRY_MODIFY) {
                                    try {
                                        if (Files.isDirectory(child)) {
                                            Logger.debug("Directory modified; " + child);
                                            for (FsMonitor.Listener listener : this.fsMonitor.getListeners()) {
                                                listener.onDirectoryModified(this.fsMonitor, this.fsMonitor.toHdfsPath(child));
                                            }
                                        }
                                        // if a file is created, we start up a timer to follow it's size growth
                                        // so we can detect when it's "really" created
                                        else if (Files.isRegularFile(child)) {

                                            this.timers.compute(child, new BiFunction<Path, Timer, Timer>()
                                            {
                                                @Override
                                                public Timer apply(Path path, Timer timer)
                                                {
                                                    //if (for what reason whatsoever) the timer exists, stop and delete it first;
                                                    // we're no longer interested because we're booting a new one
                                                    // (Note that the 'creatingFile' flag may be changing!)
                                                    if (timer != null) {
                                                        timer.cancel();
                                                    }

                                                    Timer newTimer = new Timer("GrowTimerNew " + child);

                                                    //Note: metadata is handled in the WatchGrowTimer
                                                    newTimer.scheduleAtFixedRate(new LocalWatchGrowTimer(child, false, timers, fsMonitor),
                                                                                 0, NEW_FILE_CREATED_RECHECK_MILLIS);

                                                    return newTimer;
                                                }
                                            });
                                        }
                                        else {
                                            throw new IOException("Encountered unsupported Path type modification (not a directory nor a file); " + child);
                                            //note: it's okay we continue with the watching, right?
                                        }
                                    }
                                    catch (Exception e) {
                                        Logger.error("Exception while handling ENTRY_MODIFY; watching will continue, but you should look into this", e);
                                        //note: it's okay we continue with the watching, right?
                                    }
                                }
                                else if (kind == ENTRY_DELETE) {
                                    try {
                                        //we have a problem that the real filesystem entry might have been deleted already,
                                        //so if we want to check if this is a file or a dir, it will always return false (because it's gone)
                                        //solution is to check the registeredDirs set
                                        boolean isDir = unregister(child);

                                        if (isDir) {
                                            Logger.debug("Directory deleted; " + child);
                                            for (FsMonitor.Listener listener : this.fsMonitor.getListeners()) {
                                                listener.onDirectoryDeleted(this.fsMonitor, this.fsMonitor.toHdfsPath(child));
                                            }
                                        }
                                        else {
                                            Logger.debug("File deleted; " + child);
                                            for (FsMonitor.Listener listener : this.fsMonitor.getListeners()) {
                                                listener.onFileDeleted(this.fsMonitor, this.fsMonitor.toHdfsPath(child));
                                            }
                                        }
                                    }
                                    catch (Exception e) {
                                        Logger.error("Exception while handling ENTRY_DELETE; watching will continue, but you should look into this", e);
                                        //note: it's okay we continue with the watching, right?
                                    }
                                }
                                else {
                                    Logger.error("Encountered unsupported file system watch event, this shouldn't happen; " + kind);
                                }
                            }
                        }
                    }
                    finally {
                        // It’s very important that the key must be reset after the events have been processed.
                        // If not, the key won’t receive further events.
                        // If the reset() method returns false, the directory is inaccessible (might be deleted).
                        boolean valid = key.reset();

                        // all directories are inaccessible
                        if (this.registeredDirs.isEmpty()) {
                            Logger.error("No more directories available to watch; exiting (this shouldn't happen).");
                            this.keepRunning.set(false);
                            this.error.set(true);
                        }
                    }
                }
                catch (Throwable e) {
                    Logger.error("Major error in directory watch loop, quitting", e);
                    this.keepRunning.set(false);
                    this.error.set(true);
                }
            }
        }
        finally {
            try {
                //make sure we always close the watcher
                Logger.info("Stop watching all folders under " + this.fsMonitor.getFilesystemRoot());
                this.watcher.close();
            }
            catch (Exception e) {
                Logger.error("Error while stopping java watch service", e);
                this.error.set(true);
            }
            finally {
                if (this.error.get()) {
                    Logger.error("Stopped watching the file system because of an internal setRollbackOnly, this is bad and should be fixed");
                }
                else {
                    Logger.info("Stopped watching the file system because of a shutdown");
                }
            }
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event)
    {
        return (WatchEvent<T>) event;
    }
    /**
     * Register the given directory with the WatchService
     */
    private FileVisitResult register(Path dir) throws IOException
    {
        if ((this.fsMonitor.isIncludeHidden() && Files.isHidden(dir)) || (!this.fsMonitor.isIncludeHidden() && !Files.isHidden(dir))) {

            try {
                this.registeredDirs.compute(dir, new BiFunction<Path, WatchKey, WatchKey>()
                {
                    @Override
                    public WatchKey apply(Path dir, WatchKey existingWatchKey)
                    {
                        WatchKey retVal = null;

                        try {
                            if (existingWatchKey != null) {
                                Logger.warn("Registering a directory for watching, but it's already present, overwriting; " + dir);
                                existingWatchKey.cancel();
                            }

                            // Note: response times on a os x are painfully slow. So replaced above with below. Might not be needed on linux.
                            retVal = dir.register(watcher, new WatchEvent.Kind[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY }, SensitivityWatchEventModifier.HIGH);

                            //note: we can't just register the dir itself, because we use the entries when a dir gets deleted
                            //and the equals method returns false when comparing to a deleted Path
                            Logger.debug("Registered '" + dir.toString() + "'");

                        }
                        catch (Exception e) {
                            //re-throw to the caller
                            throw new RuntimeException(e);
                        }

                        return retVal;
                    }
                });
            }
            catch (Throwable e) {
                Logger.error("Error while registering directory for watching; " + dir, e);
                throw new IOException(e);
            }

            return FileVisitResult.CONTINUE;
        }
        else {
            //Note: this will not register the meta folders, do they won't work in our listener signaling mechanism (not in registeredDirs, so always returned as files)
            return FileVisitResult.SKIP_SUBTREE;
        }
    }

    /**
     * This will un-register the specified directory.
     * Returns true if it was really registered.
     */
    private boolean unregister(Path dir)
    {
        WatchKey existing = this.registeredDirs.remove(dir);

        if (existing != null) {
            //if we're dealing with a directory, cancel the watcher
            existing.cancel();
        }

        return existing != null;
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
}
