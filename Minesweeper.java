// Minesweeper.java - @starlitnova - 13/8/24 - added comments and cursor + help menu

// Created for AP Computer Science
// I HATE using external libraries! So I made it without any.
// Java libraries are the BEST c:

// The game is entirely configurable when ran, no need to modify code,
// unless you're changing the cutoff widths because you have a larger/smaller terminal

import java.util.Random;
import java.util.Scanner;

public class Minesweeper {
    // for removing spaces in between grid cells if the grid is too wide for the terminal
    private static final int CutoffWidth1 = 34;
    private static final int CutoffWidth2 = 60;

    // pregame configurations, set in the console at the start
    private static int GridWidth = 10;
    private static int GridHeight = 10;
    private static int Mines = 5;

    // stores the tile that exploded the player
    private static int[] mLosingTile = new int[2];

    // stores the tile the cursor is currently on
    private static int[] mSelected = new int[2];

    // tells if the tile selector/cursor is currently active
    private static Boolean mCursorActive = false;

    // Grid States:
    // 0 - Unchecked, no mine
    // 1 - Unchecked, mine
    // 2 - Checked, safe
    // 3 - Flagged, mine
    // 4 - Flagged, no mine

    // Stores every grid tile and its state
    private static int[][] mGrid;

    // check(x, y) -> int
    // @arg x: int - the x coordinate of the tile
    // @arg y: int - the y coordinate of the tile
    // checks if a tile is a mine, returning 1 if it is and 0 if it isn't
    // returns 0 if the tile does not exist
    static int check(int x, int y) {
        if (x < 0 || y < 0 || x >= GridWidth || y >= GridHeight) return 0;
        return (mGrid[x][y] == 1 || mGrid[x][y] == 3) ? 1 : 0;
    }

    // getTouching(x, y) -> int
    // @arg x: int - the x coordinate of the tile
    // @arg y: int - the y coordinate of the tile
    // returns the number of mines touching the tile specified
    static int getTouching(int x, int y) {
        int x1 = x + 1, x2 = x - 1;
        int y1 = y + 1, y2 = y - 1;

        // the reason check returns an int is so the return values can be added as such
        int mines = check(x2, y1) + check(x, y1) + check(x1, y1)
                  + check(x2, y)  +                check(x1, y)
                  + check(x2, y2) + check(x, y2) + check(x1, y2);

        return mines;
    }

    // revealSafe(x, y)
    // @arg x: int - the x coordinate of the tile
    // @arg y: int - the y coordinate of the tile
    // reveals a tile as long as it is safe.
    // will recursively reveal tiles around it as long as getTouching(x, y) returns 0 for the tile.
    static void revealSafe(int x, int y) {
        if (x < 0 || y < 0 || x >= GridWidth || y >= GridHeight) return; // safety first

        // check if the tile is safe
        if (mGrid[x][y] == 0) {
            // reveal it
            mGrid[x][y] = 2;

            // reveal surrounding tiles
            if (getTouching(x, y) == 0) {
                revealSafe(x - 1, y - 1); // top left
                revealSafe(x, y - 1); // top
                revealSafe(x + 1, y - 1); // top right
                revealSafe(x - 1, y); // left
                revealSafe(x + 1, y); // right
                revealSafe(x - 1, y + 1); // bottom left
                revealSafe(x, y + 1); // bottom
                revealSafe(x + 1, y + 1); // bottom right
            }
        }
    }

    // getWriteColor(mines) -> String
    // @arg mines: int - the number of mines around the number
    // Outputs a string with the number color coded properly
    static String getWriteColor(int mines) {
        switch (mines) {
            case 1:
                return "\033[94m" + mines + "\033[0m"; // bright blue
            case 2:
                return "\033[92m" + mines + "\033[0m"; // bright green
            case 3:
                return "\033[91m" + mines + "\033[0m"; // bright red
            case 4:
                return "\033[34m" + mines + "\033[0m"; // dark blue
            case 5:
                return "\033[31m" + mines + "\033[0m"; // dark red
            case 6:
                return "\033[96m" + mines + "\033[0m"; // bright teal
            case 7:
                return "\033[32m" + mines + "\033[0m"; // dark green
            case 8:
                return "\033[90m" + mines + "\033[0m"; // bright black
            default:
                return "" + mines;
        }
    }

