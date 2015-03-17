(ns quantum.core.util.sh
  #+clj (:gen-class))


#+clj (require '[quantum.core.ns :as ns :refer :all])
#+clj (ns/require-all *ns* :clj)
#+clj (require
  '[quantum.core.io          :as io]
  '[quantum.core.string      :as str  :refer [str+]]
  '[quantum.core.collections :as coll :refer [update-in+]]
  '[quantum.core.function    :as fn   :refer [f*n]])
; As of 1.5, ProcessBuilder.start() is the preferred way to create a Process.
(def processes (atom {}))

#+clj
(defn- update-proc-info!
  [^String proc-name ^Process proc ^Keyword out-type]
  (swap! processes update-in+ [proc-name out-type] 
      (fn [record]
        (let [stream
                (condp = out-type
                  :out (.getInputStream proc)
                  :err (.getErrorStream proc))
              in (-> stream str+ (str/split #"\n"))]
          (doseq [^String line ^Vec in]
            (println "PROCESS" proc-name ":" line))
          (if (vector? record)
              (conj record in)
              [in])))))

#+clj
(defn exec!
  {:todo ["A few of them written in the code here..."
          "Record in /processes/ when process is terminated"]
   :example "(exec [:projects \"clojure-getting-started\"] \"heroku\" \"ps\")"}
  [dir-0 & args]
  (let [^ProcessBuilder pb (ProcessBuilder. ^java.util.List args)
        ^java.io.File dir-f
          (if (vector? dir-0)
              (io/file dir-0)
              (io/file [:home]))
        _ (.directory pb dir-f)
        ^String proc-name
          (if (vector? dir-0)
              (apply str/sp args)
              (apply str/sp dir-0 args))
        ^Process proc (.start pb)]
    ; TODO: Get these all the way, updating asynchronously, till done
    ; TODO: Do these simultaneously in 
    (update-proc-info! proc-name proc :out)
    (update-proc-info! proc-name proc :err)
   
    proc))



; JUST WANTED TO GET STDOUT TO STREAM AND NOT FILE.
; Problem 1) access to output stream not public
; Problem 2) UNIXProcess not public
; Problem 3) Native methods whose referents can't be found

; ProcessImpl.start(cmdarray,
;                   environment,
;                   dir,
;                   redirects,
;                   redirectErrorStream);

; (import
;   '(java.lang ProcessBuilder ProcessBuilder$Redirect UNIXProcess))
; (import '(java.io FileInputStream FileOutputStream))

; ; Convert to Unix style environ as a monolithic byte array
; ; inspired by the Windows Environment Block, except we work
; ; exclusively with bytes instead of chars, and we need only
; ; one trailing NUL on Unix.
; ; This keeps the JNI as simple and efficient as possible.
; (def fdAccess (sun.misc.SharedSecrets/getJavaIOFileDescriptorAccess))

; (defn ^"[B" toEnvironmentBlock [^java.util.Map m ^ints envc]
;   (let [n (volatile! 0)
;         ^long ct
;           (loop [entry-set-n (.entrySet m)
;                  ct-n (-> m (.size) (* 2) volatile!)] ; For added '=' and NUL
;             (if (nempty? entry-set-n)
;                 ct-n
;                 (let [^java.util.Map$Entry entry (-> entry-set-n first)]
;                   (recur
;                     (rest entry-set-n)
;                     (+ ct-n (-> entry (.getKey)   str (.getBytes) count+)
;                             (-> entry (.getValue) str (.getBytes) count+))))))
;         ^"[B" block (byte-array ct)]
;     (doseq [^java.util.Map$Entry entry (.entrySet m)]
;       (let [^"[B" k (-> entry (.getKey)   str (.getBytes))
;             ^"[B" v (-> entry (.getValue) str (.getBytes))]
;         (System/arraycopy k 0 block @n (count+ k))
;         (vswap! n + (-> k count+ inc))
;         (aset! block @n (byte \=))
;         (System/arraycopy v 0 block @n (count+ v))
;         (vswap! n + (-> v count+ inc))))
  
;     (aset! envc 0 (.size m))
;     block))


; (defn ^Process start [^"[Ljava.lang.String;" cmdarray
;                       ^java.util.Map environment
;                       ^String dir
;                       ^"[Ljava.lang.ProcessBuilder$Redirect;" redirects
;                       redirectErrorStream]
;   (assert (and (nnil? cmdarray) (> (count+ cmdarray) 0)))

;   (let [; Convert arguments to a contiguous block
;         ^"[[B" args (make-array ByteArray (-> cmdarray count+ dec))
;         size (-> args count+ volatile!) ; For added NUL bytes
;         ^"[I"  envc (int-array 1)
;         ^"[B" envBlock
;           (if (nil? map)
;               nil
;               (toEnvironmentBlock environment envc))
;         _  (dotimes [n (count+ args)]
;              (aset! args n (-> cmdarray (get+ (inc n)) str (.getBytes)))
;              (vswap! size (+ (-> args (get+ n) count+))))
;         ^"[B" argBlock (byte-array @size)]
;     ; TODO eliminate seq abstraction and use areduce or something
;     (loop [args-n args
;            n      (long 0)]
;       (when (nempty? args)
;         (System/arraycopy
;           ^"[B" (first args-n) 0
;           argBlock n
;           (-> args-n first count+))
;         (recur
;           (rest args-n)
;           (-> args-n first count+ inc (+ n) long))))

;      (let [^"[I" std_fds
;              (if (nil? redirects) 
;                  (int-array+ 3 -1 -1 -1) 
;                  (int-array+ 3))
;            ^FileInputStream  f0
;              (cond
;                (= (get+ redirects 0) (. ProcessBuilder$Redirect PIPE))
;                   (do (aset! std_fds 0 -1) nil)
;                (= (get+ redirects 0) (. ProcessBuilder$Redirect INHERIT))
;                   (do (aset! std_fds 0 0)  nil)
;                :else
;                   (let [^FileInputStream f0-n
;                           (-> redirects (get+ 0) (cast ProcessBuilder$Redirect) (.file) (FileInputStream.))]
;                     (aset! std_fds 0 (->> f0-n (.getFD) (.get fdAccess)))
;                     f0-n))
;            ^FileOutputStream f1
;              (cond
;                (= (get+ redirects 1) (. ProcessBuilder$Redirect PIPE))
;                   (do (aset! std_fds 1 -1) nil)
;                (= (get+ redirects 1) (. ProcessBuilder$Redirect INHERIT))
;                   (do (aset! std_fds 1 1) nil)
;                :else
;                  (let [^FileOutputStream f1-n
;                          (FileOutputStream.
;                            (-> redirects (get+ 1) (.file))
;                            (-> redirects (get+ 1) (.append)))]
;                    (aset! std_fds 1 (->> f1-n (.getFD) (.get fdAccess)))
;                    f1-n))
;            ^FileOutputStream f2
;              (cond
;                (= (get+ redirects 2) (. ProcessBuilder$Redirect PIPE))
;                  (do (aset! std_fds 2 -1) nil)
;                (= (get+ redirects 2) (. ProcessBuilder$Redirect INHERIT))
;                  (do (aset! std_fds 2 2 ) nil)
;                :else
;                  (let [^FileOutputStream f2-n
;                          (FileOutputStream.
;                            (-> redirects (get+ 2) (.file))
;                            (-> redirects (get+ 2) (.append)))]
;                     (aset! std_fds 2 (->> f2-n (.getFD) (.get fdAccess)))
;                     f2-n))
;            ^UnixProcess proc
;              (UNIXProcess.
;                (str/str->cstring (get+ cmdarray 0))
;                argBlock (count+ args)
;                envBlock (get+ envc 0)
;                (str/str->cstring dir)
;                std_fds
;                (boolean redirectErrorStream))]
;        (when (nnil? f0) (.close f0))
;        (when (nnil? f1) (.close f1))
;        (when (nnil? f2) (.close f2))
;        proc)))

; (proxy [Process] []
;   (getMyProc []
;     (UNIXProcess.)))
; (proxy [UNIXProcess] []
;   (getMyProc []
;     (vector (byte-array 1) (byte-array 1) 0 (byte-array 1) 0 (byte-array 1) (int-array 1) (boolean false))))

; final byte[] prog,
; final byte[] argBlock, final int argc,
; final byte[] envBlock, final int envc,
; final byte[] dir,
; final int[] fds,
; final boolean redirectErrorStream



; (import
;   '(java.io
;      BufferedInputStream BufferedOutputStream
;      ByteArrayInputStream
;      FileDescriptor FileInputStream FileOutputStream
;      IOException
;      InputStream OutputStream)
;   '(java.util Arrays)
;   '(java.util.concurrent
;     Executors Executor
;     ThreadFactory
;     TimeUnit)
;   '(java.security AccessController
;     PrivilegedAction
;     PrivilegedActionException
;     PrivilegedExceptionAction))


