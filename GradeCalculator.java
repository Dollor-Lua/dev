// GradeCalculator.java - @starlitnova - 11/8/24 - file created

import java.util.Scanner;

class GradeCalculator {
    // getDoubleInput(sc, message) -> double
    // @arg sc: Scanner - the scanner object
    // @arg message: String - the message before the user's input
    // Returns an double value, retrying if the user inputs an invalid string
    static double getDoubleInput(Scanner sc, String message) {
        while (true) {
            try {
                System.out.print(message);
                return sc.nextDouble();
            } catch (java.util.InputMismatchException e) {
                sc.next();
                System.out.println("[!] Enter a valid decimal");
                continue;
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // get the current grade, goal grade, and percent the final is worth
        double grade = getDoubleInput(scanner, "Current grade %: ");
        double goal = getDoubleInput(scanner, "Goal grade %: ");
        double finalPercentage = getDoubleInput(scanner, "Final worth %: ");

        // the percent classwork is worth is 100 - finalPercentage, divide by 100 for decimal value
        double classworkWorth = (100 - finalPercentage) / 100;
        // calculate final score
        // derived from the equation: goal = classworkWorth * grade + finalScore * finalPercentage
        // aka the equation for a weighted average.
        // * 100 is for it to be converted from a decimal value into a percentage
        double finalScore = (goal - classworkWorth * grade) / finalPercentage * 100; 

        if (finalScore > 100) {
            // if this score is unreachable, lets tell them!
            System.out.printf("You cannot reach this score without extra credit (%.2f%% required)\n", finalScore);
        } else if (finalScore <= 0) {
            // tell them they're guaranteed their score, although some people like to see the negative for fun
            System.out.printf("You are guaranteed your goal grade (%.2f%%)\n", finalScore);
        } else {
            // otherwise, if they aren't guaranteed to hit/miss, then show them what score they need
            System.out.printf("Score required to pass: %.2f%%\n", finalScore);
        }

        // if you don't close the scanner your program will LEAK MEMORY!!
        scanner.close();
    }
}

// do I use too many comments?