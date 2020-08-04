import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//  TODO: XML parsing for the system dump
//  TODO: Fade time when setting level
//  TODO: A synchronous getLevel() function
//  TODO: Button pad controls

class LutronClient implements Runnable {

    /**
     * Listen and respond to connection events.
     * An implementation of this class is required for the connection.
     */
    public abstract static class ConnectionStateListener {
        public static final int STATUS_CONNECTED = 0;
        public static final int STATUS_DISCONNECTED = 1;
        public static final int STATUS_CONNECT_FAILED = -1;
        public static final int STATUS_BAD_LOGIN = 2;
        public static final int STATUS_TOO_MANY_ATTEMPTS = 3;
        public static final int STATUS_EOF = 4;
//        public static final int STATUS_RECONNECTED


        /**
         * Called whenever connection status changes.
         * @param status The current status of the connection:
         *               {@value #STATUS_CONNECTED}
         *               {@value #STATUS_DISCONNECTED}
         *               {@value #STATUS_CONNECT_FAILED}
         *               {@value #STATUS_BAD_LOGIN}
         *               {@value #STATUS_TOO_MANY_ATTEMPTS}
         *               {@value #STATUS_EOF}
         */
        abstract void onStateChanged(LutronClient client, int status);


        /**
         * Called when an exception relating to the connection is incurred.
         * @param ex The exception
         */
        abstract void onException(LutronClient client, IOException ex);


        /**
         * This should be overridden to return the login(username) for the system.
         * @return System username
         */
        abstract String onLoginPrompt();


        /**
         * This should be overridden to return the password for the system.
         * @return System password
         */
        abstract String onPasswordPrompt();


        /**
         * Get the String representation of a status code.
         * @param status Status code
         * @return String description of the status code
         */
        static String getStatusString(int status) {
            return switch (status) {
                case STATUS_CONNECTED -> "STATUS_CONNECTED";
                case STATUS_EOF -> "STATUS_EOF";
                case STATUS_BAD_LOGIN -> "STATUS_BAD_LOGIN";
                case STATUS_TOO_MANY_ATTEMPTS -> "STATUS_TOO_MANY_ATTEMPTS";
                case STATUS_CONNECT_FAILED -> "STATUS_CONNECT_FAILED";
                case STATUS_DISCONNECTED -> "STATUS_DISCONNECTED";
                default -> "UNKNOWN_STATUS";
            };
        }
    }


    /**
     * Can be implemented and registered to listen for level changes
     */
    public interface OnLevelChangeListener {
        /**
         * Called when any device level/state is broadcast.
         * This can also be requested with {@link LutronClient#requestLevel(int)}.
         *
         * @param integrationId The device that changed its level
         * @param level The new level(%) of the device
         */
        void onLevelChange(LutronClient client, int integrationId, float level);
    }


    private final String host; /** System address */
    private final int port; /** System port */

    private Socket socket;
    private InputStream input = null;
    private PrintStream output = null;

    private StringBuffer buffer = new StringBuffer();

    private ConnectionStateListener connectionStateListener;

    private final ArrayList<OnLevelChangeListener> onLevelChangeListeners = new ArrayList<>();

    private final Pattern pattern = Pattern.compile("~OUTPUT,\\d+,1,\\d+\\.\\d\\d");


    /**
     * Register a new {@link OnLevelChangeListener} object.
     *
     * @param listener The OnLevelChangeListener object to register.
     */
    void addOnLevelChangeListener(OnLevelChangeListener listener) {
        onLevelChangeListeners.add(listener);
    }


    /**
     * Remove(unregister) a {@link OnLevelChangeListener} object.
     *
     * @param listener The OnLevelChangeListener object to remove.
     */
    void removeOnLevelChangeListener(OnLevelChangeListener listener) {
        onLevelChangeListeners.remove(listener);
    }


    /**
     * @param host System address
     * @param port System port
     */
    LutronClient(String host, int port) {
        this.host = host;
        this.port = port;
    }


    /**
     * (Re)Initialize the connection
     *
     * @param listener Required implementation of {@link ConnectionStateListener}
     *                 // TODO: Add NotNull annotation
     */
    public void connect(ConnectionStateListener listener) {
        this.connectionStateListener = listener;
        disconnect();
        try {
            socket = new Socket(host, port);
            input = socket.getInputStream();
            output = new PrintStream(socket.getOutputStream());
            new Thread(this).start();
        }
        catch(IOException ex) {
            new Thread(() -> connectionStateListener.onStateChanged(this, ConnectionStateListener.STATUS_CONNECT_FAILED)).start();
            listener.onException(this, ex);
        }
    }


    /**
     * Cleanup: Try to go out like a good boy.
     */
    private void disconnect() {
        if(input != null) {
            try {
                input.close();
            }
            catch(IOException ignored) {}
            input = null;
        }

        if(output != null) {
            output.close();
            output = null;
        }

        if(socket != null) {
            try {
                socket.close();
            }
            catch(IOException ignored) {}
            socket = null;
        }
    }

