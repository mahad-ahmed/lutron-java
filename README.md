# lutron-java
A Java client to control Lutron Homeworks devices. (This is a work-in-progress, some features will be missing!)</br>


## Usage:
### Initialize a client
`LutronClient client = new LutronClient(<IP>, <PORT>);`

</br>

### Create a ConnectionStateListener
```
LutronClient.ConnectionStateListener connectionStateListener = new LutronClient.ConnectionStateListener() {
    @Override
    public void onStateChanged(LutronClient client, int status) {
        if(status == STATUS_CONNECTED) {
            // Connected! You can start sending control commands now.
            // Open a curtain for e.g: client.openCurtain(<INTEGRATION_ID>);
            // Turn on a light: client.ledOn(<INTEGRATION_ID>);
        }
    }

    @Override
    public void onException(LutronClient client, IOException ex) {
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

### Start the connection
`client.connect(connectionStateListener);`

</br>

### Let there be light
`client.ledOn(<INTEGRATION_ID>);`

</br>
</br>

## Notes:
* #### To make it work with WearOS, you need to add a carriage return('\r') character after every message (including login & password)

</br>
</br>

## TODO:
* #### XML dump parsing (Get a list of devices)
* #### Button pad controls
* #### (?) Auto send last failed command after reconnect