    // redrawGrid(loses)
    // @arg loses: Boolean - if the last move caused the player to lose
    // Redraws the board, without any extra information
    static void redrawGrid(Boolean loses) {
        // Calculate the spaces between cells depending on the cutoffs
        int spaceCount = 2;
        if (GridWidth > CutoffWidth2) {
            spaceCount = 0;
        } else if (GridWidth > CutoffWidth1) {
            spaceCount = 1;
        }

        // Create variables to hold the exact number of spaces (and underscores) to fill excess space
        String spaces = "";
        String dashes = "";
        for (int i = 0; i < spaceCount; i++) {
            spaces += " ";
            dashes += "_";
        }

        // Calculate the maximum length of the largest coordinate number
        int columns = ("" + GridWidth).length();
        int rows = ("" + GridHeight).length();
        String maxSpaces = spaces;
        for (int i = 0; i < rows; i++) {
            maxSpaces += " ";
        }

        // Output the X coordinates, split by line and going downwards
        for (int c = columns - 1; c >= 0; c--) { // variable is c for columns, but they're actually rows
            System.out.print(maxSpaces + " "); // left padding
            for (int i = 0; i < GridWidth; i++) {
                String v = "" + i;
                // if the number is shorter than the current row (ex 3 is one char shorter than 12)
                // output a space in place of it
                if (c > v.length() - 1) {
                    System.out.print(" " + spaces);
                } else {
                    // otherwise output the digit in that place
                    System.out.print("\033[33;1m" + v.charAt(v.length() - c - 1) + "\033[0m" + spaces);
                }
            }
            System.out.println("");
        }

        // add a separating dash to keep the board and coordinates separated
        System.out.print(maxSpaces);
        for (int i = 0; i < GridWidth; i++)
            System.out.print("_" + dashes);
        System.out.println("");

        // main grid drawing
        for (int y = 0; y < GridHeight; y++) {
            // draw the row number
            // format string means left padded so that the length of the string is always rows+spaceCount
            // ex, if the largest row was 1000, then itd output "3    |" and "253  |"
            System.out.printf("\033[33;1m%-"+(rows+spaceCount)+"s\033[0m|", y);

            // loop over individual cells
            for (int x = 0; x < GridWidth; x++) {
                // if the move is losing and the losing tile is this one, mark it with an X
                // so the user knows where they lost
                if (loses && mLosingTile[0] == x && mLosingTile[1] == y) {
                    System.out.print("\033[31;1mX\033[0m" + spaces);
                    continue;
                }

                // store a temporary variable of the spaces variable
                String tempSpaces = spaces;
                if (mCursorActive && mSelected[0] == x && mSelected[1] == y) {
                    // if the cursor is active and on this cell, draw an arrow before and after
                    System.out.print("\b>");
                    // temp spaces variable was to override the spaces variable here
                    spaces = "<";
                    if (tempSpaces.length() > 0) {
                        spaces += tempSpaces.substring(1);
                    }
                } else if (mCursorActive && mSelected[0] == x - 1 && mSelected[1] == y && spaces.length() == 0) {
                    // if theres not enough room since theres no spaces (grid got hit by the cutoff)
                    // then replace the right index with an arrow (you probably don't need it right?)
                    System.out.print("<");
                    continue; // skip rendering this cell
                }

                if (mGrid[x][y] == 0 || mGrid[x][y] == 1) {
                    // if the tile is not revealed, render it as a hashtag
                    System.out.print("#" + spaces);
                } else if (mGrid[x][y] == 3 || mGrid[x][y] == 4) {
                    // if the tile is flagged, show it as an F
                    System.out.print("\033[33;1mF\033[0m" + spaces);
                } else if (mGrid[x][y] == 2) {
                    // if the tile is revealed, show it as the number of mines surrounding it
                    int count = getTouching(x, y);
                    if (count == 0) {
                        // if theres no mines, leave it empty
                        System.out.print(" " + spaces);
                    } else
                        System.out.print(getWriteColor(getTouching(x, y)) + spaces);
                }

                // restore spaces back to its original value
                spaces = tempSpaces;
            }

            System.out.println("");
        }
    }