; private final int pid;
; private int exitcode;
; private boolean hasExited;
; private OutputStream stdin;
; private InputStream  stdout;
; private InputStream  stderr;

; private static enum LaunchMechanism {
;        FORK(1),
;        VFORK(3);

;        private int value;
;        LaunchMechanism(int x) {value = x;}
; }

;    ; default is VFORK on Linux
;    private static final LaunchMechanism launchMechanism;
;    private static byte[] helperpath;

;    static {
;       launchMechanism = AccessController.doPrivileged(
;               new PrivilegedAction<LaunchMechanism>()
;       {
;           public LaunchMechanism run() {
;               String javahome = System.getProperty("java.home");
;               String osArch = System.getProperty("os.arch");

;               helperpath = (str/str->cstring (str javahome "/lib/" osArch "/jspawnhelper"));
;               String s = (System/getProperty "jdk.lang.Process.launchMechanism", "vfork");

;               try {
;                   return LaunchMechanism.valueOf(s.toUpperCase());
;               }  (catch IllegalArgumentException _) {
;                   throw new Error(str s " is not a supported process launch mechanism on this platform.");
;               }
;           }
;       });
;   }
; ; this is for the reaping thread
; private native int waitForProcessExit(int pid);

; ; Create a process. Depending on the mode flag, this is done by one of the following mechanisms. - fork(2) and exec(2) - clone(2) and exec(2) - vfork(2) and exec(2)
; ; Parameters:
; ; fds an array of three file descriptors. Indexes 0, 1, and 2 correspond to standard input, standard output and standard error, respectively. On input, a value of -1 means to create a pipe to connect child and parent processes. On output, a value which is not -1 is the parent pipe fd corresponding to the pipe which has been created. An element of this array is -1 on input if and only if it is not -1 on output.
; ; Returns:
; ; the pid of the subprocess
; private native int forkAndExec(int mode, byte[] helperpath,
;                               byte[] prog,
;                               byte[] argBlock, int argc,
;                               byte[] envBlock, int envc,
;                               byte[] dir,
;                               int[] fds,
;                               boolean redirectErrorStream);

    
; ;The thread factory used to create "process reaper" daemon threads.
;  private static class ProcessReaperThreadFactory implements ThreadFactory {
;      private final static ThreadGroup group = getRootThreadGroup();

;      private static ThreadGroup getRootThreadGroup() {
;          return doPrivileged(new PrivilegedAction<ThreadGroup> () {
;              public ThreadGroup run() {
;                  ThreadGroup root = Thread.currentThread().getThreadGroup();
;                  while (root.getParent() != null)
;                      root = root.getParent();
;                  return root;
;              }});
;      }

;      public Thread newThread(Runnable grimReaper) {
;          // Our thread stack requirement is quite modest.
;          Thread t = new Thread(group, grimReaper, "process reaper", 32768);
;          t.setDaemon(true);
;          // A small attempt (probably futile) to avoid priority inversion
;          t.setPriority(Thread.MAX_PRIORITY);
;          return t;
;      }
;  }

    
; ;The thread pool of "process reaper" daemon threads.
;  private static final Executor processReaperExecutor =
;      doPrivileged(new PrivilegedAction<Executor>() {
;          public Executor run() {
;              return Executors.newCachedThreadPool
;                  (new ProcessReaperThreadFactory());
;          }});

