#include <curses.h>
#include <iostream>

int main() {
    bool shouldClose = false;
    unsigned long long money = 0;

    while (!shouldClose) {
        char c = getch();
        switch (c) {
            case '=':
                shouldClose = true;
                break;
            case ' ':
                money += 1;
        }

        std::cout << money << "\n";
    }
}