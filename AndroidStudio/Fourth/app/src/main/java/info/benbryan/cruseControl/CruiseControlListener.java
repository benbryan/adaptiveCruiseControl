package info.benbryan.cruseControl;

public interface CruiseControlListener {
    public void lineRecieved(String line);
    public void adcReading(int adcIdx, int value);
    public void onConnect();
    public void onDisconnect();
}