; (defn launch-unix-proc! ; formerly the UNIXProcess constructor
;   [^"[B" prog
;    ^"[B" argBlock
;    argc
;    ^"[B" envBlock
;    envc
;    ^"[B" dir
;    ^"[I" fds
;    redirectErrorStream] ; boolean
;    )
;   (let [pid (forkAndExec
;              launchMechanism.value
;              helperpath
;              prog
;              argBlock (int argc)
;              envBlock (int envc)
;              dir
;              fds
;              (boolean redirectErrorStream))]

;          (java.security.AccessController/doPrivileged
;            (proxy [PrivilegedExceptionAction] []
;              (run [] 
;                (initStreams fds)
;                nil)))
;     )

     

     
;  }

;  static FileDescriptor newFileDescriptor(int fd) {
;      FileDescriptor fileDescriptor = new FileDescriptor();
;      fdAccess.set(fileDescriptor, fd);
;      return fileDescriptor;
;  }

; (defn initStreams [^"[I" fds]
;   stdin = (= (get+ fds 0) -1) ?
;       (. ProcessBuilder$NullOutputStream INSTANCE) :
;       (ProcessPipeOutputStream. fds[0]);

;   stdout = (= (get+ fds 1) -1) ?
;       (. ProcessBuilder$NullInputStream INSTANCE) :
;       (ProcessPipeInputStream. fds[1]);

;   stderr = (= (get+ fds 2) -1) ?
;       (. ProcessBuilder$NullInputStream INSTANCE):
;       (ProcessPipeInputStream. fds[2]);

;   processReaperExecutor.execute(
;    (proxy [Runnable] []
;       (run [] 
;           int exitcode = waitForProcessExit(pid)
;           UNIXProcess.this.processExited(exitcode)))))
 

; (defn processExited [exitcode]
;   synchronized (this) {
;       this.exitcode = (int exitcode);
;       hasExited = true;
;       notifyAll();
;   }

;   (when (instance? ProcessPipeInputStream stdout)
;      (.processExited stdout))
;   (when (instance? ProcessPipeInputStream stderr)
;      (.processExited stderr))
;   (when (instance? ProcessPipeOutputStream stdin)
;      (.processExited stdin)))

