package com.sam58;

public class Main {

    private static JMSWindow windowjms;
//-XX:+UnlockCommercialFeatures  -XX:+FlightRecorder -XX:StartFlightRecording=duration=120m,filename=recording.jfr
    public static void main(String[] args) {
	// write your code here
        windowjms = new JMSWindow();
        JMSApplication jmsApplication = new JMSApplication(windowjms);
        windowjms.setVisible(true);
    }
}