    // redrawGrid()
    // Redraws the board, without any extra information
    static void redrawGrid() {
        redrawGrid(false);
    }

    // cls()
    // Clears the console. Only works with consoles supporting escape sequences
    static void cls() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    // colorTest()
    // Tests the color profile for minesweeper
    static void colorTest() {
        System.out.println("Mine colors: ");
        for (int i = 1; i <= 8; i++) {
            System.out.print(getWriteColor(i) + " ");
        }

        System.out.println("");
    }

    // getIntInput(sc, message) -> int
    // @arg sc: Scanner - the scanner object
    // @arg message: String - the message before the user's input
    // Returns an integer value, retrying if the user inputs an invalid string
    static int getIntInput(Scanner sc, String message) {
        while (true) {
            try {
                System.out.print(message);
                return sc.nextInt();
            } catch (java.util.InputMismatchException e) {
                sc.next();
                System.out.println("[!] Enter a valid integer");
                continue;
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Set up the board
        GridWidth = getIntInput(scanner, "Grid Width > ");
        GridHeight = getIntInput(scanner, "Grid Height > ");
        Mines = getIntInput(scanner, "Number of Mines > ");

        mGrid = new int[GridWidth][GridHeight];

        Random rand = new Random();
        for (int x = 0; x < GridWidth; x++) {
            for (int y = 0; y < GridHeight; y++) {
                mGrid[x][y] = 0;
            }
        }

        // place the mines
        for (int m = 0; m < Mines; m++) {
            if (m >= GridWidth * GridHeight) {
                break; // safety first
            }

            while (true) {
                int x = rand.nextInt(GridWidth);
                int y = rand.nextInt(GridHeight);

                // keep trying to place the mine until it succeeds
                if (mGrid[x][y] == 0) {
                    mGrid[x][y] = 1;
                    break;
                }
            }
        }
        
        
        int firstMove = 0; // stores if the first move has happened

        // Write the cutoff warnings for the user
        cls();
        if (GridWidth > CutoffWidth2) {
            System.out.println("Grid size is very large, truncating board to fit (0 spaces)");
        } else if (GridWidth > CutoffWidth1) {
            System.out.println("Grid size is very large, truncating board to fit (1 space)");
        }

        if (GridHeight > 21) {
            System.out.println("[!] WARNING [!] Your grid height is very large and may not fit");
        }

        // main game loop
        while (true) {
            redrawGrid();

            // write basic controls for the game
            if (mCursorActive) {
                System.out.print("(" + mSelected[0] + ", " + mSelected[1] + ") ");
            }
            System.out.print("Type \"exit\" to exit. For more commands type \"help\"\n");
            System.out.print("Enter your move (R = reveal, F = flag, then place coordinate, ex R0,0)\n");
            System.out.print("> ");

            // get the user's input
            String output = scanner.nextLine();

            if (output.equals("exit")) break; // so long bowser!

            // help menu (dear god)
            if (output.equals("help")) {
                cls();
                System.out.println("NOTE: The command coordinate system DOES NOT support spaces. 'R 5, 9' is NOT supported.");
                System.out.println("Basic moves:");
                System.out.println("  R - Revealing tiles / reveals the coordinate specified (ex R5,9)");
                System.out.println("  F - Flags tiles / flags the coordinate specified (ex F5,9)");
                System.out.println("");
                System.out.println("Select/keyboard controls:");
                System.out.println("  Typing w/a/s/d and pressing enter will show a selector/cursor.");
                System.out.println("  Pressing enter (sending an empty line) while this cursor is active will reveal the tile");
                System.out.println("  up/down/left/right arrow keys are not supported.");
                System.out.println("");
                System.out.println("Win conditions:");
                System.out.println("  You can only win if either -");
                System.out.println("    1. All bombs are flagged, and there are no safe tiles flagged");
                System.out.println("    2. All safe tiles are revealed, regardless of bombs being flagged");
                System.out.println("  Your goal is to not get blown up!");
                System.out.println("  Note the number on the tile indicates how many of the 8 surrounding tiles are bombs");
                System.out.println("");
                System.out.println("Enter \"colors\" into the game input to view the color profile");
                System.out.println("");
                System.out.println("Press enter to return to the game.");
                scanner.nextLine();
                cls();
                continue;
            } else if (output.equals("colors")) {
                cls();
                colorTest();
                System.out.println("Press enter to return to the game.");
                scanner.nextLine();
                cls();
                continue;
            }

            if (output.isEmpty()) {
                // if the output is empty, but the cursor is active, reveal the tile (Enter pressed)
                if (mCursorActive) {
                    output = "R" + mSelected[0] + "," + mSelected[1];
                    mCursorActive = false;
                } else {
                    cls();
                    continue;
                }
            }

            char command = output.charAt(0); // the command

            // if the command was a movement direction, move the cursor and activate it
            if (command == 'd') {
                mSelected[0] += 1;
                if (mSelected[0] >= GridWidth) {
                    mSelected[0] = 0;
                }

                mCursorActive = true;
                cls();
                continue;
            } else if (command == 'a') {
                mSelected[0] -= 1;
                if (mSelected[0] < 0) {
                    mSelected[0] = GridWidth - 1;
                }

                mCursorActive = true;
                cls();
                continue;
            } else if (command == 's') {
                mSelected[1] += 1;
                if (mSelected[1] >= GridHeight) {
                    mSelected[1] = 0;
                }

                mCursorActive = true;
                cls();
                continue;
            } else if (command == 'w') {
                mSelected[1] -= 1;
                if (mSelected[1] < 0) {
                    mSelected[1] = GridHeight - 1;
                }

                mCursorActive = true;
                cls();
                continue;
            } else if (command == 'f') {
                // cursor based flagging
                // flag the tile (multiple cases to prevent losing of data)
                if (mGrid[mSelected[0]][mSelected[1]] == 1) {
                    mGrid[mSelected[0]][mSelected[1]] = 3;
                } else if (mGrid[mSelected[0]][mSelected[1]] == 0) {
                    mGrid[mSelected[0]][mSelected[1]] = 4;
                }

                mCursorActive = false;
                cls();
                continue;
            }

            // get the coordinates, and make sure they exist
            String[] components = output.substring(1).split(",");

            if (components.length != 2) {
                continue;
            }

            int x, y;

            try {
                x = Integer.parseInt(components[0]);
                y = Integer.parseInt(components[1]);
            } catch(NumberFormatException e) {
                continue;
            }

            if (command == 'R') {
                // if the tile is a bomb, end the game, otherwise reveal the tile
                if (mGrid[x][y] == 1 && firstMove != 0) {
                    mLosingTile[0] = x;
                    mLosingTile[1] = y;

                    cls();
                    redrawGrid(true);
                    System.out.println("You lose!");
                    break;
                } else {
                    mGrid[x][y] = 0;
                    revealSafe(x, y);
                    firstMove = 1;
                }
            } else if (command == 'F') {
                // flag the tile (multiple cases to prevent losing of data)
                if (mGrid[x][y] == 1) {
                    mGrid[x][y] = 3;
                } else if (mGrid[x][y] == 0) {
                    mGrid[x][y] = 4;
                }
            }

            // check for win conditions
            int badFlags = 0;
            int badFlags2 = 0;
            for (int xx = 0; xx < GridWidth; xx++) {
                for (int yy = 0; yy < GridHeight; yy++) {
                    if (mGrid[xx][yy] == 1 || mGrid[xx][yy] == 4)
                        badFlags++;
                    if (mGrid[xx][yy] == 0 || mGrid[xx][yy] == 4)
                        badFlags2++;
                }
            }

            // if all bombs are flagged and no safe tiles are flagged, that's a win
            if (badFlags == 0) {
                cls();
                redrawGrid();
                System.out.println("You win!");
                break;
            }

            // if all safe tiles are revealed, that's a win
            if (badFlags2 == 0) {
                cls();
                redrawGrid();
                System.out.println("You win!");
                break;
            }

            // set the console back up for redrawing the board
            mCursorActive = false;
            cls();
        }

        // prevent memory leaks (very important!!!)
        scanner.close();
    }
}