;  public synchronized int waitFor() throws InterruptedException {
;      while (!hasExited) {
;          wait();
;      }
;      return exitcode;
;  }

;  @Override
;  public synchronized boolean waitFor(long timeout, TimeUnit unit)
;      throws InterruptedException
;  {
;      if (hasExited) return true;
;      if (timeout <= 0) return false;

;      long timeoutAsNanos = unit.toNanos(timeout);
;      long startTime = System.nanoTime();
;      long rem = timeoutAsNanos;

;      while (!hasExited && (rem > 0)) {
;          wait(Math.max(TimeUnit.NANOSECONDS.toMillis(rem), 1));
;          rem = timeoutAsNanos - (System.nanoTime() - startTime);
;      }
;      return hasExited;
;  }

;  public synchronized int exitValue() {
;      if (!hasExited) {
;          throw new IllegalThreadStateException("process hasn't exited");
;      }
;      return exitcode;
;  }

;  private static native void destroyProcess(int pid, boolean force);
;  private void destroy(boolean force) {
;      ; There is a risk that pid will be recycled, causing us to
;      ; kill the wrong process!  So we only terminate processes
;      ; that appear to still be running.  Even with this check,
;      ; there is an unavoidable race condition here, but the window
;      ; is very small, and OSes try hard to not recycle pids too
;      ; soon, so this is quite safe.
;      synchronized (this) {
;          if (!hasExited)
;              destroyProcess(pid, force);
;      }
;      (try (.close stdin ) (catch IOException _))
;      (try (.close stdout) (catch IOException _))
;      (try (.close stderr) (catch IOException _))
;  }

;  public void destroy() {
;      destroy(false);
;  }

;  @Override
;  public Process destroyForcibly() {
;      destroy(true);
;      return this;
;  }

;  @Override
;  public synchronized boolean isAlive() {
;      return (not hasExited)
;  }

;  private static native void init();

;  static {
;      init();
;  }

    
; ;A buffered input stream for a subprocess pipe file descriptor that allows the underlying file descriptor to be reclaimed when the process exits, via the processExited hook. This is tricky because we do not want the user-level InputStream to be closed until the user invokes close(), and we need to continue to be able to read any buffered data lingering in the OS pipe buffer.

;  static class ProcessPipeInputStream extends BufferedInputStream {
;      private final Object closeLock = new Object();

;      ProcessPipeInputStream(int fd) {
;          super(new FileInputStream(newFileDescriptor(fd)));
;      }

;      private InputStream drainInputStream(InputStream in)
;              throws IOException {
;          int n = 0;
;          int j;
;          byte[] a = null;
;          synchronized (closeLock) {
;              if (buf == null) // asynchronous close()?
;                  return null; // discard
;              j = in.available();
;          }
;          while (j > 0) {
;              a = (a == null) ? new byte[j] : Arrays.copyOf(a, n + j);
;              synchronized (closeLock) {
;                  if (buf == null) // asynchronous close()?
;                      return null; // discard
;                  n += in.read(a, n, j);
;                  j = in.available();
;              }
;          }
;          return (a == null) ?
;                  ProcessBuilder.NullInputStream.INSTANCE :
;                  new ByteArrayInputStream(n == a.length ? a : Arrays.copyOf(a, n));
;      }

        
; ;Called by the process reaper thread when the process exits.
;      synchronized void processExited() {
;         try {
;             InputStream in = this.in;
;             if (in != null) {
;                 InputStream stragglers = drainInputStream(in);
;                 in.close();
;                 this.in = stragglers;
;             }
;         } catch (IOException ignored) { }
;     }
;      @Override
;     public void close() throws IOException {
;         // BufferedInputStream#close() is not synchronized unlike most other methods.
;         // Synchronizing helps avoid racing with drainInputStream().
;         synchronized (closeLock) {
;             super.close();
;         }
;     }
; }

    
; ;A buffered output stream for a subprocess pipe file descriptor that allows the underlying file descriptor to be reclaimed when the process exits, via the processExited hook.
; static class ProcessPipeOutputStream extends BufferedOutputStream {
;     ProcessPipeOutputStream(int fd) {
;         super(new FileOutputStream(newFileDescriptor(fd)));
;     }

        
; (defn processExited
;   "Called by the process reaper thread when the process exits."
;   {:contributor "Sun Microsystems 1995-2013"}
;   [] 
;   (locking ; same as "synchronized" before the method name
;     (let [^OutputStream out this.out]
;       (when (nnil? out) 
;         (try
;           (.close out)
;           ; We know of no reason to get an IOException, but if
;           ; we do, there's nothing else to do but carry on.
;           (catch IOException _))
;         this.out = ProcessBuilder.NullOutputStream.INSTANCE))))