    private void sendMessage(String message) {
        try {
            // TODO: Option to check and reconnect automatically?
            output.println(message);
            output.flush();
        }
        catch(Exception ex) {
//            connect(connectionStateListener);
            ex.printStackTrace();
        }
    }


    /**
     * Try to set the device to the specified level (if it's a reasonable number).
     *
     * @param integrationId The device to set
     * @param level The level to set (in %)
     */
    void setLevel(int integrationId, float level) {
        if(level > 100 || level < 0) {
            return;
        }
        sendMessage("#OUTPUT," + integrationId + ",1," + level);
    }


    /**
     * Send out a request to get the level of a device.
     *
     * NOTE: This function will not return the level value;
     * Instead register a {@link OnLevelChangeListener} before calling this function.
     *
     * @param integrationId ID of the device to investigate the level of.
     */
    void requestLevel(int integrationId) {
        sendMessage("?output," + integrationId + ",1");
    }

    void openCurtain(int integrationId) {
        sendMessage("#OUTPUT," + integrationId + ",3");
    }

    void stopCurtain(int integrationId) {
        sendMessage("#OUTPUT," + integrationId + ",4");
    }

    void closeCurtain(int integrationId) {
        sendMessage("#OUTPUT," + integrationId + ",2");
    }

    void raiseShade(int integrationId) {
        sendMessage("#OUTPUT," + integrationId + ",2");
    }

    void stopShade(int integrationId) {
        sendMessage("#OUTPUT," + integrationId + ",4");
    }

    void dropShade(int integrationId) {
        sendMessage("#OUTPUT," + integrationId + ",3");
    }

    void ledStop(int integrationId) {
        sendMessage("#OUTPUT," + integrationId + ",1,0");
    }

    void ledOn(int integrationId) {
        sendMessage("#OUTPUT," + integrationId + ",1,100");
    }

    void ledOff(int integrationId) {
        sendMessage("#OUTPUT," + integrationId + ",1,0");
    }

//    public void logout() {
//        output.println("logout");
//    }

    @Override
    public void run() {
        try {
            boolean checkLogin = false;
            char c;
            while((c = (char) input.read()) != '\uFFFF') {
                buffer.append(c);
                if(c == ':') {  /* No! endsWith() does NOT check the last character first anyway. */
                    if(buffer.toString().endsWith("login:")) {
                        output.println(connectionStateListener.onLoginPrompt());
                        output.flush();
                    }
                    else if(buffer.toString().endsWith("password:")) {
                        checkLogin = true;
                        output.println(connectionStateListener.onPasswordPrompt());
                        output.flush();
                    }
                }
                else if(checkLogin) {
                    if(buffer.toString().endsWith("NET>")) {
                        new Thread(() -> connectionStateListener.onStateChanged(this, ConnectionStateListener.STATUS_CONNECTED)).start();
                    }
                    else if(buffer.toString().endsWith("bad login")) {
                        new Thread(() -> connectionStateListener.onStateChanged(this, ConnectionStateListener.STATUS_BAD_LOGIN)).start();
                    }
                    else if(buffer.toString().endsWith("login attempts.")) {
                        new Thread(() ->connectionStateListener.onStateChanged(this, ConnectionStateListener.STATUS_TOO_MANY_ATTEMPTS)).start();
                    }
                    else {
                        continue;
                    }

                    checkLogin = false;
                }
                else if(c == '\n') {
                    String[] arr = buffer.toString().split("\n");
                    Matcher matcher = pattern.matcher(arr[arr.length - 1]);
                    if (matcher.find()) {
                        String[] str = arr[arr.length - 1].substring(matcher.start() + 8, matcher.end()).split(",");
                        if(str.length > 2) {
                            int id = Integer.parseInt(str[0]);
                            float level = Float.parseFloat(str[2]);

//                            System.out.println("\nDevice: " + id + "\nLevel: " + level + "\n");

//                            if(levelsMap.getOrDefault(id, -1f) == -9f) {
//                                levelsMap.put(id, level);
//                            }
                            for (OnLevelChangeListener onLevelChangeListener : onLevelChangeListeners) {
                                new Thread(() -> onLevelChangeListener.onLevelChange(this, id, level)).start();
                            }
                            buffer = new StringBuffer();
                        }
                    }
                }
            }

            disconnect();

            /*  Got EOF  */
            new Thread(() -> connectionStateListener.onStateChanged(this, ConnectionStateListener.STATUS_EOF)).start();
            new Thread(() -> connectionStateListener.onStateChanged(this, ConnectionStateListener.STATUS_DISCONNECTED)).start();
        }
        catch(IOException ioe) {
            //ioe.printStackTrace();
            disconnect();
            new Thread(() -> connectionStateListener.onStateChanged(this, ConnectionStateListener.STATUS_DISCONNECTED)).start();
            new Thread(() -> connectionStateListener.onException(this, ioe)).start();
        }
    }
}
