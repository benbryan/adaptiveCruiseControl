package info.benbryan.cruseControl;

public class GlobalVariables {
    private static GlobalVariables instance;

    // Global variable
    private CruiseControlService cruiseControlService;

    // Restrict the constructor from being instantiated
    private GlobalVariables(){}

    public CruiseControlService getCruiseControlService() {
        return cruiseControlService;
    }

    public void setCruiseControlService(CruiseControlService cruiseControlService) {
        this.cruiseControlService = cruiseControlService;
    }

    public static synchronized GlobalVariables getInstance(){
        if(instance==null){
            instance=new GlobalVariables();
        }
        return instance;
    }
}
