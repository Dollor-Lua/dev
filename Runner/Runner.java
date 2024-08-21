package Runner;

import java.util.Scanner;

public class Runner {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        double miles, hours, minutes;
        while (true) {
            try {
                System.out.println("Enter the miles, hours, and minutes: ");
                String input = scanner.nextLine();
                String[] inputArray = input.split(" ");
                miles = Double.parseDouble(inputArray[0]);
                hours = Double.parseDouble(inputArray[1]);
                minutes = Double.parseDouble(inputArray[2]);
            } catch (Exception e) {
                System.out.println("Invalid input. Please try again.");
                continue;
            }

            break;
        }

        MPHRunner mphRunner = new MPHRunner(miles, minutes, hours);
        mphRunner.printMPH();

        scanner.close();
    }
}
