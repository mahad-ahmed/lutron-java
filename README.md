# lutron-java
A Java client to control Lutron Homeworks devices</br></br>


## Usage:
#### Initialize a client
`LutronClient client = new LutronClient(<IP>, <PORT>);`

</br>

#### Create a ConnectionStateListener
```
LutronClient.ConnectionStateListener connectionStateListener = new LutronClient.ConnectionStateListener() {
    @Override
    public void onStateChanged(int status) {
        if(status == STATUS_CONNECTED) {
            // Connected! You can start sending control commands now.
        }
    }

    @Override
    public void onException(IOException ex) {
        ex.printStackTrace();
    }

    @Override
    public String onLoginPrompt() {
        return "<YOUR_LOGIN>";
    }

    @Override
    public String onPasswordPrompt() {
        return "<YOUR_PASSWORD>";
    }
};
```

</br>

#### Start the connection
`client.connect(connectionStateListener);`