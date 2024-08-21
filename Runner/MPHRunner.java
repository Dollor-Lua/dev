package Runner;

public class MPHRunner {
    private double mph = 0;
    private double miles = 0;
    private double minutes = 0;
    private double hours = 0;

    MPHRunner(double miles, double minutes, double hours) {
        this.miles = miles;
        this.minutes = minutes;
        this.hours = hours;
        this.mph = this.miles / (this.hours + this.minutes / 60);
    }

    public void printMPH() {
        System.out.println("MPH: " + mph);
    }

    public String toString() {
        return "MPH: " + mph;
    }
}
