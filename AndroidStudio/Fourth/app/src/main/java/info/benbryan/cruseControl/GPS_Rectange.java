package info.benbryan.cruseControl;

public class GPS_Rectange{
    public final double latitudeSpan[],
            longitudeSpan[];
    public GPS_Rectange(double[] latitudeSpan, double[] longitudeSpan) {
        if ((latitudeSpan.length != 2) || (longitudeSpan.length != 2)){
            throw new IllegalArgumentException("Spans must have 2 elements");
        }
        if (latitudeSpan[0] > latitudeSpan[1]){
            double temp = latitudeSpan[1];
            latitudeSpan[1] = latitudeSpan[0];
            latitudeSpan[0] = temp;
        }
        if (longitudeSpan[0] > longitudeSpan[1]){
            double temp = longitudeSpan[1];
            longitudeSpan[1] = longitudeSpan[0];
            longitudeSpan[0] = temp;
        }
        this.latitudeSpan = latitudeSpan;
        this.longitudeSpan = longitudeSpan;
    }
}